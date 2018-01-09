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
 *   May 13, 2016 (sampson): created
 */
package org.knime.base.node.stats.contintable;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.knime.base.node.viz.crosstable.CrosstabStatisticsCalculator;

/**
 *
 * @author Oliver Sampson, University of Konstanz
 */
/**
 * Uses the following table.
 * <table border="1">
 * <tr>
 * <td></td>
 * <th>X</th>
 * <th>&not;X</th>
 * </tr>
 * <tr>
 * <th>Y</th>
 * <td align="center">a</td>
 * <td align="center">b</td>
 * </tr>
 * <tr>
 * <th>&not;Y</th>
 * <td align="center">c</td>
 * <td align="center">d</td>
 * </tr>
 * </table>
 */
public class ContingencyTable {

    private int m_a = 0;

    private int m_b = 0;

    private int m_c = 0;

    private int m_d = 0;

    private double m_zScore = 0;

    private double m_correction = 0;

    private double m_lpCorr = 0;

    private ContingencyTable(final double confidenceLevel, final double lpCorr) {
        NormalDistribution normDist = new NormalDistribution();

        m_zScore = normDist.inverseCumulativeProbability(confidenceLevel + ((1 - confidenceLevel) / 2.0));

        m_lpCorr = lpCorr;
    }

    /**
     * @param a the upper left field in a contingency table
     * @param b the upper right field in a contingency table
     * @param c the lower left field in a contingency table
     * @param d the lower right field in a contingency table
     * @param confidenceLevel the confidence level for some measured statistics
     * @param lpCorr the Laplace correction
     */
    public ContingencyTable(final int a, final int b, final int c, final int d, final double confidenceLevel,
        final double lpCorr) {
        this(confidenceLevel, lpCorr);
        m_a = a;
        m_b = b;
        m_c = c;
        m_d = d;

        if (m_a == 0 || m_b == 0 || m_c == 0 || m_d == 0) {
            m_correction = m_lpCorr;
        }

    }

    private double getA() {
        return m_a + m_correction;
    }

    private double getB() {
        return m_b + m_correction;
    }

    private double getC() {
        return m_c + m_correction;
    }

    private double getD() {
        return m_d + m_correction;
    }

    private int getUncorrectedA() {
        return m_a;
    }

    private int getUncorrectedB() {
        return m_b;
    }

    private int getUncorrectedC() {
        return m_c;
    }

    private int getUncorrectedD() {
        return m_d;
    }

    private double getExpectedA() {
        return ((getA() + getB()) * (getA() + getC())) / getN();
    }

    private double getExpectedB() {
        return ((getA() + getB()) * (getB() + getD())) / getN();
    }

    private double getExpectedC() {
        return ((getC() + getD()) * (getA() + getC())) / getN();
    }

    private double getExpectedD() {
        return ((getC() + getD()) * (getB() + getD())) / getN();
    }

    /**
     * @return the results of Fisher's exact test
     */
    public double[] getFishersExact() {
        int[][] crosstab = new int[2][2];
        crosstab[0][0] = getUncorrectedA();
        crosstab[0][1] = getUncorrectedB();
        crosstab[1][0] = getUncorrectedC();
        crosstab[1][1] = getUncorrectedD();

        return CrosstabStatisticsCalculator.exactPValue(crosstab);
    }

