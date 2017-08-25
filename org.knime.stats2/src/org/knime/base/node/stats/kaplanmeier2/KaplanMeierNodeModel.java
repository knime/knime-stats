package org.knime.base.node.stats.kaplanmeier2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.xml.SvgCell;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.core.node.web.ValidationError;
import org.knime.js.core.JSONDataTable;
import org.knime.js.core.node.AbstractSVGWizardNodeModel;

/**
 * This is the model implementation of PMMLToJavascriptCompiler.
 *
 *
 * @author Alexander Fillbrunn
 */
public class KaplanMeierNodeModel
    extends AbstractSVGWizardNodeModel<KaplanMeierViewRepresentation, KaplanMeierViewValue>  {

    private BufferedDataTable m_table;

    private KaplanMeierConfig m_config = new KaplanMeierConfig();

    /**
     * Constructor for the node model.
     * @param viewName the name of the view this node shows
     */
    protected KaplanMeierNodeModel(final String viewName) {
        super(new PortType[]{BufferedDataTable.TYPE},
              new PortType[]{ImagePortObject.TYPE, BufferedDataTable.TYPE}, viewName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] performExecuteCreatePortObjects(final PortObject svgImageFromView,
        final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        KaplanMeierViewRepresentation representation = getViewRepresentation();
        representation.setTable(new JSONDataTable(m_table, 1, (int)m_table.size(), exec));
        representation.setRunningInView(true);
        exec.setProgress(1);
        return new PortObject[]{svgImageFromView, m_table};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performExecuteCreateView(final PortObject[] inObjects,
        final ExecutionContext exec) throws Exception {
        synchronized (getLock()) {
            boolean reexecuting = m_table != null;
            BufferedDataTable inTable = (BufferedDataTable)inObjects[0];

            int timeCol = inTable.getSpec().findColumnIndex(m_config.getTimeCol().getStringValue());
            int eventCol = inTable.getSpec().findColumnIndex(m_config.getEventCol().getStringValue());
            int groupCol = inTable.getSpec().findColumnIndex(m_config.getGroupCol().getStringValue());

            // We have to check manually because later we only use groupby and sorters
            for (DataRow row : inTable) {
                if (row.getCell(timeCol).isMissing()) {
                    throw new InvalidSettingsException("Column " + m_config.getTimeCol().getStringValue()
                                                        + " contains missing values.");
                }
                if (row.getCell(eventCol).isMissing()) {
                    throw new InvalidSettingsException("Column " + m_config.getEventCol().getStringValue()
                                                        + " contains missing values.");
                }
                if (groupCol >= 0 && row.getCell(groupCol).isMissing()) {
                    throw new InvalidSettingsException("Column " + m_config.getGroupCol().getStringValue()
                                                        + " contains missing values.");
                }
            }

            KaplanMeierCalculator calc = new KaplanMeierCalculator(m_config.getTimeCol().getStringValue(),
                m_config.getEventCol().getStringValue(), m_config.getGroupCol().getStringValue());
            BufferedDataTable transformed = calc.calculate(exec.createSubExecutionContext(0.7), inTable);

            DataTableSpec spec = transformed.getDataTableSpec();
            final int groupIndex = spec.findColumnIndex(m_config.getGroupCol().getStringValue());
            final int eventHappenedIndex = spec.findColumnIndex("#True("
                                                            + m_config.getEventCol().getStringValue() + ")");
            final int censoringHappendedIndex = spec.findColumnIndex("#False("
                                                            + m_config.getEventCol().getStringValue() + ")");

            Map<String, Integer> counts = new HashMap<String, Integer>();
            for (DataRow row : transformed) {
                String cat = groupIndex >= 0 ? ((StringValue)row.getCell(groupIndex)).getStringValue() : null;
                int num = ((IntCell)row.getCell(eventHappenedIndex)).getIntValue()
                        + ((IntCell)row.getCell(censoringHappendedIndex)).getIntValue();
                Integer n = counts.get(cat);
                if (n == null) {
                    counts.put(cat, num);
                } else {
                    counts.put(cat, n + num);
                }
            }

            ColumnRearranger colRe = createColumnRearranger(transformed.getDataTableSpec(), counts);
            m_table = exec.createColumnRearrangeTable(transformed, colRe, exec.createSubExecutionContext(1.0));

            KaplanMeierViewRepresentation representation = getViewRepresentation();
            if (!reexecuting) {
                copyConfigToView();
            }
            representation.setRunningInView(false);
            representation.setTable(new JSONDataTable(m_table, 1, (int)m_table.size(), exec));
        }
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final Map<String, Integer> groupCounts)
            throws InvalidSettingsException {

        //final int timeIndex = spec.findColumnIndex(m_timeColumn.getStringValue());
        final int groupIndex = spec.findColumnIndex(m_config.getGroupCol().getStringValue());
        final int eventHappenedIndex = spec.findColumnIndex("#True("
                                                    + m_config.getEventCol().getStringValue() + ")");
        final int censoringHappendedIndex = spec.findColumnIndex("#False("
                                                    + m_config.getEventCol().getStringValue() + ")");

        CellFactory cf = new CellFactory() {
            private Map<String, List<Double>> m_values = new HashMap<>();
            private Map<String, Integer> m_counts = new HashMap<>();

            @SuppressWarnings("deprecation")
            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                                    final RowKey lastKey, final ExecutionMonitor exec) {
                exec.setProgress((double)curRowNr / rowCount);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void setProgress(final long curRowNr, final long rowCount,
                                    final RowKey lastKey, final ExecutionMonitor exec) {
                exec.setProgress((double)curRowNr / rowCount);
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return new DataColumnSpec[]{new DataColumnSpecCreator("km-estimator", DoubleCell.TYPE).createSpec()};
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                DataCell groupCell = groupIndex >= 0 ? row.getCell(groupIndex) : null;

                String group = null;

                if (groupCell != null && !groupCell.isMissing()) {
                    group = ((StringValue)groupCell).getStringValue();
                }

                List<Double> values = m_values.get(group);
                Integer count = m_counts.get(group);

                if (values == null) {
                    values = new ArrayList<>();
                    count = groupCounts.get(group);
                    m_values.put(group, values);
                    m_counts.put(group, count);
                }

                DataCell eventCell = row.getCell(eventHappenedIndex);
                DataCell censCell = row.getCell(censoringHappendedIndex);

                if (eventCell.isMissing() || censCell.isMissing()) {
                    return new DataCell[]{DataType.getMissingCell()};
                }

                Integer eventVal = ((IntValue)eventCell).getIntValue();
                Integer censVal = ((IntValue)censCell).getIntValue();

                Integer newCount = count - eventVal;

                Double value = (double)newCount / m_counts.get(group);

                Double prod = value;
                for (Double v : m_values.get(group)) {
                    prod *= v;
                }

                m_values.get(group).add(value);
                m_counts.put(group, newCount - censVal);

                return new DataCell[]{new DoubleCell(prod)};
            }
        };

        ColumnRearranger cr = new ColumnRearranger(spec);
        cr.append(cf);
        return cr;
    }

    private DataTableSpec createIntermediateSpec(final DataTableSpec spec) throws InvalidSettingsException {
        final int timeIndex = spec.findColumnIndex(m_config.getTimeCol().getStringValue());
        final int groupIndex = spec.findColumnIndex(m_config.getGroupCol().getStringValue());

        if (timeIndex == -1) {
            throw new InvalidSettingsException("No time column selected.");
        }
        DataColumnSpec timeSpec = spec.getColumnSpec(timeIndex);
        if (!timeSpec.getType().isCompatible(DateAndTimeValue.class)
                && !timeSpec.getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("Time column has the wrong "
                                            + "data type (only double, int and datetime are allowed).");
        }
        if (spec.findColumnIndex(m_config.getEventCol().getStringValue()) == -1) {
            throw new InvalidSettingsException("No suitable event type column (boolean) selected.");
        } else if (!spec.getColumnSpec(m_config.getEventCol().getStringValue())
                    .getType().isCompatible(BooleanValue.class)) {
            throw new InvalidSettingsException("Event column is not of type boolean.");
        }

        DataColumnSpec trueCol = new DataColumnSpecCreator("#True(" + m_config.getEventCol().getStringValue() + ")",
                                                            IntCell.TYPE).createSpec();
        DataColumnSpec falseCol = new DataColumnSpecCreator("#False(" + m_config.getEventCol().getStringValue() + ")",
                                                            IntCell.TYPE).createSpec();

        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(timeSpec);
        if (groupIndex >= 0) {
            creator.addColumns(spec.getColumnSpec(groupIndex));
        }
        creator.addColumns(trueCol, falseCol);
        return creator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = createIntermediateSpec((DataTableSpec)inSpecs[0]);
        return new PortObjectSpec[]{new ImagePortObjectSpec(SvgCell.TYPE),
                                    createColumnRearranger(spec, null).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaplanMeierViewRepresentation createEmptyViewRepresentation() {
        return new KaplanMeierViewRepresentation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaplanMeierViewValue createEmptyViewValue() {
        return new KaplanMeierViewValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavascriptObjectID() {
        return "org_knime_base_node_stats_kaplanmeierplot";
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
    public ValidationError validateViewValue(final KaplanMeierViewValue viewContent) {
        return null;
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
    protected boolean generateImage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performReset() {
        m_table = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void useCurrentValueAsDefault() {
        synchronized (getLock()) {
            copyValueToConfig();
        }
    }

    private void copyConfigToView() {
        KaplanMeierViewValue value = getViewValue();
        KaplanMeierViewRepresentation representation = getViewRepresentation();
        representation.setTimeCol(m_config.getTimeCol().getStringValue());
        representation.setEventCol(m_config.getEventCol().getStringValue());
        representation.setGroupCol(m_config.getGroupCol().getStringValue());
        representation.setFullscreen(m_config.getFullscreen().getBooleanValue());
        representation.setWidth(m_config.getWidth().getIntValue());
        representation.setHeight(m_config.getHeight().getIntValue());
        representation.setDisplayFullscreenButton(m_config.getDisplayFullscreenButton().getBooleanValue());
        value.setSubtitle(m_config.getSubtitle().getStringValue());
        value.setTitle(m_config.getTitle().getStringValue());
    }

    private void copyValueToConfig() {
        KaplanMeierViewValue value = getViewValue();
        m_config.getTitle().setStringValue(value.getTitle());
        m_config.getSubtitle().setStringValue(value.getSubtitle());
    }
}
