/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.stats.correlation.rank2;

import org.knime.base.node.preproc.correlation.compute2.PValueAlternative;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog for correlation node. Shows only a column filter.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Iris Adae, University of Konstanz
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class RankCorrelationCompute2NodeDialogPane extends DefaultNodeSettingsPane {

    private final SettingsModelString m_typeModel;

    private final SettingsModelString m_pValAlternativeModel;

    /** Inits dialog, adds only a column filter. */
    public RankCorrelationCompute2NodeDialogPane() {
        m_typeModel = RankCorrelationCompute2NodeModel.createTypeModel();
        addDialogComponent(new DialogComponentStringSelection(m_typeModel, "Correlation Type:",
            RankCorrelationCompute2NodeModel.getCorrelationTypes()));
        m_typeModel.addChangeListener(a -> typeSelectionChanged());

        addDialogComponent(
            new DialogComponentColumnFilter2(RankCorrelationCompute2NodeModel.createColumnFilterModel(), 0));

        m_pValAlternativeModel = RankCorrelationCompute2NodeModel.createPValAlternativeModel();
        addDialogComponent(new DialogComponentButtonGroup(m_pValAlternativeModel, "p-value", true,
            PValueAlternative.descriptions(), PValueAlternative.names()));
    }

    private void typeSelectionChanged() {
        final boolean enabled = RankCorrelationCompute2NodeModel.CFG_SPEARMAN.equals(m_typeModel.getStringValue());
        m_pValAlternativeModel.setEnabled(enabled);
    }
}
