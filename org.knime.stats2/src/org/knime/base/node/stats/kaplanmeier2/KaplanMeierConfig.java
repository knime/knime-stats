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
 *   25.07.2016 (Alexander): created
 */
package org.knime.base.node.stats.kaplanmeier2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *
 * @author Alexander Fillbrunn
 */
public class KaplanMeierConfig {
    /**
     * The configuration key if this node is shown in wizard.
     */
    static final String HIDE_IN_WIZARD = "hideInWizard";

    /**
     * The configuration key for the time column.
     */
    static final String TIME_COL_CFG = "timeCol";

    /**
     * The configuration key for the event type column.
     */
    static final String EVENT_COL_CFG = "eventCol";

    /**
     * The configuration key for the group column.
     */
    static final String GROUP_COL_CFG = "groupCol";

    /**
     * The configuration key for the title.
     */
    static final String TITLE_CFG = "titleTxt";

    /**
     * The configuration key for the subtitle.
     */
    static final String SUBTITLE_CFG = "subtitleTxt";

    /**
     * The configuration key for the fullscreen setting.
     */
    static final String FULLSCREEN_CFG = "fullscreen";

    /**
     * The configuration key for the fullscreen setting.
     */
    static final String ENABLE_VIEW_CTRLS_CFG = "viewCtrl";

    /**
     * The configuration key for the fullscreen setting.
     */
    static final String ENABLE_TITLE_EDIT_CFG = "titleEdit";

    /**
     * The configuration key for the fullscreen setting.
     */
    static final String ENABLE_SUBTITLE_EDIT_CFG = "subtitleEdit";

    /**
     * The configuration key for the width setting.
     */
    static final String WIDTH_CFG = "width";

    /**
     * The configuration key for the height setting.
     */
    static final String HEIGHT_CFG = "height";

    /**
     * The configuration key for the fullscreen button.
     */
    static final String FULLSCREEN_BTN_CFG = "displayFullscreenButton";

    /**
     * Creates a new empty instance of <code>KaplanMeierConfig</code>.
     */
    public KaplanMeierConfig() {
        m_hideInWizard = false;
        m_timeCol = new SettingsModelString(TIME_COL_CFG, null);
        m_eventCol = new SettingsModelString(EVENT_COL_CFG, null);
        m_groupCol = new SettingsModelString(GROUP_COL_CFG, null);
        m_title = new SettingsModelString(TITLE_CFG, "");
        m_subtitle = new SettingsModelString(SUBTITLE_CFG, "");
        m_fullscreen = new SettingsModelBoolean(FULLSCREEN_CFG, true);
        m_enableSubtitle = new SettingsModelBoolean(ENABLE_SUBTITLE_EDIT_CFG, true);
        m_enableTitleEdit = new SettingsModelBoolean(ENABLE_TITLE_EDIT_CFG, true);
        m_enableViewControls = new SettingsModelBoolean(ENABLE_VIEW_CTRLS_CFG, true);
        m_width = new SettingsModelInteger(WIDTH_CFG, 800);
        m_height = new SettingsModelInteger(HEIGHT_CFG, 600);
        m_displayFullscreenButton = new SettingsModelBoolean(FULLSCREEN_BTN_CFG, true);
    }

    private boolean m_hideInWizard;

    private SettingsModelBoolean m_displayFullscreenButton;

    private SettingsModelString m_timeCol;

    private SettingsModelString m_eventCol;

    private SettingsModelString m_groupCol;

    private SettingsModelString m_title;

    private SettingsModelString m_subtitle;

    private SettingsModelBoolean m_fullscreen;

    private SettingsModelBoolean m_enableViewControls;

    private SettingsModelBoolean m_enableTitleEdit;

    private SettingsModelBoolean m_enableSubtitle;

    private SettingsModelInteger m_width;

    private SettingsModelInteger m_height;


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

    /**
     * @return the displayFullscreenButton
     */
    public SettingsModelBoolean getDisplayFullscreenButton() {
        return m_displayFullscreenButton;
    }

