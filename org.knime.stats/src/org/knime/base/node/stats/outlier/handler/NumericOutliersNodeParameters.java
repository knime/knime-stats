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

package org.knime.base.node.stats.outlier.handler;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.algorithms.outlier.NumericOutliersReviser;
import org.knime.base.algorithms.outlier.options.NumericOutliersDetectionOption;
import org.knime.base.algorithms.outlier.options.NumericOutliersReplacementStrategy;
import org.knime.base.algorithms.outlier.options.NumericOutliersTreatmentOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node parameters for Numeric Outliers.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class NumericOutliersNodeParameters implements NodeParameters {

    NumericOutliersNodeParameters() {
    }

    NumericOutliersNodeParameters(final NodeParametersInput input) {
        final var spec = input.getInTableSpec(0);
        if (spec.isEmpty()) {
            return;
        }
        final var numericColumns =
            ColumnSelectionUtil.getCompatibleColumns(spec.get(), NumericOutliersReviser.SUPPORTED_DATA_VALUES);

        m_outlierColumns = numericColumns.isEmpty() ? new ColumnFilter() : new ColumnFilter(numericColumns);
        m_groupColumns = new ColumnFilter();
    }

    @Section(title = "General Settings")
    interface GeneralSettingsSection {
    }

    @Section(title = "Outlier Treatment")
    @After(GeneralSettingsSection.class)
    interface OutlierTreatmentSection {
    }

    @Section(title = "Group Settings")
    @After(OutlierTreatmentSection.class)
    interface GroupSettingsSection {
    }

    @Widget(title = "Outlier selection", description = """
            Allows the selection of columns for which outliers have to be detected and treated. If "Compute
            outlier statistics on groups" is selected, the outliers for each of the columns are computed solely
            with respect to the different groups.
            """)
    @ColumnFilterWidget(choicesProvider = NumericColumnsProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @Persistor(OutlierColumnFilterPersistor.class)
    ColumnFilter m_outlierColumns = new ColumnFilter();

    @Layout(GeneralSettingsSection.class)
    @Widget(title = "Interquartile range multiplier (k)", description = """
            Allows scaling the interquartile range (IQR). The default is k = 1.5. Larger values will cause
            less values to be considered outliers.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = NumericOutliersNodeModel.CFG_SCALAR_PAR)
    double m_iqrScalar = NumericOutliersNodeModel.DEFAULT_SCALAR;

    @Layout(GeneralSettingsSection.class)
    @Widget(title = "Quartile calculation", description = "Allows to specify how the quartiles are computed.")
    @ValueReference(QuartileCalculationRef.class)
    @RadioButtonsWidget
    @Persistor(HeuristicPersistor.class)
    QuartileCalculationMethod m_quartileCalculation = QuartileCalculationMethod.HEURISTIC;

    @Layout(GeneralSettingsSection.class)
    @Widget(title = "Estimation type", description = """
            Specifies how the actual quartile value is computed when using full data estimate. \
            A detailed explanation of the different types can be found \
            <a href="https://en.wikipedia.org/wiki/Quantile#Estimating_quantiles_from_a_sample">here</a>.
            """)
    @Persistor(EstimationTypePersistor.class)
    @Effect(predicate = QuartileCalculationIsFullData.class, type = EffectType.SHOW)
    EstimationType m_estimationType = NumericOutliersNodeModel.DEFAULT_ESTIMATION_TYPE;

    @Layout(GeneralSettingsSection.class)
    @Widget(title = "Update domain", description = "If checked the domain of the selected outlier columns is updated.")
    @Persist(configKey = NumericOutliersNodeModel.CFG_DOMAIN_POLICY)
    boolean m_updateDomain = NumericOutliersNodeModel.DEFAULT_DOMAIN_POLICY;

    @Layout(OutlierTreatmentSection.class)
    @Widget(title = "Apply to", description = "Allows to apply the selected treatment strategy to")
    @Persistor(DetectionOptionPersistor.class)
    NumericOutliersDetectionOption m_detectionOption = NumericOutliersDetectionOption.ALL;

    @Layout(OutlierTreatmentSection.class)
    @Widget(title = "Treatment option", description = "Defines three different strategies to treat outliers:")
    @ValueReference(TreatmentOptionRef.class)
    @Persistor(TreatmentOptionPersistor.class)
    NumericOutliersTreatmentOption m_treatmentOption = NumericOutliersTreatmentOption.REPLACE;

    @Layout(OutlierTreatmentSection.class)
    @Widget(title = "Replacement strategy", description = "Defines two different strategies to replace outliers:")
    @Persistor(ReplacementStrategyPersistor.class)
    @Effect(predicate = TreatmentIsReplace.class, type = EffectType.SHOW)
    NumericOutliersReplacementStrategy m_replacementStrategy = NumericOutliersReplacementStrategy.MISSING;

    @Layout(GroupSettingsSection.class)
    @Widget(title = "Compute outlier statistics on groups", description = """
            If selected, allows the selection of columns to identify groups. A group comprises all rows of
            the input exhibiting the same values in every single column. The outliers will finally be computed
            with respect to each of the individual groups.
            """)
    @ValueReference(UseGroupsRef.class)
    @Persist(configKey = NumericOutliersNodeModel.CFG_USE_GROUPS)
    boolean m_useGroups;

    @Layout(GroupSettingsSection.class)
    @Widget(title = "Group columns", description = """
            Move the columns defining the groups into the Include list. The group definition will take
            priority, i.e. if a column is selected for both group definition and outlier handling, it will be
            used to define groups (no outlier handling done for that column).
            """)
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    @Persistor(GroupColumnFilterPersistor.class)
    @Effect(predicate = UseGroupsIsEnabled.class, type = EffectType.SHOW)
    ColumnFilter m_groupColumns = new ColumnFilter();

    @Layout(GroupSettingsSection.class)
    @Widget(title = "Process groups in memory", description = """
            Processes the groups in the memory. This option comes with higher memory requirements, but is
            faster since the table does not need any additional treatment.
            """)
    @Effect(predicate = UseGroupsIsEnabled.class, type = EffectType.SHOW)
    @Persist(configKey = NumericOutliersNodeModel.CFG_MEM_POLICY)
    boolean m_processInMemory = NumericOutliersNodeModel.DEFAULT_MEM_POLICY;

    static final class NumericColumnsProvider extends CompatibleColumnsProvider {

        protected NumericColumnsProvider() {
            super(NumericOutliersReviser.SUPPORTED_DATA_VALUES);
        }

    }

    static final class QuartileCalculationRef implements ParameterReference<QuartileCalculationMethod> {
    }

    static final class TreatmentOptionRef implements ParameterReference<NumericOutliersTreatmentOption> {
    }

    static final class UseGroupsRef implements ParameterReference<Boolean> {
    }

    static final class QuartileCalculationIsFullData implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(QuartileCalculationRef.class).isOneOf(QuartileCalculationMethod.FULL_DATA);
        }
    }

    static final class TreatmentIsReplace implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(TreatmentOptionRef.class).isOneOf(NumericOutliersTreatmentOption.REPLACE);
        }
    }

    static final class UseGroupsIsEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseGroupsRef.class).isTrue();
        }
    }

    static final class OutlierColumnFilterPersistor extends LegacyColumnFilterPersistor {
        OutlierColumnFilterPersistor() {
            super(NumericOutliersNodeModel.CFG_OUTLIER_COLS);
        }
    }

    static final class GroupColumnFilterPersistor extends LegacyColumnFilterPersistor {
        GroupColumnFilterPersistor() {
            super(NumericOutliersNodeModel.CFG_GROUP_COLS);
        }
    }

    static final class HeuristicPersistor implements NodeParametersPersistor<QuartileCalculationMethod> {
        @Override
        public QuartileCalculationMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final boolean heuristic =
                Boolean.parseBoolean(settings.getString(NumericOutliersNodeModel.CFG_HEURISTIC, "false"));
            return heuristic ? QuartileCalculationMethod.HEURISTIC : QuartileCalculationMethod.FULL_DATA;
        }

        @Override
        public void save(final QuartileCalculationMethod obj, final NodeSettingsWO settings) {
            final boolean heuristic = obj == QuartileCalculationMethod.HEURISTIC;
            settings.addString(NumericOutliersNodeModel.CFG_HEURISTIC, String.valueOf(heuristic));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NumericOutliersNodeModel.CFG_HEURISTIC}};
        }
    }

    static final class EstimationTypePersistor implements NodeParametersPersistor<EstimationType> {
        @Override
        public EstimationType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String typeName = settings.getString(NumericOutliersNodeModel.CFG_ESTIMATION_TYPE,
                NumericOutliersNodeModel.DEFAULT_ESTIMATION_TYPE.name());
            return EstimationType.valueOf(typeName);
        }

        @Override
        public void save(final EstimationType obj, final NodeSettingsWO settings) {
            settings.addString(NumericOutliersNodeModel.CFG_ESTIMATION_TYPE, obj.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NumericOutliersNodeModel.CFG_ESTIMATION_TYPE}};
        }
    }

    static final class DetectionOptionPersistor implements NodeParametersPersistor<NumericOutliersDetectionOption> {
        @Override
        public NumericOutliersDetectionOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String optionName = settings.getString(NumericOutliersNodeModel.CFG_DETECTION_OPTION,
                NumericOutliersDetectionOption.values()[0].toString());
            return NumericOutliersDetectionOption.getEnum(optionName);
        }

        @Override
        public void save(final NumericOutliersDetectionOption obj, final NodeSettingsWO settings) {
            settings.addString(NumericOutliersNodeModel.CFG_DETECTION_OPTION, obj.toString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NumericOutliersNodeModel.CFG_DETECTION_OPTION}};
        }
    }

    static final class TreatmentOptionPersistor implements NodeParametersPersistor<NumericOutliersTreatmentOption> {
        @Override
        public NumericOutliersTreatmentOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String optionName = settings.getString(NumericOutliersNodeModel.CFG_OUTLIER_TREATMENT,
                NumericOutliersTreatmentOption.values()[0].toString());
            return NumericOutliersTreatmentOption.getEnum(optionName);
        }

        @Override
        public void save(final NumericOutliersTreatmentOption obj, final NodeSettingsWO settings) {
            settings.addString(NumericOutliersNodeModel.CFG_OUTLIER_TREATMENT, obj.toString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NumericOutliersNodeModel.CFG_OUTLIER_TREATMENT}};
        }
    }

    static final class ReplacementStrategyPersistor
        implements NodeParametersPersistor<NumericOutliersReplacementStrategy> {
        @Override
        public NumericOutliersReplacementStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String strategyName = settings.getString(NumericOutliersNodeModel.CFG_OUTLIER_REPLACEMENT,
                NumericOutliersReplacementStrategy.values()[0].toString());
            return NumericOutliersReplacementStrategy.getEnum(strategyName);
        }

        @Override
        public void save(final NumericOutliersReplacementStrategy obj, final NodeSettingsWO settings) {
            settings.addString(NumericOutliersNodeModel.CFG_OUTLIER_REPLACEMENT, obj.toString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NumericOutliersNodeModel.CFG_OUTLIER_REPLACEMENT}};
        }
    }

    enum QuartileCalculationMethod {
        @Label(value = "Use heuristic (memory friendly)", description = "This option ensure that the quartiles are "
            + "calculated using a heuristical approach. This choice is recommended for large data sets due to its low "
            + "memory requirements. However, for small data sets the results of this approach can be quite far away "
            + "from the accurate results.")
        HEURISTIC,
        @Label(value = "Full data estimate", description = """
                This option typically creates more accurate results than its counterpart, but also requires far more
                additional memory. Therefore, we recommend this option for smaller data sets. <br/>
                Since the value of the quartiles often lies between two observations, this option additionally allows
                to specify how the actual value is computed, which is encoded by the various estimation types
                (LEGACY, R_1, ..., R_9). A detailed explanation of the different types can be found
                <a href="https://en.wikipedia.org/wiki/Quantile#Estimating_quantiles_from_a_sample">here</a>.
                """)
        FULL_DATA
    }

}
