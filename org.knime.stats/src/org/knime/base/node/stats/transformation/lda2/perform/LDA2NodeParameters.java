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

package org.knime.base.node.stats.transformation.lda2.perform;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.mine.transformation.settings.TransformationApplySettings;
import org.knime.base.node.mine.transformation.settings.TransformationComputeSettings;
import org.knime.base.node.mine.transformation.settings.TransformationReverseSettings;
import org.knime.base.node.stats.transformation.lda2.LDAInputColumnsChoicesProvider;
import org.knime.base.node.stats.transformation.lda2.settings.LDAComputeSettings;
import org.knime.base.node.stats.transformation.lda2.util.LDAUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Linear Discriminant Analysis.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class LDA2NodeParameters implements NodeParameters {

    @Widget(title = "Target dimensions", description = """
            Number of dimensions to reduce the input data to. This cannot exceed the number of classes minus
            one or the number of selected columns, depending on which one is smaller.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @ValueReference(TargetDimensionRef.class)
    @Persist(configKey = TransformationApplySettings.K_CFG)
    int m_targetDimensions = 1;

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
    @ValueReference(InputColumnsRef.class)
    ColumnFilter m_inputColumns = new ColumnFilter();

    @TextMessage(TargetDimensionError.class)
    Void m_targetDimensionError;

    @Widget(title = "Remove original data columns", description = """
            If checked, the columns containing the input data are removed from the output table.
            """)
    @Persist(configKey = TransformationReverseSettings.REMOVE_USED_COLS_CFG)
    boolean m_removeOriginalColumns;

    @Widget(title = "Fail if missing values are encountered", description = """
            If checked, execution fails when the selected columns contain missing values. Otherwise, rows
            containing missing values are ignored during computation.
            """)
    @Persist(configKey = TransformationApplySettings.FAIL_ON_MISSING_CFG)
    boolean m_failOnMissingValues;

    static final class TargetDimensionRef implements ParameterReference<Integer> {
    }

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    static final class InputColumnsRef implements ParameterReference<ColumnFilter> {
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

    static final class TargetDimensionError implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<Integer> m_targetDimensionSupplier;

        Supplier<String> m_classColumnSupplier;

        Supplier<ColumnFilter> m_inputColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            initializer.computeOnValueChange(TargetDimensionRef.class);
            initializer.computeOnValueChange(ClassColumnRef.class);
            initializer.computeOnValueChange(InputColumnsRef.class);
            m_targetDimensionSupplier = initializer.getValueSupplier(TargetDimensionRef.class);
            m_classColumnSupplier = initializer.getValueSupplier(ClassColumnRef.class);
            m_inputColumnsSupplier = initializer.getValueSupplier(InputColumnsRef.class);
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var targetDimension = m_targetDimensionSupplier.get();
            final var classCol = m_classColumnSupplier.get();

            final var specOpt = checkInputValues(targetDimension, classCol, parametersInput);
            if (specOpt.isEmpty()) {
                return Optional.empty();
            }
            final var spec = specOpt.get();

            final var domainOpt = Optional.ofNullable(spec.getColumnSpec(classCol));
            if (domainOpt.isEmpty()) {
                return Optional.empty();
            }
            final var domain = domainOpt.get().getDomain();

            var selectedClasses = Integer.MAX_VALUE; // indicating that the domain was not calculated
            if (domain.hasValues()) {
                selectedClasses = domain.getValues().size();
            }

            final var selectedColumns = m_inputColumnsSupplier.get().filterFromFullSpec(spec);
            final var numberOfSelectedColumns = selectedColumns == null ? 0 : selectedColumns.length;
            final var maximumDim = Math.min(selectedClasses - 1, numberOfSelectedColumns);
            if (maximumDim <= 0) {
                return Optional.of(new TextMessage.Message("Maximum dimension is zero",
                    LDAUtils.createMaxDimZeroWarning(selectedClasses, numberOfSelectedColumns, classCol),
                    TextMessage.MessageType.ERROR));
            } else if (targetDimension > maximumDim) {
                return Optional.of(new TextMessage.Message("Target dimension too high",
                    LDAUtils.createTooHighDimWarning(targetDimension,
                        maximumDim, selectedClasses, numberOfSelectedColumns, classCol),
                    TextMessage.MessageType.ERROR));
            }

            return Optional.empty();
        }

        private static Optional<DataTableSpec> checkInputValues(final Integer targetDimension,
            final String classCol, final NodeParametersInput parametersInput) {
            if (targetDimension == null) {
                return Optional.empty();
            }

            if (classCol == null || classCol.isEmpty()) {
                return Optional.empty();
            }

            return parametersInput.getInTableSpec(0);
        }

    }

}
