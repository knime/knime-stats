/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 8, 2015 (winter): created
 */
package org.knime.stats.repeatedmeasures.wilcoxon.signedrank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 *
 * @author winter
 */
public class WilcoxonSignedRankTest {

    private List<WilcoxonSignedRankSample> m_samples = new ArrayList<WilcoxonSignedRankSample>();

    private double m_wPlus = 0;

    private double m_wMinus = 0;

    private double m_zScore = 0;

    private double m_pValue = 0;

    private boolean m_executed = false;

    /**
     * Add additional sample pair to the test.
     *
     * Note: Samples can only be added if execute() has not been called yet.
     *
     * @param x1 First value of the pair
     * @param x2 Second value of the pair
     */
    public void addSample(final double x1, final double x2) {
        if (!m_executed) {
            if (!hasZeroDifference(x1, x2)) {
                m_samples.add(new WilcoxonSignedRankSample(x1, x2));
            }
        }
    }

    /**
     * Executes this test, calculating the test results based on the samples that were added.
     */
    public void execute() {
        if (m_samples.isEmpty()) {
            throw new IllegalStateException("No valid samples provided");
        }
        Collections.sort(m_samples, new Comparator<WilcoxonSignedRankSample>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(final WilcoxonSignedRankSample o1, final WilcoxonSignedRankSample o2) {
                double absoluteDifference1 = o1.getAbsoluteDifference();
                double absoluteDifference2 = o2.getAbsoluteDifference();
                if (absoluteDifference1 < absoluteDifference2) {
                    return -1;
                } else if (absoluteDifference1 > absoluteDifference2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        double wPlus = 0;
        double wMinus = 0;
        int samplesDone = 0;
        List<WilcoxonSignedRankSample> previousSamples = new ArrayList<WilcoxonSignedRankSample>();
        for (WilcoxonSignedRankSample sample : m_samples) {
            if (!previousSamples.isEmpty()) {
                if (previousSamples.get(0).compareTo(sample) != 0) {
                    double rank = samplesDone + 0.5 + (previousSamples.size() / (double)2);
                    for (WilcoxonSignedRankSample previousSample : previousSamples) {
                        if (previousSample.isPositive()) {
                            wPlus += rank;
                        } else {
                            wMinus += rank;
                        }
                        samplesDone++;
                    }
                    previousSamples = new ArrayList<WilcoxonSignedRankSample>();
                }
            }
            previousSamples.add(sample);
        }
        double rank = samplesDone + 0.5 + (previousSamples.size() / (double)2);
        for (WilcoxonSignedRankSample previousSample : previousSamples) {
            if (previousSample.isPositive()) {
                wPlus += rank;
            } else {
                wMinus += rank;
            }
        }
        m_wPlus = wPlus;
        m_wMinus = wMinus;
        int n = m_samples.size();
        double w = Math.min(wPlus, wMinus);
        m_zScore = (w - (n * (n + 1) / 4)) / Math.sqrt((n * (n + 1) * (2 * n + 1)) / 24);
        NormalDistribution standardNormal = new NormalDistribution(null, 0, 1,
            NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
        m_pValue = standardNormal.cumulativeProbability(m_zScore);
        m_executed = true;
    }

    /**
     * @return The w+ value
     */
    public double getWPlus() {
        if (!m_executed) {
            throw new IllegalStateException("The test needs to be executed before retrieving results.");
        }
        return m_wPlus;
    }

    /**
     * @return The w- value
     */
    public double getWMinus() {
        if (!m_executed) {
            throw new IllegalStateException("The test needs to be executed before retrieving results.");
        }
        return m_wMinus;
    }

    /**
     * @return The number of samples (excluding samples with a difference of 0)
     */
    public int getN() {
        return m_samples.size();
    }

    /**
     * @return The left (negative) z-Score
     */
    public double getLeftZScore() {
        if (!m_executed) {
            throw new IllegalStateException("The test needs to be executed before retrieving results.");
        }
        return m_zScore;
    }

    /**
     * @return The right (positive) z-Score
     */
    public double getRightZScore() {
        if (!m_executed) {
            throw new IllegalStateException("The test needs to be executed before retrieving results.");
        }
        return m_zScore * (-1);
    }

    /**
     * @return The p-Value for a one tailed test
     */
    public double getOneTailedPValue() {
        if (!m_executed) {
            throw new IllegalStateException("The test needs to be executed before retrieving results.");
        }
        return m_pValue;
    }

    /**
     * @return The p-Value for a two tailed test
     */
    public double getTwoTailedPValue() {
        if (!m_executed) {
            throw new IllegalStateException("The test needs to be executed before retrieving results.");
        }
        return m_pValue * 2;
    }

    private boolean hasZeroDifference(final double x1, final double x2) {
        return x1 <= x2 && x1 >= x2;
    }

}
