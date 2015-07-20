/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.stats.testing.kruskal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.knime.stats.testing.MissingValueHandler;

/**
 * NodeModel for Wilcoxon-Mann-Whitney-U-Test
 *
 * @author Christian Dietz, University of Konstanz
 */
public class KruskalWallisNodeModel extends NodeModel {

    final NodeLogger LOGGER = NodeLogger.getLogger(KruskalWallisNodeModel.class);

    /**
     * Columnname of the H-Value
     */
    static final String H_VALUE = "H-Value";

    /**
     * Columnname of P-Value
     */
    static final String P_VALUE = "p-value";

    /*
     * SettingsModel create methods
     */
    static SettingsModelString createSettingsModelTestColumn() {
        return new SettingsModelString("test_column", "");
    }

    static SettingsModelString createSettingsModelGroupColumn() {
        return new SettingsModelString("group_column", "");
    }

    static SettingsModelString createSettingsModelMissingValue() {
        return new SettingsModelString("missing_value_handling", "");
    }

    /*
     * SettingsModels used by the NodeModel
     */

    private SettingsModelString m_testColumnModel = createSettingsModelTestColumn();

    private SettingsModelString m_groupColumnModel = createSettingsModelGroupColumn();

    private SettingsModelString m_missingValueHandlerModel = createSettingsModelMissingValue();

    /**
     * Constructor
     */
    public KruskalWallisNodeModel() {
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

        return createOutSpec();
    }

    /**
     * @return the outspec of this node
     */
    private DataTableSpec[] createOutSpec() {
        return new DataTableSpec[]{new DataTableSpec(new String[]{H_VALUE, P_VALUE}, new DataType[]{DoubleCell.TYPE,
            DoubleCell.TYPE})};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final DataTableSpec spec = inData[0].getDataTableSpec();
        int groupingIndex = spec.findColumnIndex(m_groupColumnModel.getStringValue());
        if (groupingIndex == -1) {
            throw new InvalidSettingsException("Grouping column not found. Please reconfigure the node.");
        }

        int testColumnsIndex = spec.findColumnIndex(m_testColumnModel.getStringValue());

        if (testColumnsIndex == -1) {
            throw new InvalidSettingsException("Test column not found. Please reconfigure the node.");
        }

        // FIXME: A more KNIMEish (memory efficient) implementation would only be possible (write a completely own table-based ranking function)
        final double[] data = new double[inData[0].getRowCount()];
        final int[] groupIndices = new int[inData[0].getRowCount()];

        final BufferedDataContainer container = exec.createDataContainer(createOutSpec()[0]);
        if (inData[0].getRowCount() == 0) {
            LOGGER.warn("Number of observations is zero. Empty table will be returned!");
        } else {

            // we check if every group has an entry
            final List<String> groups = new ArrayList<>();

            // collect groups
            int i = 0;
            for (final DataRow row : inData[0]) {

                if (row.getCell(groupingIndex).isMissing()) {
                    LOGGER.warn("Skipping row " + row.getKey().toString()
                        + " as the value in grouping column is missing!");
                    continue;
                }

                final String group = ((StringValue)row.getCell(groupingIndex)).getStringValue();
                groupIndices[i] = groups.indexOf(group);

                if (groupIndices[i] == -1) {
                    groups.add(group);
                    groupIndices[i] = groups.size() - 1;
                }

                final DataCell cell = row.getCell(testColumnsIndex);
                if (cell.isMissing()) {
                    // nulls are handled by selected NanStrategy (apache)
                    data[i] = Double.NaN;
                } else {
                    data[i] = ((DoubleValue)cell).getDoubleValue();
                }

                ++i;
            }

            for (final DataCell domainEntry : inData[0].getSpec().getColumnSpec(groupingIndex).getDomain().getValues()) {
                final String groupToTest = ((StringValue)domainEntry).getStringValue();
                if (!groups.contains(groupToTest)) {
                    LOGGER
                        .warn("Group "
                            + groupToTest
                            + " was found in the domain of the column spec, but no values were present in the table. The group will be ignored. ");
                }
            }

            exec.setMessage("Calculating U values...");
            exec.setProgress(0.3);

            // FIXME Implement more KNIMEish Rank function etc
            final double h =
                KruskalWallisStatistics.calculateHValue(data, groupIndices, groups.size(), MissingValueHandler
                    .getHandlerByName(m_missingValueHandlerModel.getStringValue()).getStrategy());

            exec.setMessage("Calculating p-value...");
            exec.setProgress(0.6);

            final double p = KruskalWallisStatistics.calculatePValue(h, groups.size());

            exec.setMessage("Writing Output...");
            exec.setProgress(0.9);

            container.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DoubleCell(h), new DoubleCell(p)));
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
        m_groupColumnModel.saveSettingsTo(settings);
        m_testColumnModel.saveSettingsTo(settings);
        m_missingValueHandlerModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_groupColumnModel.validateSettings(settings);
        m_testColumnModel.validateSettings(settings);
        m_missingValueHandlerModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
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
