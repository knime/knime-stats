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

package org.knime.base.node.stats.testing.friedman;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.EnumFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.persistence.legacy.LongAsStringPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Friedman Test.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class FriedmanTestNodeParameters implements NodeParameters {

    private static final List<Class<? extends DataValue>> NUMERIC_TYPES =
            List.of(DoubleValue.class, IntValue.class, LongValue.class);

    private static final String COLUMNS_KEY = "Used columns";

    private static final String ALPHA_KEY = "Alpha";

    private static final String NAN_STRATEGY_KEY = "NaN-Strategy";

    private static final String TIE_STRATEGY_KEY = "Tie-Strategy";

    private static final String USE_RANDOM_SEED_KEY = "Use Random Seed?";

    private static final String SEED_KEY = "Seed";

    // ====== Significance Level Alpha

    private static final class IsAtMostOneValidation extends MaxValidation {

        @Override
        protected double getMax() {
            return 1;
        }
    }

    @Widget(title = "Significance level α", description = """
            The significance level for the hypothesis test. A difference in the samples is assumed
            based on this value, where 0 ≤ α ≤ 1. Commonly used values are 0.05 or 0.01.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = IsAtMostOneValidation.class)
    @Persist(configKey = ALPHA_KEY)
    double m_alpha = 0.05;

    // ====== Column Filter Settings

    private static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        private ColumnFilterPersistor() {
            super(COLUMNS_KEY);
        }
    }

    private static final class NumericChoicesProvider extends CompatibleColumnsProvider {
        private NumericChoicesProvider() {
            super(NUMERIC_TYPES);
        }
    }

    private static class ColumnFilterRef implements ParameterReference<ColumnFilter> {
    }

    @Widget(title = "Distributions", description = """
            Select the columns (samples/distributions) to be included in the Friedman test.
            Each column represents a treatment or condition, and each row represents a block or subject.
            At least 3 columns must be selected.
            """)
    @ChoicesProvider(NumericChoicesProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    @ValueReference(ColumnFilterRef.class)
    ColumnFilter m_usedColumns = new ColumnFilter();

    private static final class TooFewColumnsMessage implements StateProvider<Optional<TextMessage.Message>> {

        private Supplier<ColumnFilter> m_columnFilterSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_columnFilterSupplier = initializer.computeFromValueSupplier(ColumnFilterRef.class);
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var inSpec = parametersInput.getInTableSpec(FriedmanTestNodeModel.PORT_IN_DATA).orElse(null);
            if (inSpec == null) {
                return Optional.empty();
            }

            final var k = m_columnFilterSupplier.get().filter(inSpec.stream() //
                .filter(col -> NUMERIC_TYPES.stream().anyMatch(t -> col.getType().isCompatible(t))) //
                .toList()).length;
            if (k >= 3) {
                return Optional.empty();
            }

            final var message = "Not enough data columns chosen (" + k + "), please choose more than 2.";
            return Optional.of(new TextMessage.Message(message, "", TextMessage.MessageType.ERROR));
        }
    }

    @TextMessage(TooFewColumnsMessage.class)
    Void m_tooFewColumnsMessage;

    // ====== Missing Values Strategy

    private enum MissingValuesStrategy {
        @Label(value = "Minimal", description = "Missing values are considered minimal in the ordering")
        MINIMAL,
        @Label(value = "Maximal", description = "Missing values are considered maximal in the ordering")
        MAXIMAL,
        @Label(value = "Fixed", description = "Missing values are left in place")
        FIXED,
        @Label(value = "Failed", description = "Missing values result in node failure")
        FAILED;
    }

    private static final class MissingValuesStrategyPersistor
            implements NodeParametersPersistor<MissingValuesStrategy> {

        private static final EnumFieldPersistor<MissingValuesStrategy> INSTANCE =
                new EnumFieldPersistor<>(NAN_STRATEGY_KEY, MissingValuesStrategy.class, false);

        @Override
        public MissingValuesStrategy load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            return INSTANCE.load(nodeSettings);
        }

        @Override
        public void save(final MissingValuesStrategy operator, final NodeSettingsWO nodeSettings) {
            INSTANCE.save(operator, nodeSettings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] { {NAN_STRATEGY_KEY} };
        }
    }

    @Widget(title = "Missing Values Strategy", description = """
            Specifies how missing values are handled when ranking is done.
            """, advanced = true)
    @Persistor(MissingValuesStrategyPersistor.class)
    MissingValuesStrategy m_missingValuesStrategy = MissingValuesStrategy.FAILED;

    // ====== Ties Strategy

    private enum TiesStrategy {
        @Label(value = "Sequential", description = "Ties assigned sequential ranks in order of occurrence")
        SEQUENTIAL,
        @Label(value = "Minimum", description = "Ties get the minimum applicable rank")
        MINIMUM,
        @Label(value = "Maximum", description = "Ties get the maximum applicable rank")
        MAXIMUM,
        @Label(value = "Average", description = "Ties get the average of applicable ranks")
        AVERAGE,
        @Label(value = "Random", description = "Ties get a random integral value from among applicable ranks")
        RANDOM;
    }

    private static final class TiesStrategyPersistor implements NodeParametersPersistor<TiesStrategy> {

        private static final EnumFieldPersistor<TiesStrategy> INSTANCE =
                new EnumFieldPersistor<>(TIE_STRATEGY_KEY, TiesStrategy.class, false);

        @Override
        public TiesStrategy load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            return INSTANCE.load(nodeSettings);
        }

        @Override
        public void save(final TiesStrategy operator, final NodeSettingsWO nodeSettings) {
            INSTANCE.save(operator, nodeSettings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] { {TIE_STRATEGY_KEY} };
        }
    }

    @Widget(title = "Ties Strategy", description = """
            Specifies how ties in each block are handled when ranking is done.
            """, advanced = true)
    @Persistor(TiesStrategyPersistor.class)
    @ValueReference(TiesStrategyRef.class)
    TiesStrategy m_tiesStrategy = TiesStrategy.AVERAGE;

    private static class TiesStrategyRef implements ParameterReference<TiesStrategy> {
    }

    private static final class IsTiesStrategyRandom implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(TiesStrategyRef.class).isOneOf(TiesStrategy.RANDOM);
        }
    }

    // ====== Seed Settings

    static final class UseRandomSeedRef implements BooleanReference {
    }

    @Widget(title = "Use Random Seed", description = """
            Enable to use a custom seed for the random number generator when the Ties Strategy
            is set to Random. If disabled, a random seed will be used.
            """, advanced = true)
    @ValueReference(UseRandomSeedRef.class)
    @Persist(configKey = USE_RANDOM_SEED_KEY)
    @Effect(predicate = IsTiesStrategyRandom.class, type = EffectType.SHOW)
    boolean m_useRandomSeed = false;

    private static final class UseRandomSeedPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsTiesStrategyRandom.class).and(i.getPredicate(UseRandomSeedRef.class));
        }
    }

    private static final class NewSeedButtonRef implements ButtonReference {
    }

    private static final class RandomSeedValueProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(NewSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return Long.toString((long)(((Math.random() * 2.0) - 1) * Long.MAX_VALUE));
        }
    }

    private static final class SeedPersistor extends LongAsStringPersistor {
        SeedPersistor() {
            super(SEED_KEY);
        }
    }

    @Widget(title = "Seed", advanced = true,
            description = "The seed value for the random number generator used when randomizing tied ranks.")
    @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
    @Effect(predicate = UseRandomSeedPredicate.class, type = EffectType.SHOW)
    @ValueProvider(RandomSeedValueProvider.class)
    @Persistor(SeedPersistor.class)
    String m_seed = "1234567890123";

    @Widget(title = "New seed", advanced = true,
        description = "Generate a random seed and set it in the Seed input above for reproducible runs.")
    @SimpleButtonWidget(ref = NewSeedButtonRef.class)
    @Effect(predicate = UseRandomSeedPredicate.class, type = EffectType.SHOW)
    Void m_drawSeed;
}
