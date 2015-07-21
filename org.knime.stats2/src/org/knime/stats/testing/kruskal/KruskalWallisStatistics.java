package org.knime.stats.testing.kruskal;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;

/**
 * Implementation of Kruskal Wallis Test
 *
 * @author Christian Dietz, University of Konstanz
 */
public class KruskalWallisStatistics {

    /**
     * Computes the chi-squared approximated H value
     *
     * @return the H value of Kruskal Wallis Test
     */
    static KruskalWallisStatisticsResult calculateHValue(final double[] data, final int[] assignedGroups,
        final int numGroups, final NaNStrategy strategy) {

        final NaturalRanking ranking = new NaturalRanking(strategy);

        final int[] groupCount = new int[numGroups];

        final double[] ranks;
        try {
            ranks = ranking.rank(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failing because of missing value(s) in value column!");
        }

        final DescriptiveStatistics[] stats = new DescriptiveStatistics[numGroups];

        // init
        for (int i = 0; i < stats.length; ++i) {
            stats[i] = new DescriptiveStatistics();
        }

        for (int i = 0; i < data.length; i++) {
            stats[assignedGroups[i]].addValue(ranks[i]);
            groupCount[assignedGroups[i]]++;
        }

        double Htmp = 0.0;
        for (int i = 0; i < numGroups; i++) {
            Htmp += Math.pow(stats[i].getSum(), 2.0) / groupCount[assignedGroups[i]];
        }

        final KruskalWallisStatisticsResult res = new KruskalWallisStatisticsResult();

        int N = data.length;
        res.H = 12.0 / (N * (N + 1)) * Htmp - 3.0 * (N + 1);
        res.stats = stats;

        return res;
    }

    /**
     * Calculate P-Value
     *
     * @param hValue the H-value
     * @param numGroups number of groups
     * @return the p-value
     */
    static double calculatePValue(final double hValue, final int numGroups) {
        return 1.0 - new ChiSquaredDistribution(numGroups - 1).cumulativeProbability(hValue);
    }

    /**
     * Second most beautiful helper class
     *
     * @author Christian Dietz, University of Konstanz
     */
    protected static class KruskalWallisStatisticsResult {

        @SuppressWarnings("javadoc")
        // H value
        protected double H;

        @SuppressWarnings("javadoc")
        // statistics for the invididual groups
        protected DescriptiveStatistics[] stats;
    }

}
