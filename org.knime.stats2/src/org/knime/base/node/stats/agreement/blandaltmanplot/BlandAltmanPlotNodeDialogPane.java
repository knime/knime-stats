/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   13.05.2014 (Christian Albrecht, KNIME.com AG, Zurich, Switzerland): created
 */
package org.knime.base.node.stats.agreement.blandaltmanplot;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland, University of Konstanz
 * @author Patrick Winter, University of Konstanz, Germany
 */
public class BlandAltmanPlotNodeDialogPane extends NodeDialogPane {

    private final JCheckBox m_allowZoomingCheckBox;

    private final JCheckBox m_allowDragZoomingCheckBox;

    private final JCheckBox m_allowPanningCheckBox;

    private final JCheckBox m_showZoomResetCheckBox;

    private final JCheckBox m_enableDotSizeChangeCheckBox;

    private final JCheckBox m_logScale;

    private final ColumnSelectionPanel m_measurement1ColComboBox;

    private final ColumnSelectionPanel m_measurement2ColComboBox;

    private final JSpinner m_dotSize;

    private final JSpinner m_imageWidth;

    private final JSpinner m_imageHeight;

    /**
     */
    @SuppressWarnings("unchecked")
    public BlandAltmanPlotNodeDialogPane() {
        m_enableDotSizeChangeCheckBox = new JCheckBox("Enable dot size edit");
        m_allowZoomingCheckBox = new JCheckBox("Enable mouse wheel zooming");
        m_allowDragZoomingCheckBox = new JCheckBox("Enable drag zooming");
        m_allowPanningCheckBox = new JCheckBox("Enable panning");
        m_showZoomResetCheckBox = new JCheckBox("Show zoom reset button");
        m_logScale = new JCheckBox("Scale data with logarithm (base 2)");
        m_measurement1ColComboBox =
            new ColumnSelectionPanel("Measurement 1 column", DoubleValue.class);
        m_measurement2ColComboBox =
            new ColumnSelectionPanel("Measurement 2 column", DoubleValue.class);
        m_dotSize = new JSpinner();
        m_imageWidth = new JSpinner(new SpinnerNumberModel(800, 1, Integer.MAX_VALUE, 1));
        m_imageHeight = new JSpinner(new SpinnerNumberModel(600, 1, Integer.MAX_VALUE, 1));
        addTab("Options", createOptionsTab());
        addTab("View Controls", createViewControlsTab());
        addTab("Image options", createImageOptionsTab());
    }

    /**
     * @return Panel for the options tab
     */
    private Component createOptionsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        m_measurement1ColComboBox.setPreferredSize(new Dimension(260, 50));
        panel.add(m_measurement1ColComboBox, c);
        c.gridy++;
        m_measurement2ColComboBox.setPreferredSize(new Dimension(260, 50));
        panel.add(m_measurement2ColComboBox, c);
        c.gridy++;
        panel.add(m_logScale, c);
        return panel;
    }

    /**
     * @return Panel for the view controls tab
     */
    private Component createViewControlsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        panel.add(m_allowPanningCheckBox, c);
        c.gridy++;
        panel.add(m_allowZoomingCheckBox, c);
        c.gridy++;
        panel.add(m_allowDragZoomingCheckBox, c);
        c.gridy++;
        panel.add(m_showZoomResetCheckBox, c);
        return panel;
    }

    /**
     * @return Panel for the image options tab
     */
    private Component createImageOptionsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel("Image width:"), c);
        c.gridx++;
        c.weightx = 1;
        panel.add(m_imageWidth, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        panel.add(new JLabel("Image height:"), c);
        c.weightx = 1;
        c.gridx++;
        panel.add(m_imageHeight, c);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        BlandAltmanPlotViewConfig config = new BlandAltmanPlotViewConfig();
        config.loadSettingsForDialog(settings);
        m_enableDotSizeChangeCheckBox.setSelected(config.getEnableDotSizeChange());
        m_allowZoomingCheckBox.setSelected(config.getEnableZooming());
        m_allowDragZoomingCheckBox.setSelected(config.getEnableDragZooming());
        m_allowPanningCheckBox.setSelected(config.getEnablePanning());
        m_showZoomResetCheckBox.setSelected(config.getShowZoomResetButton());
        String xCol = config.getMeasurement1Column();
        if (xCol == null || xCol.isEmpty()) {
            xCol = specs[0].getColumnNames()[0];
        }
        String yCol = config.getMeasurement2Column();
        if (yCol == null || yCol.isEmpty()) {
            yCol = specs[0].getColumnNames()[specs[0].getNumColumns() > 1 ? 1 : 0];
        }
        m_measurement1ColComboBox.update(specs[0], xCol);
        m_measurement2ColComboBox.update(specs[0], yCol);
        m_dotSize.setValue(config.getDotSize());
        m_logScale.setSelected(config.getLogScale());
        m_imageWidth.setValue(config.getImageWidth());
        m_imageHeight.setValue(config.getImageHeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        BlandAltmanPlotViewConfig config = new BlandAltmanPlotViewConfig();
        config.setEnableDotSizeChange(m_enableDotSizeChangeCheckBox.isSelected());
        config.setEnableZooming(m_allowZoomingCheckBox.isSelected());
        config.setEnableDragZooming(m_allowDragZoomingCheckBox.isSelected());
        config.setEnablePanning(m_allowPanningCheckBox.isSelected());
        config.setShowZoomResetButton(m_showZoomResetCheckBox.isSelected());
        config.setMeasurement1Column(m_measurement1ColComboBox.getSelectedColumn());
        config.setMeasurement2Column(m_measurement2ColComboBox.getSelectedColumn());
        config.setDotSize((Integer)m_dotSize.getValue());
        config.setLogScale(m_logScale.isSelected());
        config.setImageWidth((int)m_imageWidth.getValue());
        config.setImageHeight((int)m_imageHeight.getValue());
        config.saveSettings(settings);
    }

}
