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

package org.knime.base.node.stats.testing.ttest;

import java.util.Arrays;
import java.util.function.Supplier;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;

/**
 * Node parameters for Paired t-test.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class PairedTTestNodeParameters implements NodeParameters {

    @Widget(title = "Test column pairs", description = """
            Define the test column pairs here. Every pair is used to perform a paired t-test separately.
            """)
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add test column pair",
        elementDefaultValueProvider = ColumnPairsDefaultValueProvider.class)
    @Persistor(ColumnPairsPersistor.class)
    @ValueReference(ColumnPairsRef.class)
    @ValueProvider(ColumnPairsProvider.class)
    ColumnPairSettings[] m_columnPairs = new ColumnPairSettings[0];

    @Persistor(ConfidenceIntervalPersistor.class)
    @Widget(title = "Confidence interval (%)", description = """
            The limits for the confidence interval are computed using this number. The default is 95 which means that
            you can be 95% confident that the true value of the parameter is in the confidence interval.
            """)
    @NumberInputWidget(minValidation = MinConfidenceIntervalValidation.class,
                       maxValidation = MaxConfidenceIntervalValidation.class)
    double m_confidenceIntervalProb = 95.0;

    private static final class ColumnPairSettings implements NodeParameters {

        ColumnPairSettings() {
        }

        ColumnPairSettings(final String leftColumn, final String rightColumn) {
            m_leftColumn = leftColumn;
            m_rightColumn = rightColumn;
        }

        @Widget(title = "Left column", description = "Select the left column for this pair.")
        @ChoicesProvider(DoubleColumnsProvider.class)
        String m_leftColumn;

        @Widget(title = "Right column", description = "Select the right column for this pair.")
        @ChoicesProvider(DoubleColumnsProvider.class)
        String m_rightColumn;

    }

    private static final class MinConfidenceIntervalValidation extends MinValidation {

        @Override
        protected double getMin() {
            return 1.0;
        }

    }

    private static final class MaxConfidenceIntervalValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 99.0;
        }
    }

    private static final class ColumnPairsRef implements ParameterReference<ColumnPairSettings[]> {
    }

    private static final class ColumnPairsProvider implements StateProvider<ColumnPairSettings[]> {

        Supplier<ColumnPairSettings[]> m_columnPairsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_columnPairsSupplier = initializer.getValueSupplier(ColumnPairsRef.class);
        }

        @Override
        public ColumnPairSettings[] computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var existingPairs = m_columnPairsSupplier.get();
            if (existingPairs.length > 0) {
                return existingPairs;
            } else {
                final var firstAvailableCol = ColumnSelectionUtil
                        .getCompatibleColumnsOfFirstPort(context, DoubleValue.class)
                        .stream().findFirst();
                if (firstAvailableCol.isEmpty()) {
                    return new ColumnPairSettings[0];
                }

                final var firstColName = firstAvailableCol.get().getName();
                return new ColumnPairSettings[] {
                    new ColumnPairSettings(firstColName, firstColName)
                };

            }

        }

    }

    private static final class ColumnPairsDefaultValueProvider implements StateProvider<ColumnPairSettings> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public ColumnPairSettings computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var firstAvailableCol = ColumnSelectionUtil
                    .getCompatibleColumnsOfFirstPort(context, DoubleValue.class)
                    .stream().findFirst();
            if (firstAvailableCol.isEmpty()) {
                return new ColumnPairSettings();
            }

            final var firstColName = firstAvailableCol.get().getName();
            return new ColumnPairSettings(firstColName, firstColName);
        }

    }

    private static final class ColumnPairsPersistor implements NodeParametersPersistor<ColumnPairSettings[]> {

        @Override
        public ColumnPairSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String[] leftColumns = settings.getStringArray(PairedTTestNodeSettings.LEFT_COLUMNS, new String[0]);
            String[] rightColumns = settings.getStringArray(PairedTTestNodeSettings.RIGHT_COLUMNS, new String[0]);

            if (leftColumns == null || rightColumns == null) {
                return new ColumnPairSettings[0];
            }

            if (leftColumns.length != rightColumns.length) {
                throw new InvalidSettingsException("The number of left columns (" + leftColumns.length
                    + ") does not match the number of right columns (" + rightColumns.length + ").");
            }

            ColumnPairSettings[] pairs = new ColumnPairSettings[leftColumns.length];
            for (int i = 0; i < leftColumns.length; i++) {
                pairs[i] = new ColumnPairSettings(leftColumns[i], rightColumns[i]);
            }
            return pairs;
        }

        @Override
        public void save(final ColumnPairSettings[] obj, final NodeSettingsWO settings) {
            String[] leftColumns = Arrays.stream(obj).map(pair -> pair.m_leftColumn).toArray(String[]::new);
            String[] rightColumns = Arrays.stream(obj).map(pair -> pair.m_rightColumn).toArray(String[]::new);
            settings.addStringArray(PairedTTestNodeSettings.LEFT_COLUMNS, leftColumns);
            settings.addStringArray(PairedTTestNodeSettings.RIGHT_COLUMNS, rightColumns);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{PairedTTestNodeSettings.LEFT_COLUMNS}, {PairedTTestNodeSettings.RIGHT_COLUMNS}};
        }

    }

    private static final class ConfidenceIntervalPersistor implements NodeParametersPersistor<Double> {

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            double decimal = settings.getDouble(PairedTTestNodeSettings.CONFIDENCE_INTERVAL_PROB);
            return decimal * 100.0;
        }

        @Override
        public void save(final Double obj, final NodeSettingsWO settings) {
            settings.addDouble(PairedTTestNodeSettings.CONFIDENCE_INTERVAL_PROB, obj / 100.0);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{PairedTTestNodeSettings.CONFIDENCE_INTERVAL_PROB}};
        }

    }

}
