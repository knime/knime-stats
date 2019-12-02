package org.knime.base.node.stats.transformation.lda2.apply;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.transformation.port.TransformationPortObject;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec.TransformationType;
import org.knime.base.node.mine.transformation.settings.TransformationApplySettings;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
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
import org.knime.core.node.util.CheckUtils;

/**
 * This is the model implementation of the LDA Apply node.
 *
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDAApplyNodeModel extends NodeModel {

    static final int MODEL_IN_PORT = 0;

    private static final int DATA_IN_PORT = 1;

    private final TransformationApplySettings m_applySettings = new TransformationApplySettings();

    /**
     * Constructor for the node model.
     */
    LDAApplyNodeModel() {
        super(new PortType[]{TransformationPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        if (!(inData[MODEL_IN_PORT] instanceof TransformationPortObject)) {
            throw new IllegalArgumentException("LDAModelPortObject as first input expected");
        }
        if (!(inData[DATA_IN_PORT] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as second input expected");
        }

        final BufferedDataTable inTable = (BufferedDataTable)inData[DATA_IN_PORT];
        final TransformationPortObject inModel = (TransformationPortObject)inData[MODEL_IN_PORT];

        final ColumnRearranger cr = createColumnRearranger(inModel, inModel.getSpec().getInputColumnNames(),
            inTable.getDataTableSpec(), inModel.getSpec().getTransformationType());

        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec);
        return new PortObject[]{out};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataSpec = (DataTableSpec)inSpecs[DATA_IN_PORT];
        final TransformationPortObjectSpec modelSpec = (TransformationPortObjectSpec)inSpecs[MODEL_IN_PORT];

        // check existing column names and find out their indices
        final String[] usedColumnNames = modelSpec.getInputColumnNames();
        final int numIncludedColumns = usedColumnNames.length;
        for (int i = 0; i < numIncludedColumns; i++) {
            final String columnName = usedColumnNames[i];
            if (dataSpec.findColumnIndex(columnName) == -1) {
                throw new InvalidSettingsException(
                    "The model is expecting column \"" + columnName + "\" which is missing in the input table.");
            }
        }

        // sanity check for flow variables
        CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() > 0,
            "The number of dimensions to project to must be a positive integer larger than 0, %s is invalid",
            m_applySettings.getDimModel().getIntValue());
        final int maxDim = modelSpec.getMaxDimToReduceTo();
        CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() <= maxDim,
            "The number of dimensions to project to must be less than or equal %s", maxDim);

        return new PortObjectSpec[]{
            createColumnRearranger(null, usedColumnNames, dataSpec, modelSpec.getTransformationType()).createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final TransformationPortObject inModel,
        final String[] usedColumnNames, final DataTableSpec dataSpec, final TransformationType transType) {
        return TransformationUtils.createColumnRearranger(dataSpec,
            inModel == null ? null : inModel.getTransformationMatrix(), m_applySettings.getDimModel().getIntValue(),
            m_applySettings.getRemoveUsedColsModel().getBooleanValue(), usedColumnNames, transType);
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {

                final TransformationPortObject inModel =
                    (TransformationPortObject)((PortObjectInput)inputs[MODEL_IN_PORT]).getPortObject();

                final ColumnRearranger cr = createColumnRearranger(inModel, inModel.getSpec().getInputColumnNames(),
                    (DataTableSpec)inSpecs[DATA_IN_PORT], inModel.getSpec().getTransformationType());

                final StreamableFunction func = cr.createStreamableFunction(DATA_IN_PORT, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_applySettings.loadValidatedSettingsFrom(settings);

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_applySettings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_applySettings.validateSettings(settings);
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File arg0, final ExecutionMonitor arg1)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void loadInternals(final File arg0, final ExecutionMonitor arg1)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }
}
