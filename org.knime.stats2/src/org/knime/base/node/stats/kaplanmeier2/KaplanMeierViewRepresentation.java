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
 *   20.07.2016 (Alexander): created
 */
package org.knime.base.node.stats.kaplanmeier2;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.JSONDataTable;
import org.knime.js.core.JSONViewContent;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *
 * @author Alexander Fillbrunn
 */
@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class KaplanMeierViewRepresentation extends JSONViewContent {

    private static final String WIDTH_CFG = "width";

    private static final String HEIGHT_CFG = "height";

    private static final String FULLSCREEN_CFG = "fullscreen";

    private static final String ENABLE_VIEW_CTRLS_CFG = "enableViewCtrls";

    private static final String ENABLE_SUBTITLE_EDIT_CFG = "enableSubtitleEdit";

    private static final String ENABLE_TITLE_EDIT_CFG = "enableTitleEdit";

    private static final String SHOW_LEGEND_CFG = "showLegend";

    private static final String TIME_COL_CFG = "timeCol";

    private static final String EVENT_COL_CFG = "eventCol";

    private static final String GROUP_COL_CFG = "groupCol";

    private static final String RUNNING_IN_VIEW_CFG = "runningInView";

    private static final String FULLSCREEN_BTN_CFG = "displayFullscreenButton";

    private JSONDataTable m_table;

    private int m_width = 800;

    private int m_height = 600;

    private boolean m_fullscreen = true;

    private boolean m_enableViewControls = true;

    private boolean m_showLegend = true;

    private boolean m_enableTitleEdit = true;

    private boolean m_enableSubtitleEdit = true;

    private String m_timeCol;

    private String m_eventCol;

    private String m_groupCol;

    private boolean m_runningInView;

    private boolean m_displayFullscreenButton;

    /**
     * @return the displayFullscreenButton
     */
    public boolean getDisplayFullscreenButton() {
        return m_displayFullscreenButton;
    }

    /**
     * @param displayFullscreenButton the displayFullscreenButton to set
     */
    public void setDisplayFullscreenButton(final boolean displayFullscreenButton) {
        m_displayFullscreenButton = displayFullscreenButton;
    }

    /**
     * Creates an empty view representation.
     */
    public KaplanMeierViewRepresentation() {

    }

    /**
     * @return the runningInView
     */
    public boolean isRunningInView() {
        return m_runningInView;
    }

    /**
     * @param runningInView the runningInView to set
     */
    public void setRunningInView(final boolean runningInView) {
        m_runningInView = runningInView;
    }

    /**
     * @return the table
     */
    public JSONDataTable getTable() {
        return m_table;
    }

    /**
     * @param table the table to set
     */
    public void setTable(final JSONDataTable table) {
        m_table = table;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return m_width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(final int width) {
        m_width = width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return m_height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(final int height) {
        m_height = height;
    }

    /**
     * @return the fullscreen
     */
    public boolean isFullscreen() {
        return m_fullscreen;
    }

    /**
     * @param fullscreen the fullscreen to set
     */
    public void setFullscreen(final boolean fullscreen) {
        m_fullscreen = fullscreen;
    }

    /**
     * @return the enableViewControls
     */
    public boolean isEnableViewControls() {
        return m_enableViewControls;
    }

    /**
     * @param enableViewControls the enableViewControls to set
     */
    public void setEnableViewControls(final boolean enableViewControls) {
        m_enableViewControls = enableViewControls;
    }

    /**
     * @return the showLegend
     */
    public boolean isShowLegend() {
        return m_showLegend;
    }

    /**
     * @param showLegend the showLegend to set
     */
    public void setShowLegend(final boolean showLegend) {
        m_showLegend = showLegend;
    }

    /**
     * @return the enableTitleEdit
     */
    public boolean isEnableTitleEdit() {
        return m_enableTitleEdit;
    }

    /**
     * @param enableTitleEdit the enableTitleEdit to set
     */
    public void setEnableTitleEdit(final boolean enableTitleEdit) {
        m_enableTitleEdit = enableTitleEdit;
    }

    /**
     * @return the enableSubitleEdit
     */
    public boolean isEnableSubtitleEdit() {
        return m_enableSubtitleEdit;
    }

    /**
     * @param enableSubitleEdit the enableSubitleEdit to set
     */
    public void setEnableSubtitleEdit(final boolean enableSubitleEdit) {
        m_enableSubtitleEdit = enableSubitleEdit;
    }

    /**
     * @return the timeCol
     */
    public String getTimeCol() {
        return m_timeCol;
    }

    /**
     * @param timeCol the timeCol to set
     */
    public void setTimeCol(final String timeCol) {
        m_timeCol = timeCol;
    }

    /**
     * @return the eventCol
     */
    public String getEventCol() {
        return m_eventCol;
    }

    /**
     * @param eventCol the eventCol to set
     */
    public void setEventCol(final String eventCol) {
        m_eventCol = eventCol;
    }

    /**
     * @return the groupCol
     */
    public String getGroupCol() {
        return m_groupCol;
    }

    /**
     * @param groupCol the groupCol to set
     */
    public void setGroupCol(final String groupCol) {
        m_groupCol = groupCol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        m_table.saveJSONToNodeSettings(settings);
        settings.addInt(WIDTH_CFG, m_width);
        settings.addInt(HEIGHT_CFG, m_height);
        settings.addBoolean(FULLSCREEN_CFG, m_fullscreen);
        settings.addBoolean(ENABLE_VIEW_CTRLS_CFG, m_enableViewControls);
        settings.addBoolean(ENABLE_SUBTITLE_EDIT_CFG, m_enableSubtitleEdit);
        settings.addBoolean(ENABLE_TITLE_EDIT_CFG, m_enableTitleEdit);
        settings.addBoolean(SHOW_LEGEND_CFG, m_showLegend);
        settings.addString(TIME_COL_CFG, m_timeCol);
        settings.addString(EVENT_COL_CFG, m_eventCol);
        settings.addString(GROUP_COL_CFG, m_groupCol);
        settings.addBoolean(RUNNING_IN_VIEW_CFG, m_runningInView);
        settings.addBoolean(FULLSCREEN_BTN_CFG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        JSONDataTable.loadFromNodeSettings(settings);
        m_width = settings.getInt(WIDTH_CFG);
        m_height = settings.getInt(HEIGHT_CFG);
        m_fullscreen = settings.getBoolean(FULLSCREEN_CFG);
        m_enableViewControls = settings.getBoolean(ENABLE_VIEW_CTRLS_CFG);
        m_enableSubtitleEdit = settings.getBoolean(ENABLE_SUBTITLE_EDIT_CFG);
        m_enableTitleEdit = settings.getBoolean(ENABLE_TITLE_EDIT_CFG);
        m_showLegend = settings.getBoolean(SHOW_LEGEND_CFG);
        m_timeCol = settings.getString(TIME_COL_CFG);
        m_eventCol = settings.getString(EVENT_COL_CFG);
        m_groupCol = settings.getString(GROUP_COL_CFG);
        m_runningInView = settings.getBoolean(RUNNING_IN_VIEW_CFG);
        m_displayFullscreenButton = settings.getBoolean(FULLSCREEN_BTN_CFG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof KaplanMeierViewRepresentation)) {
            return false;
        }
        KaplanMeierViewRepresentation other = (KaplanMeierViewRepresentation)obj;
        return new EqualsBuilder()
                .append(m_table, other.m_table)
                .append(m_width, other.m_width)
                .append(m_height, other.m_height)
                .append(m_fullscreen, other.m_fullscreen)
                .append(m_enableViewControls, other.m_enableViewControls)
                .append(m_enableSubtitleEdit, other.m_enableSubtitleEdit)
                .append(m_enableTitleEdit, other.m_enableTitleEdit)
                .append(m_showLegend, other.m_showLegend)
                .append(m_timeCol, other.m_timeCol)
                .append(m_eventCol, other.m_eventCol)
                .append(m_groupCol, other.m_groupCol)
                .append(m_runningInView, other.m_runningInView)
                .append(m_displayFullscreenButton, other.m_displayFullscreenButton)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(m_table)
                .append(m_width)
                .append(m_height)
                .append(m_fullscreen)
                .append(m_enableViewControls)
                .append(m_enableSubtitleEdit)
                .append(m_enableTitleEdit)
                .append(m_showLegend)
                .append(m_timeCol)
                .append(m_eventCol)
                .append(m_groupCol)
                .append(m_runningInView)
                .append(m_displayFullscreenButton)
                .build();
    }

}
