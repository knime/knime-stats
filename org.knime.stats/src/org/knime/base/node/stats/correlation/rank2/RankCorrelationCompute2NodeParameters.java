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

package org.knime.base.node.stats.correlation.rank2;

import java.util.List;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.correlation.pmcc.PValueAlternative;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
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
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Node parameters for Rank Correlation.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class RankCorrelationCompute2NodeParameters implements NodeParameters {

    @Widget(title = "Correlation type", description = """
            Choose the type of correlation. The coefficient must be in the range from −1 (100% negative association,
            or perfect inversion) to +1 (100% positive association, or perfect agreement). A value of zero indicates the
             absence of association.
            """)
    @Persistor(CorrelationTypePersistor.class)
    @ValueReference(CorrelationTypeRef.class)
    CorrelationType m_correlationType = CorrelationType.SPEARMAN;

    @Widget(title = "Correlation columns", description = """
            Select the columns for which correlation values should be computed.
            """)
    @ChoicesProvider(AllColumnsProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_columnFilter = new ColumnFilter().withIncludeUnknownColumns();

    @Widget(title = "Include only column pairs with a valid correlation", description = """
            Check this option if only the column pairs where the correlation could be computed should be
            included in the output table. Column pairs where the correlation could not be computed are
            then omitted from the output table.
            """)
    @Persist(configKey = RankCorrelationCompute2NodeModel.CFG_INCLUDE_VALID_COLUMN_PAIRS)
    boolean m_includeValidColumnPairs;

    @Widget(title = "p-value", description = """
            Select which p-value should be computed for Spearman's rank correlation coefficient.
            """)
    @Persist(configKey = RankCorrelationCompute2NodeModel.CFG_PVAL_ALTERNATIVE)
    @Effect(predicate = IsSpearmanCorrelation.class, type = EffectType.ENABLE)
    PValueAlternative m_pValueAlternative = PValueAlternative.TWO_SIDED;

    static final class CorrelationTypeRef implements ParameterReference<CorrelationType> {
    }

    static final class IsSpearmanCorrelation implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer initializer) {
            return initializer.getEnum(CorrelationTypeRef.class).isOneOf(CorrelationType.SPEARMAN);
        }

    }

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super(RankCorrelationCompute2NodeModel.CFG_INCLUDE_LIST);
        }

    }

    static final class CorrelationTypePersistor implements NodeParametersPersistor<CorrelationType> {

        @Override
        public CorrelationType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var legacyValue = settings.getString(RankCorrelationCompute2NodeModel.CFG_CORR_MEASURE,
                RankCorrelationCompute2NodeModel.CFG_SPEARMAN);
            return CorrelationType.getFromCorrelationType(legacyValue);
        }

        @Override
        public void save(final CorrelationType obj, final NodeSettingsWO settings) {
            settings.addString(RankCorrelationCompute2NodeModel.CFG_CORR_MEASURE, obj.getCorrelationType());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{RankCorrelationCompute2NodeModel.CFG_CORR_MEASURE}};
        }

    }

    enum CorrelationType {

        @Label(value = "Spearman's Rho", description = """
                <a href="http://en.wikipedia.org/wiki/Spearman%27s_rank_correlation_coefficient"> Spearman's rank
                correlation coefficient </a> is a statistical measure of the strength of a monotonic relationship
                between paired data. Where the monotonic relationship is characterised by a relationship between
                ordered sets that preserves the given order, i.e., either never increases or never decreases as
                its independent variable increases. The value of this measure ranges from -1 (strong negative
                correlation) to 1 (strong positive correlation). A perfect Spearman correlation of +1 or −1 occurs when
                each of the variables is a perfect monotone function of the other. For Spearman's rank correlation
                coefficient the p-value and degrees of freedom are computed. The p-value indicates the probability of an
                uncorrelated system producing a correlation at least as extreme, if the mean of the correlation is zero
                and it follows a t-distribution with <i>df</i> degrees of freedom.
                """)
        SPEARMAN(RankCorrelationCompute2NodeModel.CFG_SPEARMAN),

        @Label(value = "Kendall's Tau A", description = """
                <a href="http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient">Kendall's tau rank
                correlation coefficient</a> is used to measure the strength of association between two measured
                quantities which is based on the number of concordant and discordant pairs and can be considered as
                standardized form of Gamma. The Tau A statistic does not consider tied values (two or more observations
                having the same value) and is mostly suitable for square tables.
                """)
        KENDALL_A(RankCorrelationCompute2NodeModel.CFG_KENDALLA),

        @Label(value = "Kendall's Tau B", description = """
                <a href="http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient">Kendall's tau rank
                correlation coefficient</a> is used to measure the strength of association between two measured
                quantities which is based on the number of concordant and discordant pairs and can be considered as
                standardized form of Gamma. The Tau B statistic makes adjustments for tied values (two or more
                observations having the same value) and is most appropriately used for rectangular tables.
                """)
        KENDALL_B(RankCorrelationCompute2NodeModel.CFG_KENDALLB),

        @Label(value = "Goodman and Kruskal's Gamma", description = """
                <a href="http://en.wikipedia.org/wiki/Goodman_and_Kruskal%27s_gamma"> Goodman and Kruskal's gamma</a>
                is used to measure the strength of association between two measured quantities. It's based on
                 the number of concordant and discordant pairs.
                """)
        GAMMA(RankCorrelationCompute2NodeModel.CFG_KRUSKALAL);

        private final String m_correlationType;

        CorrelationType(final String correlationType) {
            m_correlationType = correlationType;
        }

        String getCorrelationType() {
            return m_correlationType;
        }

        static CorrelationType getFromCorrelationType(final String correlationType) throws InvalidSettingsException {
            for (final CorrelationType condition : values()) {
                if (condition.getCorrelationType().equals(correlationType)) {
                    return condition;
                }
            }

            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(correlationType));
        }

        static String createInvalidSettingsExceptionMessage(final String correlationType) {
            var values = List
                .of(RankCorrelationCompute2NodeModel.CFG_SPEARMAN, RankCorrelationCompute2NodeModel.CFG_KENDALLA,
                    RankCorrelationCompute2NodeModel.CFG_KENDALLB, RankCorrelationCompute2NodeModel.CFG_KRUSKALAL)
                    .stream().collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", correlationType, values);
        }

    }

}
