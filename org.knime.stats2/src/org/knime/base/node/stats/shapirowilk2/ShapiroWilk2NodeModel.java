package org.knime.base.node.stats.shapirowilk2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.knime.base.data.statistics.StatisticCalculator;
import org.knime.base.data.statistics.calculation.Kurtosis;
import org.knime.base.data.statistics.calculation.Mean;
import org.knime.base.data.statistics.calculation.MissingValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
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

/**
 * This is the model implementation of PMMLToJavascriptCompiler.
 *
 *
 * @author Alexander Fillbrunn
 */
public class ShapiroWilk2NodeModel extends NodeModel {

    static final int PORT_IN_DATA = 0;

    private static final double SHAPIRO_FRANCIA_KURTOSIS = 3;

    static final String PVALUE_SORT_ASCENDING = "Ascending";

    static final String PVALUE_SORT_DESCENDING = "Descending";

    static final String PVALUE_SORT_NOSORTING = "No sorting";

    private static final double MIN_ROWS = 3;

    private static final double MAX_ROWS = 5000;

    // constants for the p-value calculation
    private static final double[] C3 = {0.544, -0.39978, 0.025054, -6.714e-4},
            C4 = {1.3822, -0.77857, 0.062767, -0.0020322}, C5 = {-1.5861, -0.31082, -0.083751, 0.0038915},
            C6 = {-0.4803, -0.082676f, 0.0030302}, G = {-2.273, 0.459};

    private static final NormalDistribution DISTRIBUTION = new NormalDistribution(0, 1);

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
            final int cellIndex = inSpec.findColumnIndex(col);
            if (inSpec.getColumnSpec(cellIndex).getDomain() == null) {
                throw new InvalidSettingsException("The test column " + col + " does not have an associated domain. "
                    + "Please use a Domain Calculator node first.");
            }

            final ExecutionContext sortContext = exec.createSubExecutionContext(progCnt / (3.0 * cols.length));
            progCnt++;

            final BufferedDataTableSorter sorter =
                new BufferedDataTableSorter(inTable, Arrays.asList(col), new boolean[]{true});
            final BufferedDataTable sorted = sorter.sort(sortContext);

            final Kurtosis kurtosisStat = new Kurtosis(col);
            final Mean meanStat = new Mean(col);
            final MissingValue missingStat = new MissingValue(col);

            // Calculate mean and kurtosis
            final StatisticCalculator statCalc = new StatisticCalculator(inSpec, kurtosisStat, meanStat, missingStat);
            statCalc.evaluate(inTable, exec.createSubExecutionContext(progCnt / (3.0 * cols.length)));
            progCnt++;

            final double mean = meanStat.getResult(col);
            final double kurtosis = kurtosisStat.getResult(col);

