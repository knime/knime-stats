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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   13.05.2014 (Christian Albrecht, KNIME AG, Zurich, Switzerland): created
 */
package org.knime.base.node.stats.agreement.blandaltmanplot;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.JSONViewContent;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland, University of Konstanz
 * @author Patrick Winter, University of Konstanz, Germany
 */
@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class BlandAltmanPlotViewValue extends JSONViewContent {

    static final String X_AXIS_LABEL = "xAxisLabel";

    static final String Y_AXIS_LABEL = "yAxisLabel";

    private String m_xAxisLabel;

    private String m_yAxisLabel;

    private String m_xColumn;

    private String m_yColumn;

    private Double m_xAxisMin;

    private Double m_xAxisMax;

    private Double m_yAxisMin;

    private Double m_yAxisMax;

    private Integer m_dotSize;

    /**
     * @return the xAxisLabel
     */
    public String getxAxisLabel() {
        return m_xAxisLabel;
    }

    /**
     * @param xAxisLabel the xAxisLabel to set
     */
    public void setxAxisLabel(final String xAxisLabel) {
        m_xAxisLabel = xAxisLabel;
    }

    /**
     * @return the yAxisLabel
     */
    public String getyAxisLabel() {
        return m_yAxisLabel;
    }

    /**
     * @param yAxisLabel the yAxisLabel to set
     */
    public void setyAxisLabel(final String yAxisLabel) {
        m_yAxisLabel = yAxisLabel;
    }

    /**
     * @return the xColumn
     */
    public String getxColumn() {
        return m_xColumn;
    }

    /**
     * @param xColumn the xColumn to set
     */
    public void setxColumn(final String xColumn) {
        m_xColumn = xColumn;
    }

    /**
     * @return the yColumn
     */
    public String getyColumn() {
        return m_yColumn;
    }

    /**
     * @param yColumn the yColumn to set
     */
    public void setyColumn(final String yColumn) {
        m_yColumn = yColumn;
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
     * {@inheritDoc}
     */
    @Override
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        settings.addString(X_AXIS_LABEL, getxAxisLabel());
        settings.addString(Y_AXIS_LABEL, getyAxisLabel());
        settings.addString(BlandAltmanPlotViewConfig.MEASUREMENT_1_COL, getxColumn());
        settings.addString(BlandAltmanPlotViewConfig.MEASUREMENT_2_COL, getyColumn());
        settings.addString(BlandAltmanPlotViewConfig.X_AXIS_MIN,
            getxAxisMin() == null ? null : getxAxisMin().toString());
        settings.addString(BlandAltmanPlotViewConfig.X_AXIS_MAX,
            getxAxisMax() == null ? null : getxAxisMax().toString());
        settings.addString(BlandAltmanPlotViewConfig.Y_AXIS_MIN,
            getyAxisMin() == null ? null : getyAxisMin().toString());
        settings.addString(BlandAltmanPlotViewConfig.Y_AXIS_MAX,
            getyAxisMax() == null ? null : getyAxisMax().toString());
        settings.addString(BlandAltmanPlotViewConfig.DOT_SIZE, getDotSize() == null ? null : getDotSize().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        setxAxisLabel(settings.getString(X_AXIS_LABEL));
        setyAxisLabel(settings.getString(Y_AXIS_LABEL));
        setxColumn(settings.getString(BlandAltmanPlotViewConfig.MEASUREMENT_1_COL));
        setyColumn(settings.getString(BlandAltmanPlotViewConfig.MEASUREMENT_2_COL));
        String xMin = settings.getString(BlandAltmanPlotViewConfig.X_AXIS_MIN);
        String xMax = settings.getString(BlandAltmanPlotViewConfig.X_AXIS_MAX);
        String yMin = settings.getString(BlandAltmanPlotViewConfig.Y_AXIS_MIN);
        String yMax = settings.getString(BlandAltmanPlotViewConfig.Y_AXIS_MAX);
        String dotSize = settings.getString(BlandAltmanPlotViewConfig.DOT_SIZE);
        setxAxisMin(xMin == null ? null : Double.parseDouble(xMin));
        setxAxisMax(xMax == null ? null : Double.parseDouble(xMax));
        setyAxisMin(yMin == null ? null : Double.parseDouble(yMin));
        setyAxisMax(yMax == null ? null : Double.parseDouble(yMax));
        setDotSize(dotSize == null ? null : Integer.parseInt(dotSize));
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
        BlandAltmanPlotViewValue other = (BlandAltmanPlotViewValue)obj;
        return new EqualsBuilder().append(m_xAxisLabel, other.m_xAxisLabel).append(m_yAxisLabel, other.m_yAxisLabel)
            .append(m_xColumn, other.m_xColumn).append(m_yColumn, other.m_yColumn).append(m_xAxisMin, other.m_xAxisMin)
            .append(m_xAxisMax, other.m_xAxisMax).append(m_yAxisMin, other.m_yAxisMin)
            .append(m_yAxisMax, other.m_yAxisMax).append(m_dotSize, other.m_dotSize).isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_xAxisLabel).append(m_yAxisLabel).append(m_xColumn).append(m_yColumn)
            .append(m_xAxisMin).append(m_xAxisMax).append(m_yAxisMin).append(m_yAxisMax).append(m_dotSize).toHashCode();
    }

}
