/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */

package org.knime.base.node.stats.contintable.oddriskratio2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "ContingencyTable" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Oliver Sampson, University of Konstanz
 */
public class OddsRiskRatioNodeDialog extends DefaultNodeSettingsPane {

    private static final int INPUT_WIDTH = 5;

    private SettingsModelString m_columnX = OddsRiskRatioNodeModel.createSettingsModelColumnSelectorX();

    private SettingsModelString m_columnY = OddsRiskRatioNodeModel.createSettingsModelColumnSelectorY();

    private SettingsModelString m_valueX = OddsRiskRatioNodeModel.createSettingsModelValueSelectorX();

    private SettingsModelString m_valueY = OddsRiskRatioNodeModel.createSettingsModelValueSelectorY();

    private Map<String, List<String>> m_values = null;

    private DialogComponentColumnNameSelection m_dcnsX = null;

    private DialogComponentStringSelection m_dcssX = null;

    private DialogComponentColumnNameSelection m_dcnsY = null;

    private DialogComponentStringSelection m_dcssY = null;

    /**
     * Constructor for the dialog.
     */
    @SuppressWarnings("unchecked")
    protected OddsRiskRatioNodeDialog() {
        super();

        m_dcnsX =
            new DialogComponentColumnNameSelection(m_columnX, "Column X", OddsRiskRatioNodeModel.PORT_IN_DATA,
                StringValue.class);
        m_dcnsY =
            new DialogComponentColumnNameSelection(m_columnY, "Column Y", OddsRiskRatioNodeModel.PORT_IN_DATA,
                StringValue.class);

        m_dcssX =
            new DialogComponentStringSelection(m_valueX, "Value from Column X", new String[]{""});
        m_dcssY =
            new DialogComponentStringSelection(m_valueY, "Value from Column Y", new String[]{""});

                //m_values.get(m_values.keySet().iterator().next()));

        m_columnX.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (m_values == null) {
                    return;
                }
                if (m_values.get(m_columnX.getStringValue()) == null) {
                    return;
                }
                m_dcssX.replaceListItems(m_values.get(m_columnX.getStringValue()), null);
            }
        });

        m_columnY.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (m_values == null) {
                    return;
                }
                if (m_values.get(m_columnY.getStringValue()) == null) {
                    return;
                }
                m_dcssY.replaceListItems(m_values.get(m_columnY.getStringValue()), null);
            }
        });

        addDialogComponent(m_dcnsX);
        addDialogComponent(m_dcnsY);

        addDialogComponent(m_dcssX);

        addDialogComponent(m_dcssY);



        addDialogComponent(new DialogComponentNumber(OddsRiskRatioNodeModel.createSettingsModelConfidenceLevel(),
            "Confidence Level", OddsRiskRatioNodeModel.CONFIDENCE_LEVEL_STEPSIZE, INPUT_WIDTH));

        addDialogComponent(new DialogComponentNumber(OddsRiskRatioNodeModel.createSettingsModelLaplaceCorrection(),
            "Laplace Correction", OddsRiskRatioNodeModel.LAPLACE_CORRECTION_DEFAULT, INPUT_WIDTH));

    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        m_values = new HashMap<>();

        for (DataColumnSpec dcSpec : specs[OddsRiskRatioNodeModel.PORT_IN_DATA]) {
            if (dcSpec.getType().isCompatible(StringValue.class)) {
                Set<DataCell> vals = dcSpec.getDomain().getValues();

                if (vals == null) {
                    throw new NotConfigurableException("The domain for column " + dcSpec.getName() + " is not set.");
                }
                List<String> lVal = new ArrayList<>();
                for (DataCell v : vals) {
                    lVal.add(((StringCell)v).getStringValue());
                }
                Collections.sort(lVal);
                m_values.put(dcSpec.getName(), lVal);
            }
        }

        m_dcssX.replaceListItems(m_values.get(m_columnX.getStringValue()), null);
        m_dcssY.replaceListItems(m_values.get(m_columnY.getStringValue()), null);
        super.loadAdditionalSettingsFrom(settings, specs);

    }



}
