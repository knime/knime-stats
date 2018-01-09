/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 11, 2015 (dietzc): created
 */
package org.knime.base.node.stats.testing.wilcoxonmannwhitney;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author Christian Dietz, University of Konstanz
 */
public class WilcoxonMannWhitneyNodeDialog extends DefaultNodeSettingsPane {

    private DialogComponentStringSelection m_groupOne;

    private DialogComponentStringSelection m_groupTwo;

    private SettingsModelString m_groupColumn;

    private SettingsModelString m_groupOneModel;

    private SettingsModelString m_groupTwoModel;

    private DataTableSpec[] specs;

    /**
     * Constructor
     */
    @SuppressWarnings("unchecked")
    public WilcoxonMannWhitneyNodeDialog() {
        createNewGroup("Column Selection");
        addDialogComponent(new DialogComponentColumnNameSelection(
            WilcoxonMannWhitneyNodeModel.createSettingsModelTestColumn(), "Test Column", 0, DoubleValue.class));

        addDialogComponent(new DialogComponentColumnNameSelection(m_groupColumn =
            WilcoxonMannWhitneyNodeModel.createSettingsModelGroupColumn(), "Grouping Column", 0, StringValue.class));

        m_groupColumn.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                try {
                    if (specs != null) {
                        updateGroupSelection(specs);
                    }
                } catch (NotConfigurableException e1) {
                    throw new RuntimeException(e.toString());
                }
            }
        });

        createNewGroup("Group Selection");
        addDialogComponent(m_groupOne =
            new DialogComponentStringSelection(m_groupOneModel =
                WilcoxonMannWhitneyNodeModel.createSettingsModelGroupOne(), "Group One", ""));

        addDialogComponent(m_groupTwo =
            new DialogComponentStringSelection(m_groupTwoModel =
                WilcoxonMannWhitneyNodeModel.createSettingsModelGroupTwo(), "Group Two", ""));

        createNewGroup("Advanced Settings");
        addDialogComponent(new DialogComponentStringSelection(
            WilcoxonMannWhitneyNodeModel.createSettingsModelMissingValue(), "Missing Value Strategy",
            MissingValueHandler.names()));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("hiding")
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        this.specs = specs;
        updateGroupSelection(specs);
    }

    /**
     * @param specs
     * @throws NotConfigurableException
     */
    @SuppressWarnings("hiding")
    private void updateGroupSelection(final DataTableSpec[] specs) throws NotConfigurableException {
        final int groupColIdx = specs[0].findColumnIndex(m_groupColumn.getStringValue());
        if (groupColIdx != -1) {
            final Set<DataCell> values = specs[0].getColumnSpec(groupColIdx).getDomain().getValues();

            if (values == null || values.size() < 2) {
                throw new NotConfigurableException("At least two groups are required to configure this node.");
            }

            final Set<String> groups = new HashSet<>();
            for (final DataCell cell : values) {
                final String group = ((StringValue)cell).getStringValue();
                groups.add(group);
            }

            final Iterator<String> it = groups.iterator();
            if (groups.contains(m_groupOneModel.getStringValue())) {
                m_groupOne.replaceListItems(groups, m_groupOneModel.getStringValue());
            } else {
                final String val = it.next();
                m_groupOne.replaceListItems(groups, val);
                m_groupOneModel.setStringValue(val);
            }

            if (groups.contains(m_groupTwoModel.getStringValue())) {
                m_groupTwo.replaceListItems(groups, m_groupTwoModel.getStringValue());
            } else {
                final String val = it.next();
                m_groupTwo.replaceListItems(groups, val);
                m_groupTwoModel.setStringValue(val);
            }
        }
    }
}
