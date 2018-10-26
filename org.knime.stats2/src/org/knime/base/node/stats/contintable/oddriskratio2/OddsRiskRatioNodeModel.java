/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
package org.knime.base.node.stats.contintable.oddriskratio2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.stats.contintable.ContingencyTable;
import org.knime.base.node.viz.crosstable.CrosstabStatisticsCalculator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of OddsRatio.
 *
 *
 * @author Oliver Sampson, University of Konstanz
 */
public class OddsRiskRatioNodeModel extends NodeModel {

    // the logger instance
    private static final NodeLogger LOGGER = NodeLogger.getLogger(OddsRiskRatioNodeModel.class);

    /**
     * the settings key which is used to retrieve and store the settings (from the dialog or from a settings file)
     * (package visibility to be usable from the dialog).
     */
    private static final String CFGKEY_COLUMN_X = "ColumnX";

    private static final String CFGKEY_COLUMN_Y = "ColumnY";

    private static final String CFGKEY_VALUE_X = "ValueX";

    private static final String CFGKEY_VALUE_Y = "ValueY";

    private static final String CFGKEY_CONFIDENCE_LEVEL = "ConfidenceLevel";

    private static final String CFGKEY_LAPLACE_CORRECTION = "LaplaceCorrection";

    /**
     * Port index for the data table.
     */
    public static final int PORT_IN_DATA = 0;

    /**
     * Port index for the results table.
     */
    protected static final int PORT_OUT_RESULTS = 0;

    /**
     * Port index for the contingency table port.
     */
    protected static final int PORT_OUT_CONTINGENCY_TABLE = 1;

    private static final double CONFIDENCE_LEVEL_DEFAULT = 0.95;

    private static final double CONFIDENCE_LEVEL_MIN = 0.0;

    private static final double CONFIDENCE_LEVEL_MAX = 1.0;

    private static final double YATES_CORRECTION = 0.5;

    /**
     * Default value for the Laplace correction.
     */
    protected static final double LAPLACE_CORRECTION_DEFAULT = 1;

    private static final double LAPLACE_CORRECTION_MIN = 0;

    private static final double LAPLACE_CORRECTION_MAX = Double.POSITIVE_INFINITY;

    /**
     * Default stepsize for the confidence interval.
     */
    public static final double CONFIDENCE_LEVEL_STEPSIZE = 0.01;

    private Map<String, List<String>> m_valueMap = new HashMap<>();

    private SettingsModelString m_columnX = createSettingsModelColumnSelectorX();

    private SettingsModelString m_columnY = createSettingsModelColumnSelectorY();

    private SettingsModelString m_valueX = createSettingsModelValueSelectorX();

    private SettingsModelString m_valueY = createSettingsModelValueSelectorY();

    private SettingsModelDoubleBounded m_confidenceLevel = createSettingsModelConfidenceLevel();

    private SettingsModelDoubleBounded m_lapaceCorrection = createSettingsModelLaplaceCorrection();

