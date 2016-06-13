package org.knime.base.node.stats.kaplanmeier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This is the model implementation of PMMLToJavascriptCompiler.
 *
 *
 * @author Alexander Fillbrunn
 */
public class KaplanMeierNodeModel extends NodeModel {

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
     * Constructor for the node model.
     */
    protected KaplanMeierNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Creates a settings model for the time column.
     *
     * @return the settings model
     */
    public static SettingsModelString createTimeColSettingsModel() {
        return new SettingsModelString(TIME_COL_CFG, null);
    }

    private SettingsModelString m_timeColumn = createTimeColSettingsModel();


    /**
     * Creates a settings model for the event type column.
     *
     * @return the settings model
     */
    public static SettingsModelString createEventColSettingsModel() {
        return new SettingsModelString(EVENT_COL_CFG, null);
    }

    private SettingsModelString m_eventColumn = createEventColSettingsModel();


    /**
     * Creates a settings model for the group column.
     *
     * @return the settings model
     */
    public static SettingsModelColumnName createGroupColSettingsModel() {
        return new SettingsModelColumnName(GROUP_COL_CFG, null);
    }

    private SettingsModelString m_groupColumn = createGroupColSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        KaplanMeierCalculator calc = new KaplanMeierCalculator(m_timeColumn.getStringValue(),
                                            m_eventColumn.getStringValue(), m_groupColumn.getStringValue());
        BufferedDataTable transformed = calc.calculate(exec.createSubExecutionContext(0.7), inTable);

        DataTableSpec spec = transformed.getDataTableSpec();
        final int groupIndex = spec.findColumnIndex(m_groupColumn.getStringValue());
        final int eventHappenedIndex = spec.findColumnIndex("#True(" + m_eventColumn.getStringValue() + ")");
        final int censoringHappendedIndex = spec.findColumnIndex("#False(" + m_eventColumn.getStringValue() + ")");

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
        BufferedDataTable result = exec.createColumnRearrangeTable(transformed, colRe,
                                                                    exec.createSubExecutionContext(1.0));

        return new PortObject[]{result};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final Map<String, Integer> groupCounts) {

        //final int timeIndex = spec.findColumnIndex(m_timeColumn.getStringValue());
        final int groupIndex = spec.findColumnIndex(m_groupColumn.getStringValue());
        final int eventHappenedIndex = spec.findColumnIndex("#True(" + m_eventColumn.getStringValue() + ")");
        final int censoringHappendedIndex = spec.findColumnIndex("#False(" + m_eventColumn.getStringValue() + ")");

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    private DataTableSpec createIntermediateSpec(final DataTableSpec spec) {
        final int timeIndex = spec.findColumnIndex(m_timeColumn.getStringValue());
        final int groupIndex = spec.findColumnIndex(m_groupColumn.getStringValue());
        DataColumnSpec trueCol = new DataColumnSpecCreator("#True(" + m_eventColumn.getStringValue() + ")",
                                                            IntCell.TYPE).createSpec();
        DataColumnSpec falseCol = new DataColumnSpecCreator("#False(" + m_eventColumn.getStringValue() + ")",
                                                            IntCell.TYPE).createSpec();

        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(spec.getColumnSpec(timeIndex));
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
        return new PortObjectSpec[]{createColumnRearranger(spec, null).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_timeColumn.saveSettingsTo(settings);
        m_eventColumn.saveSettingsTo(settings);
        m_groupColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_timeColumn.loadSettingsFrom(settings);
        m_eventColumn.loadSettingsFrom(settings);
        m_groupColumn.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_timeColumn.validateSettings(settings);
        m_eventColumn.validateSettings(settings);
        m_groupColumn.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }
}
