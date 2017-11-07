/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   5 Jul 2017 (albrecht): created
 */
package org.knime.base.node.stats.dataexplorer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.statistics.HistogramColumn;
import org.knime.base.data.statistics.HistogramColumn.BinNumberSelectionStrategy;
import org.knime.base.data.statistics.HistogramModel;
import org.knime.base.data.statistics.Statistic;
import org.knime.base.data.statistics.StatisticCalculator;
import org.knime.base.data.statistics.calculation.Kurtosis;
import org.knime.base.data.statistics.calculation.Mean;
import org.knime.base.data.statistics.calculation.Median;
import org.knime.base.data.statistics.calculation.MinMax;
import org.knime.base.data.statistics.calculation.MissingValue;
import org.knime.base.data.statistics.calculation.NominalValue;
import org.knime.base.data.statistics.calculation.Skewness;
import org.knime.base.data.statistics.calculation.SpecialDoubleCells;
import org.knime.base.data.statistics.calculation.StandardDeviation;
import org.knime.base.data.statistics.calculation.Sum;
import org.knime.base.data.statistics.calculation.Variance;
import org.knime.base.data.statistics.calculation.ZeroNumber;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.web.ValidationError;
import org.knime.js.core.JSONDataTable;
import org.knime.js.core.JSONDataTable.JSONDataTableRow;
import org.knime.js.core.JSONDataTableSpec;
import org.knime.js.core.JSONDataTableSpec.JSTypes;
import org.knime.js.core.node.AbstractWizardNodeModel;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @author Anastasia Zhukova, KNIME GmbH, Konstanz, Germany
 */
public class DataExplorerNodeModel extends AbstractWizardNodeModel<DataExplorerNodeRepresentation, DataExplorerNodeValue> {

    private DataExplorerConfig m_config;

    private List<HistogramModel<?>> m_javaNumericHistograms;
    private List<HistogramModel<?>> m_javaNominalHistograms;


