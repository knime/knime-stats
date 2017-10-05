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
 *   14.05.2014 (Christian Albrecht, KNIME.com AG, Zurich, Switzerland): created
 */
package org.knime.base.node.stats.agreement.blandaltmanplot;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland, University of Konstanz
 * @author Patrick Winter, University of Konstanz, Germany
 */
final class BlandAltmanPlotViewConfig {

    static final String HIDE_IN_WIZARD = "hideInWizard";

    static final String ENABLE_DOT_SIZE_CHANGE = "enableDotSizeChange";

    static final String ENABLE_ZOOMING = "enableZooming";

    static final String ENABLE_DRAG_ZOOMING = "enableDragZooming";

    static final String ENABLE_PANNING = "enablePanning";

    static final String SHOW_ZOOM_RESET_BUTTON = "showZoomResetButton";

    static final String MEASUREMENT_1_COL = "measurement1Col";

    static final String MEASUREMENT_2_COL = "measurement2Col";

    static final String X_AXIS_MIN = "xAxisMin";

    static final String X_AXIS_MAX = "xAxisMax";

    static final String Y_AXIS_MIN = "yAxisMin";

    static final String Y_AXIS_MAX = "yAxisMax";

    static final String DOT_SIZE = "dot_size";

    static final String LOG_SCALE = "logScale";

    static final String IMAGE_WIDTH = "imageWidth";

    static final String IMAGE_HEIGHT = "imageHeight";

    private boolean m_hideInWizard = false;

    private boolean m_enableDotSizeChange = false;

    private boolean m_enableZooming = true;

    private boolean m_enablePanning = true;

    private boolean m_enableDragZooming = false;

    private boolean m_showZoomResetButton = false;

    private String m_measurement1Column;

    private String m_measurement2Column;

    private Double m_xAxisMin;

    private Double m_xAxisMax;

    private Double m_yAxisMin;

    private Double m_yAxisMax;

    private Integer m_dotSize = 3;

    private boolean m_logScale = false;

    private int m_imageWidth = 800;

    private int m_imageHeight = 600;

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
     * @return the measurement1Column
     */
    public String getMeasurement1Column() {
        return m_measurement1Column;
    }

    /**
     * @param measurement1Column the measurement1Column to set
     */
    public void setMeasurement1Column(final String measurement1Column) {
        m_measurement1Column = measurement1Column;
    }

    /**
     * @return the measurement2Column
     */
    public String getMeasurement2Column() {
        return m_measurement2Column;
    }

