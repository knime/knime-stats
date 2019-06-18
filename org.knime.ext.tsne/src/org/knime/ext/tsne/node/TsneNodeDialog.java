/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.ext.tsne.node;

import java.awt.Color;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentSeed;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TsneNodeDialog extends DefaultNodeSettingsPane {

    private static final String DIM_ERROR_TEMPLATE =
        "The current number of output dimensions (%s) is higher than the number of input dimensions (%s).";

    private DataTableSpec m_tableSpec;

    private final SettingsModelColumnFilter2 m_featuresModel = TsneNodeModel.createFeaturesModel();

    private final SettingsModelIntegerBounded m_outputDimensionModel = TsneNodeModel.createOutputDimensionsModel();

    private final DialogComponentNumber m_outputDimensionComponent =
        new DialogComponentNumber(m_outputDimensionModel, "Dimension(s) to reduce to", 1);

    private final DialogComponentLabel m_errorLbl = new DialogComponentLabel("");

    TsneNodeDialog() {
        final DialogComponentNumber iterationsComp =
            new DialogComponentNumber(TsneNodeModel.createIterationsModel(), "Iterations", 10);
        final DialogComponentNumber learningRate =
            new DialogComponentNumber(TsneNodeModel.createThetaModel(), "Theta", 0.1);
        final DialogComponentNumber perplexity =
            new DialogComponentNumber(TsneNodeModel.createPerplexityModel(), "Perplexity", 1.0);
        final DialogComponentBoolean removeOriginalColumns = new DialogComponentBoolean(
            TsneNodeModel.createRemoveOriginalColumnsModel(), "Remove original data columns");
        final DialogComponentBoolean failOnMissingValues = new DialogComponentBoolean(
            TsneNodeModel.createFailOnMissingValuesModel(), "Fail if missing values are encountered");
        final DialogComponentSeed seed = new DialogComponentSeed(TsneNodeModel.createSeedModel(), "Seed");
        final DialogComponentColumnFilter2 featuresComponent = new DialogComponentColumnFilter2(m_featuresModel, 0);
        addDialogComponent(featuresComponent);
        addDialogComponent(m_outputDimensionComponent);
        addDialogComponent(iterationsComp);
        addDialogComponent(learningRate);
        addDialogComponent(perplexity);
        addDialogComponent(
            new DialogComponentNumber(TsneNodeModel.createNumberOfThreadsModel(), "Number of threads", 1));
        addDialogComponent(removeOriginalColumns);
        addDialogComponent(failOnMissingValues);
        addDialogComponent(seed);
        addDialogComponent(m_errorLbl);
        setDefaultTabTitle("Settings");
        ((JLabel)m_errorLbl.getComponentPanel().getComponent(0)).setForeground(Color.RED);
        featuresComponent.getModel().addChangeListener(e -> updateOutputDimensionComponent());
        m_outputDimensionComponent.getModel().addChangeListener(e -> updateOutputDimensionComponent());
    }

    private void updateOutputDimensionComponent() {
        if (m_tableSpec != null) {
            final int inputDimensionality = getInputDimensionality();
            final int outputDimensionality = getOutputDimensionality();
            if (inputDimensionality < outputDimensionality) {
                setDimensionSpinnerBackground(Color.RED);
                m_errorLbl.setText(String.format(DIM_ERROR_TEMPLATE, outputDimensionality, inputDimensionality));
            } else {
                setDimensionSpinnerBackground(Color.WHITE);
                m_errorLbl.setText("");
            }
        }
    }

    private void setDimensionSpinnerBackground(final Color color) {
        final JSpinner spinner = m_outputDimensionComponent.getSpinner();
        final JFormattedTextField spinnerTextField = ((DefaultEditor)spinner.getEditor()).getTextField();
        spinnerTextField.setBackground(color);
    }

    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
        throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        m_tableSpec = specs[0];
        updateOutputDimensionComponent();
    }

    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_tableSpec != null) {
            final int inputDimensionality = getInputDimensionality();
            final int outputDimensionality = getOutputDimensionality();
            CheckUtils.checkSetting(inputDimensionality >= outputDimensionality, DIM_ERROR_TEMPLATE,
                outputDimensionality, inputDimensionality);
        }
        super.saveAdditionalSettingsTo(settings);
    }

    /**
     * @return
     */
    private int getOutputDimensionality() {
        return m_outputDimensionModel.getIntValue();
    }

    /**
     * @return
     */
    private int getInputDimensionality() {
        assert m_tableSpec != null : "Calling methods have to ensure that m_tableSpec != null";
        final FilterResult fr = m_featuresModel.applyTo(m_tableSpec);
        return fr.getIncludes().length;
    }

}
