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
 *   01.04.2016 (Alexander): created
 */
package org.knime.base.node.stats.kaplanmeier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.booleancell.FalseCountOperator;
import org.knime.base.data.aggregation.booleancell.TrueCountOperator;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.MemoryGroupByTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Alexander Fillbrunn
 */
public class KaplanMeierCalculator {

    private String m_timeCol;
    private String m_eventCol;
    private String m_groupCol;

    /**
     * Constructs a new instance of the <code>KaplanMeierCalculator</code>.
     * @param timeCol the name of the column containing the time of the event/censoring occurrence
     * @param eventCol the name of the event type column
     * @param groupCol the name of the group column or null
     */
    public KaplanMeierCalculator(final String timeCol, final String eventCol, final String groupCol) {
        m_timeCol = timeCol;
        m_eventCol = eventCol;
        m_groupCol = groupCol;
    }

    /**
     * Calculates a statistics table where the data is grouped and sorted by group
     * and time and the number of events is counted.
     * @param exec the execution context to use
     * @param inTable the input table
     * @return the grouped and sorted table
     * @throws CanceledExecutionException when the execution is cancelled via the ExecutionContext
     */
    public BufferedDataTable calculate(final ExecutionContext exec, final BufferedDataTable inTable)
            throws CanceledExecutionException {
        // Column spec for the boolean column containing the event type
        DataColumnSpec eventColSpec = inTable.getDataTableSpec().getColumnSpec(m_eventCol);

        // The columns to group by. Either only time or time and group are used.
        List<String> groupByColumns = new ArrayList<>();
        groupByColumns.add(m_timeCol);
        if (m_groupCol != null) {
            groupByColumns.add(m_groupCol);
        }

        // Aggregators for counting the number of event occurrences and the number of right-censorings.
        ColumnAggregator[] aggregators = new ColumnAggregator[] {
            new ColumnAggregator(eventColSpec, new TrueCountOperator(GlobalSettings.DEFAULT,
                                    new OperatorColumnSettings(false, eventColSpec))),
            new ColumnAggregator(eventColSpec, new FalseCountOperator(GlobalSettings.DEFAULT,
                                    new OperatorColumnSettings(false, eventColSpec)))
        };

        // Execute the group by
        ExecutionContext groupExec = exec.createSubExecutionContext(0.5);
        MemoryGroupByTable gbTable = new MemoryGroupByTable(groupExec, inTable, groupByColumns, aggregators,
                                            GlobalSettings.DEFAULT, false,
                                            ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME, false);

        BufferedDataTable groupedTable = gbTable.getBufferedTable();

        // Now sort the data by group and time
        ExecutionContext sortExec = exec.createSubExecutionContext(1.0);
        List<String> includeCols = new ArrayList<>();
        if (m_groupCol != null) {
            includeCols.add(m_groupCol);
        }
        includeCols.add(m_timeCol);

        boolean[] order = new boolean[includeCols.size()];
        Arrays.fill(order, true);

        BufferedDataTableSorter sorter = new BufferedDataTableSorter(groupedTable, includeCols, order);
        BufferedDataTable sorted = sorter.sort(sortExec);

        return sorted;
    }

}
