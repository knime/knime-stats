/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   26.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.wilcoxonmannwhitney;

import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.util.FastMath;
import org.knime.core.data.DoubleValue;

/**
 * Helper for Wilcoxon Mann Whitney Statistics.
 *
 * @author Christian Dietz, University of Konstanz
 */
class WilcoxonMannWhitneyStatistics {

    /**
     * NB: This is a copy of {@link MannWhitneyUTest}{@link #calculateAsymptoticPValue(double, int, int)}, as this
     * method is not accessible from outside.
     *
     * @param Umin smallest Mann-Whitney U value
     * @param n1 number of subjects in first sample
     * @param n2 number of subjects in second sample
     * @return two-sided asymptotic p-value
     * @throws ConvergenceException if the p-value cannot be computed due to a convergence error
     * @throws MaxCountExceededException if the maximum number of iterations is exceeded
     */
    static double calculateAsymptoticPValue(final double Umin, final int n1, final int n2) throws ConvergenceException,
        MaxCountExceededException {

        /* long multiplication to avoid overflow (double not used due to efficiency
         * and to avoid precision loss)
         */
        final long n1n2prod = (long)n1 * n2;

        // http://en.wikipedia.org/wiki/Mann%E2%80%93Whitney_U#Normal_approximation
        final double EU = n1n2prod / 2.0;
        final double VarU = n1n2prod * (n1 + n2 + 1) / 12.0;

        final double z = (Umin - EU) / FastMath.sqrt(VarU);

        // No try-catch or advertised exception because args are valid
        // pass a null rng to avoid unneeded overhead as we will not sample from this distribution
        final NormalDistribution standardNormal =
            new NormalDistribution(null, 0, 1, NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);

        return 2 * standardNormal.cumulativeProbability(z);
    }

    /**
     * NB: This is a modified copy of {@link MannWhitneyUTest}#mannWhitneyU(double[], double[]) of ApacheMathCommons.
     *
     * Computes the <a href="http://en.wikipedia.org/wiki/Mann%E2%80%93Whitney_U"> Mann-Whitney U statistic</a>
     * comparing mean for two independent samples possibly of different length.
     * <p>
     * This statistic can be used to perform a Mann-Whitney U test evaluating the null hypothesis that the two
     * independent samples has equal mean.
     * </p>
     * <p>
     * Let X<sub>i</sub> denote the i'th individual of the first sample and Y<sub>j</sub> the j'th individual in the
     * second sample. Note that the samples would often have different length.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>All observations in the two samples are independent.</li>
     * <li>The observations are at least ordinal (continuous are also ordinal).</li>
     * </ul>
     * </p>
     *
     * @param groupAValues the first sample
     * @param groupBValues the second sample
     * @return Mann-Whitney U statistic (maximum of U<sup>x</sup> and U<sup>y</sup>)
     * @throws NullArgumentException if {@code x} or {@code y} are {@code null}.
     * @throws NoDataException if {@code x} or {@code y} are zero-length.
     */
    static MannWhitneyUTestResult mannWhitneyU(final List<DoubleValue> groupAValues,
        final List<DoubleValue> groupBValues, final NaturalRanking ranking) throws NullArgumentException,
        NoDataException, IllegalStateException {

        final int xSize = groupAValues.size();
        final int ySize = groupBValues.size();

        double[] z = new double[xSize + ySize];
        for (int i = 0; i < xSize; i++) {
            z[i] = groupAValues.get(i).getDoubleValue();
        }

        for (int i = 0; i < ySize; i++) {
            z[i + xSize] = groupBValues.get(i).getDoubleValue();
        }

        final double[] ranks;
        try {
            ranks = ranking.rank(z);
        } catch (Exception e) {
            throw new IllegalStateException("Failing because of missing value(s) in value column!");
        }
        /*
         * The ranks for x is in the first x.length entries in ranks because x
         * is in the first x.length entries in z
         */
        final DescriptiveStatistics statsX = new DescriptiveStatistics();
        for (int i = 0; i < xSize; ++i) {
            statsX.addValue(ranks[i]);
        }

        final DescriptiveStatistics statsY = new DescriptiveStatistics();
        for (int i = xSize; i < z.length; ++i) {
            statsY.addValue(ranks[i]);
        }

        /*
         * U1 = R1 - (n1 * (n1 + 1)) / 2 where R1 is sum of ranks for sample 1,
         * e.g. x, n1 is the number of observations in sample 1.
         */
        final double U1 = statsX.getSum() - ((long)xSize * (xSize + 1)) / 2;

        /*
         * It can be shown that U1 + U2 = n1 * n2
         */
        final double U2 = (long)xSize * ySize - U1;

        final MannWhitneyUTestResult res = new MannWhitneyUTestResult();
        if (U1 > U2) {
            res.uMin = U2;
            res.uMax = U1;
        } else {
            res.uMin = U1;
            res.uMax = U2;
        }

        res.meanA = statsX.getMean();
        res.meanB = statsY.getMean();
        res.medianA = statsX.getPercentile(50);
        res.medianB = statsY.getPercentile(50);

        return res;
    }

    // most beautiful implementation of a helper class.
    static class MannWhitneyUTestResult {
        protected double uMin, uMax, meanA, meanB, medianA, medianB;
    }
}