            final int n = (int)inTable.size() - missingStat.getNumberMissingValues(col);
            if (n < MIN_ROWS) {
                throw new InvalidSettingsException("Not enough data points to calculate the statistic.");
            }

            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += Math.pow(getM(i + 1, n), 2);
            }
            final double sqrtSumInv = 1.0 / Math.sqrt(sum);

            double w;

            String warning = null;

            // Shapiro-Francia test is better for leptokurtic samples
            if (kurtosis > SHAPIRO_FRANCIA_KURTOSIS && m_shapiroFrancia.getBooleanValue()) {
                double weightedSum = 0;
                int counter = 0;
                double sum4Var = 0;
                for (final DataRow row : sorted) {
                    final DataCell cell = row.getCell(cellIndex);
                    if (!cell.isMissing()) {
                        final double val = ((DoubleValue)cell).getDoubleValue();
                        weightedSum += sqrtSumInv * getM(counter + 1, n) * val;
                        sum4Var += Math.pow(val - mean, 2);
                        counter++;
                    } else if (warning == null) {
                        warning = "Input contains missing values. They will be ignored";
                    }
                }
                w = weightedSum * weightedSum / sum4Var;
            } else { // Shapiro-Wilk test is better for platykurtic samples
                final double cn = sqrtSumInv * getM(n, n);
                final double cn1 = sqrtSumInv * getM(n - 1, n);
                final double u = 1.0 / Math.sqrt(n);

                final double[] p1 = new double[]{-2.706056, 4.434685, -2.071190, -0.147981, 0.221157, cn};
                final double[] p2 = new double[]{-3.582633, 5.682633, -1.752461, -0.293762, 0.042981, cn1};

                double wn = polyval(p1, u);
                double w1 = -wn;
                double wn1 = Double.NaN;
                double w2 = Double.NaN;
                double phi;

                int ct = 2;

                if (n == 3) {
                    w1 = 0.707106781;
                    wn = -w1;
                    phi = 1;
                } else if (n >= 6) {
                    wn1 = polyval(p2, u);
                    w2 = -wn1;

                    ct = 3;
                    phi = (sum - 2 * Math.pow(getM(n, n), 2) - 2 * Math.pow(getM(n - 1, n), 2))
                        / (1 - 2 * Math.pow(wn, 2) - 2 * Math.pow(wn1, 2));
                } else {
                    phi = (sum - 2 * Math.pow(getM(n, n), 2) / (1 - 2 * Math.pow(wn, 2)));
                }

                double weightedSum = 0;
                int counter = 0;
                double sum4Var = 0;
                for (final DataRow row : sorted) {
                    final DataCell cell = row.getCell(cellIndex);
                    if (!cell.isMissing()) {
                        double weight = 0;
                        // We might have to use the precalculated w1, w2, wn or wn - 1
                        if (counter < ct - 1) {
                            if (counter == 0) {
                                weight = w1;
                            } else if (counter == 1) {
                                weight = w2;
                            }
                        } else if (counter >= n - ct + 1) {
                            if (counter == n - 1) {
                                weight = wn;
                            } else if (counter == n - 2) {
                                weight = wn1;
                            }
                        } else {
                            weight = getM(counter + 1, n) / Math.sqrt(phi);
                        }

                        final double val = ((DoubleValue)cell).getDoubleValue();
                        weightedSum += weight * val;
                        sum4Var += Math.pow(val - mean, 2);
                        counter++;
                    } else if (warning == null) {
                        warning = "Input contains missing values. They will be ignored";
                    }
                }
                w = Math.pow(weightedSum, 2) / sum4Var;
            }

            final double pVal = shapiroWilkPalue(w, n);

            pushFlowVariableDouble("shapiro-p-value", pVal);
            pushFlowVariableDouble("shapiro-statistic", w);

            dc.addRowToTable(new DefaultRow(new RowKey(col), new DoubleCell(w), new DoubleCell(pVal)));
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

    /**
     *
     * @param w the w calculated with shapiro-wilk
     * @param n The length of the array
     * @return p value
     */
    private static final double shapiroWilkPalue(final double w, final int n) {

        if (n < MIN_ROWS) {
            return 1;
        }
        if (n == MIN_ROWS) {
            return Math.max(0, 1.90985931710274 * (Math.asin(Math.sqrt(w)) - 1.04719755119660));
        }

        double y = Math.log(1 - w);
        final double xx = Math.log(n);
        double gamma, m, s;

        if (n <= 11) {
            gamma = polyval2(G, n);
            if (y >= gamma) {
                return Double.MIN_VALUE;
            }
            y = -Math.log(gamma - y);
            m = polyval2(C3, n);
            s = Math.exp(polyval2(C4, n));
        } else {
            m = polyval2(C5, xx);
            s = Math.exp(polyval2(C6, xx));
        }
        return 1 - cndf((y - m) / s);
    }

    /**
     * Calculates the cumulative distribution function of the normal distribution for a specific value.
     *
     * @param x the value to calculate the cdf value for
     * @return the cdf value
     */
    private static double cndf(final double x) {
        final int neg = (x < 0d) ? 1 : 0;
        final double xn = (neg == 1) ? -x : x;

        final double k = (1d / (1d + 0.2316419 * xn));
        double y = ((((1.330274429 * k - 1.821255978) * k + 1.781477937) * k - 0.356563782) * k + 0.319381530) * k;
        y = 1.0 - 0.398942280401 * Math.exp(-0.5 * xn * xn) * y;

        return (1d - neg) * y + neg * (1d - y);
    }

    /**
     * @param i the index
     * @param n the total number of data points
     * @return Returns the m value for calculating the weight of a data point
     */
    private static double getM(final int i, final int n) {
        return DISTRIBUTION.inverseCumulativeProbability((i - 3.0 / 8.0) / (n + 0.25));
    }

    /**
     * Calculates a polynomial with x^0 * c[c.length-1] + x^1 * c[c.length-2] + ...
     *
     * @param c the array of coefficients
     * @param x the value
     * @return the polynomial value
     */
    private static double polyval(final double[] c, final double x) {
        double sum = 0;
        for (int i = 0; i < c.length; i++) {
            sum += Math.pow(x, i) * c[c.length - 1 - i];
        }
        return sum;
    }

    /**
     * Calculates a polynomial with x^0 * c[0] + x^1 * c[1] + ...
     *
     * @param c the array of coefficients
     * @param x the value
     * @return the polynomial value
     */
    private static double polyval2(final double[] c, final double x) {
        double sum = 0;
        for (int i = 0; i < c.length; i++) {
            sum += Math.pow(x, i) * c[i];
        }
        return sum;
    }

    private static DataTableSpec createSpec() {
        final DataColumnSpec measure = new DataColumnSpecCreator("W", DoubleCell.TYPE).createSpec();
        final DataColumnSpec p = new DataColumnSpecCreator("P", DoubleCell.TYPE).createSpec();

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
