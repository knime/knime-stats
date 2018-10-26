package org.knime.base.node.stats.shapirowilk;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
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
@Deprecated
public class ShapiroWilkNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the node.
     */
    @SuppressWarnings("unchecked")
    protected ShapiroWilkNodeDialog() {
        DialogComponentColumnNameSelection testCol = new DialogComponentColumnNameSelection(
                        ShapiroWilkNodeModel.createTestColSettingsModel(),
                      "Test column", 0, org.knime.core.data.DoubleValue.class);
        DialogComponentBoolean shapFrancia = new DialogComponentBoolean(
            ShapiroWilkNodeModel.createShapiroFranciaSettingsModel(),
            "Use Shapiro-Francia for leptokurtic samples");
        addDialogComponent(testCol);
        addDialogComponent(shapFrancia);
    }
}

