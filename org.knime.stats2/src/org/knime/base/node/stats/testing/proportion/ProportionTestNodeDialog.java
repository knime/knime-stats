/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 */

package org.knime.base.node.stats.testing.proportion;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "ProportionTest" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public class ProportionTestNodeDialog extends DefaultNodeSettingsPane {

    private static final int INPUT_WIDTH = 5;

    private static final int INPUT_HEIGHT = 25;

    private DataTableSpec[] m_lastSpecs;

    private final SettingsModelString m_categoryModel;

    private SettingsModelString m_categoryColumnModel;

    private final DialogComponentStringSelection m_categoryComponent;

    private boolean m_dirtyConfigure;

    private final DialogComponentLabel m_categoryWarning;

    private NodeSettingsRO m_lastSettings = null;

    /**
     * New pane for configuring ProportionTest node dialog.
     */
    @SuppressWarnings({"unchecked"})
    protected ProportionTestNodeDialog() {
        m_categoryColumnModel = ProportionTestNodeModel.createSettingsModelCategoryColumn();
        final DialogComponentColumnNameSelection catColComponent = new DialogComponentColumnNameSelection(
            m_categoryColumnModel, "Category column", ProportionTestNodeModel.PORT_IN_DATA, NominalValue.class);
        addDialogComponent(catColComponent);
        // smaller size for long names, with tooltips
        catColComponent.getComponentPanel().getComponent(1).setPreferredSize(new Dimension(260, INPUT_HEIGHT));

        // update the category selection options, as these deoend on the selected column
        m_categoryColumnModel.addChangeListener(e -> {
            if (m_lastSpecs != null) {
                try {
                    updateCategorySelection(m_lastSettings);
                } catch (final InvalidSettingsException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        });

        m_categoryModel = ProportionTestNodeModel.createSettingsModelCategory();
        // TODO: add tooltip support to DialogComponentStringSelection? - to be done in StringIconListCellRenderer
        m_categoryComponent = new DialogComponentStringSelection(m_categoryModel, "Category", "");
        m_categoryComponent.setSizeComponents(260, INPUT_HEIGHT);
        addDialogComponent(m_categoryComponent);

        // create a HTML-layouted warning message if the currently selected column won't work.
        final StringBuilder warningMessageSB =
                new StringBuilder("No category available: " + ProportionTestNodeModel.ERRORMESSAGE);
        int i = 0;
        warningMessageSB.insert(0, "<html>");
        while ((i = warningMessageSB.indexOf(" ", i + 50)) != -1) {
            // add newline
            warningMessageSB.replace(i, i + 1, "<br/>");
        }
        warningMessageSB.insert(warningMessageSB.length(), "</html>");

        m_categoryWarning = new DialogComponentLabel(warningMessageSB.toString());
        m_categoryWarning.getComponentPanel().getComponent(0).setForeground(Color.red);
        m_categoryWarning.getComponentPanel().getComponent(0).setVisible(false);
        addDialogComponent(m_categoryWarning);

        final DialogComponentNumber p0Component = new DialogComponentNumber(
            ProportionTestNodeModel.createSettingsModelNullHypothesis(), "Null hypothesis", 0.01, INPUT_WIDTH);
        addDialogComponent(p0Component);

        final List<String> hA = Stream.of(AlternativeHypothesis.values()).map(Enum::name).collect(Collectors.toList());
        final DialogComponentStringSelection alternativeComponent = new DialogComponentStringSelection(
            ProportionTestNodeModel.createSettingsModelAlternativeHypothesis(), "Alternative hypothesis", hA);
        alternativeComponent.setSizeComponents(150, INPUT_HEIGHT);
        addDialogComponent(alternativeComponent);

        final DialogComponentNumber alphaComponent = new DialogComponentNumber(
            ProportionTestNodeModel.createSettingsModelAlpha(), "Significance level (alpha)", 0.01, INPUT_WIDTH);
        addDialogComponent(alphaComponent);

        createNewTab("Advanced Settings");
        addDialogComponent(
            new DialogComponentBoolean(ProportionTestNodeModel.createSettingsModelSampleProportionStdError(),
                    "Use sample proportion to compute standard error"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        m_lastSpecs = specs;
        m_lastSettings = settings;
        try {
            updateCategorySelection(m_lastSettings);
        } catch (final InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }

        /*  TODO: can we take out the final of super.loadSettingsFrom() and load
         *  the settings there before filling the autosuggested default category value?
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_dirtyConfigure) {
            throw new InvalidSettingsException(
                "No compatible column in spec available: " + ProportionTestNodeModel.ERRORMESSAGE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen() {
        updateWarning();
    }

    private void updateWarning() {
        if (m_dirtyConfigure) {
            m_categoryWarning.getComponentPanel().getComponent(0).setVisible(true);
            m_categoryModel.setEnabled(false);
        } else {
            m_categoryWarning.getComponentPanel().getComponent(0).setVisible(false);
            m_categoryModel.setEnabled(true);
        }
    }

    /**
     *
     * Tries to set the the category selection options and set the first value as the default.
     *
     * @throws InvalidSettingsException
     */
    private void updateCategorySelection(final NodeSettingsRO settings) throws InvalidSettingsException {
        final int categoryColIdx = m_lastSpecs[0].findColumnIndex(m_categoryColumnModel.getStringValue());
        if (categoryColIdx != -1) {
            final DataColumnSpec columnSpec = m_lastSpecs[0].getColumnSpec(categoryColIdx);
            final Set<DataCell> values = columnSpec.getDomain().getValues();

            if ((values == null) || (values.size() < 2)) {
                m_dirtyConfigure = true;
                updateWarning();
                return;
            }

            final Set<String> categories = new HashSet<>();
            if (columnSpec.getType().isCompatible(BooleanValue.class)) {
                categories.add("true");
                categories.add("false");
            } else {
                for (final DataCell value : values) {
                    categories.add(((StringValue)value).getStringValue());
                }
            }

            // in case an exception is thrown below, update the warning message here
            m_dirtyConfigure = false;
            updateWarning();

            // reload settings
            if (settings != null) {
                m_categoryComponent.replaceListItems(categories, null);
                m_categoryModel.loadSettingsFrom(settings);
            } else {
                final String defaultCategory = values.iterator().next().toString();
                m_categoryComponent.replaceListItems(categories, defaultCategory);
            }
        }
    }
}
