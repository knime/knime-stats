/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Jul 19, 2016 (winter): created
 */
package org.knime.base.node.stats.testing.wilcoxonsignedrank2;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * @author Patrick Winter, University of Konstanz
 */
public class ColumnStatistics {

    private final String m_column;

    private int m_missing = 0;

    private double m_sum = 0;

    private List<Double> m_values = new ArrayList<Double>();

    private ColumnStatistics(final String column) {
        m_column = column;
    }

    /**
     * @return Specs for the column stats table
     */
    public static DataTableSpec createSpec() {
        DataTableSpecCreator tableSpecCreator = new DataTableSpecCreator();
        tableSpecCreator.addColumns(new DataColumnSpecCreator("Column", StringCell.TYPE).createSpec());
        tableSpecCreator.addColumns(new DataColumnSpecCreator("N", IntCell.TYPE).createSpec());
        tableSpecCreator.addColumns(new DataColumnSpecCreator("Missing Count", IntCell.TYPE).createSpec());
        tableSpecCreator.addColumns(new DataColumnSpecCreator("Mean", DoubleCell.TYPE).createSpec());
        tableSpecCreator.addColumns(new DataColumnSpecCreator("Standard Deviation", DoubleCell.TYPE).createSpec());
        tableSpecCreator.addColumns(new DataColumnSpecCreator("Standard Error Mean", DoubleCell.TYPE).createSpec());
        return tableSpecCreator.createSpec();
    }

    /**
     * Creates a table containing statistics for the given columns.
     *
     * @param table Table containing the columns
     * @param columns The columns to calculate statistics from
     * @param exec Execution context used to create the new table
     * @return Table containing the stats for the given columns
     */
    public static BufferedDataTable createTable(final DataTable table, final List<String> columns,
        final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(createSpec());
        List<Integer> indexs = new ArrayList<Integer>();
        List<ColumnStatistics> stats = new ArrayList<ColumnStatistics>();
        for (String column : columns) {
            indexs.add(table.getDataTableSpec().findColumnIndex(column));
            stats.add(new ColumnStatistics(column));
        }
        for (DataRow row : table) {
            for (int i = 0; i < indexs.size(); i++) {
                DataCell cell = row.getCell(indexs.get(i));
                ColumnStatistics colStats = stats.get(i);
                if (cell.isMissing()) {
                    colStats.addMissing();
                } else {
                    colStats.addValue(((DoubleValue)cell).getDoubleValue());
                }
            }
        }
        int i = 0;
        for (ColumnStatistics colStats : stats) {
            container.addRowToTable(colStats.createRow("Row" + i++));
        }
        container.close();
        return container.getTable();
    }

    private void addValue(final double value) {
        m_sum += value;
        m_values.add(value);
    }

    private void addMissing() {
        m_missing++;
    }

    private DataRow createRow(final String id) {
        return new DefaultRow(id, new StringCell(m_column), new IntCell(m_values.size()), new IntCell(m_missing),
            new DoubleCell(calcMean()), new DoubleCell(calcStdDeviation()), new DoubleCell(calcStdError()));
    }

    private double calcMean() {
        return m_sum / m_values.size();
    }

    private double calcStdDeviation() {
        double mean = calcMean();
        double temp = 0;
        for (Double a : m_values) {
            temp += (a - mean) * (a - mean);
        }
        return Math.sqrt(temp / (m_values.size() - 1));
    }

    private double calcStdError() {
        return calcStdDeviation() / Math.sqrt(m_values.size());
    }

}
