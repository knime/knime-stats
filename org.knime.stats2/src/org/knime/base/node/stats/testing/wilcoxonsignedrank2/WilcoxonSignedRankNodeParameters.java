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

package org.knime.base.node.stats.testing.wilcoxonsignedrank2;

import java.util.List;
import java.util.Optional;

import org.knime.base.node.stats.testing.wilcoxonsignedrank2.WilcoxonSignedRankNodeParameters.ColumnPair.ColumnPairDefaultProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;

/**
 * Node parameters for Wilcoxon Signed-Rank.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
final class WilcoxonSignedRankNodeParameters implements NodeParameters {

    @Widget(title = "Column pairs", description = """
            Define the test column pairs here. Each pair is used to perform a Wilcoxon signed-rank test separately. \
            The Wilcoxon signed-rank test compares two related samples by calculating the differences between paired \
            values, ranking them, and determining whether the differences are statistically significant.
            """)
    @ArrayWidget(addButtonText = "Add column pair", elementTitle = "Pair",
        elementDefaultValueProvider = ColumnPairDefaultProvider.class)
    @PersistArray(ColumnPairsPersistor.class)
    ColumnPair[] m_columnPairs = new ColumnPair[0];

    @Widget(title = "Calculate median values", description = """
            Calculate the median values for each column in the column pairs. This option is computationally expensive \
            for large datasets as it requires sorting the data.
            """)
    @Persist(configKey = WilcoxonSignedRankNodeConfig.ENABLE_COMPUTE_MEDIAN_CFG)
    boolean m_computeMedian;

    static final class ColumnPair implements NodeParameters {

        ColumnPair() {
        }

        ColumnPair(final String leftColumn, final String rightColumn) {
            m_leftColumn = leftColumn;
            m_rightColumn = rightColumn;
        }

        @Widget(title = "Left column", description = "Select the left column of the pair for comparison.")
        @ChoicesProvider(DoubleColumnsProvider.class)
        @ValueReference(LeftColumnReference.class)
        @ValueProvider(LeftColumnValueProvider.class)
        @PersistArrayElement(LeftColumnPersistor.class)
        String m_leftColumn;

        @Widget(title = "Right column", description = "Select the right column of the pair for comparison.")
        @ChoicesProvider(DoubleColumnsProvider.class)
        @ValueReference(RightColumnReference.class)
        @ValueProvider(RightColumnValueProvider.class)
        @PersistArrayElement(RightColumnPersistor.class)
        String m_rightColumn;

        private static final class LeftColumnReference implements ParameterReference<String> {
        }

        private static final class RightColumnReference implements ParameterReference<String> {
        }

        private static final class LeftColumnValueProvider extends ColumnNameAutoGuessValueProvider {

            LeftColumnValueProvider() {
                super(LeftColumnReference.class);
            }

            @Override
            protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
                return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, DoubleValue.class);
            }

        }

        private static final class RightColumnValueProvider extends ColumnNameAutoGuessValueProvider {

            RightColumnValueProvider() {
                super(RightColumnReference.class);
            }

            @Override
            protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
                return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, DoubleValue.class);
            }

        }

        static final class ColumnPairDefaultProvider implements StateProvider<ColumnPair> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
            }

            @Override
            public ColumnPair computeState(final NodeParametersInput parametersInput) {
                final var col =
                    ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, DoubleValue.class);
                if (col.isEmpty()) {
                    return new ColumnPair("", "");
                }
                final var colName = col.get().getName();
                return new ColumnPair(colName, colName);
            }
        }

        private static final class LeftColumnPersistor implements ElementFieldPersistor<String, Integer, ColumnPair> {

            @Override
            public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                final var firstColumns =
                    nodeSettings.getStringArray(WilcoxonSignedRankNodeConfig.FIRST_COLUMNS_CFG, new String[0]);
                return firstColumns[loadContext];
            }

            @Override
            public void save(final String param, final ColumnPair saveDTO) {
                saveDTO.m_leftColumn = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{WilcoxonSignedRankNodeConfig.FIRST_COLUMNS_CFG}};
            }

        }

        private static final class RightColumnPersistor implements ElementFieldPersistor<String, Integer, ColumnPair> {

            @Override
            public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                final var secondColumns =
                    nodeSettings.getStringArray(WilcoxonSignedRankNodeConfig.SECOND_COLUMNS_CFG, new String[0]);
                return secondColumns[loadContext];
            }

            @Override
            public void save(final String param, final ColumnPair saveDTO) {
                saveDTO.m_rightColumn = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{WilcoxonSignedRankNodeConfig.SECOND_COLUMNS_CFG}};
            }

        }

    }

    private static final class ColumnPairsPersistor implements ArrayPersistor<Integer, ColumnPair> {

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            return nodeSettings.getStringArray(WilcoxonSignedRankNodeConfig.FIRST_COLUMNS_CFG, new String[0]).length;
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public ColumnPair createElementSaveDTO(final int index) {
            return new ColumnPair();
        }

        @Override
        public void save(final List<ColumnPair> savedElements, final NodeSettingsWO nodeSettings) {
            final var firstCols = savedElements.stream().map(cp -> cp.m_leftColumn).toArray(String[]::new);
            final var secondCols = savedElements.stream().map(cp -> cp.m_rightColumn).toArray(String[]::new);
            nodeSettings.addStringArray(WilcoxonSignedRankNodeConfig.FIRST_COLUMNS_CFG, firstCols);
            nodeSettings.addStringArray(WilcoxonSignedRankNodeConfig.SECOND_COLUMNS_CFG, secondCols);
        }

    }
}