    /**
     * @param viewName
     */
    protected DataExplorerNodeModel(final String viewName) {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE}, viewName);
        m_config = new DataExplorerConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataExplorerNodeRepresentation createEmptyViewRepresentation() {
        return new DataExplorerNodeRepresentation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataExplorerNodeValue createEmptyViewValue() {
        return new DataExplorerNodeValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputSpec = (DataTableSpec)inSpecs[0];

        PortObjectSpec[] out = new PortObjectSpec[]{inputSpec};
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavascriptObjectID() {
        return "org_knime_base_node_stats_dataexplorer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHideInWizard() {
        return m_config.getHideInWizard();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHideInWizard(final boolean hide) {
        m_config.setHideInWizard(hide);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationError validateViewValue(final DataExplorerNodeValue viewContent) {
        /*always valid */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveCurrentValue(final NodeSettingsWO content) { /* not used */ }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] performExecute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        BufferedDataTable table = (BufferedDataTable)inObjects[0];
        DataExplorerNodeRepresentation rep = getViewRepresentation();
        double subProgress = 0.8;
        ColumnRearranger rearranger = new ColumnRearranger(table.getDataTableSpec());
        if (rep.getStatistics() == null) {
            subProgress = 0.1;
            rep.setStatistics(calculateStatistics((BufferedDataTable)inObjects[0], exec.createSubExecutionContext(0.8)));
            rep.setNominal(calculateNominal((BufferedDataTable)inObjects[0], exec.createSubExecutionContext(0.2)));
            rep.setDataPreview(calculatePreview((BufferedDataTable)inObjects[0]));
            copyConfigToRepresentation();
        }
        DataExplorerNodeValue val = getViewValue();
        BufferedDataTable result = table;
        String[] filterCols = val.getSelection();
        if (filterCols != null && filterCols.length > 0) {
            rearranger.remove(filterCols);
            result = exec.createColumnRearrangeTable(table, rearranger, exec.createSubProgress(subProgress));
        }

        return new PortObject[]{result};
    }

    /**
     * @param bufferedDataTable
     * @return
     */
    private JSONDataTable calculateNominal(final BufferedDataTable table, final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        DataTableSpec spec = table.getSpec();
        List<String> nominalCols = new ArrayList<String>();
        for (DataColumnSpec columnSpec : spec) {
            if (columnSpec.getType().isCompatible(StringValue.class)) {
                nominalCols.add(columnSpec.getName());
            }
        }
        String[] includeColumns = nominalCols.toArray(new String[0]);
        List<Statistic> statistics = new ArrayList<Statistic>();
        NominalValue nominal = new NominalValue(m_config.getMaxNominalValues(), nominalCols.toArray(new String[0]));
        statistics.add(nominal);
        MissingValue missing = new MissingValue(includeColumns);
        statistics.add(missing);
        StatisticCalculator calc = new StatisticCalculator(spec, statistics.toArray(new Statistic[0]));
        calc.evaluate(table, exec.createSubExecutionContext(0.5));

        List<JSNominalHistogram> jsHistograms = new ArrayList<JSNominalHistogram>();
        m_javaNominalHistograms = new ArrayList<HistogramModel<?>>();
        JSONDataTableRow[] rows = new JSONDataTableRow[includeColumns.length];

        for (int i = 0; i < includeColumns.length; i++) {
            String col = includeColumns[i];

            List<Object> rowValues = new ArrayList<Object>();
            rowValues.add(missing.getNumberMissingValues(col));

            Map<DataValue, Integer> nomValue = nominal.getNominalValues(i);
            rowValues.add(nomValue.size());

            m_javaNominalHistograms.add(calculateNominalHistograms(nomValue, i, col));
            jsHistograms.add(new JSNominalHistogram(col, i, nomValue));

            rows[i] = new JSONDataTableRow(col, rowValues.toArray(new Object[0]));
        }
        JSONDataTableSpec jSpec = createStatsJSONSpecNominal(includeColumns.length);
        JSONDataTable jTable = new JSONDataTable();
        jTable.setSpec(jSpec);
        jTable.setRows(rows);
        jTable.setId("nominal");

        getViewRepresentation().setJsNominalHistograms(jsHistograms);
        return jTable;
    }

    private JSONDataTable calculateStatistics(final BufferedDataTable table, final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        DataTableSpec spec = table.getSpec();
        List<String> doubleCols = new ArrayList<String>();
        for (DataColumnSpec columnSpec : spec) {
            if (columnSpec.getType().isCompatible(DoubleValue.class)) {
                doubleCols.add(columnSpec.getName());
            }
        }
        String[] includeColumns = doubleCols.toArray(new String[0]);
        List<Statistic> statistics = new ArrayList<Statistic>();
        MinMax minMax = new MinMax(includeColumns);
        statistics.add(minMax);
        Mean mean = new Mean(includeColumns);
        statistics.add(mean);
        StandardDeviation stdDev = new StandardDeviation(includeColumns);
        statistics.add(stdDev);
        Variance variance = new Variance(includeColumns);
        statistics.add(variance);
        Skewness skewness = new Skewness(includeColumns);
        statistics.add(skewness);
        Kurtosis kurtosis = new Kurtosis(includeColumns);
        statistics.add(kurtosis);
        Sum sum = new Sum(includeColumns);
        statistics.add(sum);
        ZeroNumber zeros = new ZeroNumber(includeColumns);
        statistics.add(zeros);
        MissingValue missing = new MissingValue(includeColumns);
        statistics.add(missing);
        SpecialDoubleCells spDouble = new SpecialDoubleCells(includeColumns);
        statistics.add(spDouble);
        Median median = new Median(includeColumns);
        if (m_config.getShowMedian()) {
            statistics.add(median);
        }

        StatisticCalculator calc = new StatisticCalculator(spec, statistics.toArray(new Statistic[0]));
        calc.evaluate(table, exec.createSubExecutionContext(0.5));

        JSONDataTableRow[] rows = new JSONDataTableRow[includeColumns.length];
        JSONDataTableSpec jSpec = createStatsJSONSpec(includeColumns.length);
        JSONDataTable jTable = new JSONDataTable();
        jTable.setSpec(jSpec);

        for (int i = 0; i < includeColumns.length; i++) {
            String col = includeColumns[i];
            List<Object> rowValues = new ArrayList<Object>();
            DataCell min = minMax.getMin(col);
            DataCell max = minMax.getMax(col);
            rowValues.add(min.isMissing() ? null : ((DoubleValue)min).getDoubleValue());
            rowValues.add(max.isMissing() ? null : ((DoubleValue)max).getDoubleValue());
            Double dMean = mean.getResult(col);
            rowValues.add(dMean.isNaN() ? null : dMean);
            if (m_config.getShowMedian()) {
                DataCell med = median.getMedian(col);
                rowValues.add(med.isMissing() ? null : ((DoubleValue)med).getDoubleValue());
            }
            Double dDev = stdDev.getResult(col);
            rowValues.add(dDev.isNaN() ? null : dDev);
            Double dVar = variance.getResult(col);
            rowValues.add(dVar.isNaN() ? null : dVar);
            Double dSkew = skewness.getResult(col);
            rowValues.add(dSkew.isNaN() ? null : dSkew);
            Double dKurt = kurtosis.getResult(col);
            rowValues.add(dKurt.isNaN() ? null : dKurt);
            Double dSum = sum.getResult(col);
            rowValues.add(dSum.isNaN() ? null : dSum);
            rowValues.add(zeros.getNumberZeroValues(col));
            rowValues.add(missing.getNumberMissingValues(col));
            rowValues.add(spDouble.getNumberNaNValues(col));
            rowValues.add(spDouble.getNumberPositiveInfiniteValues(col));
            rowValues.add(spDouble.getNumberNegativeInfiniteValues(col));
            rows[i] = new JSONDataTableRow(col, rowValues.toArray(new Object[0]));
        }

        jTable.setRows(rows);
        jTable.setId("numeric");

        Map<Integer, ? extends HistogramModel<?>> javaHistograms = calculateNumericHistograms(table, exec.createSubExecutionContext(0.5), minMax, mean, includeColumns);
        m_javaNumericHistograms = new ArrayList<HistogramModel<?>>();
        m_javaNumericHistograms.addAll(javaHistograms.values());

        List<JSNumericHistogram> jsHistograms = new ArrayList<JSNumericHistogram>();
        for (int i = 0; i < includeColumns.length; i++) {
            JSNumericHistogram histTest = new JSNumericHistogram(includeColumns[i], i, table,  ((DoubleValue)minMax.getMin(includeColumns[i])).getDoubleValue(),
                ((DoubleValue)minMax.getMax(includeColumns[i])).getDoubleValue(),  mean.getResult(includeColumns[i]));
            jsHistograms.add(histTest);
        }
        getViewRepresentation().setJsNumericHistograms(jsHistograms);
        return jTable;
    }

    private JSONDataTable calculatePreview (final BufferedDataTable table) {
        JSONDataTable jTable = new JSONDataTable();
        JSONDataTableSpec jSpec = new JSONDataTableSpec(table.getDataTableSpec(), m_config.getDisplayRowNumber());
        JSONDataTableRow[] rows = new JSONDataTableRow[m_config.getDisplayRowNumber()];
        int numCol = table.getDataTableSpec().getNumColumns();
        int i = 0;
        for (DataRow row : table) {
            if (i == m_config.getDisplayRowNumber()) {
                break;
            }
            List<Object> rowValues = new ArrayList<Object>();
            for (int j = 0; j < numCol; j++) {
                DataCell cell = row.getCell(j);
                if (cell instanceof DoubleValue) {
                    rowValues.add(((DoubleValue)cell).getDoubleValue());
                } else if (cell instanceof StringValue) {
                    rowValues.add(((StringValue)cell).getStringValue());
                } else {
                    rowValues.add(null);
                }
            }
            rows[i] = new JSONDataTableRow(row.getKey().getString(), rowValues.toArray(new Object[0]));
            i++;
        }
        jTable.setSpec(jSpec);
        jTable.setRows(rows);
        jTable.setId("preview");
        return jTable;
    }

    private JSONDataTableSpec createStatsJSONSpecNominal(final int numColumns) {
        String knimeInt = ((ExtensibleUtilityFactory)LongValue.UTILITY).getName();
        String knimeString = ((ExtensibleUtilityFactory)StringValue.UTILITY).getName();
        JSONDataTableSpec spec = new JSONDataTableSpec();
        List<String> colNames = new ArrayList<String>();
        List<String> knimeTypes = new ArrayList<String>();
        colNames.add(DataExplorerConfig.MISSING);
        knimeTypes.add(knimeInt);
        colNames.add(DataExplorerConfig.UNIQUE_NOMINAL);
        knimeTypes.add(knimeInt);
        colNames.add(DataExplorerConfig.TOP_FREQ_VAL);
        knimeTypes.add(knimeString);
        colNames.add(DataExplorerConfig.TOP_INFREQ_VAL);
        knimeTypes.add(knimeString);
        colNames.add(DataExplorerConfig.ALL_NOMINAL_VAL);
        knimeTypes.add(knimeString);

        JSTypes[] colTypes = new JSTypes[knimeTypes.size()];
        for (int i = 0; i < knimeTypes.size(); i++) {
            if (knimeTypes.get(i) == knimeInt) {
                colTypes[i] = JSTypes.NUMBER;
            } else {
                colTypes[i] = JSTypes.STRING;
            }
        }
        spec.setNumColumns(colNames.size());
        spec.setColNames(colNames.toArray(new String[0]));
        spec.setColTypes(colTypes);
        spec.setKnimeTypes(knimeTypes.toArray(new String[0]));
        spec.setNumRows(numColumns);
        return spec;
    }

    private JSONDataTableSpec createStatsJSONSpec(final int numColumns) {
        String knimeDouble = ((ExtensibleUtilityFactory)DoubleValue.UTILITY).getName();
        String knimeInt = ((ExtensibleUtilityFactory)LongValue.UTILITY).getName();
        JSONDataTableSpec spec = new JSONDataTableSpec();
        List<String> colNames = new ArrayList<String>();
        List<String> knimeTypes = new ArrayList<String>();
        colNames.add(DataExplorerConfig.MIN);
        knimeTypes.add(knimeDouble);
        colNames.add(DataExplorerConfig.MAX);
        knimeTypes.add(knimeDouble);
        colNames.add(DataExplorerConfig.MEAN);
        knimeTypes.add(knimeDouble);
        if (m_config.getShowMedian()) {
            colNames.add(DataExplorerConfig.MEDIAN);
            knimeTypes.add(knimeDouble);
        }
        colNames.add(DataExplorerConfig.STD_DEV);
        knimeTypes.add(knimeDouble);
        colNames.add(DataExplorerConfig.VARIANCE);
        knimeTypes.add(knimeDouble);
        colNames.add(DataExplorerConfig.SKEWNESS);
        knimeTypes.add(knimeDouble);
        colNames.add(DataExplorerConfig.KURTOSIS);
        knimeTypes.add(knimeDouble);
        colNames.add(DataExplorerConfig.SUM);
        knimeTypes.add(knimeDouble);
        colNames.add(DataExplorerConfig.ZEROS);
        knimeTypes.add(knimeInt);
        colNames.add(DataExplorerConfig.MISSING);
        knimeTypes.add(knimeInt);
        colNames.add(DataExplorerConfig.NAN);
        knimeTypes.add(knimeInt);
        colNames.add(DataExplorerConfig.P_INFINITY);
        knimeTypes.add(knimeInt);
        colNames.add(DataExplorerConfig.N_INFINITY);
        knimeTypes.add(knimeInt);

        JSTypes[] colTypes = new JSTypes[colNames.size()];
        Arrays.fill(colTypes, JSTypes.NUMBER);

        spec.setNumColumns(colNames.size());
        spec.setColNames(colNames.toArray(new String[0]));
        spec.setColTypes(colTypes);
        spec.setKnimeTypes(knimeTypes.toArray(new String[0]));
        spec.setNumRows(numColumns);
        return spec;
    }

   private HistogramModel<?> calculateNominalHistograms(final Map<? extends DataValue, Integer> counts, final int colIndex,
       final String colName) {
       HistogramColumn hCol = HistogramColumn.getDefaultInstance();
       return hCol.fromNominalModel(counts, colIndex, colName);
   }

   private Map<Integer, ? extends HistogramModel<?>> calculateNumericHistograms(final BufferedDataTable table, final ExecutionContext exec, final MinMax minMax, final Mean mean, final String[] includeColumns) {
       HistogramColumn hCol = HistogramColumn.getDefaultInstance()
               .withNumberOfBins(10)
               .withBinSelectionStrategy(BinNumberSelectionStrategy.DecimalRange);
       int noCols = includeColumns.length;
       double[] mins = new double[noCols];
       double[] maxs = new double[noCols];
       double[] means = new double[noCols];
       for (int i = 0; i < noCols; i++) {
           DataCell min = minMax.getMin(includeColumns[i]);
           mins[i] = min.isMissing() ? Double.NaN : ((DoubleValue)min).getDoubleValue();
           DataCell max = minMax.getMax(includeColumns[i]);
           maxs[i] = max.isMissing() ? Double.NaN : ((DoubleValue)max).getDoubleValue();
           means[i] = mean.getResult(includeColumns[i]);
       }
       return hCol.histograms(table, new HiLiteHandler(), mins, maxs, means, includeColumns);
   }

    private void copyConfigToRepresentation() {
        synchronized(getLock()) {
            DataExplorerNodeRepresentation viewRepresentation = getViewRepresentation();
            viewRepresentation.setEnablePaging(m_config.getEnablePaging());
            viewRepresentation.setInitialPageSize(m_config.getInitialPageSize());
            viewRepresentation.setEnablePageSizeChange(m_config.getEnablePageSizeChange());
            viewRepresentation.setAllowedPageSizes(m_config.getAllowedPageSizes());
            viewRepresentation.setPageSizeShowAll(m_config.getPageSizeShowAll());
            viewRepresentation.setEnableJumpToPage(m_config.getEnableJumpToPage());
            viewRepresentation.setDisplayRowIds(m_config.getDisplayRowIds());
            viewRepresentation.setDisplayColumnHeaders(m_config.getDisplayColumnHeaders());
            viewRepresentation.setFixedHeaders(m_config.getFixedHeaders());
            viewRepresentation.setTitle(m_config.getTitle());
            viewRepresentation.setSubtitle(m_config.getSubtitle());
            viewRepresentation.setEnableSelection(m_config.getEnableSelection());
            viewRepresentation.setEnableSearching(m_config.getEnableSearching());
            viewRepresentation.setEnableSorting(m_config.getEnableSorting());
            viewRepresentation.setEnableClearSortButton(m_config.getEnableClearSortButton());
            viewRepresentation.setEnableGlobalNumberFormat(m_config.getEnableGlobalNumberFormat());
            viewRepresentation.setGlobalNumberFormatDecimals(m_config.getGlobalNumberFormatDecimals());
            viewRepresentation.setDisplayFullscreenButton(m_config.getDisplayFullscreenButton());
            viewRepresentation.setDisplayMissingValueAsQuestionMark(m_config.getDisplayMissingValueAsQuestionMark());
            viewRepresentation.setDisplayRowNumber(m_config.getDisplayRowNumber());
            viewRepresentation.setEnableFreqValDisplay(m_config.getEnableFreqValDisplay());
            viewRepresentation.setFreqValues(m_config.getFreqValues());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performReset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void useCurrentValueAsDefault() {
        DataExplorerNodeValue value = getViewValue();
        m_config.setInitialPageSize(value.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        (new DataExplorerConfig()).loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.saveInternals(nodeInternDir, exec);
        File hNumFile = new File(nodeInternDir, "numericHistograms.xml.gz");
        File hNomFile = new File(nodeInternDir, "nominalHistograms.xml.gz");

        if (m_javaNumericHistograms != null) {
            Map<Integer, HistogramModel<?>> hNumMap = new HashMap<Integer, HistogramModel<?>>();
            for (HistogramModel<?> histogramModel : m_javaNumericHistograms) {
                hNumMap.put(histogramModel.getColIndex(), histogramModel);
            }
            HistogramColumn.saveHistogramData(hNumMap, hNumFile);
        }

        if (m_javaNominalHistograms != null) {
            Map<Integer, HistogramModel<?>> hNomMap = new HashMap<Integer, HistogramModel<?>>();
            for (HistogramModel<?> histogramModel : m_javaNominalHistograms) {
                hNomMap.put(histogramModel.getColIndex(), histogramModel);
            }
            HistogramColumn.saveNominalHistogramData(hNomMap, hNomFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.loadInternals(nodeInternDir, exec);
        File hNumFile = new File(nodeInternDir, "numericHistograms.xml.gz");
        File hNomFile = new File(nodeInternDir, "nominalHistograms.xml.gz");
        DataExplorerNodeRepresentation rep = getViewRepresentation();
        if (hNumFile.exists()) {
            double[] means = rep.getMeans();
            if (means == null) {
                throw new IOException("Means could not be retrieved from representation.");
            }
            try {
                Map<Integer, ? extends HistogramModel<?>> numHistograms = HistogramColumn.loadHistograms(hNumFile,
                    new HashMap<Integer, Map<Integer, Set<RowKey>>>(), BinNumberSelectionStrategy.DecimalRange, means);
                List<HistogramModel<?>> hList = new ArrayList<HistogramModel<?>>();
                hList.addAll(numHistograms.values());
                //rep.setJavaNumericHistograms(hList);

                List<JSNumericHistogram> jsNumHist = new ArrayList<JSNumericHistogram>();
                for (int i = 0; i < hList.size(); i++) {
                    jsNumHist.add(new JSNumericHistogram(hList.get(i)));
                }
                rep.setJsNumericHistograms(jsNumHist);

            } catch (InvalidSettingsException e) {
                throw new IOException(e);
            }
        }
        if (hNomFile.exists()) {
            try {
                Map<Integer, ? extends HistogramModel<?>> nomHistograms =HistogramColumn.loadNominalHistograms(hNomFile, rep.getNominalValuesSize());

                List<JSNominalHistogram> jsNomHist = new ArrayList<JSNominalHistogram>();
                for (HistogramModel<?> hist : nomHistograms.values()) {
                    jsNomHist.add(new JSNominalHistogram(hist));
                }
                rep.setJsNominalHistograms(jsNomHist);

            } catch (InvalidSettingsException e) {
                throw new IOException(e);
            }
        }

    }

}
