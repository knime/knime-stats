package org.knime.base.node.stats.lda2;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;

/**
 * Node dialog for the LDA Apply Node.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDAApplyNodeDialog extends DefaultNodeSettingsPane {
    protected LDAApplyNodeDialog() {
        addDialogComponent(new DialogComponentBoolean(LDAApplyNodeModel.createRemoveUsedColsSettingsModel(),
            "Remove original data columns"));
    }
}
