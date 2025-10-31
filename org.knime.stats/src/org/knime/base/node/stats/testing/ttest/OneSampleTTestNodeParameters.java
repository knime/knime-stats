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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;

/**
 * Node parameters for Single sample t-test.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class OneSampleTTestNodeParameters implements NodeParameters {

    @Persist(configKey = OneSampleTTestNodeSettings.TEST_VALUE)
    @Widget(title = "Test value", description = """
            The hypothesized value to test against.
            """)
    @NumberInputWidget
    double m_testValue;

    @Widget(title = "Confidence interval (in %)", description = """
            The limits for the confidence interval are computed using this number. The default is 95 which means that
            you can be 95% confident that the true value of the parameter is in the confidence interval.
            """)
    @NumberInputWidget(minValidation = IsMinimumConfidence.class, maxValidation = IsMaximumConfidence.class)
    @Persistor(ConfidenceIntervalPersistor.class)
    double m_confidenceIntervalProb = OneSampleTTestNodeSettings.DEFAULT_CONFIDENCE_INTERVAL_PROB * 100.0;

    @Widget(title = "Test columns", description = """
            A single sample t-test is performed separately for each of these columns.
            """)
    @ChoicesProvider(DoubleColumnsProvider.class)
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

    static final class TestColumnsPersistor extends LegacyColumnFilterPersistor {

        TestColumnsPersistor() {
            super(OneSampleTTestNodeSettings.TEST_COLUMNS);
        }

    }

    static final class ConfidenceIntervalPersistor implements NodeParametersPersistor<Double> {

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final double prob = settings.getDouble(OneSampleTTestNodeSettings.CONFIDENCE_INTERVAL_PROB, 0.95);
            return prob * 100.0;
        }

        @Override
        public void save(final Double percentage, final NodeSettingsWO settings) {
            final double prob = percentage / 100.0;
            settings.addDouble(OneSampleTTestNodeSettings.CONFIDENCE_INTERVAL_PROB, prob);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{OneSampleTTestNodeSettings.CONFIDENCE_INTERVAL_PROB}};
        }

    }

}
