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
 *   Oct 19, 2017 (annaz): created
 */
package org.knime.base.node.stats.dataexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;


/**
 *
 * @author Anastasia Zhukova, KNIME GmbH, Konstanz, Germany
 */
public abstract class JSHistogram {

    String m_colName;

    int m_colIndex;

    List<?> m_bins;

    int m_maxCount;

    private static final String HISTOGRAMS = "Histograms";

    private static final String HISTOGRAM = "histogram";

    private static final String NUMERIC_COLUMNS = "numeric column indices";

    JSHistogram(final String colName, final int colIndex) {
        this.m_colIndex = colIndex;
        this.m_colName = colName;
    }

    /**
     * @return column index
     */
    public int getColIndex() {
        return m_colIndex;
    }

    /**
     * @return column name
     */
    public String getColumnName() {
        return m_colName;
    }

    /**
     * @return bins
     */
    public List<?> getBins() {
        return m_bins;
    }

    /**
     * @return max value in bins
     */
    public int getMaxCount() {
        return m_maxCount;
    }

    /**
     * @param histograms
     * @param histogramsFile
     * @throws IOException
     */
    public static void saveHistograms(final Map<Integer, ?> histograms, final File histogramsFile)
            throws IOException {
        Config histogramData = new NodeSettings(HISTOGRAMS);
        final FileOutputStream os = new FileOutputStream(histogramsFile);
        final GZIPOutputStream dataOS = new GZIPOutputStream(os);
        List<Integer> colIndices = new ArrayList<Integer>(histograms.keySet());
        Collections.sort(colIndices);
        int[] numericColumnIndices = new int[colIndices.size()];
        for (int i = colIndices.size(); i-- > 0;) {
            numericColumnIndices[i] = colIndices.get(i).intValue();
        }
        histogramData.addIntArray(NUMERIC_COLUMNS, numericColumnIndices);

        for (Integer colIdx : colIndices) {
            Object object = histograms.get(colIdx);
            if (object instanceof JSHistogram) {
                //common part
                if (object instanceof JSNumericHistogram) {

                }

                if (object instanceof JSNominalHistogram) {

                }
            }
         }
        histogramData.saveToXML(dataOS);
    }

//    public static void saveHistogramData(final Map<Integer, ?> histograms, final File histogramsFile)
//            throws IOException {
//            Config histogramData = new NodeSettings(HISTOGRAMS);
//            final FileOutputStream os = new FileOutputStream(histogramsFile);
//            final GZIPOutputStream dataOS = new GZIPOutputStream(os);
//
//            List<Integer> colIndices = new ArrayList<Integer>(histograms.keySet());
//            Collections.sort(colIndices);
//            int[] numericColumnIndices = new int[colIndices.size()];
//            for (int i = colIndices.size(); i-- > 0;) {
//                numericColumnIndices[i] = colIndices.get(i).intValue();
//            }
//            histogramData.addIntArray(NUMERIC_COLUMNS, numericColumnIndices);
//            for (Integer colIdx : colIndices) {
//                Object object = histograms.get(colIdx);
//                if (object instanceof HistogramNumericModel) {
//                    HistogramNumericModel hd = (HistogramNumericModel)object;
//                    assert hd.getColIndex() == colIdx.intValue() : "colIdx: " + colIdx + ", but: " + hd.getColIndex();
//                    Config h = histogramData.addConfig(HISTOGRAM + colIdx);
//                    h.addDouble(MIN, hd.m_min);
//                    h.addDouble(MAX, hd.m_max);
//                    h.addDouble(WIDTH, hd.m_width);
//                    h.addInt(MAX_COUNT, hd.getMaxCount());
//                    h.addInt(ROW_COUNT, hd.getRowCount());
//                    h.addInt(COL_INDEX, hd.getColIndex());
//                    h.addString(COL_NAME, hd.getColName());
//                    double[] minValues = new double[hd.getBins().size()], maxValues = new double[hd.getBins().size()];
//                    int[] counts = new int[hd.getBins().size()];
//                    for (int c = 0; c < hd.getBins().size(); c++) {
//                        HistogramNumericModel.NumericBin bin = (HistogramNumericModel.NumericBin)hd.getBins().get(c);
//                        minValues[c] = bin.getDef().getFirst().doubleValue();
//                        maxValues[c] = bin.getDef().getSecond().doubleValue();
//                        counts[c] = bin.getCount();
//                    }
//                    h.addDoubleArray(BIN_MINS, minValues);
//                    h.addDoubleArray(BIN_MAXES, maxValues);
//                    h.addIntArray(BIN_COUNTS, counts);
//                } else {
//                    throw new IllegalStateException("Illegal argument: " + colIdx + ": " + object.getClass() + "\n   "
//                        + object);
//                }
//            }
//            histogramData.saveToXML(dataOS);
//        }


//    private static Map<Integer, HistogramNumericModel> loadHistogramsPrivate(final File histogramsGz,
//        final Map<Integer, Map<Integer, Set<RowKey>>> numericKeys, final BinNumberSelectionStrategy strategy,
//        final double[] means) throws IOException, InvalidSettingsException {
//        final FileInputStream is = new FileInputStream(histogramsGz);
//        final GZIPInputStream inData = new GZIPInputStream(is);
//        final ConfigRO config = NodeSettings.loadFromXML(inData);
//        Map<Integer, HistogramNumericModel> histograms = new HashMap<Integer, HistogramNumericModel>();
//        ConfigRO hs = config;//.getConfig(HISTOGRAMS);
//        int[] numColumnIndices = config.getIntArray(NUMERIC_COLUMNS);
//        for (int colIdx : numColumnIndices) {
//            Config h = hs.getConfig(HISTOGRAM + colIdx);
//            double min = h.getDouble(MIN), max = h.getDouble(MAX), width = h.getDouble(WIDTH);
//            int maxCount = h.getInt(MAX_COUNT);
//            int rowCount = h.getInt(ROW_COUNT);
//            String colName = h.getString(COL_NAME);
//            double[] binMins = h.getDoubleArray(BIN_MINS), binMaxes = h.getDoubleArray(BIN_MAXES);
//            int[] binCounts = h.getIntArray(BIN_COUNTS);
//            double mean = means[colIdx];
//            HistogramNumericModel histogramData =
//                new HistogramNumericModel(min, max, binMins.length, colIdx, colName, min, max, mean);
//            for (int i = binMins.length; i-- > 0;) {
//                histogramData.getBins().set(i, histogramData.new NumericBin(binMins[i], binMaxes[i]));
//                histogramData.getBins().get(i).setCount(binCounts[i]);
//            }
//            histogramData.setMaxCount(maxCount);
//            histogramData.setRowCount(rowCount);
//            assert Math.abs(histogramData.m_width - width) < 1e-9: "histogram data width: " + histogramData.m_width + " width: " + width;
//            histograms.put(colIdx, histogramData);
//            numericKeys.put(colIdx, new HashMap<Integer, Set<RowKey>>());
//        }
//        return histograms;
//    }

}
