package org.knime.base.node.stats.lda2.apply;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.linear.MatrixUtils;
import org.knime.base.node.mine.pca.PCAModelPortObject;
import org.knime.base.node.mine.pca.PCAModelPortObjectSpec;
import org.knime.base.node.stats.lda2.algorithm.LDA2;
import org.knime.base.node.stats.lda2.algorithm.LDAUtils;
import org.knime.base.node.stats.lda2.settings.LDAApplySettings;
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

    static final int PORT_IN_MODEL = 0;

    private static final int PORT_IN_DATA = 1;

    private final LDAApplySettings m_applySettings = new LDAApplySettings();

    /**
     * Constructor for the node model.
     */
    LDAApplyNodeModel() {
        super(new PortType[]{PCAModelPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        if (!(inData[PORT_IN_MODEL] instanceof PCAModelPortObject)) {
            throw new IllegalArgumentException("LDAModelPortObject as first input expected");
        }
        if (!(inData[PORT_IN_DATA] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as second input expected");
        }

        final BufferedDataTable inTable = (BufferedDataTable)inData[PORT_IN_DATA];
        final PCAModelPortObject inModel = (PCAModelPortObject)inData[PORT_IN_MODEL];

        final ColumnRearranger cr = createColumnRearranger(inModel, inModel.getInputColumnNames(),
            inTable.getDataTableSpec(), inModel.getSpec().getColPrefix());

        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec);
        return new PortObject[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataSpec = (DataTableSpec)inSpecs[PORT_IN_DATA];
        final PCAModelPortObjectSpec modelSpec = (PCAModelPortObjectSpec)inSpecs[PORT_IN_MODEL];

        // check existing column names and find out their indices
        final String[] usedColumnNames = modelSpec.getColumnNames();
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
        final int maxDim = getMaxDim(modelSpec);
        CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() <= maxDim,
            "The number of dimensions to project to must be less than or equal %s", maxDim);

        return new PortObjectSpec[]{createColumnRearranger(null, usedColumnNames, dataSpec, modelSpec.getColPrefix()).createSpec()};
    }

    /**
     * Returns the maximum number of dimensions to project to.
     *
     * @param modelSpec the model spec
     * @return the maximum number of dimensions to project to
     */
    static int getMaxDim(final PCAModelPortObjectSpec modelSpec) {
        if (modelSpec.getEigenValues() != null) {
            return modelSpec.getEigenValues().length;
        }
        return modelSpec.getColumnNames().length;
    }

    private ColumnRearranger createColumnRearranger(final PCAModelPortObject inModel, final String[] usedColumnNames,
        final DataTableSpec dataSpec, final String colPrefix) {
        if (inModel == null) {
            return LDAUtils.createColumnRearranger(dataSpec, null, m_applySettings.getDimModel().getIntValue(),
                m_applySettings.getRemoveUsedColsModel().getBooleanValue(), usedColumnNames, colPrefix);
        }
        final int[] cIndices = Arrays.stream(usedColumnNames)//
            .mapToInt(cName -> dataSpec.findColumnIndex(cName))//
            .toArray();
        final LDA2 lda = new LDA2(cIndices, MatrixUtils.createRealMatrix(inModel.getEigenVectors()),
            m_applySettings.getFailOnMissingsModel().getBooleanValue());

        return LDAUtils.createColumnRearranger(dataSpec, lda, m_applySettings.getDimModel().getIntValue(),
            m_applySettings.getRemoveUsedColsModel().getBooleanValue(), usedColumnNames, colPrefix);
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

                final PCAModelPortObject inModel =
                    (PCAModelPortObject)((PortObjectInput)inputs[PORT_IN_MODEL]).getPortObject();

                final ColumnRearranger cr = createColumnRearranger(inModel, inModel.getInputColumnNames(),
                    (DataTableSpec)inSpecs[PORT_IN_DATA], inModel.getSpec().getColPrefix());

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
        m_applySettings.loadValidatedSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_applySettings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_applySettings.validateSettings(settings);
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
