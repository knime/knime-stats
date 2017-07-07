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
 *   5 Jul 2017 (albrecht): created
 */
package org.knime.base.node.stats.dataexplorer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.knime.base.data.statistics.calculation.Variance;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.web.ValidationError;
import org.knime.js.core.node.AbstractWizardNodeModel;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class DataExplorerNodeModel extends AbstractWizardNodeModel<DataExplorerNodeRepresentation, DataExplorerNodeValue> {

    private DataExplorerConfig m_config;

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
        double subProgress = 1.0;
        if (rep.getStatistics() == null) {
            subProgress = 0.1;
            rep.setStatistics(calculateStatistics((BufferedDataTable)inObjects[0], exec.createSubExecutionContext(0.9)));
        }
        DataExplorerNodeValue val = getViewValue();
        BufferedDataTable result = table;
        if (val.getFilterCols() != null && val.getFilterCols().size() > 0) {
            ColumnRearranger rearranger = new ColumnRearranger(table.getDataTableSpec());
            rearranger.remove(val.getFilterCols().toArray(new String[0]));
            result = exec.createColumnRearrangeTable(table, rearranger, exec.createSubProgress(subProgress));
        }

        return new PortObject[]{result};
    }

    private List<JSONStatisticColumn> calculateStatistics(final BufferedDataTable table, final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        DataTableSpec spec = table.getSpec();
        List<String> doubleCols = new ArrayList<String>();
        List<String> nominalCols = new ArrayList<String>();
        for (DataColumnSpec columnSpec : spec) {
            if (columnSpec.getType().isCompatible(DoubleValue.class)) {
                doubleCols.add(columnSpec.getName());
            } else if (columnSpec.getType().isCompatible(StringValue.class)) {
                nominalCols.add(columnSpec.getName());
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
        //TODO sum
        MissingValue missing = new MissingValue(includeColumns);
        statistics.add(missing);
        SpecialDoubleCells spDouble = new SpecialDoubleCells(includeColumns);
        statistics.add(spDouble);
        Median median = new Median(includeColumns);
        statistics.add(median);
        NominalValue nominal = new NominalValue(100, nominalCols.toArray(new String[0]));
        statistics.add(nominal);
        StatisticCalculator calc = new StatisticCalculator(spec, statistics.toArray(new Statistic[0]));
        calc.evaluate(table, exec);
        List<JSONStatisticColumn> jStats = new ArrayList<JSONStatisticColumn>();
        for (String col : includeColumns) {
            Map<String, Double> colVals = new LinkedHashMap<String, Double>();
            colVals.put(DataExplorerConfig.MIN, ((DoubleValue)minMax.getMin(col)).getDoubleValue());
            colVals.put(DataExplorerConfig.MAX, ((DoubleValue)minMax.getMax(col)).getDoubleValue());
            colVals.put(DataExplorerConfig.MEAN, mean.getResult(col));
            colVals.put(DataExplorerConfig.MEDIAN, ((DoubleValue)median.getMedian(col)).getDoubleValue());
            colVals.put(DataExplorerConfig.STD_DEV, stdDev.getResult(col));
            colVals.put(DataExplorerConfig.VARIANCE, variance.getResult(col));
            colVals.put(DataExplorerConfig.SKEWNESS, skewness.getResult(col));
            colVals.put(DataExplorerConfig.KURTOSIS, kurtosis.getResult(col));
            //TODO sum
            colVals.put(DataExplorerConfig.MISSING, (double)missing.getNumberMissingValues(col));
            colVals.put(DataExplorerConfig.NAN, (double)spDouble.getNumberNaNValues(col));
            colVals.put(DataExplorerConfig.P_INFINITY, (double)spDouble.getNumberPositiveInfiniteValues(col));
            colVals.put(DataExplorerConfig.N_INFINITY, (double)spDouble.getNumberNegativeInfiniteValues(col));
            JSONStatisticColumn jCol = new JSONStatisticColumn();
            jCol.setName(col);
            jCol.setValues(colVals);
            jStats.add(jCol);
        }
        return jStats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performReset() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void useCurrentValueAsDefault() {
        // TODO Auto-generated method stub

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

}
