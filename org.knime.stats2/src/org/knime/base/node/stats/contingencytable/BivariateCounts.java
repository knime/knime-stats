package org.knime.base.node.stats.contingencytable;

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

        Set<String> aNames = new HashSet<String>();
        for (DataCell s : sdcA) {
            aNames.add(((StringCell)s).getStringValue());
        }

        Set<DataCell> sdcB = dataTable.getDataTableSpec().getColumnSpec(bIdx).getDomain().getValues();

        Set<String> bNames = new HashSet<String>();
        for (DataCell s : sdcB) {
            bNames.add(((StringCell)s).getStringValue());
        }

        Map<String, Map<String, Integer>> counts = new HashMap<String, Map<String, Integer>>();

        for (String aName : aNames) {
            Map<String, Integer> bMap = new HashMap<String, Integer>(bNames.size());
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
