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
 *  propagation of KNIME.
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

package org.knime.base.node.stats.testing.ttest;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Independent groups t-test.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class TwoSampleTTestNodeParameters implements NodeParameters {

    @Persist(configKey = TwoSampleTTestNodeSettings.GROUPING_COLUMN)
    @Widget(title = "Grouping column", description = """
            Column holding the grouping information data.
            """)
    @ChoicesProvider(GroupingColumnChoicesProvider.class)
    @ValueReference(GroupingColumnRef.class)
    String m_groupingColumn;

    @Persist(configKey = TwoSampleTTestNodeSettings.GROUP_ONE)
    @Widget(title = "Group one", description = """
            All rows of the input table which have this this value in the grouping column are assigned to group one.
            """)
    @TextInputWidget
    @ChoicesProvider(GroupValuesChoicesProvider.class)
    String m_groupOne;

    @Persist(configKey = TwoSampleTTestNodeSettings.GROUP_TWO)
    @Widget(title = "Group two", description = """
            All rows of the input table which have this this value in the grouping column are assigned to group two.
            """)
    @TextInputWidget
    @ChoicesProvider(GroupValuesChoicesProvider.class)
    String m_groupTwo;

    @Widget(title = "Confidence Interval (in %)", description = """
            The limits for the confidence interval are computed using this number. The default is 95 which means that
            you can be 95% confident that the true value of the parameter is in the confidence interval.
            """)
    @NumberInputWidget(minValidation = IsMinimumConfidence.class, maxValidation = IsMaximumConfidence.class)
    @Persistor(ConfidenceIntervalPersistor.class)
    double m_confidenceIntervalProb = TwoSampleTTestNodeSettings.DEFAULT_CONFIDENCE_INTERVAL_PROB * 100.0;

    @Widget(title = "Test columns", description = """
            A independent groups t-test is performed separately for each of these columns.
            """)
    @ChoicesProvider(TestColumnsChoicesProvider.class)
    @Persistor(TestColumnsPersistor.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    ColumnFilter m_testColumns = new ColumnFilter();

    static final class IsMinimumConfidence extends MinValidation {

        @Override
        public double getMin() {
            return 1;
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

    }

    static final class IsMaximumConfidence extends MaxValidation {

        @Override
        public double getMax() {
            return 99;
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

    }

    static final class GroupingColumnRef implements ParameterReference<String> {
    }

    static final class GroupingColumnChoicesProvider extends CompatibleColumnsProvider {
        GroupingColumnChoicesProvider() {
            super(List.of(NominalValue.class, DoubleValue.class));
        }
    }

    static final class TestColumnsChoicesProvider extends CompatibleColumnsProvider {
        TestColumnsChoicesProvider() {
            super(List.of(DoubleValue.class));
        }
    }

    static final class GroupValuesChoicesProvider implements StringChoicesProvider {

        private Supplier<String> m_groupingColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            initializer.computeOnValueChange(GroupingColumnRef.class);
            m_groupingColumnSupplier = initializer.getValueSupplier(GroupingColumnRef.class);
        }

        @Override
        public List<String> choices(final NodeParametersInput context) {
            final var groupingColumn = m_groupingColumnSupplier.get();
            if (groupingColumn == null || groupingColumn.isEmpty()) {
                return List.of();
            }

            final var specOpt = context.getInTableSpec(0);
            if (specOpt.isEmpty()) {
                return List.of();
            }

            final var spec = specOpt.get();
            final DataColumnSpec colSpec = spec.getColumnSpec(groupingColumn);
            final DataColumnDomain domain = colSpec.getDomain();

            if (domain.hasValues()) {
                return domain.getValues().stream()
                    .map(DataCell::toString)
                    .toList();
            } else if (domain.hasBounds()) {
                return List.of(
                    domain.getLowerBound().toString(),
                    domain.getUpperBound().toString()
                );
            }

            return List.of();
        }
    }

    static final class TestColumnsPersistor extends LegacyColumnFilterPersistor {
        TestColumnsPersistor() {
            super(TwoSampleTTestNodeSettings.TEST_COLUMNS);
        }
    }

    static final class ConfidenceIntervalPersistor implements NodeParametersPersistor<Double> {

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final double prob = settings.getDouble(TwoSampleTTestNodeSettings.CONFIDENCE_INTERVAL_PROB, 0.95);
            return prob * 100.0;
        }

        @Override
        public void save(final Double percentage, final NodeSettingsWO settings) {
            final double prob = percentage / 100.0;
            settings.addDouble(TwoSampleTTestNodeSettings.CONFIDENCE_INTERVAL_PROB, prob);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{TwoSampleTTestNodeSettings.CONFIDENCE_INTERVAL_PROB}};
        }
    }
}
