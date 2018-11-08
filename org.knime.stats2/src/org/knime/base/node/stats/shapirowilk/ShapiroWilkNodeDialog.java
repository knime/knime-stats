package org.knime.base.node.stats.shapirowilk;

import org.knime.base.node.stats.shapirowilk2.ShapiroWilk2NodeDialog;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

/**
 * @deprecated Use the {@link ShapiroWilk2NodeDialog} instead.
 * <code>NodeDialog</code> for the "ShapiroWilk" Node.
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

