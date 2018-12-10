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

package org.knime.base.node.stats.testing.gtest;

import java.awt.Dimension;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "GTest" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class GTestNodeDialog extends DefaultNodeSettingsPane {

    private static final int INPUT_WIDTH = 5;

    private static final int LONG_INPUT_WIDTH = 260;

    private static final int INPUT_HEIGHT = 25;

    private SettingsModelString m_observedColumnModel;

    private SettingsModelString m_observed2ColumnModel;

    private SettingsModelString m_expectedColumnModel;

    /**
     * New pane for configuring ProportionTest node dialog.
     */
    @SuppressWarnings({"unchecked"})
    protected GTestNodeDialog() {
        m_observedColumnModel = GTestNodeModel.createSettingsModelObserved();
        final DialogComponentColumnNameSelection observedComponent = new DialogComponentColumnNameSelection(
            m_observedColumnModel, "Observed column", GTestNodeModel.PORT_IN_DATA, LongValue.class);
        addDialogComponent(observedComponent);
        observedComponent.getComponentPanel().getComponent(1)
            .setPreferredSize(new Dimension(LONG_INPUT_WIDTH, INPUT_HEIGHT));

        m_observed2ColumnModel = GTestNodeModel.createSettingsModelObserved2();
        final DialogComponentColumnNameSelection observed2Component = new DialogComponentColumnNameSelection(
            m_observed2ColumnModel, "2nd Observed column", GTestNodeModel.PORT_IN_DATA, LongValue.class);
        addDialogComponent(observed2Component);
        observed2Component.getComponentPanel().getComponent(1)
            .setPreferredSize(new Dimension(LONG_INPUT_WIDTH, INPUT_HEIGHT));

        m_expectedColumnModel = GTestNodeModel.createSettingsModelExpected();
        final DialogComponentColumnNameSelection expectedComponent = new DialogComponentColumnNameSelection(
            m_expectedColumnModel, "Expected column", GTestNodeModel.PORT_IN_DATA, DoubleValue.class);
        addDialogComponent(expectedComponent);
        expectedComponent.getComponentPanel().getComponent(1)
            .setPreferredSize(new Dimension(LONG_INPUT_WIDTH, INPUT_HEIGHT));

        final DialogComponentNumber alphaComponent = new DialogComponentNumber(
            GTestNodeModel.createSettingsModelAlpha(), "Significance level (alpha)", 0.01, INPUT_WIDTH);
        addDialogComponent(alphaComponent);

        // TODO: check min values in currently selected columns for >=0 resp >0?
        // TODO: chosen columns have to be different
    }
}
