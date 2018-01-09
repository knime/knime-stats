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
 *   Jun 11, 2015 (dietzc): created
 */
package org.knime.base.node.stats.testing.wilcoxonmannwhitney;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.knime.base.node.stats.testing.wilcoxonmannwhitney.WilcoxonMannWhitneyStatistics.MannWhitneyUTestResult;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * NodeModel for Wilcoxon-Mann-Whitney-U-Test
 *
 * @author Christian Dietz, University of Konstanz
 */
public class WilcoxonMannWhitneyNodeModel extends NodeModel {

    final NodeLogger LOGGER = NodeLogger.getLogger(WilcoxonMannWhitneyNodeModel.class);

    /**
     * Columnname of the U-Value
     */
    static final String U_MAX_VALUE = "Maximum U-Value";

    /**
     * Columnname of the U-Value
     */
    static final String U_MIN_VALUE = "Minimum U-Value";

    /**
     * Columnname of P-Value
     */
    static final String P_VALUE = "p-value";

    /**
     * Columnname of mean of group A
     */
    static final String MEAN_A = "Mean Rank Group (A)";

    /**
     * Columnname of mean of group B
     */
    static final String MEAN_B = "Mean Rank Group (B)";

    /**
     * Columnname of median of group A
     */
    static final String MEDIAN_A = "Median Rank Group (A)";

    /**
     * Columnname of median of group B
     */
    static final String MEDIAN_B = "Median Rank Group (B)";

    /*
     * SettingsModel create methods
     */
    static SettingsModelString createSettingsModelTestColumn() {
        return new SettingsModelString("test_column", "");
    }

    static SettingsModelString createSettingsModelGroupColumn() {
        return new SettingsModelString("group_column", "");
    }

    static SettingsModelString createSettingsModelGroupOne() {
        return new SettingsModelString("group_a", "");
    }

    static SettingsModelString createSettingsModelGroupTwo() {
        return new SettingsModelString("group_b", "");
    }

    static SettingsModelString createSettingsModelMissingValue() {
        return new SettingsModelString("missing_value_handling", "");
    }

    /*
     * SettingsModels used by the NodeModel
     */

    private SettingsModelString m_testColumnModel = createSettingsModelTestColumn();

    private SettingsModelString m_groupColumnModel = createSettingsModelGroupColumn();

    private SettingsModelString m_groupAModel = createSettingsModelGroupOne();

    private SettingsModelString m_groupBModel = createSettingsModelGroupTwo();

    private SettingsModelString m_missingValueHandlerModel = createSettingsModelMissingValue();

    /**
     * Constructor
     */
    public WilcoxonMannWhitneyNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        if (!inSpecs[0].containsName(m_testColumnModel.getStringValue())) {
            throw new InvalidSettingsException("Please define a test column.");
        }

        if (!inSpecs[0].containsName(m_groupColumnModel.getStringValue())) {
            throw new InvalidSettingsException("Please define a grouping column.");
        }

        if (m_groupAModel.getStringValue().equals("")) {
            throw new InvalidSettingsException("Value of group one is not set.");
        }

        if (m_groupBModel.getStringValue().equals("")) {
            throw new InvalidSettingsException("Value of group two is not set.");
        }