    /**
     * Constructor for the node model.
     */
    protected OddsRiskRatioNodeModel() {
        super(1, 2);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        LOGGER.info("Starting OddsRatioNodeModel.");

        BufferedDataTable dataTable = (BufferedDataTable)inData[PORT_IN_DATA];

        BufferedDataContainer resultsContainer = exec.createDataContainer(createResultsTableSpec());

        Map<String, Map<String, Integer>> counts =
            BivariateCounts.getCounts(dataTable, m_columnX.getStringValue(), m_columnY.getStringValue());

        String valX = m_valueX.getStringValue();
        String valY = m_valueY.getStringValue();

        if (m_columnX.getStringValue().equals(m_columnY.getStringValue())) {
            throw new InvalidSettingsException("The two columns must be different.");
        }

        if (dataTable.getRowCount() == 0) {
            throw new InvalidSettingsException("ERROR: Input table is empty.");
        }

        int a = 0;
        if (counts.get(valX).containsKey(valY)) {
            a = counts.get(valX).get(valY);
        } else {
            a = 0;
        }
        int b = 0;
        int c = 0;
        int d = 0;

        for (String x : counts.keySet()) {
            for (String y : counts.get(x).keySet()) {
                if (!x.equals(valX) && y.equals(valY)) {
                    b += counts.get(x).get(y);
                } else if (x.equals(valX) && !y.equals(valY)) {
                    c += counts.get(x).get(y);
                } else if (!x.equals(valX) && !y.equals(valY)) {
                    d += counts.get(x).get(y);
                }
            }
        }

        double lpCorr = 0.0;
        if (a == 0 || b == 0 || c == 0 || d == 0) {
            lpCorr = m_lapaceCorrection.getDoubleValue();
        }

        ContingencyTable orr = new ContingencyTable(a, b, c, d, m_confidenceLevel.getDoubleValue(), lpCorr);

        addResultsRow(resultsContainer, orr);
        resultsContainer.close();

        BufferedDataContainer contingencyTableContainer = exec.createDataContainer(createContingencyTableSpec());
        contingencyTableContainer.addRowToTable(getIntegerRow("a", a));
        contingencyTableContainer.addRowToTable(getIntegerRow("b", b));
        contingencyTableContainer.addRowToTable(getIntegerRow("c", c));
        contingencyTableContainer.addRowToTable(getIntegerRow("d", d));

        contingencyTableContainer.close();
        return new BufferedDataTable[]{resultsContainer.getTable(), contingencyTableContainer.getTable()};
    }

    private static void addResultsRow(final BufferedDataContainer resultsContainer, final ContingencyTable orr) {
        final List<DataCell> cells = new ArrayList<>(13);
        cells.add(new DoubleCell(orr.getOddsRatio()));
        cells.add(new DoubleCell(orr.getOddsRatioLowerCI()));
        cells.add(new DoubleCell(orr.getOddsRatioUpperCI()));
        cells.add(new DoubleCell(orr.getRiskRatio()));
        cells.add(new DoubleCell(orr.getRiskRatioLowerCI()));
        cells.add(new DoubleCell(orr.getRiskRatioUpperCI()));
        double[] fisher = orr.getFishersExact();
        cells.add(new DoubleCell(fisher[CrosstabStatisticsCalculator.FISHERS_TWO_TAILED]));
        cells.add(new DoubleCell(fisher[CrosstabStatisticsCalculator.FISHERS_LEFT_TAILED]));
        cells.add(new DoubleCell(fisher[CrosstabStatisticsCalculator.FISHERS_RIGHT_TAILED]));
        cells.add(new DoubleCell(orr.getChiSquared()));
        cells.add(new DoubleCell(orr.getYatesCorrected()));
        cells.add(new DoubleCell(orr.getPearsons()));
        cells.add(new DoubleCell(orr.getCramers()));

        final RowKey key = new RowKey("Results");
        final DataRow outRow = new DefaultRow(key, cells);
        resultsContainer.addRowToTable(outRow);
    }

    private static DefaultRow getIntegerRow(final String rowKey, final int i) {
        return new DefaultRow(new RowKey(rowKey), new DataCell[]{new IntCell(i)});
    }

