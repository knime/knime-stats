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
 *   5 Jul 2017 (albrecht): created
 */
package org.knime.base.node.stats.dataexplorer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class DataExplorerConfig {

    static final String MIN = "Minimum";
    static final String MAX = "Maximum";
    static final String MEAN = "Mean";
    static final String MEDIAN = "Median";
    static final String STD_DEV = "Standard Deviation";
    static final String VARIANCE = "Variance";
    static final String SKEWNESS = "Skewness";
    static final String KURTOSIS = "Kurtosis";
    static final String SUM = "Overall Sum";
    static final String MISSING = "No. missings";
    static final String NAN = "No. NaN";
    static final String P_INFINITY = "No. +∞";
    static final String N_INFINITY = "No. -∞";

    private static final String CFG_HIDE_IN_WIZARD = "hideInWizard";
    private static final boolean DEFAULT_HIDE_IN_WIZARD = false;
    private boolean m_hideInWizard = DEFAULT_HIDE_IN_WIZARD;

    /**
     * @return the hideInWizard
     */
    public boolean getHideInWizard() {
        return m_hideInWizard;
    }

    /**
     * @param hideInWizard the hideInWizard to set
     */
    public void setHideInWizard(final boolean hideInWizard) {
        m_hideInWizard = hideInWizard;
    }

    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_HIDE_IN_WIZARD, m_hideInWizard);
    }

    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_HIDE_IN_WIZARD);
    }

    protected void loadSettingsForDialog(final NodeSettingsRO settings) {
        settings.getBoolean(CFG_HIDE_IN_WIZARD, DEFAULT_HIDE_IN_WIZARD);
    }

}
