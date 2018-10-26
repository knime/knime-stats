package org.knime.base.node.stats.shapirowilk;

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
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of PMMLToJavascriptCompiler.
 *
 *
 * @author Alexander Fillbrunn
 */
@Deprecated
public class ShapiroWilkNodeModel extends NodeModel {

    private static final double SHAPIRO_FRANCIA_KURTOSIS = 3;

    private static final double MIN_ROWS = 3;

    private static final double MAX_ROWS = 5000;

    // constants for the p-value calculation
    private static final double[] C3 = {0.544, -0.39978, 0.025054, -6.714e-4},
            C4 = {1.3822, -0.77857, 0.062767, -0.0020322}, C5 = {-1.5861, -0.31082, -0.083751, 0.0038915},
            C6 = {-0.4803, -0.082676f, 0.0030302}, G = {-2.273, 0.459};

    private static final NormalDistribution DISTRIBUTION = new NormalDistribution(0, 1);

    /**
     * The configuration key for the class column.
     */
    static final String TEST_COL_CFG = "testCols";

    /**
     * The configuration key for the setting that controls if shapiro francia is used for leptokurtic samples.
     */
    static final String SHAPIRO_FRANCIA_CFG = "shapFrancia";

    /**
     * Constructor for the node model.
     */
    protected ShapiroWilkNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Creates a settings model for the class column.
     *
     * @return the settings model
     */
    public static SettingsModelString createTestColSettingsModel() {
        return new SettingsModelString(TEST_COL_CFG, null);
    }

    private SettingsModelString m_testColumn = createTestColSettingsModel();

    /**
     * Creates a settings model for shapiro-francia.
     *
     * @return the settings model
     */
    public static SettingsModelBoolean createShapiroFranciaSettingsModel() {
        return new SettingsModelBoolean(SHAPIRO_FRANCIA_CFG, true);
    }

    private SettingsModelBoolean m_shapiroFrancia = createShapiroFranciaSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        String col = m_testColumn.getStringValue();
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getDataTableSpec();
        final int cellIndex = inSpec.findColumnIndex(col);

        if (inTable.size() > Integer.MAX_VALUE) {
            throw new InvalidSettingsException("Too many data points to calculate the statistic.");
        } else if (inTable.size() > MAX_ROWS) {
            setWarningMessage("The test might be inaccurate for data sets with more than 5000 data points.");
        }

        if (inSpec.getColumnSpec(cellIndex).getDomain() == null) {
            throw new InvalidSettingsException(
                "The test column does not have an associated domain. " + "Please use a Domain Calculator node first.");
        }

        ExecutionContext sortContext = exec.createSubExecutionContext(0.3);

        BufferedDataTableSorter sorter = new BufferedDataTableSorter(inTable, Arrays.asList(col), new boolean[]{true});
        BufferedDataTable sorted = sorter.sort(sortContext);

        Kurtosis kurtosisStat = new Kurtosis(col);
        Mean meanStat = new Mean(col);
        MissingValue missingStat = new MissingValue(col);

        // Calculate mean and kurtosis
        StatisticCalculator statCalc = new StatisticCalculator(inSpec, kurtosisStat, meanStat, missingStat);
        statCalc.evaluate(inTable, exec.createSubExecutionContext(0.6));
        double mean = meanStat.getResult(col);
        double kurtosis = kurtosisStat.getResult(col);

