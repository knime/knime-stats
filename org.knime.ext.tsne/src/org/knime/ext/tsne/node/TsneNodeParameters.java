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

package org.knime.ext.tsne.node;

import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.persistence.legacy.LongAsStringPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for t-SNE (L. Jonsson).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class TsneNodeParameters implements NodeParameters {

    @Persistor(FeaturesPersistor.class)
    @Widget(title = "Columns", description = """
            Select the columns that are included by t-SNE i.e. the original features. Note that currently
            only numerical columns are supported.
            """)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    ColumnFilter m_features = new ColumnFilter();

    @Persist(configKey = TsneNodeModel.CFG_OUTPUT_DIMENSIONS)
    @Widget(title = "Dimension(s) to reduce to", description = """
            The number of dimension of the target embedding (for visualization typically 2 or 3).
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_outputDimensions = 2;

    @Persist(configKey = TsneNodeModel.CFG_ITERATIONS)
    @Widget(title = "Iterations", description = """
            The number of learning iterations to be performed. Too few iterations might result in a bad
            embedding, while too many iterations take a long time to train.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_iterations = 1000;

    @Persist(configKey = TsneNodeModel.CFG_THETA)
    @Widget(title = "Theta", description = """
            Controls the tradeoff between runtime and accuracy of the Barnes-Hut approximation algorithm for
            t-SNE. Lower values result in a more accurate approximation at the cost of higher runtimes and
            memory demands. A theta of zero results in the originally proposed t-SNE algorithm. However, for
            most datasets a theta of 0.5 does not result in a perceivable loss of quality.
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class)
    double m_theta = 0.5;

    @Persist(configKey = TsneNodeModel.CFG_PERPLEXITY)
    @Widget(title = "Perplexity", description = """
            Informally, the perplexity is the number of neighbors for each datapoint. Small perplexities
            focus more on local structure while larger perplexities take more global relationships into
            account. In most cases values in range [5,50] are sufficient. Note: The perplexity must be
            less than or equal to (Number of rows - 1) / 3.
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class)
    double m_perplexity = 30.0;

    @Persist(configKey = TsneNodeModel.CFG_NUMBER_OF_THREADS)
    @Widget(title = "Number of threads", description = """
            Number of threads used for parallel computation. The default is set to the number of cores your
            computer has and usually doesn't require tuning. Note that no parallelization is used if theta is
            zero because the exact t-SNE algorithm isn't parallelizable.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_numberOfThreads = Runtime.getRuntime().availableProcessors();

    @Persist(configKey = TsneNodeModel.CFG_REMOVE_ORIGINAL_COLUMNS)
    @Widget(title = "Remove original data columns", description = """
            Check this box if you want to remove the columns used to learn the embedding.
            """)
    boolean m_removeOriginalColumns;

    @Persist(configKey = TsneNodeModel.CFG_FAIL_ON_MISSING_VALUES)
    @Widget(title = "Fail if missing values are encountered", description = """
            If this box is checked, the node fails if it encounters a missing value in one of the columns
            used for learning. Otherwise, rows containing missing values in the learning columns will be
            ignored during learning and the corresponding embedding consists of missing values.
            """)
    boolean m_failOnMissingValues;

    @Persist(configKey = TsneNodeModel.CFG_SEED + "_BOOL")
    @Widget(title = "Use seed", description = """
            When enabled, the t-SNE algorithm will use a fixed seed value, making the results reproducible
            across multiple executions. When disabled, each execution will produce different random results.
            """)
    @ValueReference(UseSeedRef.class)
    boolean m_useSeed;

    @Persistor(SeedPersistor.class)
    @Widget(title = "Seed value", description = """
            The seed value used for random number generation. Use the same seed to get identical results
            across multiple executions. You can enter a custom value or use the random seed generation
            button below.
            """)
    @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
    @ValueProvider(SeedValueProvider.class)
    @Effect(predicate = UseSeedPredicate.class, type = EffectType.SHOW)
    String m_seed = Long.toString(TsneNodeModel.DEFAULT_SEED);

    @Widget(title = "Draw seed", description = """
            Generate a random seed and set it in the seed input above for reproducible runs.
            """)
    @SimpleButtonWidget(ref = DrawSeedButtonRef.class)
    @Effect(predicate = UseSeedPredicate.class, type = EffectType.SHOW)
    Void m_drawSeed;

    static final class UseSeedRef implements BooleanReference {
    }

    static final class DrawSeedButtonRef implements ButtonReference {
    }

    static final class UseSeedPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(UseSeedRef.class);
        }

    }

    static final class SeedValueProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(DrawSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            long l1 = Double.doubleToLongBits(Math.random());
            long l2 = Double.doubleToLongBits(Math.random());
            long l = ((0xFFFFFFFFL & l1) << 32) + (0xFFFFFFFFL & l2);
            return Long.toString(l);
        }
    }

    static final class FeaturesPersistor extends LegacyColumnFilterPersistor {

        protected FeaturesPersistor() {
            super(TsneNodeModel.CFG_FEATURES);
        }

    }

    static final class SeedPersistor extends LongAsStringPersistor {

        SeedPersistor() {
            super(TsneNodeModel.CFG_SEED);
        }

    }

}