    private static DataTableSpec createResultsTableSpec() {
        List<DataColumnSpec> outColSpecs = new ArrayList<>(13);
        outColSpecs.add(new DataColumnSpecCreator("Odds Ratio", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("OR Lower CI", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("OR Upper CI", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("Risk Ratio", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("RR Lower CI", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("RR Upper CI", DoubleCell.TYPE).createSpec());
        outColSpecs.add(
            StatsUtil.createDataColumnSpec("Fishers Two Sided", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));
        outColSpecs.add(
            StatsUtil.createDataColumnSpec("Fishers Left Sided", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));
        outColSpecs.add(
            StatsUtil.createDataColumnSpec("Fishers Right Sided", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));
        outColSpecs.add(new DataColumnSpecCreator("ChiSq", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("Yates Corrected", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("Pearsons", DoubleCell.TYPE).createSpec());
        outColSpecs.add(new DataColumnSpecCreator("Cramers V", DoubleCell.TYPE).createSpec());

        return new DataTableSpec(outColSpecs.toArray(new DataColumnSpec[outColSpecs.size()]));
    }

    private static DataTableSpec createContingencyTableSpec() {
        List<DataColumnSpec> outColSpecs = new ArrayList<>();
        outColSpecs.add(new DataColumnSpecCreator("Counts", IntCell.TYPE).createSpec());

        return new DataTableSpec(outColSpecs.toArray(new DataColumnSpec[outColSpecs.size()]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnX.saveSettingsTo(settings);
        m_columnY.saveSettingsTo(settings);
        m_valueX.saveSettingsTo(settings);
        m_valueY.saveSettingsTo(settings);
        m_confidenceLevel.saveSettingsTo(settings);
        m_lapaceCorrection.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnX.loadSettingsFrom(settings);
        m_columnY.loadSettingsFrom(settings);
        m_valueX.loadSettingsFrom(settings);
        m_valueY.loadSettingsFrom(settings);
        m_confidenceLevel.loadSettingsFrom(settings);
        m_lapaceCorrection.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnX.validateSettings(settings);
        m_columnY.validateSettings(settings);
        m_valueX.validateSettings(settings);
        m_valueY.validateSettings(settings);
        m_confidenceLevel.validateSettings(settings);
        m_lapaceCorrection.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    /**
     * @return SettingsModel for the confidence level
     */
    public static SettingsModelDoubleBounded createSettingsModelConfidenceLevel() {
        return new SettingsModelDoubleBounded(CFGKEY_CONFIDENCE_LEVEL, CONFIDENCE_LEVEL_DEFAULT, CONFIDENCE_LEVEL_MIN,
            CONFIDENCE_LEVEL_MAX);
    }

    /**
     * @return SettingsModel for the LaPlace
     */
    public static SettingsModelDoubleBounded createSettingsModelLaplaceCorrection() {
        return new SettingsModelDoubleBounded(CFGKEY_LAPLACE_CORRECTION, LAPLACE_CORRECTION_DEFAULT,
            LAPLACE_CORRECTION_MIN, LAPLACE_CORRECTION_MAX);
    }

    @Override
    protected void reset() {
        // Nothing to do.
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        // check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        DataTableSpec dtSpec = (DataTableSpec)inSpecs[PORT_IN_DATA];

        m_valueMap.clear();

        int count = 0;
        for (DataColumnSpec dcSpec : dtSpec) {
            if (dcSpec.getType().isCompatible(StringValue.class)) {
                count++;
                Set<DataCell> vals = dcSpec.getDomain().getValues();
                if (vals == null) {
                    throw new InvalidSettingsException("The domain for column " + dcSpec.getName() + " is not set.");
                }
                List<String> lVal = new ArrayList<>();
                for (DataCell v : vals) {
                    lVal.add(((StringCell)v).getStringValue());
                }
                Collections.sort(lVal);
                m_valueMap.put(dcSpec.getName(), lVal);
            }
        }

        if (count < 2) {
            throw new InvalidSettingsException("There must be at least two String columns in the input table.");
        }

        return new DataTableSpec[]{createResultsTableSpec(), createContingencyTableSpec()};
    }

    /**
     * @return SettingsModel for column X
     */
    protected static SettingsModelString createSettingsModelColumnSelectorX() {
        return new SettingsModelString(CFGKEY_COLUMN_X, "");
    }

    /**
     * @return SettingsModel for column Y
     */
    protected static SettingsModelString createSettingsModelColumnSelectorY() {
        return new SettingsModelString(CFGKEY_COLUMN_Y, "");
    }

    /**
     * @return the list of values for the selected column X
     */
    protected static SettingsModelString createSettingsModelValueSelectorX() {
        return new SettingsModelString(CFGKEY_VALUE_X, "");
    }

    /**
     * @return the list of values for the selected column Y
     */
    protected static SettingsModelString createSettingsModelValueSelectorY() {
        return new SettingsModelString(CFGKEY_VALUE_Y, "");
    }

}
