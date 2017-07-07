/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   5 Jul 2017 (albrecht): created
 */
package org.knime.base.node.stats.dataexplorer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.JSONViewContent;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class DataExplorerNodeRepresentation extends JSONViewContent {

    private static final String CFG_STATISTICS = "statistics";
    private static final String CFG_NO_VALUES = "noValues";
    private static final String CFG_SINGLE_STAT = "stat_";
    private List<JSONStatisticColumn> m_statistics;

    /**
     * @return the statistics
     */
    public List<JSONStatisticColumn> getStatistics() {
        return m_statistics;
    }

    /**
     * @param statistics the statistics to set
     */
    public void setStatistics(final List<JSONStatisticColumn> statistics) {
        m_statistics = statistics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        NodeSettingsWO statSettings = settings.addNodeSettings(CFG_STATISTICS);
        int noValues = m_statistics == null ? 0 : m_statistics.size();
        statSettings.addInt(CFG_NO_VALUES, noValues);
        for (int i = 0; i < noValues; i++) {
            NodeSettingsWO singleColSettings = statSettings.addNodeSettings(CFG_SINGLE_STAT + i);
            m_statistics.get(i).saveToNodeSettings(singleColSettings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO statSettings = settings.getNodeSettings(CFG_STATISTICS);
        int noValues = statSettings.getInt(CFG_NO_VALUES);
        if (noValues > 0) {
            m_statistics = new ArrayList<JSONStatisticColumn>(noValues);
            for (int i = 0; i < noValues; i++) {
                NodeSettingsRO singleColSettings = statSettings.getNodeSettings(CFG_SINGLE_STAT + i);
                JSONStatisticColumn col = new JSONStatisticColumn();
                col.loadFromNodeSettings(singleColSettings);
                m_statistics.add(col);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        DataExplorerNodeRepresentation other = (DataExplorerNodeRepresentation)obj;
        return new EqualsBuilder()
                .append(m_statistics, other.m_statistics)
                .isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(m_statistics)
                .toHashCode();
    }

}
