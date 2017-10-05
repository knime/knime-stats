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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.xml.SvgCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.wizard.WizardViewCreator;
import org.knime.js.core.JavaScriptViewCreator;
import org.knime.js.core.datasets.JSONKeyedValues2DDataset;
import org.knime.js.core.datasets.JSONKeyedValuesRow;
import org.knime.js.core.node.AbstractSVGWizardNodeModel;

/**
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland, University of Konstanz
 * @author Patrick Winter, University of Konstanz, Germany
 */
final class BlandAltmanPlotNodeModel
    extends AbstractSVGWizardNodeModel<BlandAltmanPlotViewRepresentation, BlandAltmanPlotViewValue> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BlandAltmanPlotNodeModel.class);

    private final Object m_lock = new Object();

    private final BlandAltmanPlotViewConfig m_config;

    private BlandAltmanPlotViewRepresentation m_representation;

    private BlandAltmanPlotViewValue m_viewValue;

    private BufferedDataTable m_outTable1;

    private BufferedDataTable m_outTable2;

    BlandAltmanPlotNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{ImagePortObject.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE}, "Bland-Altman Plot");
        m_config = new BlandAltmanPlotViewConfig();
        m_representation = createEmptyViewRepresentation();
        m_viewValue = createEmptyViewValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec tableSpec = (DataTableSpec) inSpecs[0];
        String measurement1Column = m_config.getMeasurement1Column();
        if (measurement1Column == null || measurement1Column.isEmpty()) {
            throw new InvalidSettingsException("Measurement 1 column is not configured");
        }
        int column1Index = tableSpec.findColumnIndex(measurement1Column);
        if (column1Index < 0) {
            throw new InvalidSettingsException("The column '" + measurement1Column + "' is not available");
        }
        if (!tableSpec.getColumnSpec(column1Index).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("The column '" + measurement1Column + "' is not numeric");
        }
        String measurement2Column = m_config.getMeasurement2Column();
        if (measurement2Column == null || measurement2Column.isEmpty()) {
            throw new InvalidSettingsException("Measurement 2 column is not configured");
        }
        int column2Index = tableSpec.findColumnIndex(measurement2Column);
        if (column2Index < 0) {
            throw new InvalidSettingsException("The column '" + measurement2Column + "' is not available");
        }
        if (!tableSpec.getColumnSpec(column2Index).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("The column '" + measurement2Column + "' is not numeric");
        }
        if (column1Index == column2Index) {
            throw new InvalidSettingsException("Can't select the same column for both measurements");
        }
        return new PortObjectSpec[]{new ImagePortObjectSpec(SvgCell.TYPE), createOutput1Spec(), createOutput2Spec()};
    }

    private DataTableSpec createOutput1Spec() {
        DataColumnSpec[] colSpecs = new DataColumnSpec[4];
        colSpecs[0] = new DataColumnSpecCreator("Measurement 1", DoubleCell.TYPE).createSpec();
        colSpecs[1] = new DataColumnSpecCreator("Measurement 2", DoubleCell.TYPE).createSpec();
        colSpecs[2] = new DataColumnSpecCreator("Mean", DoubleCell.TYPE).createSpec();
        colSpecs[3] = new DataColumnSpecCreator("Difference", DoubleCell.TYPE).createSpec();
        return new DataTableSpec(colSpecs);
    }

    private DataTableSpec createOutput2Spec() {
        DataColumnSpec[] colSpecs = new DataColumnSpec[3];
        colSpecs[0] = new DataColumnSpecCreator("Bias", DoubleCell.TYPE).createSpec();
        colSpecs[1] = new DataColumnSpecCreator("Upper limit of agreement", DoubleCell.TYPE).createSpec();
        colSpecs[2] = new DataColumnSpecCreator("Lower limit of agreement", DoubleCell.TYPE).createSpec();
        return new DataTableSpec(colSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationError validateViewValue(final BlandAltmanPlotViewValue viewContent) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadViewValue(final BlandAltmanPlotViewValue viewValue, final boolean useAsDefault) {
        synchronized (m_lock) {
            m_viewValue = viewValue;
            if (useAsDefault) {
                copyValueToConfig();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlandAltmanPlotViewRepresentation getViewRepresentation() {
        synchronized (m_lock) {
            return m_representation;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlandAltmanPlotViewValue getViewValue() {
        synchronized (m_lock) {
            return m_viewValue;
        }
    }

    private void copyConfigToView() {
        m_representation.setEnableDotSizeChange(m_config.getEnableDotSizeChange());
        m_representation.setEnableZooming(m_config.getEnableZooming());
        m_representation.setEnableDragZooming(m_config.getEnableDragZooming());
        m_representation.setEnablePanning(m_config.getEnablePanning());
        m_representation.setShowZoomResetButton(m_config.getShowZoomResetButton());
        m_representation.setImageWidth(m_config.getImageWidth());
        m_representation.setImageHeight(m_config.getImageHeight());
        m_viewValue.setxAxisLabel(getXAxisLabel());
        m_viewValue.setyAxisLabel(getYAxisLabel());
        m_viewValue.setxColumn("Mean");
        m_viewValue.setyColumn("Difference");
        m_viewValue.setDotSize(m_config.getDotSize());
    }

    private void copyValueToConfig() {
        m_config.setMeasurement1Column(m_viewValue.getxColumn());
        m_config.setMeasurement2Column(m_viewValue.getyColumn());
        m_config.setxAxisMin(m_viewValue.getxAxisMin());
        m_config.setxAxisMax(m_viewValue.getxAxisMax());
        m_config.setyAxisMin(m_viewValue.getyAxisMin());
        m_config.setyAxisMax(m_viewValue.getyAxisMax());
        m_config.setDotSize(m_viewValue.getDotSize());
    }

    private String getXAxisLabel() {
        if (m_config.getLogScale()) {
            return "(log₂ " + m_config.getMeasurement1Column() + " + log₂ " + m_config.getMeasurement2Column() + ") ÷ 2";
        } else {
            return "(" + m_config.getMeasurement1Column() + " + " + m_config.getMeasurement2Column() + ") ÷ 2";
        }
    }

    private String getYAxisLabel() {
        if (m_config.getLogScale()) {
            return "log₂ " + m_config.getMeasurement1Column() + " - log₂ " + m_config.getMeasurement2Column();
        } else {
            return m_config.getMeasurement1Column() + " - " + m_config.getMeasurement2Column();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlandAltmanPlotViewRepresentation createEmptyViewRepresentation() {
        return new BlandAltmanPlotViewRepresentation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlandAltmanPlotViewValue createEmptyViewValue() {
        return new BlandAltmanPlotViewValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavascriptObjectID() {
        return "org_knime_base_node_stats_agreement_blandaltmanplot";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        File repFile = new File(nodeInternDir, "representation.xml");
        File valFile = new File(nodeInternDir, "value.xml");
        NodeSettingsRO repSettings = NodeSettings.loadFromXML(new FileInputStream(repFile));
        NodeSettingsRO valSettings = NodeSettings.loadFromXML(new FileInputStream(valFile));
        m_representation = createEmptyViewRepresentation();
        m_viewValue = createEmptyViewValue();
        try {
            m_representation.loadFromNodeSettings(repSettings);
            m_viewValue.loadFromNodeSettings(valSettings);
        } catch (InvalidSettingsException e) {
            LOGGER.error("Error loading internals: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        NodeSettings repSettings = new NodeSettings("scatterPlotViewRepresentation");
        NodeSettings valSettings = new NodeSettings("scatterPlotViewValue");
        if (m_representation != null) {
            m_representation.saveToNodeSettings(repSettings);
        }
        if (m_viewValue != null) {
            m_viewValue.saveToNodeSettings(valSettings);
        }
        File repFile = new File(nodeInternDir, "representation.xml");
        File valFile = new File(nodeInternDir, "value.xml");
        repSettings.saveToXML(new FileOutputStream(repFile));
        valSettings.saveToXML(new FileOutputStream(valFile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new BlandAltmanPlotViewConfig().loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHideInWizard() {
        return m_config.getHideInWizard();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHideInWizard(final boolean hide) {
        m_config.setHideInWizard(hide);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveCurrentValue(final NodeSettingsWO content) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WizardViewCreator<BlandAltmanPlotViewRepresentation, BlandAltmanPlotViewValue> getViewCreator() {
        return new JavaScriptViewCreator<>(getJavascriptObjectID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performExecuteCreateView(final PortObject[] inObjects, final ExecutionContext exec)
        throws Exception {
        BufferedDataContainer container1 = exec.createDataContainer(createOutput1Spec());
        BufferedDataContainer container2 = exec.createDataContainer(createOutput2Spec());
        synchronized (m_lock) {
            int measure1Index =
                ((BufferedDataTable)inObjects[0]).getDataTableSpec().findColumnIndex(m_config.getMeasurement1Column());
            int measure2Index =
                ((BufferedDataTable)inObjects[0]).getDataTableSpec().findColumnIndex(m_config.getMeasurement2Column());
            double meanDifference = 0;
            List<JSONKeyedValuesRow> rows = new ArrayList<JSONKeyedValuesRow>();
            double logBase = 2;
            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            for (DataRow row : (BufferedDataTable)inObjects[0]) {
                DataCell measure1Cell = row.getCell(measure1Index);
                DataCell measure2Cell = row.getCell(measure2Index);
                if (measure1Cell.isMissing() || measure2Cell.isMissing()) {
                    setWarningMessage("Found rows with missing measurements. They have been skipped.");
                    continue;
                }
                double measurement1 = ((DoubleValue)measure1Cell).getDoubleValue();
                double measurement2 = ((DoubleValue)measure2Cell).getDoubleValue();
                if (m_config.getLogScale()) {
                    measurement1 = fixedLog(measurement1, logBase);
                    measurement2 = fixedLog(measurement2, logBase);
                }
                double mean = (measurement1 + measurement2) / 2;
                double difference = measurement1 - measurement2;
                meanDifference += difference;
                container1.addRowToTable(new DefaultRow(row.getKey(), new DataCell[]{new DoubleCell(measurement1),
                    new DoubleCell(measurement2), new DoubleCell(mean), new DoubleCell(difference)}));
                rows.add(new JSONKeyedValuesRow(row.getKey().getString(), new Double[]{mean, difference}));
                minX = Math.min(minX, mean);
                maxX = Math.max(maxX, mean);
                minY = Math.min(minY, difference);
                maxY = Math.max(maxY, difference);
            }
            container1.close();
            if (rows.size() < 1) {
                throw new Exception("No valid values have been found.");
            }
            meanDifference /= rows.size();
            double variance = 0;
            for (JSONKeyedValuesRow row : rows) {
                double difference = row.getValues()[1];
                variance += Math.pow(difference - meanDifference, 2);
            }
            variance /= (rows.size() - 1);
            double stdDeviation = Math.sqrt(variance);
            double upperLimit = meanDifference + 1.96 * stdDeviation;
            double bias = meanDifference;
            double lowerLimit = meanDifference - 1.96 * stdDeviation;
            container2.addRowToTable(new DefaultRow("Row0",
                new DataCell[]{new DoubleCell(bias), new DoubleCell(upperLimit), new DoubleCell(lowerLimit)}));
            container2.close();
            pushFlowVariableDouble("Bias", bias);
            pushFlowVariableDouble("Upper limit of agreement", upperLimit);
            pushFlowVariableDouble("Lower limit of agreement", lowerLimit);
            minY = Math.min(minY, lowerLimit);
            maxY = Math.max(maxY, upperLimit);
            JSONKeyedValues2DDataset dataset = new JSONKeyedValues2DDataset(getTableId(0), new String[]{"Mean", "Difference"},
                rows.toArray(new JSONKeyedValuesRow[rows.size()]));
            m_representation.setKeyedDataset(dataset);
            m_representation.setUpperLimit(upperLimit);
            m_representation.setBias(bias);
            m_representation.setLowerLimit(lowerLimit);
            m_representation.setResizeToWindow(false);
            m_viewValue.setxAxisMin(minX);
            m_viewValue.setxAxisMax(maxX);
            m_viewValue.setyAxisMin(minY);
            m_viewValue.setyAxisMax(maxY);
            copyConfigToView();
        }
        exec.setProgress(1);
        m_outTable1 = container1.getTable();
        m_outTable2 = container2.getTable();
        setOptionalViewWaitTime((long)(0.2 * m_outTable1.size()));
    }

    private double fixedLog(final double value, final double logBase) {
        if (value > 1) {
            return Math.log(value) / Math.log(logBase);
        } else if (value < -1) {
            return -1 * (Math.log(-1 * value) / Math.log(logBase));
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] performExecuteCreatePortObjects(final PortObject svgImageFromView,
        final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        m_representation.setResizeToWindow(true);
        BufferedDataTable table1 = m_outTable1;
        BufferedDataTable table2 = m_outTable2;
        m_outTable1 = null;
        m_outTable2 = null;
        return new PortObject[]{svgImageFromView, table1, table2};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean generateImage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performReset() {
        synchronized (m_lock) {
            m_representation = createEmptyViewRepresentation();
            m_viewValue = createEmptyViewValue();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void useCurrentValueAsDefault() {
    }

}
