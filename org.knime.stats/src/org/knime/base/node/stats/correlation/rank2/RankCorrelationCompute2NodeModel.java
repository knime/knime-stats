/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */
package org.knime.base.node.stats.correlation.rank2;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.node.preproc.correlation.CorrelationUtils;
import org.knime.base.node.preproc.correlation.CorrelationUtils.CorrelationResult;
import org.knime.base.node.preproc.correlation.compute2.PValueAlternative;
import org.knime.base.node.preproc.correlation.pmcc.PMCCPortObjectAndSpec;
import org.knime.base.util.HalfDoubleMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer.FullPrecisionRendererFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * @author Bernd Wiswedel, University of Konstanz
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class RankCorrelationCompute2NodeModel extends NodeModel implements BufferedDataTableHolder {

    /** Full precision renderer for double values */
    private static final String FULL_PRECISION_RENDERER = new FullPrecisionRendererFactory().getDescription();

    /** Progress of the first step */
    private static final double PROG_STEP1 = 0.48;

    /** Progress of the second step */
    private static final double PROG_STEP2 = 0.48;

    /** Progress of the last step */
    private static final double PROG_FINISH = 1 - PROG_STEP1 - PROG_STEP2;

    /** the configuration key for using spearmans Rhu. */
    static final String CFG_SPEARMAN = "Spearmans Rho";

    /** the configuration key for using Kendalls Tau A. */
    static final String CFG_KENDALLA = "Kendalls Tau A";

    /** the configuration key for using Kendalls Tau B. */
    static final String CFG_KENDALLB = "Kendalls Tau B";

    /** the configuration key for using Goodman and Kruskals Gamma. */
    static final String CFG_KRUSKALAL = "Goodman and Kruskal's Gamma";

    /**
     * @return the list of all correlation types
     */
    static List<String> getCorrelationTypes() {
        LinkedList<String> ret = new LinkedList<>();
        ret.add(CFG_SPEARMAN);
        ret.add(CFG_KENDALLA);
        ret.add(CFG_KENDALLB);
        ret.add(CFG_KRUSKALAL);
        return ret;
    }

    /**
     * @return A new settings object for filtering columns.
     */
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("include-list");
    }

    /**
     * @return a new model
     */
    static SettingsModelString createTypeModel() {
        return new SettingsModelString("corr-measure", CFG_SPEARMAN);
    }

    /**
     * Factory method to create the string model for the p-value alternative.
     *
     * @return A new model.
     */
    static SettingsModelString createPValAlternativeModel() {
        return new SettingsModelString("pvalAlternative", PValueAlternative.TWO_SIDED.name());
    }

    private SettingsModelColumnFilter2 m_columnFilterModel;

    private SettingsModelString m_corrType = createTypeModel();

    private final SettingsModelString m_pValAlternativeModel = createPValAlternativeModel();

    private BufferedDataTable m_correlationTable;

    /**
     * One input, one output.
     */
    RankCorrelationCompute2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, PMCCPortObjectAndSpec.TYPE, BufferedDataTable.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec in = (DataTableSpec)inSpecs[0];
        final String[] includes;
        if (m_columnFilterModel == null) {
            m_columnFilterModel = createColumnFilterModel();
            // auto-configure, no previous configuration
            m_columnFilterModel.loadDefaults(in);
            includes = m_columnFilterModel.applyTo(in).getIncludes();
            setWarningMessage("Auto configuration: Using all suitable columns (in total " + includes.length + ")");
        } else {
            FilterResult applyTo = m_columnFilterModel.applyTo(in);
            includes = applyTo.getIncludes();
        }
        if (includes.length == 0) {
            throw new InvalidSettingsException("No columns selected");
        }
        final DataTableSpec tableSpecs;
        if (CFG_SPEARMAN.equals(m_corrType.getStringValue())) {
            tableSpecs = CorrelationUtils.createCorrelationOutputTableSpec();
        } else {
            tableSpecs = createCorrelationOutputTableSpec();
        }
        return new PortObjectSpec[]{tableSpecs, new PMCCPortObjectAndSpec(includes), null};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable in = (BufferedDataTable)inData[0];
        final DataTableSpec inSpec = in.getDataTableSpec();

        // Filter included columns
        final ColumnRearranger filteredTableRearranger = new ColumnRearranger(inSpec);
        final String[] includeNames = m_columnFilterModel.applyTo(inSpec).getIncludes();
        filteredTableRearranger.keepOnly(includeNames);
        final BufferedDataTable filteredTable =
            exec.createColumnRearrangeTable(in, filteredTableRearranger, exec.createSilentSubExecutionContext(0.0));

        // Filter missing values
        final BufferedDataTable noMissTable = filterMissings(filteredTable, exec);
        if (noMissTable.size() < filteredTable.size()) {
            setWarningMessage(
                "Rows containing missing values are filtered. Please resolve them" + " with the Missing Value node.");
        }

        // Calculate ranking
        final SortedCorrelationComputer2 calculator = new SortedCorrelationComputer2();
        exec.setMessage("Generate ranking");
        ExecutionContext execStep1 = exec.createSubExecutionContext(PROG_STEP1);
        calculator.calculateRank(noMissTable, execStep1);
        execStep1.setProgress(1.0);

        // Calculate correlation
        exec.setMessage("Calculating correlation values");
        final ExecutionContext execStep2 = exec.createSubExecutionContext(PROG_STEP2);
        final BufferedDataTable out;
        final HalfDoubleMatrix correlationMatrix;
        if (m_corrType.getStringValue().equals(CFG_SPEARMAN)) {
            final CorrelationResult correlationResult =
                calculator.calculateSpearman(execStep2, selectedPValAlternative());
            correlationMatrix = correlationResult.getCorrelationMatrix();

            // Assemble output
            exec.setMessage("Assembling output");
            final ExecutionContext execFinish1 = exec.createSubExecutionContext(PROG_FINISH / 2);
            out = CorrelationUtils.createCorrelationOutputTable(correlationResult, includeNames, execFinish1);
        } else {
            correlationMatrix = calculator.calculateKendallInMemory(m_corrType.getStringValue(), execStep2);

            // Assemble output
            exec.setMessage("Assembling output");
            final ExecutionContext execFinish1 = exec.createSubExecutionContext(PROG_FINISH / 2);
            out = createCorrelationOutputTable(correlationMatrix, includeNames, execFinish1);
        }
        // Correlation matrix
        final ExecutionContext execFinish2 = exec.createSubExecutionContext(PROG_FINISH / 2);
        final PMCCPortObjectAndSpec pmccModel = new PMCCPortObjectAndSpec(includeNames, correlationMatrix);
        m_correlationTable = pmccModel.createCorrelationMatrix(execFinish2);
        execStep2.setProgress(1.0);

        // Empty table handling
        if (in.size() == 0) {
            // TODO check if the warning is correct
            setWarningMessage("Empty input table! Generating missing values as correlation values.");
        }
        return new PortObject[]{out, pmccModel, calculator.getRankTable()};
    }

    /** Correlation table without p-values and degrees of freedom */
    private static BufferedDataTable createCorrelationOutputTable(final HalfDoubleMatrix corrMatrix,
        final String[] includeNames, final ExecutionContext exec) throws CanceledExecutionException {
        final DataTableSpec outSpec = createCorrelationOutputTableSpec();
        final BufferedDataContainer dataContainer = exec.createDataContainer(outSpec);

        // Fill the table
        int numInc = includeNames.length;
        int rowIndex = 0;
        final double rowCount = numInc * (numInc - 1) / 2;
        for (int i = 0; i < numInc; i++) {
            for (int j = i + 1; j < numInc; j++) {
                final DoubleCell corr = new DoubleCell(corrMatrix.get(i, j));
                final RowKey rowKey = new RowKey(getRowKey(includeNames[i], includeNames[j]));
                final DefaultRow row = new DefaultRow(rowKey, corr);
                exec.checkCanceled();
                dataContainer.addRowToTable(row);
                exec.setProgress(++rowIndex / rowCount);
            }
        }
        dataContainer.close();
        return dataContainer.getTable();
    }

    /** Correlation table specs without p-values and degrees of freedom */
    private static DataTableSpec createCorrelationOutputTableSpec() {
        // Column spec creators
        final DataColumnSpecCreator corrColSpecCreator =
            new DataColumnSpecCreator(CorrelationUtils.CORRELATION_VALUE_COL_NAME, DoubleCell.TYPE);

        // Set the full precision renderer for the p value column
        final DataColumnProperties fullPrecRendererProps = new DataColumnProperties(
            Collections.singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, FULL_PRECISION_RENDERER));
        corrColSpecCreator.setProperties(fullPrecRendererProps);

        return new DataTableSpec(corrColSpecCreator.createSpec());
    }

    private static String getRowKey(final String columnNameA, final String columnNameB) {
        return columnNameA + "_" + columnNameB;
    }

    private PValueAlternative selectedPValAlternative() {
        return PValueAlternative.valueOf(m_pValAlternativeModel.getStringValue());
    }

    /**
     * @param filteredTable a Buffered Data Table.
     * @param exec The execution context
     * @return the table without any rows containing missing values.
     */
    private static BufferedDataTable filterMissings(final BufferedDataTable filteredTable,
        final ExecutionContext exec) {
        BufferedDataContainer tab = exec.createDataContainer(filteredTable.getDataTableSpec());
        for (DataRow row : filteredTable) {
            boolean includeRow = true;
            // check row for missingvalues
            for (DataCell cell : row) {
                if (cell.isMissing()) {
                    includeRow = false;
                    break;
                }
            }
            if (includeRow) {
                tab.addRowToTable(row);
            }
        }
        tab.close();
        return tab.getTable();
    }

    /**
     * Getter for correlation table to display. <code>null</code> if not executed.
     *
     * @return the correlationTable
     */
    public DataTable getCorrelationTable() {
        return m_correlationTable;
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_columnFilterModel != null) {
            m_columnFilterModel.saveSettingsTo(settings);
        }
        m_corrType.saveSettingsTo(settings);
        m_pValAlternativeModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        createColumnFilterModel().validateSettings(settings);
        m_corrType.validateSettings(settings);
        m_pValAlternativeModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_columnFilterModel == null) {
            m_columnFilterModel = createColumnFilterModel();
        }
        m_columnFilterModel.loadSettingsFrom(settings);
        m_corrType.loadSettingsFrom(settings);
        m_pValAlternativeModel.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_correlationTable};
    }

    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_correlationTable = tables[0];
    }

}
