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
package org.knime.base.node.stats.contintable.oddriskratio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;

/**
 * @author Oliver Sampson, University of Konstanz
 *
 */
public class BivariateCounts {
    /**
     * @param dataTable the table from which to count the values
     * @param colA the first of two columns whose values are to be counted
     * @param colB the second of two columns whose values are to be counted
     * @return the counts of each combination of values for two columns with string values
     */
    public static Map<String, Map<String, Integer>> getCounts(final BufferedDataTable dataTable, final String colA,
        final String colB) {

        int aIdx = dataTable.getDataTableSpec().findColumnIndex(colA);
        int bIdx = dataTable.getDataTableSpec().findColumnIndex(colB);

        Set<DataCell> sdcA = dataTable.getDataTableSpec().getColumnSpec(aIdx).getDomain().getValues();

        Set<String> aNames = new HashSet<>();
        for (DataCell s : sdcA) {
            aNames.add(((StringCell)s).getStringValue());
        }

        Set<DataCell> sdcB = dataTable.getDataTableSpec().getColumnSpec(bIdx).getDomain().getValues();

        Set<String> bNames = new HashSet<>();
        for (DataCell s : sdcB) {
            bNames.add(((StringCell)s).getStringValue());
        }

        Map<String, Map<String, Integer>> counts = new HashMap<>();

        for (String aName : aNames) {
            Map<String, Integer> bMap = new HashMap<>(bNames.size());
            for (String bName : bNames) {
                bMap.put(bName, 0);
            }
            counts.put(aName, bMap);
        }

        // Get the counts
        for (DataRow row : dataTable) {
            if (!(row.getCell(aIdx).isMissing() || row.getCell(bIdx).isMissing())) {
                String aVal = ((StringCell)row.getCell(aIdx)).getStringValue();
                String bVal = ((StringCell)row.getCell(bIdx)).getStringValue();
                counts.get(aVal).put(bVal, counts.get(aVal).get(bVal) + 1);
            }
        }

        return counts;
    }

    /**
     * Constructor.
     */
    protected BivariateCounts() {

    }
}
