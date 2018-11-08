package org.knime.base.node.stats.shapirowilk;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.stats.shapirowilk2.ShapiroWilk2NodeModel;
import org.knime.base.node.stats.shapirowilk2.ShapiroWilkCalculator;
import org.knime.base.node.stats.shapirowilk2.ShapiroWilkStatistic;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.stats.StatsUtil;

/**
 * @deprecated Use the {@link ShapiroWilk2NodeModel} instead.
 * This is the model implementation of ShapiroWilk.
 *
 *
 * @author Alexander Fillbrunn
 */
@Deprecated
public class ShapiroWilkNodeModel extends NodeModel {

    private static final double MAX_ROWS = 5000;

    /**
     * The configuration key for the class column.
     */
    static final String TEST_COL_CFG = "testCols";

    /**
     * The configuration key for the setting that controls if shapiro francia is used for leptokurtic samples.
     */
    static final String SHAPIRO_FRANCIA_CFG = "shapFrancia";

    /**
     * Constructor for the node model.
     */
    protected ShapiroWilkNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Creates a settings model for the class column.
     *
     * @return the settings model
     */
    public static SettingsModelString createTestColSettingsModel() {
        return new SettingsModelString(TEST_COL_CFG, null);
    }

    private SettingsModelString m_testColumn = createTestColSettingsModel();

    /**
     * Creates a settings model for shapiro-francia.
     *
     * @return the settings model
     */
    public static SettingsModelBoolean createShapiroFranciaSettingsModel() {
        return new SettingsModelBoolean(SHAPIRO_FRANCIA_CFG, true);
    }

    private SettingsModelBoolean m_shapiroFrancia = createShapiroFranciaSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        String col = m_testColumn.getStringValue();
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getDataTableSpec();
        final int cellIndex = inSpec.findColumnIndex(col);

        if (inTable.size() > Integer.MAX_VALUE) {
            throw new InvalidSettingsException("Too many data points to calculate the statistic.");
        } else if (inTable.size() > MAX_ROWS) {
            setWarningMessage("The test might be inaccurate for data sets with more than 5000 data points.");
        }

        if (inSpec.getColumnSpec(cellIndex).getDomain() == null) {
            throw new InvalidSettingsException(
                "The test column does not have an associated domain. " + "Please use a Domain Calculator node first.");
        }

        ShapiroWilkStatistic stat = ShapiroWilkCalculator.calculateSWStatistic(exec, inTable, col, m_shapiroFrancia.getBooleanValue(), 0.3, 0.6);

        pushFlowVariableDouble("shapiro-p-value", stat.getPvalue());
        pushFlowVariableDouble("shapiro-statistic", stat.getStatistic());

        DataContainer dc = exec.createDataContainer(createSpec());

        dc.addRowToTable(
            new DefaultRow(new RowKey("Value"), new DoubleCell(stat.getStatistic()), new DoubleCell(stat.getPvalue())));
        dc.close();

        return new PortObject[]{(BufferedDataTable)dc.getTable()};
    }

    private static DataTableSpec createSpec() {
        DataColumnSpec measure =
            StatsUtil.createDataColumnSpec("W", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE);
        DataColumnSpec p = StatsUtil.createDataColumnSpec("P", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE);

        return new DataTableSpec(measure, p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];

        if (m_testColumn.getStringValue() == null || inSpec.findColumnIndex(m_testColumn.getStringValue()) == -1) {
            for (int i = 0; i < inSpec.getNumColumns(); i++) {
                DataColumnSpec colSpec = inSpec.getColumnSpec(i);
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    setWarningMessage("No column selected or selected column not available. Using " + colSpec.getName()
                        + " instead.");
                    m_testColumn.setStringValue(colSpec.getName());
                    break;
                }
            }
        }
        return new PortObjectSpec[]{createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_testColumn.saveSettingsTo(settings);
        m_shapiroFrancia.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_testColumn.loadSettingsFrom(settings);
        m_shapiroFrancia.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_testColumn.validateSettings(settings);
        m_shapiroFrancia.validateSettings(settings);
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
