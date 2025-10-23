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

package org.knime.base.node.stats.shapirowilk2;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;

/**
 * Node parameters for Shapiro-Wilk Test.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ShapiroWilk2NodeParameters implements NodeParameters {

    @Widget(title = "Significance level α", description = """
            Significance level at which the null hypothesis can be rejected, 0 &lt; &#945; &lt; 1.
            """)
    @NumberInputWidget(minValidation = AlphaMinValidation.class, maxValidation = AlphaMaxValidation.class)
    @Persist(configKey = ShapiroWilk2NodeModel.SIGNIFICANCE_ALPHA_CFG)
    double m_alpha = 0.05;

    @Widget(title = "Test Columns", description = """
            The columns to test for normality using the Shapiro-Wilk test.
            """)
    @ChoicesProvider(NumericColumnsProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @Persistor(TestColumnsFilterPersistor.class)
    ColumnFilter m_testColumns = new ColumnFilter();

    @Widget(title = "Use Shapiro-Francia for leptokurtic samples", description = """
            Checks if the samples are leptokurtic, and if so uses Shapiro-Francia. Otherwise, falls back to
            Shapiro-Wilk.
            """)
    @Persist(configKey = ShapiroWilk2NodeModel.SHAPIRO_FRANCIA_CFG)
    boolean m_shapiroFrancia = true;

    static final class AlphaMinValidation extends NumberInputWidgetValidation.MinValidation {

        @Override
        protected double getMin() {
            return 0.0;
        }

        @Override
        public boolean isExclusive() {
            return true;
        }

    }

    static final class AlphaMaxValidation extends NumberInputWidgetValidation.MaxValidation {

        @Override
        protected double getMax() {
            return 1.0;
        }

        @Override
        public boolean isExclusive() {
            return true;
        }

    }

    static final class NumericColumnsProvider extends CompatibleColumnsProvider {

        NumericColumnsProvider() {
            super(java.util.List.of(DoubleValue.class, IntValue.class, LongValue.class));
        }

    }

    static final class TestColumnsFilterPersistor extends LegacyColumnFilterPersistor {

        TestColumnsFilterPersistor() {
            super(ShapiroWilk2NodeModel.USED_COLS_CFG);
        }

    }

}
