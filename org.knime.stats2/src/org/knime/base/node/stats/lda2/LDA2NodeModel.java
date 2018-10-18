package org.knime.base.node.stats.lda2;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This is the model implementation of the LDA Node.
 *
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDA2NodeModel extends AbstractLDANodeModel {

    /**
     * The configuration key whether to remove the used columns.
     */
    private static final String REMOVE_USED_COLS_CFG = "remove_used_columns";

    /**
     * Constructor for the node model.
     */
    LDA2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE});
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
            throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        final DataTableSpec inSpec = inTable.getDataTableSpec();

        final LDA2 lda = new LDA2(m_indices);
        lda.calculateTransformationMatrix(exec.createSubExecutionContext(0.5), inTable, m_k.getIntValue(),
            m_classColIdx);

        final ColumnRearranger cr = createColumnRearranger(inSpec, lda, m_k.getIntValue(),
            m_removeUsedCols.getBooleanValue(), m_usedColumnNames);

        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec.createSubProgress(0.5));
        return new PortObject[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new PortObjectSpec[]{createColumnRearranger(inSpec, null, m_k.getIntValue(),
            m_removeUsedCols.getBooleanValue(), m_usedColumnNames).createSpec()};
    }

    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
        m_removeUsedCols.saveSettingsTo(settings);
    }

    @Override
    protected void loadAdditionalValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeUsedCols.loadSettingsFrom(settings);
    }

    @Override
    protected void validateAdditionalSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeUsedCols.validateSettings(settings);
    }
}
