package org.knime.base.node.stats.transformation.lda2.compute;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.node.stats.transformation.lda2.AbstractLDANodeModel;
import org.knime.base.node.stats.transformation.lda2.settings.LDAComputeSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for the LDA Learner Node.
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
class LDAComputeNodeDialog extends NodeDialogPane {

    private static final int INPUT_HEIGHT = 25;

    private final SettingsModelString m_classColModel;

    private final DialogComponentColumnNameSelection m_classColComponent;

    private final SettingsModelColumnFilter2 m_usedColsModel;

    private final DialogComponentColumnFilter2 m_usedColsComponent;

    private final DialogComponentBoolean m_failOnMissingsComp;

    private DataTableSpec[] m_lastSpecs;

    private final JPanel m_panel;

    /**
     * New pane for configuring the node with given settings models.
     */
    @SuppressWarnings("unchecked")
    LDAComputeNodeDialog() {
        final LDAComputeSettings s = new LDAComputeSettings();
        m_classColModel = s.getClassModel();
        m_usedColsModel = s.getUsedColsModel();

        m_classColModel.addChangeListener(l -> {
            try {
                classColChanged();
            } catch (InvalidSettingsException | NotConfigurableException e) {
                // pass - should already have failed when initially opened the dialog
            }
        });
        m_classColComponent = new DialogComponentColumnNameSelection(m_classColModel, "Class column",
            AbstractLDANodeModel.DATA_IN_PORT, NominalValue.class);
        // Smaller size for long names, but with tooltips.
        m_classColComponent.getComponentPanel().getComponent(1).setPreferredSize(new Dimension(260, INPUT_HEIGHT));

        m_usedColsComponent = new DialogComponentColumnFilter2(m_usedColsModel, AbstractLDANodeModel.DATA_IN_PORT);

        m_failOnMissingsComp = new DialogComponentBoolean(s.getFailOnMissingsModel(),
            "Fail if missing values are encountered");

        m_panel = new JPanel();
        final BoxLayout bl = new BoxLayout(m_panel, 1);
        m_panel.setLayout(bl);
        m_panel.add(m_classColComponent.getComponentPanel());
        m_panel.add(m_usedColsComponent.getComponentPanel());
        m_panel.add(m_failOnMissingsComp.getComponentPanel());
        addTab("Settings", m_panel);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_classColComponent.saveSettingsTo(settings);
        m_usedColsComponent.saveSettingsTo(settings);
        m_failOnMissingsComp.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_lastSpecs = specs; // Needed to get the number of classes.
        m_classColComponent.loadSettingsFrom(settings, specs);
        m_usedColsComponent.loadSettingsFrom(settings, new DataTableSpec[]{removeTargetColumnFromLastSpec()});
        m_failOnMissingsComp.loadSettingsFrom(settings, specs);
    }

    /**
     * Adrian Nembachs trick to not show the selected class column in the used column twinlist: save the current
     * twinlist settings and reload them with a fitting featureSpec which excludes the class column.
     *
     * @throws InvalidSettingsException
     * @throws NotConfigurableException
     */
    private void classColChanged() throws InvalidSettingsException, NotConfigurableException {
        // save overhead if anyway no list is to be excluded (e.g. first load)
        if (m_classColComponent.getSelected() == null) {
            return;
        }

        // save the current settings and reload with the modified table spec
        final NodeSettings tmpSettings = new NodeSettings("temp");
        m_usedColsComponent.saveSettingsTo(tmpSettings);
        m_usedColsComponent.loadSettingsFrom(tmpSettings, new DataTableSpec[]{removeTargetColumnFromLastSpec()});
    }

    private DataTableSpec removeTargetColumnFromLastSpec() {
        final String targetColumn = m_classColComponent.getSelected();
        final ColumnRearranger cr = new ColumnRearranger(m_lastSpecs[AbstractLDANodeModel.DATA_IN_PORT]);
        cr.remove(targetColumn);
        return cr.createSpec();
    }
}