    /**
     * @return the enableViewControls
     */
    public SettingsModelBoolean getEnableViewControls() {
        return m_enableViewControls;
    }

    /**
     * @return the enableTitleEdit
     */
    public SettingsModelBoolean getEnableTitleEdit() {
        return m_enableTitleEdit;
    }

    /**
     * @return the enableSubtitle
     */
    public SettingsModelBoolean getEnableSubtitleEdit() {
        return m_enableSubtitle;
    }

    /**
     * @return the timeCol
     */
    public SettingsModelString getTimeCol() {
        return m_timeCol;
    }

    /**
     * @return the eventCol
     */
    public SettingsModelString getEventCol() {
        return m_eventCol;
    }

    /**
     * @return the groupCol
     */
    public SettingsModelString getGroupCol() {
        return m_groupCol;
    }

    /**
     * @return the title
     */
    public SettingsModelString getTitle() {
        return m_title;
    }

    /**
     * @return the subtitle
     */
    public SettingsModelString getSubtitle() {
        return m_subtitle;
    }

    /**
     * @return the fullscreen
     */
    public SettingsModelBoolean getFullscreen() {
        return m_fullscreen;
    }

    /**
     * @return the width
     */
    public SettingsModelInteger getWidth() {
        return m_width;
    }

    /**
     * @return the height
     */
    public SettingsModelInteger getHeight() {
        return m_height;
    }

    /**
     * Loads the settings from the given node settings.
     * @param settings the settings
     * @throws InvalidSettingsException when the settings cannot be loaded
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_eventCol.loadSettingsFrom(settings);
        m_groupCol.loadSettingsFrom(settings);
        m_timeCol.loadSettingsFrom(settings);
        m_title.loadSettingsFrom(settings);
        m_subtitle.loadSettingsFrom(settings);
        m_fullscreen.loadSettingsFrom(settings);
        m_enableSubtitle.loadSettingsFrom(settings);
        m_enableTitleEdit.loadSettingsFrom(settings);
        m_enableViewControls.loadSettingsFrom(settings);
        m_width.loadSettingsFrom(settings);
        m_height.loadSettingsFrom(settings);
        m_displayFullscreenButton.loadSettingsFrom(settings);

        //added with 3.5
        m_hideInWizard = settings.getBoolean(HIDE_IN_WIZARD, false);
    }

    /**
     * Saves this settings object to the given node settings.
     * @param settings the settings to save to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_eventCol.saveSettingsTo(settings);
        m_groupCol.saveSettingsTo(settings);
        m_timeCol.saveSettingsTo(settings);
        m_title.saveSettingsTo(settings);
        m_subtitle.saveSettingsTo(settings);
        m_fullscreen.saveSettingsTo(settings);
        m_enableSubtitle.saveSettingsTo(settings);
        m_enableTitleEdit.saveSettingsTo(settings);
        m_enableViewControls.saveSettingsTo(settings);
        m_width.saveSettingsTo(settings);
        m_height.saveSettingsTo(settings);
        m_displayFullscreenButton.saveSettingsTo(settings);

        //added with 3.5
        settings.addBoolean(HIDE_IN_WIZARD, m_hideInWizard);
    }

    /**
     * Validates the settings stored in the given settings object.
     * @param settings the settings to validate
     * @throws InvalidSettingsException when the validation fails
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_eventCol.validateSettings(settings);
        m_groupCol.validateSettings(settings);
        m_timeCol.validateSettings(settings);
        m_title.validateSettings(settings);
        m_subtitle.validateSettings(settings);
        m_fullscreen.validateSettings(settings);
        m_enableSubtitle.validateSettings(settings);
        m_enableTitleEdit.validateSettings(settings);
        m_enableViewControls.validateSettings(settings);
        m_width.validateSettings(settings);
        m_height.validateSettings(settings);
        m_displayFullscreenButton.validateSettings(settings);

        //added with 3.5 for completeness
        settings.getBoolean(HIDE_IN_WIZARD, false);
    }

}