    /**
     * @param measurement2Column the measurement2Column to set
     */
    public void setMeasurement2Column(final String measurement2Column) {
        m_measurement2Column = measurement2Column;
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
     * @return the xAxisMin
     */
    public Double getxAxisMin() {
        return m_xAxisMin;
    }

    /**
     * @param xAxisMin the xAxisMin to set
     */
    public void setxAxisMin(final Double xAxisMin) {
        m_xAxisMin = xAxisMin;
    }

    /**
     * @return the xAxisMax
     */
    public Double getxAxisMax() {
        return m_xAxisMax;
    }

    /**
     * @param xAxisMax the xAxisMax to set
     */
    public void setxAxisMax(final Double xAxisMax) {
        m_xAxisMax = xAxisMax;
    }

    /**
     * @return the yAxisMin
     */
    public Double getyAxisMin() {
        return m_yAxisMin;
    }

    /**
     * @param yAxisMin the yAxisMin to set
     */
    public void setyAxisMin(final Double yAxisMin) {
        m_yAxisMin = yAxisMin;
    }

    /**
     * @return the yAxisMax
     */
    public Double getyAxisMax() {
        return m_yAxisMax;
    }

    /**
     * @param yAxisMax the yAxisMax to set
     */
    public void setyAxisMax(final Double yAxisMax) {
        m_yAxisMax = yAxisMax;
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
     * @return the dotSize
     */
    public Integer getDotSize() {
        return m_dotSize;
    }

    /**
     * @param dotSize the dotSize to set
     */
    public void setDotSize(final Integer dotSize) {
        m_dotSize = dotSize;
    }

    /**
     * @return the logScale
     */
    public boolean getLogScale() {
        return m_logScale;
    }

    /**
     * @param logScale the logScale to set
     */
    public void setLogScale(final boolean logScale) {
        m_logScale = logScale;
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
     * Saves current parameters to settings object.
     *
     * @param settings To save to.
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean(ENABLE_DOT_SIZE_CHANGE, getEnableDotSizeChange());
        settings.addBoolean(ENABLE_ZOOMING, getEnableZooming());
        settings.addBoolean(ENABLE_DRAG_ZOOMING, getEnableDragZooming());
        settings.addBoolean(ENABLE_PANNING, getEnablePanning());
        settings.addBoolean(SHOW_ZOOM_RESET_BUTTON, getShowZoomResetButton());
        settings.addString(MEASUREMENT_1_COL, getMeasurement1Column());
        settings.addString(MEASUREMENT_2_COL, getMeasurement2Column());
        settings.addString(X_AXIS_MIN, getxAxisMin() == null ? null : getxAxisMin().toString());
        settings.addString(X_AXIS_MAX, getxAxisMax() == null ? null : getxAxisMax().toString());
        settings.addString(Y_AXIS_MIN, getyAxisMin() == null ? null : getyAxisMin().toString());
        settings.addString(Y_AXIS_MAX, getyAxisMax() == null ? null : getyAxisMax().toString());
        settings.addString(DOT_SIZE, getDotSize() == null ? null : getDotSize().toString());
        settings.addBoolean(LOG_SCALE, getLogScale());
        settings.addInt(IMAGE_WIDTH, m_imageWidth);
        settings.addInt(IMAGE_HEIGHT, m_imageHeight);

        //added with 3.5
        settings.addBoolean(HIDE_IN_WIZARD, m_hideInWizard);
    }

    /**
     * Loads parameters in NodeModel.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        setEnableDotSizeChange(settings.getBoolean(ENABLE_DOT_SIZE_CHANGE));
        setEnableZooming(settings.getBoolean(ENABLE_ZOOMING));
        setEnableDragZooming(settings.getBoolean(ENABLE_DRAG_ZOOMING));
        setEnablePanning(settings.getBoolean(ENABLE_PANNING));
        setShowZoomResetButton(settings.getBoolean(SHOW_ZOOM_RESET_BUTTON));
        setMeasurement1Column(settings.getString(MEASUREMENT_1_COL));
        setMeasurement2Column(settings.getString(MEASUREMENT_2_COL));
        String xMin = settings.getString(X_AXIS_MIN);
        String xMax = settings.getString(X_AXIS_MAX);
        String yMin = settings.getString(Y_AXIS_MIN);
        String yMax = settings.getString(Y_AXIS_MAX);
        String dotSize = settings.getString(DOT_SIZE);
        setxAxisMin(xMin == null ? null : Double.parseDouble(xMin));
        setxAxisMax(xMax == null ? null : Double.parseDouble(xMax));
        setyAxisMin(yMin == null ? null : Double.parseDouble(yMin));
        setyAxisMax(yMax == null ? null : Double.parseDouble(yMax));
        setDotSize(dotSize == null ? null : Integer.parseInt(dotSize));
        setLogScale(settings.getBoolean(LOG_SCALE));
        setImageWidth(settings.getInt(IMAGE_WIDTH));
        setImageHeight(settings.getInt(IMAGE_HEIGHT));

        //added with 3.5
        setHideInWizard(settings.getBoolean(HIDE_IN_WIZARD, false));
    }

    /**
     * Loads parameters in Dialog.
     *
     * @param settings To load from.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        setEnableDotSizeChange(settings.getBoolean(ENABLE_DOT_SIZE_CHANGE, false));
        setEnableZooming(settings.getBoolean(ENABLE_ZOOMING, true));
        setEnableDragZooming(settings.getBoolean(ENABLE_DRAG_ZOOMING, false));
        setEnablePanning(settings.getBoolean(ENABLE_PANNING, true));
        setShowZoomResetButton(settings.getBoolean(SHOW_ZOOM_RESET_BUTTON, false));
        setMeasurement1Column(settings.getString(MEASUREMENT_1_COL, null));
        setMeasurement2Column(settings.getString(MEASUREMENT_2_COL, null));
        String xMin = settings.getString(X_AXIS_MIN, null);
        String xMax = settings.getString(X_AXIS_MAX, null);
        String yMin = settings.getString(Y_AXIS_MIN, null);
        String yMax = settings.getString(Y_AXIS_MAX, null);
        String dotSize = settings.getString(DOT_SIZE, "3");
        setxAxisMin(xMin == null ? null : Double.parseDouble(xMin));
        setxAxisMax(xMax == null ? null : Double.parseDouble(xMax));
        setyAxisMin(yMin == null ? null : Double.parseDouble(yMin));
        setyAxisMax(yMax == null ? null : Double.parseDouble(yMax));
        setDotSize(dotSize == null ? null : Integer.parseInt(dotSize));
        setLogScale(settings.getBoolean(LOG_SCALE, false));
        setImageWidth(settings.getInt(IMAGE_WIDTH, 800));
        setImageHeight(settings.getInt(IMAGE_HEIGHT, 600));

        //added with 3.5
        setHideInWizard(settings.getBoolean(HIDE_IN_WIZARD, false));
    }
}
