package org.knime.base.node.stats.kaplanmeier;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

/**
 * <code>NodeDialog</code> for the "PMMLToJavascriptCompiler" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Alexander Fillbrunn
 */
public class KaplanMeierNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the node.
     */
    @SuppressWarnings("unchecked")
    protected KaplanMeierNodeDialog() {
        DialogComponentColumnNameSelection timeCol = new DialogComponentColumnNameSelection(
                        KaplanMeierNodeModel.createTimeColSettingsModel(),
                      "Time column", 0, org.knime.core.data.IntValue.class);

        DialogComponentColumnNameSelection eventCol = new DialogComponentColumnNameSelection(
            KaplanMeierNodeModel.createEventColSettingsModel(),
          "Event column", 0, org.knime.core.data.BooleanValue.class);
        DialogComponentColumnNameSelection groupCol = new DialogComponentColumnNameSelection(
            KaplanMeierNodeModel.createGroupColSettingsModel(),
          "Group column", 0, false, true, org.knime.core.data.NominalValue.class);

        addDialogComponent(timeCol);
        addDialogComponent(eventCol);
        addDialogComponent(groupCol);
    }
}

