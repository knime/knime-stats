package org.knime.base.node.stats.lda2.perform;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;

import org.apache.commons.lang3.text.WordUtils;
import org.knime.base.node.stats.lda2.AbstractLDANodeModel;
import org.knime.base.node.stats.lda2.algorithm.LDAUtils;
import org.knime.base.node.stats.lda2.settings.LDAApplySettings;
import org.knime.base.node.stats.lda2.settings.LDAComputeSettings;
import org.knime.core.data.DataColumnDomain;
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
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for the LDA Learner Node.
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDA2NodeDialog extends NodeDialogPane {

    private final SettingsModelInteger m_dimensionModel;

    private final DialogComponentNumber m_dimensionComponent;

    private final DialogComponentLabel m_tooHighDimLabel;

    private static final int INPUT_HEIGHT = 25;

    private final SettingsModelString m_classColModel;

    private final DialogComponentColumnNameSelection m_classColComponent;

    private final SettingsModelColumnFilter2 m_usedColsModel;

    private final DialogComponentColumnFilter2 m_usedColsComponent;

    private final DialogComponentLabel m_maxDimZeroLabel;

    private final DialogComponentBoolean m_remUsedColsComp;

    private final DialogComponentBoolean m_failOnMissingsComp;

    private int m_selectedClasses;

    private int m_selectedColumns;

    private int m_maximumDim;

    private DataTableSpec[] m_lastSpecs;

    protected final JPanel m_panel;

    /**
     * New pane for configuring the node with given settings models.
     *
     * @param classColModel
     * @param usedColsModel
     * @param dimensionModel
     */
    @SuppressWarnings("unchecked")
    LDA2NodeDialog() {
        final LDAComputeSettings compSettings = new LDAComputeSettings();
        final LDAApplySettings applySettings = new LDAApplySettings();
        m_classColModel = compSettings.getClassModel();
        m_usedColsModel = compSettings.getPredModel();
        m_dimensionModel = applySettings.getDimModel();

        m_dimensionModel.addChangeListener((e) -> {
            updateSpinner();
            updateWarnings();
        });
        m_dimensionComponent = new DialogComponentNumber(m_dimensionModel, "Dimensions", 1, 5);

        m_tooHighDimLabel = new DialogComponentLabel(wrapText(""));
        m_tooHighDimLabel.getComponentPanel().getComponent(0).setForeground(Color.red);

        m_classColModel.addChangeListener(l -> {
            updateSettings();
            // update the twinlist to not show the just selected column
            try {
                classColChanged();
            } catch (InvalidSettingsException | NotConfigurableException e) {
                // pass - should already have failed when initially opened the dialog
            }
        });
        m_classColComponent = new DialogComponentColumnNameSelection(m_classColModel, "Class column",
            AbstractLDANodeModel.PORT_IN_DATA, NominalValue.class);
        // Smaller size for long names, but with tooltips.
        m_classColComponent.getComponentPanel().getComponent(1).setPreferredSize(new Dimension(260, INPUT_HEIGHT));

        m_usedColsModel.addChangeListener(l -> updateSettings());
        m_usedColsComponent = new DialogComponentColumnFilter2(m_usedColsModel, AbstractLDANodeModel.PORT_IN_DATA);

        // This messages will be made more precise once shown.
        m_maxDimZeroLabel = new DialogComponentLabel(wrapText(""));
        m_maxDimZeroLabel.getComponentPanel().getComponent(0).setForeground(Color.red);

        m_remUsedColsComp =
            new DialogComponentBoolean(applySettings.getRemoveUsedColsModel(), "Remove original data columns");

        m_failOnMissingsComp = new DialogComponentBoolean(compSettings.getFailOnMissingsModel(),
            "Fail if missing values are encountered");

        m_panel = new JPanel();
        final BoxLayout bl = new BoxLayout(m_panel, 1);
        m_panel.setLayout(bl);
        m_panel.add(m_dimensionComponent.getComponentPanel(), 0);
        m_panel.add(m_classColComponent.getComponentPanel());
        m_panel.add(m_usedColsComponent.getComponentPanel());
        m_panel.add(m_remUsedColsComp.getComponentPanel());
        m_panel.add(m_failOnMissingsComp.getComponentPanel());
        m_panel.add(m_maxDimZeroLabel.getComponentPanel());
        m_panel.add(m_tooHighDimLabel.getComponentPanel());
        addTab("Settings", m_panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_maximumDim <= 0) {
            throw new InvalidSettingsException(LDAUtils.createMaxDimZeroWarning(m_selectedClasses, m_selectedColumns,
                m_classColComponent.getSelected()));
        }

        if (m_dimensionModel.getIntValue() > m_maximumDim) {
            throw new InvalidSettingsException(LDAUtils.createTooHighDimWarning(m_dimensionModel.getIntValue(),
                m_maximumDim, m_selectedClasses, m_selectedColumns, m_classColComponent.getSelected()));
        }

        m_classColComponent.saveSettingsTo(settings);
        m_usedColsComponent.saveSettingsTo(settings);
        m_dimensionComponent.saveSettingsTo(settings);
        m_remUsedColsComp.saveSettingsTo(settings);
        m_failOnMissingsComp.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_lastSpecs = specs; // Needed to get the number of classes.

        m_classColComponent.loadSettingsFrom(settings, specs);
        m_usedColsComponent.loadSettingsFrom(settings, new DataTableSpec[]{removeTargetColumnFromLastSpec()});
        m_remUsedColsComp.loadSettingsFrom(settings, specs);
        m_failOnMissingsComp.loadSettingsFrom(settings, specs);
        updateSettings();

        // for the case of a too large maxDim - do not fire the changeListener, else the dimension will be reset
        m_dimensionComponent.loadSettingsFrom(settings, specs);

        updateSpinner();
        updateWarnings();
    }

    private void updateWarnings() {
        final JLabel maxDimZeroComponent = (JLabel)m_maxDimZeroLabel.getComponentPanel().getComponent(0);
        final JLabel tooHighDimComponent = (JLabel)m_tooHighDimLabel.getComponentPanel().getComponent(0);

        if (m_maximumDim <= 0) {
            // AP-10106 case 1
            maxDimZeroComponent.setText(wrapText(LDAUtils.createMaxDimZeroWarning(m_selectedClasses, m_selectedColumns,
                m_classColComponent.getSelected())));
            maxDimZeroComponent.setVisible(true);

            tooHighDimComponent.setVisible(false);
        } else if (m_dimensionModel.getIntValue() > m_maximumDim) {
            // AP-10106 case 2
            tooHighDimComponent.setText(wrapText(LDAUtils.createTooHighDimWarning(m_dimensionModel.getIntValue(),
                m_maximumDim, m_selectedClasses, m_selectedColumns, m_classColComponent.getSelected())));
            tooHighDimComponent.setVisible(true);

            maxDimZeroComponent.setVisible(false);
        } else {
            // all good
            maxDimZeroComponent.setVisible(false);
            tooHighDimComponent.setVisible(false);
        }
    }

    private void updateSettings() {
        // Save number of columns and number of classes to show a specific warning.
        if ((m_lastSpecs != null) && (m_lastSpecs.length > 0) && (m_classColComponent.getSelected() != null)
            && (m_classColComponent.getSelected().length() > 0)) {
            m_selectedColumns = m_usedColsModel.applyTo(removeTargetColumnFromLastSpec()).getIncludes().length;
            final DataColumnDomain domain = m_lastSpecs[AbstractLDANodeModel.PORT_IN_DATA]
                .getColumnSpec(m_classColComponent.getSelected()).getDomain();
            if (domain.hasValues()) {
                m_selectedClasses = domain.getValues().size();
            } else {
                // set to a flag value indicating that the domain was not calculated
                m_selectedClasses = Integer.MAX_VALUE;
            }
        }

        m_maximumDim = Math.min(m_selectedClasses - 1, m_selectedColumns);
        updateWarnings();
        updateSpinner();
    }

    private void updateSpinner() {
        m_dimensionComponent.setToolTipText("Maximum dimension: " + m_maximumDim);

        final JSpinner spinner = (JSpinner)m_dimensionComponent.getComponentPanel().getComponent(1);
        final JFormattedTextField spinnerTextField = ((DefaultEditor)spinner.getEditor()).getTextField();
        if (m_maximumDim <= 0) {
            spinnerTextField.setBackground(Color.RED);
            // en-/disabeling the spinner fires its change listener, causing the boundaries to adjust...
            // circumvent the firing of the change listener by disabling the components individually
            spinnerTextField.setEnabled(false);
            spinner.setEnabled(false);
        } else if (m_dimensionModel.getIntValue() > m_maximumDim) {
            spinnerTextField.setBackground(Color.RED);
            spinnerTextField.setEnabled(true);
            spinner.setEnabled(true);
        } else {
            spinnerTextField.setBackground(Color.WHITE);
            spinnerTextField.setEnabled(true);
            spinner.setEnabled(true);
        }
    }

    /**
     *
     * Brings the given text into a html-format such that it can be shown as a red warning message in the config dialog.
     *
     * @param text
     * @return wrapped text
     */
    private static String wrapText(final String text) {
        return "<html>" + WordUtils.wrap(text, 75, "<br/>", true) + "</html>";
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
        final ColumnRearranger cr = new ColumnRearranger(m_lastSpecs[AbstractLDANodeModel.PORT_IN_DATA]);
        cr.remove(targetColumn);
        return cr.createSpec();
    }

}
