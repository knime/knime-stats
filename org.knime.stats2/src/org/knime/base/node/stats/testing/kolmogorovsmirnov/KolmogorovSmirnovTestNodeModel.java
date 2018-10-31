package org.knime.base.node.stats.testing.kolmogorovsmirnov;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.util.FastMath;
import org.knime.base.data.statistics.StatisticCalculator;
import org.knime.base.data.statistics.calculation.MissingValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of KolmogorovSmirnovTest.
 *
 *
 * @author Kevin Kress, KNIME GmbH, Konstanz, Germany
 */
final class KolmogorovSmirnovTestNodeModel extends NodeModel {

    static final int PORT_IN_DATA = 0;

    private static final double MIN_ROWS = 2;

    private static final int EXACT_P_MAX_VALUES = 10000;

    static final String NAN_STRATEGY_FAILED = "FAILED";

    static final String NAN_STRATEGY_REMOVED = "REMOVED";

    static final String CFGKEY_COLUMN1 = "testCol1";

    static final String CFGKEY_COLUMN2 = "testCol2";

    private final SettingsModelDoubleBounded m_alpha = createSettingsModelAlpha();

    private final SettingsModelString m_nanStrategy = createSettingsModelNANStrategy();

    private final SettingsModelBoolean m_exact = createSettingsModelExactP();

    private final SettingsModelIntegerBounded m_iterations = createSettingsModelIterations();

    private final SettingsModelDoubleBounded m_cauchyCriterion = createSettingsModelTolerance();

    private final SettingsModelString m_testColumn1 = createSettingsModelCol(CFGKEY_COLUMN1);

    private final SettingsModelString m_testColumn2 = createSettingsModelCol(CFGKEY_COLUMN2);

    /**
     * Constructor for the node model.
     */
    protected KolmogorovSmirnovTestNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final NodeProgressMonitor progMon = exec.getProgressMonitor();
        int progCnt = 0;

        final BufferedDataTable inTable = inData[PORT_IN_DATA];

        final BufferedDataContainer outContainer = exec.createDataContainer(createOutputSpec());
        if (inTable.size() == 0) {
            outContainer.close();
            return new BufferedDataTable[]{outContainer.getTable()};
        }

        final DataTableSpec inSpec = inTable.getDataTableSpec();

        final String col1 = m_testColumn1.getStringValue();
        final String col2 = m_testColumn2.getStringValue();

        final MissingValue missingStat = new MissingValue(col1, col2);
        final StatisticCalculator statCalc = new StatisticCalculator(inSpec, missingStat);
        statCalc.evaluate(inTable, exec.createSubExecutionContext(0.2));

        final long m = inTable.size() - missingStat.getNumberMissingValues(col1);
        final long n = inTable.size() - missingStat.getNumberMissingValues(col2);

        if (m_nanStrategy.getStringValue().equals(NAN_STRATEGY_FAILED) && n + m < 2 * inTable.size()) {

            throw new InvalidSettingsException(
                "Missing values are not allowed. Either remove them or set the option to ignore missing values");
        }

        if (m < MIN_ROWS || n < MIN_ROWS) {
            throw new InvalidSettingsException(
                "Not enough data points to calculate the statistic. Need at least 3 values per sample");
        } else if (m > Integer.MAX_VALUE || n > Integer.MAX_VALUE) {
            throw new InvalidSettingsException("Too many data points to calculate the statistic.");
        } else if (MemoryAlertSystem.getMaximumMemory() * MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD < (m + n) * 8) {
            throw new InvalidSettingsException(
                "Not enough memory to calculate the test. If possible increase the the available memory or decrease the dataset.");
        }
        final int cellIndex1 = inSpec.findColumnIndex(col1);
        final int cellIndex2 = inSpec.findColumnIndex(col2);

        int count1 = 0;
        int count2 = 0;
        final double[] colData1 = new double[(int)m];
        final double[] colData2 = new double[(int)n];

        try (CloseableRowIterator rowIterator =
            inTable.iteratorBuilder().filterColumns(cellIndex1, cellIndex2).build()) {
            while (rowIterator.hasNext()) {
                final DataRow row = rowIterator.next();
                progMon.setProgress(progCnt / (10.0 * inTable.size()));
                progCnt++;
                if (!row.getCell(cellIndex1).isMissing()) {
                    colData1[count1] = ((DoubleValue)row.getCell(cellIndex1)).getDoubleValue();
                    count1++;
                }
                if (!row.getCell(cellIndex2).isMissing()) {
                    colData2[count2] = ((DoubleValue)row.getCell(cellIndex2)).getDoubleValue();
                    count2++;
                }
            }
        }

        final double statistic = calculateKSStatistic(colData1, colData2);
        progMon.setProgress(0.5);
        if (m_exact.getBooleanValue()) {
            if (colData1.length * colData2.length > EXACT_P_MAX_VALUES) {
                throw new InvalidSettingsException(
                    "Too many values to compute an exact p-value. Either reduce the sample size or change the settings of exact p-value.");
            } else if (findTies(colData1, colData2)) {
                throw new InvalidSettingsException(
                    "Cannot compute exact p-value with ties. Provide data without duplicate values or change the settings of exact p-value.");
            }
        }

