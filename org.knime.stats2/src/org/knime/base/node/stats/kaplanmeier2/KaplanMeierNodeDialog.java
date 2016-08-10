package org.knime.base.node.stats.kaplanmeier2;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

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

        KaplanMeierConfig cfg = new KaplanMeierConfig();

        DialogComponentColumnNameSelection timeCol = new DialogComponentColumnNameSelection(
                        cfg.getTimeCol(),
                      "Time column", 0, true, false, DoubleValue.class, DateAndTimeValue.class);

        DialogComponentColumnNameSelection eventCol = new DialogComponentColumnNameSelection(
            cfg.getEventCol(),
          "Event column", 0, true, false, org.knime.core.data.BooleanValue.class);
        DialogComponentColumnNameSelection groupCol = new DialogComponentColumnNameSelection(
            cfg.getGroupCol(),
          "Group column", 0, false, true, org.knime.core.data.NominalValue.class);

        addDialogComponent(timeCol);
        addDialogComponent(eventCol);
        addDialogComponent(groupCol);

        createNewTab("View Settings");
        createNewGroup("Titles");
        DialogComponentString title = new DialogComponentString(cfg.getTitle(), "Title");
        DialogComponentString subtitle = new DialogComponentString(cfg.getSubtitle(), "Subtitle");

        addDialogComponent(title);
        addDialogComponent(subtitle);
        closeCurrentGroup();

        createNewGroup("Display");
        DialogComponentBoolean fullscreen = new DialogComponentBoolean(cfg.getFullscreen(), "Fullscreen");
        addDialogComponent(fullscreen);

        DialogComponentBoolean enableCtrls = new DialogComponentBoolean(
            cfg.getEnableViewControls(), "Enable view controls");
        addDialogComponent(enableCtrls);

        DialogComponentBoolean enableTitleEdit = new DialogComponentBoolean(
            cfg.getEnableTitleEdit(), "Enable title edit");
        addDialogComponent(enableTitleEdit);

        DialogComponentBoolean enableSubtitleEdit = new DialogComponentBoolean(
            cfg.getEnableSubtitleEdit(), "Enable subtitle edit");
        addDialogComponent(enableSubtitleEdit);

        closeCurrentGroup();
    }
}

