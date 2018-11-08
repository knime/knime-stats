package org.knime.base.node.stats.shapirowilk2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of ShapiroWilk2.
 *
 * @author Alexander Fillbrunn
 */
public class ShapiroWilk2NodeModel extends NodeModel {

    static final int PORT_IN_DATA = 0;

    static final String PVALUE_SORT_ASCENDING = "Ascending";

    static final String PVALUE_SORT_DESCENDING = "Descending";

    static final String PVALUE_SORT_NOSORTING = "No sorting";

    private static final double MAX_ROWS = 5000;

    /**
     * The configuration key for the setting that controls if shapiro francia is used for leptokurtic samples.
     */
    static final String SHAPIRO_FRANCIA_CFG = "shapFrancia";

    /**
     * The configuration key for the setting that controls if the result table should be sorted by the p-Value.
     */
    static final String PVALUE_SORT_CFG = "pValueSort";

    /**
     * The configuration key for the setting that controls on which columns the test should be executed on.
     */
    static final String USED_COLS_CFG = "Used columns";

    /**
     * The configuration key for the class column. Only for old nodes, since new ones use the columnfilter.
     */
    static final String TEST_COL_CFG = "testCols";

    /**
     * Constructor for the node model.
     */
    protected ShapiroWilk2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Creates a settings model for the used columns.
     *
     * @return the settings model
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createSettingsModelCols() {
        return new SettingsModelColumnFilter2(USED_COLS_CFG, DoubleValue.class, IntValue.class, LongValue.class);
    }

    /**
     * Creates a settings model for shapiro-francia.
     *
     * @return the settings model
     */
    public static SettingsModelBoolean createShapiroFranciaSettingsModel() {
        return new SettingsModelBoolean(SHAPIRO_FRANCIA_CFG, true);
    }

    /**
     * Creates a settings model for the sort by p-Value.
     *
     * @return the settings model
     */
    public static SettingsModelString createSortByPValueSettingsModel() {
        return new SettingsModelString(PVALUE_SORT_CFG, PVALUE_SORT_NOSORTING);
    }

    private final SettingsModelBoolean m_shapiroFrancia = createShapiroFranciaSettingsModel();

    private final SettingsModelColumnFilter2 m_usedCols = createSettingsModelCols();

    private final SettingsModelString m_sortByPValue = createSortByPValueSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        final BufferedDataTable inTable = (BufferedDataTable)inData[PORT_IN_DATA];
        final DataTableSpec inSpec = inTable.getDataTableSpec();
        final String[] cols = m_usedCols.applyTo(inSpec).getIncludes();

        final BufferedDataContainer dc = exec.createDataContainer(createSpec());
        if (inTable.size() == 0) {
            dc.close();
            return new BufferedDataTable[]{dc.getTable()};
        }

        if (inTable.size() > Integer.MAX_VALUE) {
            throw new InvalidSettingsException("Too many data points to calculate the statistic.");
        } else if (inTable.size() > MAX_ROWS) {
            setWarningMessage("The test might be inaccurate for data sets with more than 5000 data points.");
        }
        int progCnt = 0;

        for (final String col : cols) {
            ShapiroWilkStatistic stat = ShapiroWilkCalculator.calculateSWStatistic(exec, inTable, col,
                m_shapiroFrancia.getBooleanValue(), progCnt / (3.0 * cols.length), progCnt++ / (3.0 * cols.length));

            pushFlowVariableDouble("shapiro-p-value", stat.getPvalue());
            pushFlowVariableDouble("shapiro-statistic", stat.getStatistic());

            dc.addRowToTable(
                new DefaultRow(new RowKey(col), new DoubleCell(stat.getStatistic()), new DoubleCell(stat.getPvalue())));

            progCnt++;
        }

        dc.close();
        if (m_sortByPValue.getStringValue().equals(PVALUE_SORT_ASCENDING)) {
            final BufferedDataTableSorter sorter =
                new BufferedDataTableSorter(dc.getTable(), Arrays.asList("P"), new boolean[]{true});
            return new PortObject[]{sorter.sort(exec.createSubExecutionContext(1))};
        } else if (m_sortByPValue.getStringValue().equals(PVALUE_SORT_DESCENDING)) {
            final BufferedDataTableSorter sorter =
                new BufferedDataTableSorter(dc.getTable(), Arrays.asList("P"), new boolean[]{false});
            return new PortObject[]{sorter.sort(exec.createSubExecutionContext(1))};
        }
        return new PortObject[]{dc.getTable()};
    }

    private static DataTableSpec createSpec() {
        DataColumnSpec measure =
            StatsUtil.createDataColumnSpec("W", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE);
        DataColumnSpec p = StatsUtil.createDataColumnSpec("P", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE);

        return new DataTableSpec(measure, p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[0];

        final FilterResult filterResult = m_usedCols.applyTo(inSpec);
        if (filterResult.getIncludes().length == 0) {
            if (filterResult.getExcludes().length > 0) {
                throw new InvalidSettingsException("Please select at least one test column.");
            } else {
                throw new InvalidSettingsException("There are no numeric columns "
                    + "in the input table. At least one numeric column is needed to perform the test.");
            }
        }
        return new PortObjectSpec[]{createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_usedCols.saveSettingsTo(settings);
        m_shapiroFrancia.saveSettingsTo(settings);
        m_sortByPValue.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_shapiroFrancia.loadSettingsFrom(settings);
        m_usedCols.loadSettingsFrom(settings);
        m_sortByPValue.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_usedCols.validateSettings(settings);
        m_shapiroFrancia.validateSettings(settings);
        m_sortByPValue.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //nothing to do
    }

    static int checkUsedColumns(final SettingsModelColumnFilter2 usedCols, final DataTableSpec tableSpec)
        throws InvalidSettingsException {
        final String[] cols = usedCols.applyTo(tableSpec).getIncludes();
        final int k = cols.length;
        if (k < 1) {
            throw new InvalidSettingsException("Not enough data columns chosen (" + k + "), please choose at least 1.");
        }
        return k;
    }
}
