package org.knime.base.node.stats.testing.kruskalwallis;

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

        // Implementation of formula for H which does not need a correction for ties.
        // See: https://en.wikipedia.org/wiki/Kruskal%E2%80%93Wallis_one-way_analysis_of_variance
        //
        // H = (n - 1) * ( sum_i=1^g(n_i * (r_bar_i - r_bar)^2) / (sum_i=1^n(r_i - r_bar)^2) )
        //        n := total number of data points
        //        g := number of groups
        //      n_i := number of data points in group i
        //  r_bar_i := mean of ranks in group i
        //    r_bar := total mean of ranks
        //      r_i := rank of data point i

        double n = data.length;
        double numerator = 0.0;
        double overallRankMean = (n + 1) / 2.0;
        for (int i = 0; i < numGroups; i++) {
            numerator += groupCount[i] * Math.pow((stats[i].getMean() - overallRankMean), 2);
        }

        double nominator = 0.0;
        for (double rank : ranks) {
            nominator += Math.pow((rank - overallRankMean), 2);
        }

        final KruskalWallisStatisticsResult res = new KruskalWallisStatisticsResult();
        res.H = (n - 1) * (numerator / nominator);
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
