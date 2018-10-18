package org.knime.base.node.stats.lda2;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;

/**
 * Node dialog for the LDA Learner Node.
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDA2NodeDialog extends LDAComputeNodeDialog {

    private final DialogComponentBoolean m_removeUsedCols;

    /**
     * New pane for configuring the node.
     */
    protected LDA2NodeDialog() {
        super(AbstractLDANodeModel.createClassColSettingsModel(), AbstractLDANodeModel.createUsedColsSettingsModel(),
            AbstractLDANodeModel.createKSettingsModel());
        m_removeUsedCols = new DialogComponentBoolean(LDA2NodeModel.createRemoveUsedColsSettingsModel(),
            "Remove original data columns");

        m_panel.add(m_removeUsedCols.getComponentPanel(), 3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_removeUsedCols.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_removeUsedCols.saveSettingsTo(settings);
    }
}
