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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.stats.transformation.lda2.compute;

import java.util.List;

import org.knime.base.node.mine.transformation.settings.TransformationComputeSettings;
import org.knime.base.node.stats.transformation.lda2.LDAInputColumnsChoicesProvider;
import org.knime.base.node.stats.transformation.lda2.settings.LDAComputeSettings;
import org.knime.core.data.NominalValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Linear Discriminant Analysis Compute.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class LDAComputeNodeParameters implements NodeParameters {

    @Widget(title = "Class column", description = """
            Column containing class information.
            """)
    @Persist(configKey = LDAComputeSettings.CLASS_COL_CFG)
    @ChoicesProvider(ClassColumnChoicesProvider.class)
    @ValueReference(ClassColumnRef.class)
    String m_classColumn;

    @Widget(title = "Column selection", description = """
            Columns containing the input data.
            """)
    @ColumnFilterWidget(choicesProvider = InputColumnsChoicesProvider.class)
    @Persistor(InputColumnsPersistor.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    ColumnFilter m_inputColumns = new ColumnFilter();

    @Widget(title = "Fail if missing values are encountered", description = """
            If checked, execution fails when the selected columns contain missing values. Otherwise, rows
            containing missing values are ignored during computation.
            """)
    @Persist(configKey = TransformationComputeSettings.FAIL_ON_MISSING_CFG)
    boolean m_failOnMissingValues;

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    static final class ClassColumnChoicesProvider extends CompatibleColumnsProvider {
        ClassColumnChoicesProvider() {
            super(List.of(NominalValue.class));
        }
    }

    static final class InputColumnsChoicesProvider extends LDAInputColumnsChoicesProvider {

        InputColumnsChoicesProvider() {
            super(ClassColumnRef.class);
        }

    }

    static final class InputColumnsPersistor extends LegacyColumnFilterPersistor {
        InputColumnsPersistor() {
            super(TransformationComputeSettings.USED_COLS_CFG);
        }
    }

}
