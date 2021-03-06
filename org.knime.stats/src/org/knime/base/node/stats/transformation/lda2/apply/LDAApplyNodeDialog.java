package org.knime.base.node.stats.transformation.lda2.apply;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.settings.TransformationApplySettings;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.base.node.stats.transformation.lda2.util.LDAUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Node dialog for the LDA Apply Node.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDAApplyNodeDialog extends NodeDialogPane {

    private final SettingsModelInteger m_maxDimModel;

    private final DimensionChangeListener m_dimListener;

    private final DialogComponentNumber m_maxDimDiaComp;

    private final DialogComponentBoolean m_remUsedCols;

    private final DialogComponentBoolean m_failOnMissingsComp;

    private final JLabel m_errorMsg;

    LDAApplyNodeDialog() {
        final TransformationApplySettings applySettings = new TransformationApplySettings();
        m_maxDimModel = applySettings.getDimModel();
        m_maxDimDiaComp = new DialogComponentNumber(m_maxDimModel, "Target dimensions", 1, 5) {
            @Override
            protected void clearError(final JTextField field) {
                return;
            }
        };
        m_dimListener = new DimensionChangeListener();
        m_maxDimDiaComp.getModel().addChangeListener(m_dimListener);
        m_remUsedCols =
            new DialogComponentBoolean(applySettings.getRemoveUsedColsModel(), "Remove original data columns");
        m_failOnMissingsComp = new DialogComponentBoolean(applySettings.getFailOnMissingsModel(),
            "Fail if missing values are encountered");
        m_errorMsg = new JLabel("");
        m_errorMsg.setForeground(Color.RED);
        addTab("Settings", createPanel());
    }

    private Component createPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridy = 0;
        p.add(m_maxDimDiaComp.getComponentPanel(), gbc);
        ++gbc.gridy;
        p.add(m_remUsedCols.getComponentPanel(), gbc);
        ++gbc.gridy;
        p.add(m_failOnMissingsComp.getComponentPanel(), gbc);
        ++gbc.gridy;
        gbc.insets = new Insets(0, 5, 0, 0);
        p.add(m_errorMsg, gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        p.add(Box.createVerticalBox(), gbc);
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_errorMsg.isVisible()) {
            throw new InvalidSettingsException("The specified number of dimensions is higher than allowed.");
        }
        m_maxDimDiaComp.saveSettingsTo(settings);
        m_remUsedCols.saveSettingsTo(settings);
        m_failOnMissingsComp.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if (specs[LDAApplyNodeModel.MODEL_IN_PORT] == null) {
            throw new NotConfigurableException("Model input missing");
        }
        final int maxDimToReduceTo =
            ((TransformationPortObjectSpec)specs[LDAApplyNodeModel.MODEL_IN_PORT]).getMaxDimToReduceTo();
        m_maxDimDiaComp.setToolTipText("Maximum dimensions to reduce to: " + maxDimToReduceTo);

        m_dimListener.setMaxDim(maxDimToReduceTo);
        m_dimListener.validateOnly(true);

        m_maxDimDiaComp.loadSettingsFrom(settings, specs);
        m_remUsedCols.loadSettingsFrom(settings, specs);
        m_failOnMissingsComp.loadSettingsFrom(settings, specs);

        m_dimListener.validateOnly(false);

    }

    private class DimensionChangeListener implements ChangeListener {

        private int m_maxDim = 0;

        private boolean m_validateOnly;

        @Override
        public void stateChanged(final ChangeEvent e) {
            final int curDim = m_maxDimModel.getIntValue();
            if (!m_validateOnly) {
                int validDim = curDim;
                validDim = Math.min(validDim, m_maxDim);
                validDim = Math.max(validDim, 1);

                if (validDim != curDim) {
                    // this calls the change listener again
                    m_maxDimModel.setIntValue(validDim);
                    return;
                }
            }
            validateDimensions(curDim, m_maxDim);
        }

        void setMaxDim(final int maxDim) {
            m_maxDim = maxDim;
        }

        void validateOnly(final boolean validateOnly) {
            m_validateOnly = validateOnly;
        }

        private void validateDimensions(final int curDim, final int maxDim) {
            if (m_maxDimModel.getIntValue() > maxDim) {
                setErrorMsg(curDim, maxDim);
            } else {
                clearErrorMsg();
            }
        }

        private void clearErrorMsg() {
            m_maxDimDiaComp.getSpinner().getEditor().getComponent(0).setForeground(Color.BLACK);
            m_errorMsg.setVisible(false);
        }

        private void setErrorMsg(final int curDim, final int maxDim) {
            m_maxDimDiaComp.getSpinner().getEditor().getComponent(0).setForeground(Color.RED);
            m_errorMsg.setText(TransformationUtils.wrapText(LDAUtils.createTooHighDimBaseWarning(curDim, maxDim)));
            m_errorMsg.setVisible(true);
        }

    }
}
