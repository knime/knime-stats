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

/**
 * Container for the the shapiro wilk normality test results
 *
 * @author Kevin Kress, Knime GmbH, Konstanz
 */
public class ShapiroWilkStatistic {

    private double m_statistic;

    private double m_pvalue;

    private String m_warning;

    /**
     * Creates a ShapiroWilkStatistic instance with the given values
     *
     * @param statistic the statistic of the shapiro-wilk test
     * @param pvalue the pvalue of the shapiro-wilk test
     * @param warning the warning message to show
     */
    ShapiroWilkStatistic(final double statistic, final double pvalue, final String warning) {
        this.m_statistic = statistic;
        this.m_pvalue = pvalue;
        if (warning != null) {
            this.m_warning = warning;
        } else {
            this.m_warning = "";
        }

    }

    /**
     * @return the statistic
     */
    public double getStatistic() {
        return m_statistic;
    }

    /**
     * @return the pvalue
     */
    public double getPvalue() {
        return m_pvalue;
    }

    /**
     * @return the warning
     */
    public String getWarning() {
        return m_warning;
    }
}
