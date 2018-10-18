package org.knime.base.node.stats.lda2;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * This is the model implementation of the LDA Apply node.
 *
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDAApplyNodeModel extends NodeModel {

    private static final int PORT_IN_MODEL = 0;

    private static final int PORT_IN_DATA = 1;

    /**
     * The configuration key whether to remove the used columns.
     */
    private static final String REMOVE_USED_COLS_CFG = "remove_used_columns";

    /**
     * Constructor for the node model.
     */
    LDAApplyNodeModel() {
        super(new PortType[]{LDAModelPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Creates a settings model whether to remove the used columns.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createRemoveUsedColsSettingsModel() {
        return new SettingsModelBoolean(REMOVE_USED_COLS_CFG, false);
    }

    private final SettingsModelBoolean m_removeUsedCols = createRemoveUsedColsSettingsModel();

    private int[] m_indices;

    private int m_k;

    String[] m_usedColumnNames;

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        if (!(inData[PORT_IN_MODEL] instanceof LDAModelPortObject)) {
            throw new IllegalArgumentException("LDAModelPortObject as first input expected");
        }
        if (!(inData[PORT_IN_DATA] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as second input expected");
        }

        final BufferedDataTable inTable = (BufferedDataTable)inData[PORT_IN_DATA];
        final LDAModelPortObject inModel = (LDAModelPortObject)inData[PORT_IN_MODEL];

        final ColumnRearranger cr = createColumnRearranger(inModel, inTable.getDataTableSpec());

        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec);
        return new PortObject[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataSpec = (DataTableSpec)inSpecs[PORT_IN_DATA];
        final LDAModelPortObjectSpec modelSpec = (LDAModelPortObjectSpec)inSpecs[PORT_IN_MODEL];

        // check existing column names and find out their indices
        m_usedColumnNames = modelSpec.getColumnNames();
        final int numIncludedColumns = m_usedColumnNames.length;
        m_indices = new int[numIncludedColumns];
        for (int i = 0; i < numIncludedColumns; i++) {
            final String columnName = m_usedColumnNames[i];
            if (!dataSpec.containsName(columnName)) {
                throw new InvalidSettingsException(
                    "The model is expecting column \"" + columnName + "\" which is missing in the input table.");
            }
            m_indices[i] = dataSpec.findColumnIndex(m_usedColumnNames[i]);
        }

        m_k = modelSpec.getTargetDimensions();
        return new PortObjectSpec[]{createColumnRearranger(null, dataSpec).createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final LDAModelPortObject inModel,
        final DataTableSpec dataSpec) {
        if (inModel == null) {
            return AbstractLDANodeModel.createColumnRearranger(dataSpec, null, m_k,
                m_removeUsedCols.getBooleanValue(), m_usedColumnNames);
        }
        final LDA2 lda = new LDA2(m_indices, inModel.getTransformationMatrix());

        return AbstractLDANodeModel.createColumnRearranger(dataSpec, lda, m_k,
            m_removeUsedCols.getBooleanValue(), m_usedColumnNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {

                final LDAModelPortObject inModel =
                    (LDAModelPortObject)((PortObjectInput)inputs[PORT_IN_MODEL]).getPortObject();

                final ColumnRearranger cr = createColumnRearranger(inModel, (DataTableSpec)inSpecs[PORT_IN_DATA]);

                final StreamableFunction func = cr.createStreamableFunction(PORT_IN_DATA, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeUsedCols.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_removeUsedCols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeUsedCols.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File arg0, final ExecutionMonitor arg1)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File arg0, final ExecutionMonitor arg1)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }
}
