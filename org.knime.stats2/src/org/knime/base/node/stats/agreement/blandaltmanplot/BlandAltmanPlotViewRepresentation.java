/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   13.05.2014 (Christian Albrecht, KNIME.com AG, Zurich, Switzerland): created
 */
package org.knime.base.node.stats.agreement.blandaltmanplot;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.JSONViewContent;
import org.knime.js.core.datasets.JSONKeyedValues2DDataset;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland, University of Konstanz
 * @author Patrick Winter, University of Konstanz, Germany
 */
@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class BlandAltmanPlotViewRepresentation extends JSONViewContent {

    static final String UPPER_LIMIT = "upperLimit";

    static final String BIAS = "bias";

    static final String LOWER_LIMIT = "lowerLimit";

    static final String RESIZE_TO_WINDOW = "resizeToWindow";

    private JSONKeyedValues2DDataset m_keyedDataset;

    private double m_upperLimit;

    private double m_bias;

    private double m_lowerLimit;

    private boolean m_enableDotSizeChange;

    private boolean m_enableZooming;

    private boolean m_enableDragZooming;

    private boolean m_enablePanning;

    private boolean m_showZoomResetButton;

    private boolean m_resizeToWindow;

    private int m_imageWidth;

    private int m_imageHeight;

    /**
     * @return the keyedDataset
     */
    public JSONKeyedValues2DDataset getKeyedDataset() {
        return m_keyedDataset;
    }

    /**
     * @param keyedDataset the keyedDataset to set
     */
    public void setKeyedDataset(final JSONKeyedValues2DDataset keyedDataset) {
        m_keyedDataset = keyedDataset;
    }

    /**
     * @return the upperLimit
     */
    public double getUpperLimit() {
        return m_upperLimit;
    }

    /**
     * @param upperLimit the upperLimit to set
     */
    public void setUpperLimit(final double upperLimit) {
        m_upperLimit = upperLimit;
    }

    /**
     * @return the bias
     */
    public double getBias() {
        return m_bias;
    }

    /**
     * @param bias the bias to set
     */
    public void setBias(final double bias) {
        m_bias = bias;
    }

    /**
     * @return the lowerLimit
     */
    public double getLowerLimit() {
        return m_lowerLimit;
    }

    /**
     * @param lowerLimit the lowerLimit to set
     */
    public void setLowerLimit(final double lowerLimit) {
        m_lowerLimit = lowerLimit;
    }

    /**
     * @return the allowDotSizeChange
     */
    public boolean getEnableDotSizeChange() {
        return m_enableDotSizeChange;
    }

    /**
     * @param enableDotSizeChange the allowDotSizeChange to set
     */
    public void setEnableDotSizeChange(final boolean enableDotSizeChange) {
        m_enableDotSizeChange = enableDotSizeChange;
    }

    /**
     * @return the allowZooming
     */
    public boolean getEnableZooming() {
        return m_enableZooming;
    }

    /**
     * @param enableZooming the allowZooming to set
     */
    public void setEnableZooming(final boolean enableZooming) {
        m_enableZooming = enableZooming;
    }

    /**
     * @return the enableDragZooming
     */
    public boolean getEnableDragZooming() {
        return m_enableDragZooming;
    }

    /**
     * @param enableDragZooming the enableDragZooming to set
     */
    public void setEnableDragZooming(final boolean enableDragZooming) {
        m_enableDragZooming = enableDragZooming;
    }

    /**
     * @return the allowPanning
     */
    public boolean getEnablePanning() {
        return m_enablePanning;
    }

    /**
     * @param enablePanning the allowPanning to set
     */
    public void setEnablePanning(final boolean enablePanning) {
        m_enablePanning = enablePanning;
    }

    /**
     * @return the showZoomResetButton
     */
    public boolean getShowZoomResetButton() {
        return m_showZoomResetButton;
    }

    /**
     * @param showZoomResetButton the showZoomResetButton to set
     */
    public void setShowZoomResetButton(final boolean showZoomResetButton) {
        m_showZoomResetButton = showZoomResetButton;
    }

    /**
     * @return the resizeToWindow
     */
    public boolean getResizeToWindow() {
        return m_resizeToWindow;
    }

    /**
     * @param resizeToWindow the resizeToWindow to set
     */
    public void setResizeToWindow(final boolean resizeToWindow) {
        m_resizeToWindow = resizeToWindow;
    }

    /**
     * @return the imageWidth
     */
    public int getImageWidth() {
        return m_imageWidth;
    }

    /**
     * @param imageWidth the imageWidth to set
     */
    public void setImageWidth(final int imageWidth) {
        m_imageWidth = imageWidth;
    }

    /**
     * @return the imageHeight
     */
    public int getImageHeight() {
        return m_imageHeight;
    }

    /**
     * @param imageHeight the imageHeight to set
     */
    public void setImageHeight(final int imageHeight) {
        m_imageHeight = imageHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        settings.addBoolean(BlandAltmanPlotViewConfig.ENABLE_DOT_SIZE_CHANGE, getEnableDotSizeChange());
        settings.addBoolean(BlandAltmanPlotViewConfig.ENABLE_ZOOMING, getEnableZooming());
        settings.addBoolean(BlandAltmanPlotViewConfig.ENABLE_DRAG_ZOOMING, getEnableDragZooming());
        settings.addBoolean(BlandAltmanPlotViewConfig.ENABLE_PANNING, getEnablePanning());
        settings.addBoolean(BlandAltmanPlotViewConfig.SHOW_ZOOM_RESET_BUTTON, getShowZoomResetButton());
        settings.addBoolean("hasDataset", m_keyedDataset != null);
        if (m_keyedDataset != null) {
            NodeSettingsWO datasetSettings = settings.addNodeSettings("dataset");
            m_keyedDataset.saveToNodeSettings(datasetSettings);
            settings.addDouble(UPPER_LIMIT, m_upperLimit);
            settings.addDouble(BIAS, m_bias);
            settings.addDouble(LOWER_LIMIT, m_lowerLimit);
        }
        settings.addBoolean(RESIZE_TO_WINDOW, m_resizeToWindow);
        settings.addInt(BlandAltmanPlotViewConfig.IMAGE_WIDTH, m_imageWidth);
        settings.addInt(BlandAltmanPlotViewConfig.IMAGE_HEIGHT, m_imageHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        setEnableDotSizeChange(settings.getBoolean(BlandAltmanPlotViewConfig.ENABLE_DOT_SIZE_CHANGE));
        setEnableZooming(settings.getBoolean(BlandAltmanPlotViewConfig.ENABLE_ZOOMING));
        setEnableDragZooming(settings.getBoolean(BlandAltmanPlotViewConfig.ENABLE_DRAG_ZOOMING));
        setEnablePanning(settings.getBoolean(BlandAltmanPlotViewConfig.ENABLE_PANNING));
        setShowZoomResetButton(settings.getBoolean(BlandAltmanPlotViewConfig.SHOW_ZOOM_RESET_BUTTON));
        m_keyedDataset = null;
        boolean hasDataset = settings.getBoolean("hasDataset");
        if (hasDataset) {
            NodeSettingsRO datasetSettings = settings.getNodeSettings("dataset");
            m_keyedDataset = new JSONKeyedValues2DDataset();
            m_keyedDataset.loadFromNodeSettings(datasetSettings);
            m_upperLimit = settings.getDouble(UPPER_LIMIT);
            m_bias = settings.getDouble(BIAS);
            m_lowerLimit = settings.getDouble(LOWER_LIMIT);
        }
        setResizeToWindow(settings.getBoolean(RESIZE_TO_WINDOW));
        setImageWidth(settings.getInt(BlandAltmanPlotViewConfig.IMAGE_WIDTH));
        setImageHeight(settings.getInt(BlandAltmanPlotViewConfig.IMAGE_HEIGHT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        BlandAltmanPlotViewRepresentation other = (BlandAltmanPlotViewRepresentation)obj;
        return new EqualsBuilder().append(m_keyedDataset, other.m_keyedDataset)
            .append(m_enableDotSizeChange, other.m_enableDotSizeChange).append(m_enableZooming, other.m_enableZooming)
            .append(m_enableDragZooming, other.m_enableDragZooming).append(m_enablePanning, other.m_enablePanning)
            .append(m_showZoomResetButton, other.m_showZoomResetButton).append(m_upperLimit, other.m_upperLimit)
            .append(m_bias, other.m_bias).append(m_lowerLimit, other.m_lowerLimit)
            .append(m_resizeToWindow, other.m_resizeToWindow).append(m_imageWidth, other.m_imageWidth)
            .append(m_imageHeight, other.m_imageHeight).isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_keyedDataset).append(m_enableDotSizeChange).append(m_enableZooming)
            .append(m_enableDragZooming).append(m_enablePanning).append(m_showZoomResetButton).append(m_upperLimit)
            .append(m_bias).append(m_lowerLimit).append(m_resizeToWindow).append(m_imageWidth).append(m_imageHeight)
            .toHashCode();
    }
}
