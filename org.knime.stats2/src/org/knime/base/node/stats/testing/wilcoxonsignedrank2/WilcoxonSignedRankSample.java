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
package org.knime.base.node.stats.testing.wilcoxonsignedrank2;

/**
 *
 * @author winter
 */
public class WilcoxonSignedRankSample implements Comparable<WilcoxonSignedRankSample> {

    private double m_absoluteDifference;

    private boolean m_positive;

    /**
     * A sample representing the difference of a pair of values
     *
     * @param x1 First value of the pair
     * @param x2 Second value of the pair
     */
    public WilcoxonSignedRankSample(final double x1, final double x2) {
        double difference = x1 - x2;
        m_positive = difference >= 0;
        m_absoluteDifference = m_positive ? difference : difference * -1;
    }

    /**
     * @return The absolute difference of this sample
     */
    public double getAbsoluteDifference() {
        return m_absoluteDifference;
    }

    /**
     * @return true if x1>=x2, false otherwise
     */
    public boolean isPositive() {
        return m_positive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final WilcoxonSignedRankSample o) {
        double absoluteDifference1 = getAbsoluteDifference();
        double absoluteDifference2 = o.getAbsoluteDifference();
        if (absoluteDifference1 < absoluteDifference2) {
            return -1;
        } else if (absoluteDifference1 > absoluteDifference2) {
            return 1;
        } else {
            return 0;
        }
    }

}
