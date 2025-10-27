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

package org.knime.base.node.stats.testing.anova;

import java.util.Arrays;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
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
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;

/**
 * Node parameters for One-way ANOVA.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class OneWayANOVANodeParameters implements NodeParameters {

    static final Class<? extends DataValue>[] GROUPING_COLUMN_TYPES =
            new Class[]{NominalValue.class, DoubleValue.class};

    @Widget(title = "Factor", description = """
            Column holding the grouping information data. This column defines the groups
            to be compared in the one-way ANOVA test.
            """)
    @ChoicesProvider(GroupingColumnChoicesProvider.class)
    @Persist(configKey = OneWayANOVANodeSettings.GROUPING_COLUMN)
    @ValueReference(GroupColumnRef.class)
    @ValueProvider(GroupColumnDefaultProvider.class)
    String m_groupingColumn;

    @Widget(title = "Confidence Interval (in %)", description = """
            The limits for the confidence interval are computed using this number. The default is 95 which
            means that you can be 95% confident that the true value of the parameter is in the confidence
            interval.
            """)
    @NumberInputWidget(minValidation = IsMinimumConfidence.class, maxValidation = IsMaximumConfidence.class)
    @Persistor(ConfidenceIntervalPersistor.class)
    double m_confidenceIntervalProb = OneWayANOVANodeSettings.DEFAULT_CONFIDENCE_INTERVAL_PROB * 100.0;

    @Widget(title = "Test columns", description = """
            A one-way ANOVA is performed separately for each of these columns.
            Only columns of numeric type (Double) can be selected for testing.
            """)
    @ColumnFilterWidget(choicesProvider = DoubleColumnsProvider.class)
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

    static final class GroupColumnRef implements ParameterReference<String> {
    }

    static final class GroupingColumnChoicesProvider extends CompatibleColumnsProvider {
        protected GroupingColumnChoicesProvider() {
            super(Arrays.asList(GROUPING_COLUMN_TYPES));
        }
    }

    static final class DoubleColumnsProvider extends CompatibleColumnsProvider {
        protected DoubleColumnsProvider() {
            super(DoubleValue.class);
        }
    }

    static final class GroupColumnDefaultProvider extends ColumnNameAutoGuessValueProvider {

        protected GroupColumnDefaultProvider() {
            super(GroupColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(
                parametersInput, GROUPING_COLUMN_TYPES);
            return compatibleColumns.isEmpty() ? Optional.empty() :
                Optional.of(compatibleColumns.get(compatibleColumns.size()-1));
        }

    }

    static final class ConfidenceIntervalPersistor implements NodeParametersPersistor<Double> {

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getDouble(OneWayANOVANodeSettings.CONFIDENCE_INTERVAL_PROB,
                OneWayANOVANodeSettings.DEFAULT_CONFIDENCE_INTERVAL_PROB) * 100.0;
        }

        @Override
        public void save(final Double param, final NodeSettingsWO settings) {
            settings.addDouble(OneWayANOVANodeSettings.CONFIDENCE_INTERVAL_PROB, param / 100.0);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{OneWayANOVANodeSettings.CONFIDENCE_INTERVAL_PROB}};
        }
    }

    static final class TestColumnsPersistor extends LegacyColumnFilterPersistor {
        TestColumnsPersistor() {
            super(OneWayANOVANodeSettings.TEST_COLUMNS);
        }
    }

}
