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
 */
package org.knime.base.node.stats.testing.gtest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.inference.GTest;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of ProportionTest.
 *
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class GTestNodeModel extends NodeModel {

    final NodeLogger LOGGER = NodeLogger.getLogger(GTestNodeModel.class);

    static final int PORT_IN_DATA = 0;

    private final SettingsModelDoubleBounded m_alphaModel = createSettingsModelAlpha();

    private final SettingsModelString m_observedModel = createSettingsModelObserved();

    private final SettingsModelString m_observed2Model = createSettingsModelObserved2();

    private final SettingsModelString m_expectedModel = createSettingsModelExpected();

    /**
     * Constructor for the node model.
     */
    GTestNodeModel() {
        super(1, 1);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        final BufferedDataTable data = inData[PORT_IN_DATA];
        if (data.size() <= 1) {
            throw new InvalidSettingsException("Not enough data rows in the table (" + data.size() + ").");
        }

        final BufferedDataContainer outContainer = exec.createDataContainer(createOutputSpec());

        final int obsIdx = data.getDataTableSpec().findColumnIndex(m_observedModel.getStringValue());
        final int obs2Idx = data.getDataTableSpec().findColumnIndex(m_observed2Model.getStringValue());
        final int expIdx = data.getDataTableSpec().findColumnIndex(m_expectedModel.getStringValue());
        if (obsIdx == -1) {
            throw new InvalidSettingsException("Category column not found. Please reconfigure the node.");
        }

        if ((obs2Idx == -1) || (expIdx == -1)) {
            throw new InvalidSettingsException(
                    "Neither second observed column nor expected column found. Please reconfigure the node.");
        }

        /*
         * Convert the relevat columns to arrays in order to feed them to the apache commons tests.
         *
         * See e.g.
         * http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/stat/inference/GTest.html
         * or
         * http://blog.mcbryan.co.uk/2013/07/the-g-test-and-python.html
         * or
         * https://rcompanion.org/rcompanion/b_06.html
         * for further explanation.
         */

        long progCnt = 0;
        int numMissing = 0;

        final ArrayList<Long> obs = new ArrayList<>();
        final ArrayList<Long> obs2 = new ArrayList<>();
        final ArrayList<Double> exp = new ArrayList<>();

        for (final DataRow row : data) {
            final DataCell obsCell = row.getCell(obsIdx);
            final DataCell obs2Cell = row.getCell(obs2Idx);
            final DataCell expCell = row.getCell(expIdx);

            // be sure that its not a missing value
            if (!obsCell.isMissing() && !obs2Cell.isMissing() && !expCell.isMissing()) {
                final long o = ((LongValue)obsCell).getLongValue();
                final long o2 = ((LongValue)obs2Cell).getLongValue();
                final double e = ((DoubleValue)expCell).getDoubleValue();

                if ((o < 0) || (o2 < 0) || (e < 0)) {
                    throw new IllegalArgumentException(
                        "Row '" + row.getKey() + "' contains values < 0 which are not allowed.");
                }

                obs.add(o);
                obs2.add(o2);
                exp.add(e);
            } else {
                numMissing++;
            }
            progCnt++;
            exec.checkCanceled();
            final long progCntFinal = progCnt;
            exec.setProgress((double)progCnt / data.size(), () -> String.format("Processed row %d/%d (\"%s\")",
                progCntFinal, data.size(), row.getKey().toString()));
        }

        if (numMissing > 0) {
            setWarningMessage("Skipped " + numMissing + " row" + (numMissing == 1 ? "" : "s") + " with missing value"
                    + (numMissing == 1 ? "" : "s") + "!");
        }

        // convert to arrays
        final long[] obsArr = ArrayUtils.toPrimitive(obs.toArray(new Long[obs.size()]));
        final long[] obs2Arr = ArrayUtils.toPrimitive(obs2.toArray(new Long[obs2.size()]));
        final double[] expArr = ArrayUtils.toPrimitive(exp.toArray(new Double[exp.size()]));

        final GTest gTest = new GTest();

        final double alpha = m_alphaModel.getDoubleValue();

        // observed - expected stats
        final double gStat = gTest.g(expArr, obsArr);
        final double pval = gTest.gTest(expArr, obsArr);
        final DataCell[] cells =
                new DataCell[]{new DoubleCell(gStat), BooleanCellFactory.create(pval < alpha), new DoubleCell(pval)};
        final RowKey rowKey = new RowKey(
            "G Test ('" + m_observedModel.getStringValue() + "' + '" + m_expectedModel.getStringValue() + "')");
        outContainer.addRowToTable(new DefaultRow(rowKey, cells));

        // observed - observed2 stats
        final double gStatSet = gTest.gTestDataSetsComparison(obsArr, obs2Arr);
        final double pvalSet = gTest.gDataSetsComparison(obsArr, obs2Arr);
        final DataCell[] cellsSet =
                new DataCell[]{new DoubleCell(gStatSet), BooleanCellFactory.create(pvalSet < alpha), new DoubleCell(pvalSet)};
        final RowKey rowKeySet = new RowKey(
            "G Test ('" + m_observedModel.getStringValue() + "' + '" + m_observed2Model.getStringValue() + "')");
        outContainer.addRowToTable(new DefaultRow(rowKeySet, cellsSet));

        outContainer.close();
        return new BufferedDataTable[]{outContainer.getTable()};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // in case a flow variable is chosen badly
        final double alpha = m_alphaModel.getDoubleValue();
        if ((alpha < 0) || (alpha > 1)) {
            throw new InvalidSettingsException("Please chose an alpha in range (0, 1) (is: " + alpha + ").");
        }

        // Auto detect columns
        if (!inSpecs[PORT_IN_DATA].containsName(m_observedModel.getStringValue())) {
            // Choose the first Int column that has two or more distinct values.
            for (final DataColumnSpec col : inSpecs[PORT_IN_DATA]) {
                if (col.getType().isCompatible(LongValue.class)) {
                    m_observedModel.setStringValue(col.getName());
                    LOGGER.warn("Auto detection suggested column \"" + col.getName() + "\" as the observed column.");
                    break;
                }
            }
        }

        if (!inSpecs[PORT_IN_DATA].containsName(m_observed2Model.getStringValue())) {
            // Choose the first Int column that has two or more distinct values.
            for (final DataColumnSpec col : inSpecs[PORT_IN_DATA]) {
                if (col.getType().isCompatible(LongValue.class)
                        && !col.getName().equals(m_observedModel.getStringValue())) {
                    m_observed2Model.setStringValue(col.getName());
                    LOGGER.warn(
                        "Auto detection suggested column \"" + col.getName() + "\" as the second observed column.");
                    break;
                }
            }
        }

        if (!inSpecs[PORT_IN_DATA].containsName(m_expectedModel.getStringValue())) {
            // Choose the first Int column that has two or more distinct values.
            for (final DataColumnSpec col : inSpecs[PORT_IN_DATA]) {
                if (col.getType().isCompatible(DoubleValue.class)
                        && !col.getName().equals(m_observedModel.getStringValue())
                        && !col.getName().equals(m_observed2Model.getStringValue())) {
                    m_expectedModel.setStringValue(col.getName());
                    LOGGER.warn(
                        "Auto detection suggested column \"" + col.getName() + "\" as the expected column.");
                    break;
                }
            }
        }

        return new DataTableSpec[]{createOutputSpec()};
    }

    private static DataTableSpec createOutputSpec() {
        final DataColumnSpec[] allColSpecs =
                new DataColumnSpec[]{new DataColumnSpecCreator("G-Value", DoubleCell.TYPE).createSpec(),
                    new DataColumnSpecCreator("Reject H0", BooleanCell.TYPE).createSpec(),
                    StatsUtil.createDataColumnSpec("p-Value", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE)};

        return new DataTableSpec(allColSpecs);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_alphaModel.saveSettingsTo(settings);
        m_observedModel.saveSettingsTo(settings);
        m_observed2Model.saveSettingsTo(settings);
        m_expectedModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alphaModel.loadSettingsFrom(settings);
        m_observedModel.loadSettingsFrom(settings);
        m_observed2Model.loadSettingsFrom(settings);
        m_expectedModel.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alphaModel.validateSettings(settings);
        m_observedModel.validateSettings(settings);
        m_observed2Model.validateSettings(settings);
        m_expectedModel.validateSettings(settings);
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

    /**
     * Creates a settings model for the significance level alpha.
     *
     * @return the settings model
     */
    static SettingsModelDoubleBounded createSettingsModelAlpha() {
        return new SettingsModelDoubleBounded("Alpha", 0.05, 0, 1);
    }

    /**
     * Creates a settings model for the observed column.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelObserved() {
        return new SettingsModelString("Observed", "");
    }

    /**
     * Creates a settings model for second the observed column.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelObserved2() {
        return new SettingsModelString("Observed 2", "");
    }

    /**
     * Creates a settings model for second the observed column.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelExpected() {
        return new SettingsModelString("Expected", "");
    }
}
