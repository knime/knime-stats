package org.knime.stats.testing.kruskal;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
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
    static double calculateHValue(final double[] data, final int[] assignedGroups, final int numGroups,
        final NaNStrategy strategy) {

        final NaturalRanking ranking = new NaturalRanking(strategy);

        final int[] groupCount = new int[numGroups];

        final double[] ranks = ranking.rank(data);
        final double[] rankSums = new double[numGroups];

        for (int i = 0; i < data.length; i++) {
            rankSums[assignedGroups[i]] += ranks[i];
            groupCount[assignedGroups[i]]++;
        }

        double H = 0.0;
        for (int i = 0; i < numGroups; i++) {
            H += Math.pow(rankSums[i], 2.0) / groupCount[assignedGroups[i]];
        }

        int N = data.length;
        return 12.0 / (N * (N + 1)) * H - 3.0 * (N + 1);
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

}