    /**
     * @return the Odds' Ratio
     */
    public double getOddsRatio() {
        if (getB() == 0 || getC() == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return getA() * getD() / (getB() * getC());
    }

    private double getOddsRatioStdErr() {
        return Math.sqrt(1 / getA() + 1 / getB() + 1 / getC() + 1 / getD());
    }

    /**
     * @return the Odds Ratio lower Confidence Interval
     */
    public double getOddsRatioLowerCI() {
        return Math.exp(Math.log(this.getOddsRatio()) - m_zScore * getOddsRatioStdErr());
    }

    /**
     * @return the Odds Ratio upper Confidence Interval
     */
    public double getOddsRatioUpperCI() {
        return Math.exp(Math.log(this.getOddsRatio()) + m_zScore * getOddsRatioStdErr());
    }

    /**
     * @return the Risk Ratio
     */
    public double getRiskRatio() {
        if (getA() + getB() == 0) {
            return Double.POSITIVE_INFINITY;
        } else if (getC() + getD() == 0) {
            return Double.POSITIVE_INFINITY;
        } else {
            return (getA() / (getA() + getB())) / (getC() / (getC() + getD()));
        }
    }

    private double getRiskRatioStdErr() {
        if (getA() + getB() == 0) {
            return Double.POSITIVE_INFINITY;
        } else if (getC() + getD() == 0) {
            return Double.POSITIVE_INFINITY;
        } else {
            return Math.sqrt((getB() / (getA() * (getA() + getB())) + getD() / (getC() * (getC() + getD()))));
        }
    }

    /**
     * @return Risk Ratio Lower Confidence Interval value
     */
    public double getRiskRatioLowerCI() {
        return Math.exp(Math.log(this.getRiskRatio()) - m_zScore * getRiskRatioStdErr());
    }

    /**
     * @returnthe Risk Ration upper Confidence Interval value.
     */
    public double getRiskRatioUpperCI() {
        return Math.exp(Math.log(this.getRiskRatio()) + m_zScore * getRiskRatioStdErr());
    }

    /**
     * @return Cramer's V
     * @see <a href="https://en.wikipedia.org/wiki/Cram%C3%A9r's_V">Cramers V on Wikipedia</a>
     */
    public double getCramers() {
        return Math.sqrt(getChiSquared() / getN());
    }

    /**
     * @return Pearson's Chi-sqared test
     * @see <a href="https://en.wikipedia.org/wiki/Pearson's_chi-squared_test">Pearsons Chi-squared Test on
     *      Wikipedia</a>
     */
    public double getPearsons() {
        return Math.sqrt(getChiSquared() / (getChiSquared() + getN()));
    }

    private double getN() {
        return getA() + getB() + getC() + getD();
    }

    private static double getChiSquaredNumerator(final double observed, final double expected) {
        return Math.pow(observed - expected, 2);
    }

    /**
     * @return the ChiSquare Statistic
     */
    public double getChiSquared() {
        double a = getChiSquaredNumerator(getA(), getExpectedA()) / getExpectedA();
        double b = getChiSquaredNumerator(getB(), getExpectedB()) / getExpectedB();
        double c = getChiSquaredNumerator(getC(), getExpectedC()) / getExpectedC();
        double d = getChiSquaredNumerator(getD(), getExpectedD()) / getExpectedD();

        return a + b + c + d;

    }

    /**
     * @return the YatesCorrected value
     */
    public double getYatesCorrected() {
        return getN() * Math.pow(Math.max(0, Math.abs(getA() * getD() - getB() * getC()) - getN() / 2.0), 2)
            / ((getA() + getC()) * (getB() + getD()) * (getA() + getB()) * (getC() + getD()));
    }

    /**
     * @param confidence
     * @return the critical Chi Squared value at the level of confidence for 1 degree of freedom (which is used for
     *         contingency tables.)
     */
    public static double getCriticalChiSquare(final double confidence) {

        ChiSquaredDistribution chiDist = new ChiSquaredDistribution(1);

        return chiDist.inverseCumulativeProbability(confidence);
    }

    /**
     * @return the p-value associated with this Chi-squared test for this contingeny table.
     */
    public double getChiSquaredPValue() {
        long[][] t = new long[2][2];
        t[0][0] = (long)getA();
        t[0][1] = (long)getB();
        t[1][0] = (long)getC();
        t[1][1] = (long)getD();

        ChiSquareTest c = new ChiSquareTest();

        return c.chiSquareTest(t);
    }



}