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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 20, 2017 (Anastasia Zhukova): created
 */
package org.knime.base.node.stats.dataexplorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.base.data.statistics.HistogramColumn;
import org.knime.base.data.statistics.HistogramModel;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.Pair;

/**
 *
 * @author Anastasia Zhukova, KNIME GmbH, Konstanz, Germany
 */
public class JSNominalHistogram extends JSHistogram {

    /**
     * @param colName Name of the column.
     * @param colIndex Index of the column.
     */
    JSNominalHistogram(final String colName, final int colIndex, final Map<DataValue, Integer> nomValue) {
        super(colName, colIndex);
        HistogramColumn hcol = HistogramColumn.getDefaultInstance();
        HistogramModel<?> hist = hcol.fromNominalModel(nomValue, colIndex, colName);
        this.m_maxCount = hist.getMaxCount();
        this.m_bins = binsUnwrapper(hist);
    }

    /**
     * @param javaHistogram HistogramNominalModel histogram to convert into JSNominalHistogram.
     */
    JSNominalHistogram(final HistogramModel<?> histogram) {
        super(histogram.getColName(), histogram.getColIndex());
        this.m_maxCount = histogram.getMaxCount();
        this.m_bins = binsUnwrapper(histogram);
    }

    private List<Pair<String, Integer>> binsUnwrapper (final HistogramModel<?> hist) {
        List<Pair<String, Integer>> bins = new ArrayList<Pair<String, Integer>>();
        for (int i = 0; i < hist.getBins().size(); i++) {
            if (hist.getBins().get(i).getDef() instanceof StringCell) {
                bins.add(new Pair<String, Integer>(((StringCell)hist.getBins().get(i).getDef()).getStringValue(), hist.getBins().get(i).getCount()));
            } else {
                bins.add(new Pair<String, Integer>("?", hist.getBins().get(i).getCount()));
            }
        }
        return bins;
    }
}
