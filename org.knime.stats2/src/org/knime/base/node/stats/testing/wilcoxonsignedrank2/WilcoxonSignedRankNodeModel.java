/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.base.node.stats.testing.wilcoxonsignedrank2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Patrick Winter, University of Konstanz
 */
class WilcoxonSignedRankNodeModel extends NodeModel {

    private WilcoxonSignedRankNodeConfig m_config = new WilcoxonSignedRankNodeConfig();

    protected WilcoxonSignedRankNodeModel() {
        super(1, 3);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inTables, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable table = inTables[0];
        BufferedDataContainer container = exec.createDataContainer(createSpec());
        BufferedDataContainer rankStatsContainer = exec.createDataContainer(RankStatistics.createSpec());
        String[] firstColumns = m_config.getFirstColumns();
        String[] secondColumns = m_config.getSecondColumns();
        Set<String> uniqueColumns = new TreeSet<String>();
        uniqueColumns.addAll(Arrays.asList(firstColumns));
        uniqueColumns.addAll(Arrays.asList(secondColumns));
        BufferedDataTable statsTable = ColumnStatistics.createTable(table, new ArrayList<String>(uniqueColumns), exec);
        for (int i = 0; i < firstColumns.length; i++) {
            int column1Index = table.getDataTableSpec().findColumnIndex(firstColumns[i]);
            int column2Index = table.getDataTableSpec().findColumnIndex(secondColumns[i]);
            WilcoxonSignedRankTest test = new WilcoxonSignedRankTest();
            for (DataRow row : table) {
                DataCell cell1 = row.getCell(column1Index);
                DataCell cell2 = row.getCell(column2Index);
                if (!cell1.isMissing() && !cell2.isMissing()) {
                    double value1 = ((DoubleValue)cell1).getDoubleValue();
                    double value2 = ((DoubleValue)cell2).getDoubleValue();
                    test.addSample(value1, value2);
                }
            }
            test.execute();
            pushFlowVariableDouble("w (plus)", test.getWPlus());
            pushFlowVariableDouble("w (minus)", test.getWMinus());
            pushFlowVariableDouble("z-score (left)", test.getLeftZScore());
            pushFlowVariableDouble("z-score (right)", test.getRightZScore());
            pushFlowVariableDouble("p-value (one tailed)", test.getOneTailedPValue());
            pushFlowVariableDouble("p-value (two tailed)", test.getTwoTailedPValue());
            DataCell[] cells = new DataCell[8];
            cells[0] = new StringCell(firstColumns[i]);
            cells[1] = new StringCell(secondColumns[i]);
            cells[2] = new DoubleCell(test.getWPlus());
            cells[3] = new DoubleCell(test.getWMinus());
            cells[4] = new DoubleCell(test.getLeftZScore());
            cells[5] = new DoubleCell(test.getRightZScore());
            cells[6] = new DoubleCell(test.getOneTailedPValue());
            cells[7] = new DoubleCell(test.getTwoTailedPValue());
            container.addRowToTable(new DefaultRow("Row" + i, cells));
            test.addRankStatisticsToTable(i+1, rankStatsContainer);
        }
        container.close();
        rankStatsContainer.close();
        return new BufferedDataTable[]{container.getTable(), statsTable, rankStatsContainer.getTable()};
    }

    private DataTableSpec createSpec() {
        DataColumnSpec[] colSpecs = new DataColumnSpec[8];
        colSpecs[0] = new DataColumnSpecCreator("Left column", StringCell.TYPE).createSpec();
        colSpecs[1] = new DataColumnSpecCreator("Right column", StringCell.TYPE).createSpec();
        colSpecs[2] = new DataColumnSpecCreator("w (plus)", DoubleCell.TYPE).createSpec();
        colSpecs[3] = new DataColumnSpecCreator("w (minus)", DoubleCell.TYPE).createSpec();
        colSpecs[4] = new DataColumnSpecCreator("z-score (left)", DoubleCell.TYPE).createSpec();
        colSpecs[5] = new DataColumnSpecCreator("z-score (right)", DoubleCell.TYPE).createSpec();
        colSpecs[6] = new DataColumnSpecCreator("p-value (one tailed)", DoubleCell.TYPE).createSpec();
        colSpecs[7] = new DataColumnSpecCreator("p-value (two tailed)", DoubleCell.TYPE).createSpec();
        return new DataTableSpecCreator().addColumns(colSpecs).createSpec();
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        String[] firstColumns = m_config.getFirstColumns();
        String[] secondColumns = m_config.getSecondColumns();
        if (firstColumns.length < 1) {
            throw new InvalidSettingsException("At least one pair of columns has to be selected");
        }
        for (int i = 0; i < firstColumns.length; i++) {
            String column1 = firstColumns[i];
            String column2 = secondColumns[i];
            if (!inSpec.containsName(column1)) {
                throw new InvalidSettingsException("Selected column '" + column1 + "' does not exist");
            }
            if (!inSpec.getColumnSpec(column1).getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column '" + column1 + "' is not numerical");
            }
            if (!inSpec.containsName(column2)) {
                throw new InvalidSettingsException("Selected column '" + column2 + "' does not exist");
            }
            if (!inSpec.getColumnSpec(column2).getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column '" + column2 + "' is not numerical");
            }
        }
        return new DataTableSpec[]{createSpec(), ColumnStatistics.createSpec(), RankStatistics.createSpec()};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.save(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        WilcoxonSignedRankNodeConfig config = new WilcoxonSignedRankNodeConfig();
        config.load(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        WilcoxonSignedRankNodeConfig config = new WilcoxonSignedRankNodeConfig();
        config.load(settings);
        m_config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

}