        return createOutSpec();
    }

    /**
     * @return the outspec of this node
     */
    private DataTableSpec[] createOutSpec() {
        return new DataTableSpec[]{new DataTableSpec(new String[]{U_MIN_VALUE, U_MAX_VALUE, P_VALUE, MEAN_A, MEAN_B,
            MEDIAN_A, MEDIAN_B}, new DataType[]{DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE,
            DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE})};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        if (inData[0].getRowCount() == 0) {
            throw new InvalidSettingsException("Input table is empty!");
        }

        final DataTableSpec spec = inData[0].getDataTableSpec();
        int groupingIndex = spec.findColumnIndex(m_groupColumnModel.getStringValue());
        if (groupingIndex == -1) {
            throw new InvalidSettingsException("Grouping column not found. Please reconfigure the node.");
        }

        int testColumnsIndex = spec.findColumnIndex(m_testColumnModel.getStringValue());

        if (testColumnsIndex == -1) {
            throw new InvalidSettingsException("Test column not found. Please reconfigure the node.");
        }

        // FIXME: A more KNIMEish (memory efficient) implementation would only be possible if we had a RandomAccess on the KNIME Table.
        final List<DoubleValue> a = new ArrayList<>();
        final List<DoubleValue> b = new ArrayList<>();

        // TODO: Can we generalize this to DataCell? How to select values of DataCell in Dialog?
        final String groupA = m_groupAModel.getStringValue();
        final String groupB = m_groupBModel.getStringValue();

        // collect groups
        for (final DataRow row : inData[0]) {

            if (row.getCell(groupingIndex).isMissing()) {
                LOGGER.warn("Skipping row " + row.getKey().toString() + " as the value in grouping column is missing!");
                continue;
            }

            final String group = ((StringValue)row.getCell(groupingIndex)).getStringValue();
            final List<DoubleValue> currentList;
            if (groupA.equalsIgnoreCase(group)) {
                currentList = a;
            } else if (groupB.equalsIgnoreCase(group)) {
                currentList = b;
            } else {
                continue;
            }

            final DataCell cell = row.getCell(testColumnsIndex);
            if (cell.isMissing()) {
                // nulls are handled by selected NanStrategy (apache)
                currentList.add(new DoubleCell(Double.NaN));
            } else {
                currentList.add((DoubleValue)cell);
            }
        }

        final BufferedDataContainer container = exec.createDataContainer(createOutSpec()[0]);
        if (a.size() == 0 || b.size() == 0) {
            throw new IllegalStateException("Number of observations is zero for one or both of the selected groups!");
        } else {
            if ((a.size() + b.size()) < 20) {
                LOGGER
                    .warn("Number of observations is small. Approximating U values using standard normal distribution, which is only suitable for n > 20. ");
            }

            exec.setMessage("Calculating U values...");
            exec.setProgress(0.3);

            // Statistics (which required copying the data into another format.
            // FIXME Implement more KNIMEish Rank function etc
            final MannWhitneyUTestResult mannWhitneyU =
                WilcoxonMannWhitneyStatistics.mannWhitneyU(a, b, new NaturalRanking(MissingValueHandler
                    .getHandlerByName(m_missingValueHandlerModel.getStringValue()).getStrategy()));

            exec.setMessage("Calculating p-value...");
            exec.setProgress(0.6);

            final double p =
                WilcoxonMannWhitneyStatistics.calculateAsymptoticPValue(mannWhitneyU.uMin, a.size(), b.size());

            exec.setMessage("Writing Output...");
            exec.setProgress(0.9);

            container.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DoubleCell(mannWhitneyU.uMin),
                new DoubleCell(mannWhitneyU.uMax), new DoubleCell(p), new DoubleCell(mannWhitneyU.meanA),
                new DoubleCell(mannWhitneyU.meanB), new DoubleCell(mannWhitneyU.medianA), new DoubleCell(
                    mannWhitneyU.medianB)));

        }
        // Create Output
        exec.setProgress(1.0);
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Nothing to do here...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Nothing to do here...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_groupAModel.saveSettingsTo(settings);
        m_groupBModel.saveSettingsTo(settings);
        m_groupColumnModel.saveSettingsTo(settings);
        m_testColumnModel.saveSettingsTo(settings);
        m_missingValueHandlerModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_groupAModel.validateSettings(settings);
        m_groupBModel.validateSettings(settings);
        m_groupColumnModel.validateSettings(settings);
        m_testColumnModel.validateSettings(settings);
        m_missingValueHandlerModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_groupAModel.loadSettingsFrom(settings);
        m_groupBModel.loadSettingsFrom(settings);
        m_groupColumnModel.loadSettingsFrom(settings);
        m_testColumnModel.loadSettingsFrom(settings);
        m_missingValueHandlerModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do here...
    }

}
