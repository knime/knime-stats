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
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 8, 2018 (knime): created
 */
package org.knime.base.node.stats.shapirowilk2;

import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.knime.base.data.statistics.StatisticCalculator;
import org.knime.base.data.statistics.calculation.Kurtosis;
import org.knime.base.data.statistics.calculation.Mean;
import org.knime.base.data.statistics.calculation.MissingValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 * Helper class which provides the necessary function to perform the shapiro-wilk normality test
 *
 * @author Kevin Kress, Knime GmbH, Konstanz
 */
public class ShapiroWilkCalculator {

    private ShapiroWilkCalculator() {
    }

    private static final NormalDistribution DISTRIBUTION = new NormalDistribution(0, 1);

    private static final double MIN_ROWS = 3;

    private static final double SHAPIRO_FRANCIA_KURTOSIS = 3;

    // constants for the p-value calculation
    private static final double[] C3 = {0.544, -0.39978, 0.025054, -6.714e-4},
            C4 = {1.3822, -0.77857, 0.062767, -0.0020322}, C5 = {-1.5861, -0.31082, -0.083751, 0.0038915},
            C6 = {-0.4803, -0.082676f, 0.0030302}, G = {-2.273, 0.459};

    /**
     * Calculates the statistc and the pvalue of the shapiro-wilk normality test
     *
     * @param exec the executionContext to set progress
     * @param inTable the bufferedDataTable with the data
     * @param col the column name to extract the data from
     * @param shapiroFrancia if the shapiro-francia test should be used
     * @param maxProg1 the maximum progress to set for the sorting
     * @param maxProg2 the maximum progress to set for the statistic calculator
     * @return the shapiroWilkStatistic with the calculated statistic and pvalue
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    public static ShapiroWilkStatistic calculateSWStatistic(final ExecutionContext exec,
        final BufferedDataTable inTable, final String col, final boolean shapiroFrancia, final double maxProg1,
        final double maxProg2) throws InvalidSettingsException, CanceledExecutionException {

        final DataTableSpec inSpec = inTable.getDataTableSpec();
        final int cellIndex = inSpec.findColumnIndex(col);
        if (inSpec.getColumnSpec(cellIndex).getDomain() == null) {
            throw new InvalidSettingsException("The test column " + col + " does not have an associated domain. "
                + "Please use a Domain Calculator node first.");
        }

        final ExecutionContext sortContext = exec.createSubExecutionContext(maxProg1);

        final BufferedDataTableSorter sorter =
            new BufferedDataTableSorter(inTable, Arrays.asList(col), new boolean[]{true});
        final BufferedDataTable sorted = sorter.sort(sortContext);

        final Mean meanStat = new Mean(col);
        final MissingValue missingStat = new MissingValue(col);

        // Calculate mean and kurtosis
        final StatisticCalculator statCalc = new StatisticCalculator(inSpec, meanStat, missingStat);
        statCalc.evaluate(inTable, exec.createSubExecutionContext(maxProg2));

        final double mean = meanStat.getResult(col);

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
        if (shapiroFrancia) {
            final Kurtosis kurtosisStat = new Kurtosis(col);
            new StatisticCalculator(inSpec, kurtosisStat).evaluate(inTable, exec.createSubExecutionContext(maxProg2));
            final double kurtosis = kurtosisStat.getResult(col);
            if (kurtosis <= SHAPIRO_FRANCIA_KURTOSIS) {
                warning = "Some samples are not leptokurtic. Shapiro-Wil test was be used for them instead.";
            } else {
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

                final double pVal = 1 - shapiroFranciaPvalue(w, n);

                return new ShapiroWilkStatistic(w, pVal, warning);
            }
        }

        // Shapiro-Wilk test is better for platykurtic samples
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

        final double pVal = shapiroWilkPvalue(w, n);

        return new ShapiroWilkStatistic(w, pVal, warning);
    }

    /**
     * @param w
     * @param n
     * @return
     */
    private static double shapiroFranciaPvalue(final double w, final int n) {
        final double u = Math.log(n);
        final double v = Math.log(u);
        final double mu = -1.2725 + 1.0521 * (v - u);
        final double sig = 1.0308 - 0.26758 * (v + 2 / u);
        final double z = (Math.log(1 - w) - mu) / sig;

        return cndf(z);
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
     *
     * @param w the w calculated with shapiro-wilk
     * @param n The length of the array
     * @return p value
     */
    private static final double shapiroWilkPvalue(final double w, final int n) {

        if (n < MIN_ROWS) {
            return 1;
        }
        if (n == MIN_ROWS) {
            return Math.max(0, 1.90985931710274 * (Math.asin(Math.sqrt(w)) - 1.04719755119660));
        }

        double y = Math.log(1 - w);
        final double xx = Math.log(n);
        double gamma;
        double m;
        double s;

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
}