        final double pvalue = calculatePValue(colData1.length, colData2.length, statistic, m_exact.getBooleanValue());
        progMon.setProgress(0.9);

        final List<DataCell> cells = new ArrayList<>(3);
        cells.add(BooleanCellFactory.create(pvalue < m_alpha.getDoubleValue()));
        cells.add(new DoubleCell(statistic));
        cells.add(new DoubleCell(pvalue));
        final RowKey key = new RowKey(
            "Kolmogorov-Smirnov (" + m_testColumn1.getStringValue() + ", " + m_testColumn2.getStringValue() + ")");
        final DataRow outRow = new DefaultRow(key, cells);
        outContainer.addRowToTable(outRow);

        // once we are done, we close the container and return its table
        outContainer.close();

        return new BufferedDataTable[]{outContainer.getTable()};
    }

    private static boolean findTies(final double[] x, final double[] y) {
        int indexX = 0;
        int indexY = 0;
        while (indexX < x.length && indexY < y.length) {
            final double xValue = x[indexX];
            final double yValue = y[indexY];
            if (xValue < yValue) {
                indexX++;
                if (indexX < x.length && Double.compare(xValue, x[indexX]) == 0) {
                    return true;
                }
            } else if (xValue > yValue) {
                indexY++;
                if (indexY < y.length && Double.compare(yValue, y[indexY]) == 0) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Method copied from org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest with small changes
     */
    private static double calculateKSStatistic(final double[] x, final double[] y) {
        final int n = x.length;
        final int m = y.length;
        Arrays.sort(x);
        Arrays.sort(y);

        // Find the max difference between cdf_x and cdf_y
        double supD = 0d;
        // First walk x points
        for (int i = 0; i < n; i++) {
            final double cdf_x = (i + 1d) / n;
            final int yIndex = Arrays.binarySearch(y, x[i]);
            final double cdf_y = yIndex >= 0 ? (yIndex + 1d) / m : (-yIndex - 1d) / m;
            final double curD = FastMath.abs(cdf_x - cdf_y);
            if (curD > supD) {
                supD = curD;
            }
        }
        // Now look at y
        for (int i = 0; i < m; i++) {
            final double cdf_y = (i + 1d) / m;
            final int xIndex = Arrays.binarySearch(x, y[i]);
            final double cdf_x = xIndex >= 0 ? (xIndex + 1d) / n : (-xIndex - 1d) / n;
            final double curD = FastMath.abs(cdf_x - cdf_y);
            if (curD > supD) {
                supD = curD;
            }
        }
        return supD;
    }

    private double calculatePValue(final int col1Len, final int col2Len, final double statistic, final boolean exact) {
        double p;
        if (exact) {
            p = 1 - exactP(statistic, col1Len, col2Len);
        } else {
            p = approximateP(statistic, col1Len, col2Len);
        }
        return p;
    }

    /**
     * Method from R to calculate the exact p-value
     * https://github.com/SurajGupta/r-source/blob/master/src/library/stats/src/ks.c
     * https://github.com/SurajGupta/r-source/blob/master/src/library/stats/R/ks.test.R
     */
    private static double exactP(final double d, int n, int m) {
        int i;
        if (m > n) {
            i = n;
            n = m;
            m = i;
        }
        final double md = m;
        final double nd = n;

        /*
        q has 0.5/mn added to ensure that rounding error doesn't
        turn an equality into an inequality, eg abs(1/2-4/5)>3/10
        */
        final double q = (0.5 + FastMath.floor(d * md * nd - 1e-7)) / (md * nd);
        final double[] u = new double[n + 1];
        for (int j = 0; j <= n; j++) {
            u[j] = j / nd > q ? 0 : 1;
        }
        for (i = 1; i <= m; i++) {
            final double w = (double)i / (double)(i + n);
            if (i / md > q) {
                u[0] = 0;
            } else {
                u[0] = w * u[0];
            }
            for (int j = 1; j <= n; j++) {
                if (FastMath.abs(i / md - j / nd) > q) {
                    u[j] = 0;
                } else {
                    u[j] = w * u[j] + u[j - 1];
                }
            }
        }
        return u[n];
    }

    private double approximateP(final double d, final int n, final int m) {
        final double dm = m;
        final double dn = n;
        return 1 - ksSum(d * FastMath.sqrt((dm * dn) / (dm + dn)), m_cauchyCriterion.getDoubleValue(),
            m_iterations.getIntValue());
    }

    /**
     * Method copied from org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest with small changes. Added the
     * case if t is smaller than one
     */
    private static double ksSum(final double t, final double tolerance, final int maxIterations) {
        if (t < 1) {
            final int k_max = (int)FastMath.sqrt(2 - FastMath.log(tolerance));
            final double x = -((Math.PI / 2) * (Math.PI / 4)) / (t * t);
            final double w = FastMath.log(t);
            double s = 0;
            for (int k = 1; k < k_max; k += 2) {
                s += FastMath.exp(k * k * x - w);
            }
            return s * FastMath.sqrt(Math.PI * 2);
        }
        final double x = -2 * t * t;
        int sign = -1;
        long i = 1;
        double partialSum = 0.5d;
        double delta = 1;
        while (delta > tolerance && i < maxIterations) {
            delta = FastMath.exp(x * i * i);
            partialSum += sign * delta;
            sign *= -1;
            i++;
        }
        if (i == maxIterations) {
            throw new TooManyIterationsException(maxIterations);
        }
        return partialSum * 2;
    }

    private static DataTableSpec createOutputSpec() {
        final List<DataColumnSpec> allColSpecs = new ArrayList<>(3);
        allColSpecs.add(new DataColumnSpecCreator("Reject H0", BooleanCell.TYPE).createSpec());
        allColSpecs
            .add(StatsUtil.createDataColumnSpec("Statistic", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));
        allColSpecs.add(StatsUtil.createDataColumnSpec("p-Value", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));

        return new DataTableSpec(allColSpecs.toArray(new DataColumnSpec[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = inSpecs[0];

        final int k = inSpec.getNumColumns();
        if (k < 2) {
            throw new InvalidSettingsException(
                "Not enough data columns available (" + k + "), please provide a data table with at least 2.");
        }

        if (m_testColumn1.getStringValue() == null || m_testColumn2.getStringValue() == null) {
            for (final DataColumnSpec column : inSpec) {
                if (column.getType().isCompatible(DoubleValue.class)) {
                    if (m_testColumn1.getStringValue() == null) {
                        m_testColumn1.setStringValue(column.getName());
                    } else if (m_testColumn2.getStringValue() == null) {
                        m_testColumn2.setStringValue(column.getName());
                        break;
                    } else {
                        break;
                    }
                }
            }
        }

        if (m_testColumn1.getStringValue() == null) {
            throw new InvalidSettingsException(
                "Not enough numerical data columns available, please provide a data table with at least one.");
        } else if (m_testColumn2.getStringValue() == null) {
            m_testColumn2.setStringValue(m_testColumn1.getStringValue());
        }

        if (!inSpec.containsName(m_testColumn1.getStringValue())
            || !inSpec.getColumnSpec(m_testColumn1.getStringValue()).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException(
                "Test column " + m_testColumn1.getStringValue() + " not found or incompatible");
        } else if (!inSpec.containsName(m_testColumn2.getStringValue())
            || !inSpec.getColumnSpec(m_testColumn2.getStringValue()).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException(
                "Test column " + m_testColumn2.getStringValue() + " not found or incompatible");
        }
        if (m_testColumn1.getStringValue().equals(m_testColumn2.getStringValue())) {
            setWarningMessage("The two columns should be different.");
        }

        return new DataTableSpec[]{createOutputSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_alpha.saveSettingsTo(settings);
        m_nanStrategy.saveSettingsTo(settings);
        m_exact.saveSettingsTo(settings);
        m_iterations.saveSettingsTo(settings);
        m_cauchyCriterion.saveSettingsTo(settings);
        m_testColumn1.saveSettingsTo(settings);
        m_testColumn2.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alpha.loadSettingsFrom(settings);
        m_nanStrategy.loadSettingsFrom(settings);
        m_exact.loadSettingsFrom(settings);
        m_iterations.loadSettingsFrom(settings);
        m_cauchyCriterion.loadSettingsFrom(settings);
        m_testColumn1.loadSettingsFrom(settings);
        m_testColumn2.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alpha.validateSettings(settings);
        m_nanStrategy.validateSettings(settings);
        m_exact.validateSettings(settings);
        m_iterations.validateSettings(settings);
        m_cauchyCriterion.validateSettings(settings);
        m_testColumn1.validateSettings(settings);
        m_testColumn2.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do
    }

    /**
     * Creates a settings model for the significance level alpha.
     *
     * @return the settings model
     */
    static SettingsModelDoubleBounded createSettingsModelAlpha() {
        return new SettingsModelDoubleBounded("Alpha", 0.05, 0, 1);
    }

    /**
     * Creates a settings model for one of the used columns.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelCol(final String cfgKey) {
        return new SettingsModelString(cfgKey, null);
    }

    /**
     * Creates a settings model for the NaN-Strategy.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelNANStrategy() {
        return new SettingsModelString("NaN-Strategy", NAN_STRATEGY_REMOVED);
    }

    /**
     * Creates a settings model for the option to calculate the p-value exact.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createSettingsModelExactP() {
        return new SettingsModelBoolean("Exact P-Value", false);
    }

    /**
     * Creates a settings model for the option to set the maximum iteration for the appproximation of p.
     *
     * @return the settings model
     */
    static SettingsModelIntegerBounded createSettingsModelIterations() {
        return new SettingsModelIntegerBounded("Iterations", 1000000, 0, Integer.MAX_VALUE);
    }

    /**
     * Creates a settings model for the option to set the cauchy criterion for the appproximation of p.
     *
     * @return the settings model
     */
    static SettingsModelDoubleBounded createSettingsModelTolerance() {
        return new SettingsModelDoubleBounded("Cauchy Criterion", 1E-6, 0, 1);
    }
}
