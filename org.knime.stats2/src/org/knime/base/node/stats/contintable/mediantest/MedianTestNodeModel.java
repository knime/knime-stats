/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   June 14, 2016 (sampson): created
 */
package org.knime.base.node.stats.contintable.mediantest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.knime.base.node.stats.contintable.ContingencyTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of MedianTest.
 *
 *
 * @author Oliver Sampson, University of Konstanz
 */
public class MedianTestNodeModel extends NodeModel {

    // the logger instance
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MedianTestNodeModel.class);

    private static final String CFGKEY_COLUMN1 = "Col1";

    private static final String CFGKEY_COLUMN2 = "Col2";

    private static final String CFGKEY_LAPLACE_CORRECTION = "LaplaceCorrection";

    private static final String CFGKEY_CONFIDENCE_LEVEL = "ConfidenceLevel";

    private static final String CFGKEY_IGNORE_MISSING_VALUES = "IgnoreMissingValues";

    static final int PORT_IN_DATA = 0;

    /**
     * Default value for the Laplace correction.
     */
    protected static final double LAPLACE_CORRECTION_DEFAULT = 1;

    private static final double LAPLACE_CORRECTION_MIN = 0;

    private static final double LAPLACE_CORRECTION_MAX = Double.POSITIVE_INFINITY;

    static final double CONFIDENCE_LEVEL_DEFAULT = 0.95;

    private static final double CONFIDENCE_LEVEL_MIN = 0.0;

    private static final double CONFIDENCE_LEVEL_MAX = 1.0;

    private final SettingsModelString m_col1 = createSettingsModelCol1();

    private final SettingsModelString m_col2 = createSettingsModelCol2();

    private SettingsModelDoubleBounded m_lapaceCorrection = createSettingsModelLaplaceCorrection();

    private SettingsModelDoubleBounded m_confidenceLevel = createSettingsModelConfidenceLevel();

    private PriorityQueue<Double> m_lower = null;

    private PriorityQueue<Double> m_upper = null;

    private SettingsModelBoolean m_ignoreMissingValues = createSettingsModelIgnoreMissingValues();

    /**
     * Constructor for the node model.
     */
    protected MedianTestNodeModel() {
        super(1, 1);
    }

    @SuppressWarnings("resource")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        LOGGER.info("Begin MedianTestNodeModel");

        BufferedDataTable data = inData[PORT_IN_DATA];
        int col1Ind = data.getDataTableSpec().findColumnIndex(m_col1.getStringValue());
        int col2Ind = data.getDataTableSpec().findColumnIndex(m_col2.getStringValue());

        DataTableSpec outputSpec = createOutputSpec();

        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        if (data.size() != 0) {

            m_lower = new PriorityQueue<>((int)data.size() / 2, new BiggerIsBetter());
            m_upper = new PriorityQueue<>((int)data.size() / 2);

            Iterator<DataRow> it = data.iterator();

            while (it.hasNext()) {
                DataRow row = it.next();

                addToQueues(row.getCell(col1Ind));

                addToQueues(row.getCell(col2Ind));

                // Re-balance the queues if necessary
                if (m_lower.size() > m_upper.size()) {
                    m_upper.add((m_lower.poll()));
                }
                if (m_upper.size() > m_lower.size()) {
                    m_lower.add((m_upper.poll()));
                }

            }

            double median = (m_upper.poll() + m_lower.poll()) / 2;

            m_upper.clear();
            m_lower.clear();

            int a = 0; // Col1 above
            int b = 0; // Col2 above
            int c = 0; // Col1 below
            int d = 0; // Col2 below

            for (DataRow row : data) {
                if (!row.getCell(col1Ind).isMissing()) {
                    if (((DoubleValue)row.getCell(col1Ind)).getDoubleValue() > median) {
                        a++;
                    } else {
                        c++;
                    }
                }

                if (!row.getCell(col2Ind).isMissing()) {
                    if (((DoubleValue)row.getCell(col2Ind)).getDoubleValue() > median) {
                        b++;
                    } else {
                        d++;
                    }
                }
            }

            ContingencyTable ct = new ContingencyTable(a, b, c, d, m_confidenceLevel.getDoubleValue(),
                m_lapaceCorrection.getDoubleValue());

            // check if the execution monitor was canceled
            exec.checkCanceled();

            RowKey key = new RowKey(m_col1.getStringValue() + " + " + m_col2.getStringValue());

            List<DataCell> cells = new ArrayList<>(4);
            double x2 = ct.getYatesCorrected();
            double x2critical = ContingencyTable.getCriticalChiSquare(m_confidenceLevel.getDoubleValue());
            cells.add(BooleanCellFactory.create(x2 >= x2critical));
            cells.add(new DoubleCell(x2));
            cells.add(new DoubleCell(x2critical));
            cells.add(new DoubleCell(ct.getChiSquaredPValue()));

            DataRow row = new DefaultRow(key, cells);
            container.addRowToTable(row);
        }
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    private void addToQueues(final DataCell cell) throws InvalidSettingsException {
        if (cell.isMissing()) {
            if (!m_ignoreMissingValues.getBooleanValue()) {
                throw new InvalidSettingsException("Not set to handle missing values.");
            } // else ignore
        } else {
            double val = ((DoubleValue)cell).getDoubleValue();
            if (m_lower.isEmpty()) { // the very first addition
                m_lower.add(val);
            } else { // all other cases
                if (val < m_lower.peek()) {
                    m_lower.add(val);
                } else {
                    m_upper.add(val);
                }
            }
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = inSpecs[0];

        final Iterator<DataColumnSpec> colIter = inSpec.iterator();
        while ((m_col1.getStringValue() == null || m_col2.getStringValue() == null) && colIter.hasNext()) {
            final DataColumnSpec colSpec = colIter.next();
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                if (m_col1.getStringValue() == null) {
                    m_col1.setStringValue(colSpec.getName());
                } else if (m_col2.getStringValue() == null) {
                    m_col2.setStringValue(colSpec.getName());
                }
            }
        }

        if (m_col1.getStringValue() == null) {
            throw new InvalidSettingsException(
                    "Not enough numerical data columns available, please provide a data table with at least one.");
        } else if (m_col2.getStringValue() == null) {
            m_col2.setStringValue(m_col1.getStringValue());
        }

        if (!inSpec.containsName(m_col1.getStringValue())
            || !inSpec.getColumnSpec(m_col1.getStringValue()).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException(
                "Test column " + m_col1.getStringValue() + " not found or incompatible");
        } else if (!inSpec.containsName(m_col2.getStringValue())
            || !inSpec.getColumnSpec(m_col2.getStringValue()).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException(
                "Test column " + m_col2.getStringValue() + " not found or incompatible");
        }
        if (m_col1.getStringValue().equals(m_col2.getStringValue())) {
            setWarningMessage("The two columns selected to test are identical but should be different.");
        }
        return new DataTableSpec[]{createOutputSpec()};
    }

    private static DataTableSpec createOutputSpec() {
        List<DataColumnSpec> allColSpecs = new ArrayList<>(4);
        allColSpecs.add(new DataColumnSpecCreator("Reject H0", BooleanCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator("ChiSq Value", DoubleCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator("Critical ChiSq Value", DoubleCell.TYPE).createSpec());
        allColSpecs.add(StatsUtil.createDataColumnSpec("p-Value", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));

        return new DataTableSpec(allColSpecs.toArray(new DataColumnSpec[0]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_col1.saveSettingsTo(settings);
        m_col2.saveSettingsTo(settings);
        m_lapaceCorrection.saveSettingsTo(settings);
        m_confidenceLevel.saveSettingsTo(settings);
        m_ignoreMissingValues.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_col1.loadSettingsFrom(settings);
        m_col2.loadSettingsFrom(settings);
        m_lapaceCorrection.loadSettingsFrom(settings);
        m_confidenceLevel.loadSettingsFrom(settings);
        m_ignoreMissingValues.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_col1.validateSettings(settings);
        m_col2.validateSettings(settings);
        m_lapaceCorrection.validateSettings(settings);
        m_confidenceLevel.validateSettings(settings);
        m_ignoreMissingValues.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    @Override
    protected void reset() {
        // Nothing to do.
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    static SettingsModelString createSettingsModelCol2() {
        return new SettingsModelString(CFGKEY_COLUMN2, null);
    }

    static SettingsModelString createSettingsModelCol1() {
        return new SettingsModelString(CFGKEY_COLUMN1, null);
    }

    static SettingsModelDoubleBounded createSettingsModelLaplaceCorrection() {
        return new SettingsModelDoubleBounded(CFGKEY_LAPLACE_CORRECTION, LAPLACE_CORRECTION_DEFAULT,
            LAPLACE_CORRECTION_MIN, LAPLACE_CORRECTION_MAX);
    }

    static SettingsModelDoubleBounded createSettingsModelConfidenceLevel() {
        return new SettingsModelDoubleBounded(CFGKEY_CONFIDENCE_LEVEL, CONFIDENCE_LEVEL_DEFAULT, CONFIDENCE_LEVEL_MIN,
            CONFIDENCE_LEVEL_MAX);
    }

    static SettingsModelBoolean createSettingsModelIgnoreMissingValues() {
        return new SettingsModelBoolean(CFGKEY_IGNORE_MISSING_VALUES, true);
    }

    private class BiggerIsBetter implements Comparator<Double> {

        @Override
        public int compare(final Double o1, final Double o2) {

            return o1 > o2 ? -1 : o1 < o2 ? 1 : 0;
        }

    }
}
