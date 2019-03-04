package org.knime.base.node.stats.lda2.perform;

import org.knime.base.node.stats.lda2.AbstractLDANodeModel;
import org.knime.base.node.stats.lda2.algorithm.LDA2;
import org.knime.base.node.stats.lda2.algorithm.LDAUtils;
import org.knime.base.node.stats.lda2.settings.LDAApplySettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 * This is the model implementation of the LDA Node.
 *
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDA2NodeModel extends AbstractLDANodeModel {

    private final LDAApplySettings m_applySettings = new LDAApplySettings();

    /**
     * Constructor for the node model.
     */
    LDA2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        final DataTableSpec inSpec = inTable.getDataTableSpec();

        final LDA2 lda = new LDA2(m_indices, m_computeSettings.getFailOnMissingsModel().getBooleanValue());
        lda.calculateTransformationMatrix(exec.createSubExecutionContext(0.5), inTable,
            m_applySettings.getDimModel().getIntValue(), m_classColIdx);

        final ColumnRearranger cr =
            LDAUtils.createColumnRearranger(inSpec, lda, m_applySettings.getDimModel().getIntValue(),
                m_applySettings.getRemoveUsedColsModel().getBooleanValue(), m_usedColumnNames);

        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec.createSubProgress(0.5));
        return new PortObject[]{out};
    }

    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        // Sanity check settings even though dialog checks, in case any flow variables went bad.
        CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() > 0,
            "The number of dimensions to project to must be a positive integer larger than 0, %s is invalid",
            m_applySettings.getDimModel().getIntValue());
        final int maxDim =
            LDAUtils.calcPositiveMaxDim(inSpec, m_computeSettings.getClassModel().getStringValue(), m_indices.length);
        CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() <= maxDim,
            "The number of dimensions to project to must be less than or equal %s", maxDim);

        return new PortObjectSpec[]{
            LDAUtils.createColumnRearranger(inSpec, null, m_applySettings.getDimModel().getIntValue(),
                m_applySettings.getRemoveUsedColsModel().getBooleanValue(), m_usedColumnNames).createSpec()};
    }

    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
        m_applySettings.saveSettingsTo(settings);
    }

    @Override
    protected void loadAdditionalValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_applySettings.loadValidatedSettingsFrom(settings);

    }

    @Override
    protected void validateAdditionalSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_applySettings.validateSettings(settings);
    }

}
