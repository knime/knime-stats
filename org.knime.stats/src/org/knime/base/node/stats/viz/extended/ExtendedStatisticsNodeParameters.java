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

package org.knime.base.node.stats.viz.extended;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Statistics.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ExtendedStatisticsNodeParameters implements NodeParameters {

    @Section(title = "Nominal Values")
    interface NominalValuesSection {
    }

    @Section(title = "Histogram",
            description = "The node outputs a image column showing a histogram in the first and second port.")
    @After(NominalValuesSection.class)
    interface HistogramSection {
    }

    @Widget(title = "Calculate median values (computationally expensive)", description = """
            Select this option if for all numeric columns the medians are computed. Note, this computation
            might be expensive, since it requires to sort all column independently to find the values that
            divides the distribution into two halves of the same number of values.
            """)
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_COMPUTE_MEDIAN)
    boolean m_computeMedian = ExtendedStatisticsNodeModel.DEFAULT_COMPUTE_MEDIAN;

    @Layout(NominalValuesSection.class)
    @Widget(title = "Column filter", description = """
            Filter columns for counting all possible values.
            """)
    @ChoicesProvider(AllColumnsProvider.class)
    @Persistor(NominalColumnFilterPersistor.class)
    ColumnFilter m_nominalColumnFilter = new ColumnFilter();

    @Layout(NominalValuesSection.class)
    @Widget(title = "Max no. of most frequent and infrequent values (in view)", description = """
            Adjusts the number of counts for both, top number of frequent and infrequent occurrences of
            categorical values per column (displayed in the node view!).
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_NUM_NOMINAL_VALUES)
    int m_nominalValues = ExtendedStatisticsNodeModel.DEFAULT_NUM_NOMINAL_VALUES;

    @Layout(NominalValuesSection.class)
    @Widget(title = "Max no. of possible values per column (in output table)", description = """
            Adjusts the maximum number of possible values per column in the nominal output table.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_NUM_NOMINAL_VALUES_OUTPUT)
    int m_nominalValuesOutput = ExtendedStatisticsNodeModel.DEFAULT_NUM_NOMINAL_VALUES_OUTPUT;

    @Layout(NominalValuesSection.class)
    @Widget(title = "Enable HiLite", description = """
            Enable HiLite functionality for interactive highlighting of data points.
            """)
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_ENABLE_HILITE)
    boolean m_enableHiLite = ExtendedStatisticsNodeModel.DEFAULT_ENABLE_HILITE;

    @Layout(HistogramSection.class)
    @Widget(title = "Histogram format", description = """
            The image format of histogram cells.
            """)
    @ValueSwitchWidget
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_IMAGE_FORMAT)
    HistogramFormat m_histogramFormat = HistogramFormat.SVG;

    @Layout(HistogramSection.class)
    @Widget(title = "Width", description = """
            The width of the histogram.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_HISTOGRAM_WIDTH)
    int m_histogramWidth = ExtendedStatisticsNodeModel.DEFAULT_HISTOGRAM_WIDTH;

    @Layout(HistogramSection.class)
    @Widget(title = "Height", description = """
            The height of the histogram.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_HISTOGRAM_HEIGHT)
    int m_histogramHeight = ExtendedStatisticsNodeModel.DEFAULT_HISTOGRAM_HEIGHT;

    @Layout(HistogramSection.class)
    @Widget(title = "Show min/max values", description = """
            Show or do not show the numeric min/max values on histograms.
            """)
    @Persist(configKey = ExtendedStatisticsNodeModel.CFGKEY_SHOW_MIN_MAX)
    boolean m_showMinMax = ExtendedStatisticsNodeModel.DEFAULT_SHOW_MIN_MAX;

    static final class NominlaColumnFilterRef implements ParameterReference<ColumnFilter> {
    }

    static final class NominalColumnFilterPersistor extends LegacyColumnFilterPersistor {

        NominalColumnFilterPersistor() {
            super(ExtendedStatisticsNodeModel.CFGKEY_FILTER_NOMINAL_COLUMNS);
        }

    }

    enum HistogramFormat {
        @Label(value = "SVG", description = "Histogram in SVG format")
        SVG, //
        @Label(value = "PNG", description = "Histogram in PNG format")
        PNG;
    }

}