        final int n = (int)inTable.size() - missingStat.getNumberMissingValues(col);
        if (n < MIN_ROWS) {
            throw new InvalidSettingsException("Not enough data points to calculate the statistic.");
        }

        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += Math.pow(getM(i + 1, n), 2);
        }
        double sqrtSumInv = 1.0 / Math.sqrt(sum);

        double w;

        String warning = null;

        // Shapiro-Francia test is better for leptokurtic samples
        if (kurtosis > SHAPIRO_FRANCIA_KURTOSIS && m_shapiroFrancia.getBooleanValue()) {
            double weightedSum = 0;
            int counter = 0;
            double sum4Var = 0;
            for (DataRow row : sorted) {
                DataCell cell = row.getCell(cellIndex);
                if (!cell.isMissing()) {
                    double val = ((DoubleValue)cell).getDoubleValue();
                    weightedSum += sqrtSumInv * getM(counter + 1, n) * val;
                    sum4Var += Math.pow(val - mean, 2);
                    counter++;
                } else if (warning == null) {
                    warning = "Input contains missing values. They will be ignored";
                }
            }
            w = weightedSum * weightedSum / sum4Var;
        } else { // Shapiro-Wilk test is better for platykurtic samples
            double cn = sqrtSumInv * getM(n, n);
            double cn1 = sqrtSumInv * getM(n - 1, n);
            double u = 1.0 / Math.sqrt(n);

            double[] p1 = new double[]{-2.706056, 4.434685, -2.071190, -0.147981, 0.221157, cn};
            double[] p2 = new double[]{-3.582633, 5.682633, -1.752461, -0.293762, 0.042981, cn1};

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
            for (DataRow row : sorted) {
                DataCell cell = row.getCell(cellIndex);
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

                    double val = ((DoubleValue)cell).getDoubleValue();
                    weightedSum += weight * val;
                    sum4Var += Math.pow(val - mean, 2);
                    counter++;
                } else if (warning == null) {
                    warning = "Input contains missing values. They will be ignored";
                }
            }
            w = Math.pow(weightedSum, 2) / sum4Var;
        }

        double pVal = shapiroWilkPalue(w, n);

        pushFlowVariableDouble("shapiro-p-value", pVal);
        pushFlowVariableDouble("shapiro-statistic", w);

        DataContainer dc = exec.createDataContainer(createSpec());
        dc.addRowToTable(new DefaultRow(new RowKey("Value"), new DoubleCell(w), new DoubleCell(pVal)));
        dc.close();

        return new PortObject[]{(BufferedDataTable)dc.getTable()};
    }

    /**
     *
     * @param w the w calculated with shapiro-wilk
     * @param n The length of the array
     * @return p value
     */
    private final double shapiroWilkPalue(final double w, final int n) {

        if (n < MIN_ROWS) {
            return 1;
        }
        if (n == MIN_ROWS) {
            return Math.max(0, 1.90985931710274 * (Math.asin(Math.sqrt(w)) - 1.04719755119660));
        }

        double y = Math.log(1 - w), xx = Math.log(n), gamma, m, s;

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
    private double cndf(final double x) {
        int neg = (x < 0d) ? 1 : 0;
        double xn = (neg == 1) ? -x : x;

        double k = (1d / (1d + 0.2316419 * xn));
        double y = ((((1.330274429 * k - 1.821255978) * k + 1.781477937) * k - 0.356563782) * k + 0.319381530) * k;
        y = 1.0 - 0.398942280401 * Math.exp(-0.5 * xn * xn) * y;

        return (1d - neg) * y + neg * (1d - y);
    }

    /**
     * @param i the index
     * @param n the total number of data points
     * @return Returns the m value for calculating the weight of a data point
     */
    private double getM(final int i, final int n) {
        return DISTRIBUTION.inverseCumulativeProbability((i - 3.0 / 8.0) / (n + 0.25));
    }

    /**
     * Calculates a polynomial with x^0 * c[c.length-1] + x^1 * c[c.length-2] + ...
     *
     * @param c the array of coefficients
     * @param x the value
     * @return the polynomial value
     */
    private double polyval(final double[] c, final double x) {
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
    private double polyval2(final double[] c, final double x) {
        double sum = 0;
        for (int i = 0; i < c.length; i++) {
            sum += Math.pow(x, i) * c[i];
        }
        return sum;
    }

    private DataTableSpec createSpec() {
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];

        if (m_testColumn.getStringValue() == null || inSpec.findColumnIndex(m_testColumn.getStringValue()) == -1) {
            for (int i = 0; i < inSpec.getNumColumns(); i++) {
                DataColumnSpec colSpec = inSpec.getColumnSpec(i);
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    setWarningMessage("No column selected or selected column not available. Using " + colSpec.getName()
                        + " instead.");
                    m_testColumn.setStringValue(colSpec.getName());
                    break;
                }
            }
        }
        return new PortObjectSpec[]{createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_testColumn.saveSettingsTo(settings);
        m_shapiroFrancia.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_testColumn.loadSettingsFrom(settings);
        m_shapiroFrancia.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_testColumn.validateSettings(settings);
        m_shapiroFrancia.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }
}
