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
 *   Jan 28, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.stats.lda2.settings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Class storing the LDA apply settings.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class LDAApplySettings {

    /** The configuration key for the number of dimensions to reduce to. */
    private static final String K_CFG = "number_of_dimensions";

    /** The configuration key of the remove used columns flag. */
    private static final String REMOVE_USED_COLS_CFG = "remove_used_columns";

    /** The configuration key of the fail on missings flag. */
    private static final String FAIL_ON_MISSING_CFG = "fail_on_missings";

    /** Settings model for the dimension to reduce to. */
    private final SettingsModelInteger m_dims = new SettingsModelIntegerBounded(K_CFG, 1, 1, Integer.MAX_VALUE);

    /** Settings model indicating whether or not to remove the used columns. */
    private final SettingsModelBoolean m_removeUsedCols = new SettingsModelBoolean(REMOVE_USED_COLS_CFG, false);

    /** Settings model indicating whether or not to fail the computation if the input table contains missing values. */
    private final SettingsModelBoolean m_failOnMissings = new SettingsModelBoolean(FAIL_ON_MISSING_CFG, false);

    /**
     * Returns the model storing the number of dimensions to reduce to.
     *
     * @return model storing the number of dimension to reduce to
     */
    public SettingsModelInteger getDimModel() {
        return m_dims;
    }

    /**
     * Returns the model storing the remove used columns flag.
     *
     * @return the model storing the remove used columns flag
     */
    public SettingsModelBoolean getRemoveUsedColsModel() {
        return m_removeUsedCols;
    }

    /**
     * Returns the model storing the fail on missings flag.
     *
     * @return model storing the fail on missings flag
     */
    public SettingsModelBoolean getFailOnMissingsModel() {
        return m_failOnMissings;
    }

    /**
     * Validates the settings.
     *
     * @param settings the settings to validate
     * @throws InvalidSettingsException - If the validation failed
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dims.validateSettings(settings);
        m_removeUsedCols.validateSettings(settings);
        m_failOnMissings.validateSettings(settings);
    }

    /**
     * Loads the validated settings
     *
     * @param settings the settings to load
     * @throws InvalidSettingsException - If loading failed
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dims.loadSettingsFrom(settings);
        m_removeUsedCols.loadSettingsFrom(settings);
        m_failOnMissings.loadSettingsFrom(settings);
    }

    /**
     * Saves the settings
     *
     * @param settings settings to save to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_dims.saveSettingsTo(settings);
        m_removeUsedCols.saveSettingsTo(settings);
        m_failOnMissings.saveSettingsTo(settings);
    }
}
