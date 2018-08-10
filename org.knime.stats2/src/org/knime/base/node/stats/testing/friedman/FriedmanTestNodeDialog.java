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
 */

package org.knime.base.node.stats.testing.friedman;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "FriedmanTest" Node.
 *
 * @author Lukas Siedentop, University of Konstanz
 */
public class FriedmanTestNodeDialog extends DefaultNodeSettingsPane {

    private static final int INPUT_WIDTH = 5;

    private DataTableSpec m_tableSpec;

    private final SettingsModelColumnFilter2 m_usedCols;

    /**
     * New pane for configuring FriedmanTest node dialog.
     */
    protected FriedmanTestNodeDialog() {
        super();

        addDialogComponent(new DialogComponentNumber(FriedmanTestNodeModel.createSettingsModelAlpha(),
            "Significance Level alpha", 0.01, INPUT_WIDTH));

        m_usedCols = FriedmanTestNodeModel.createSettingsModelCols();
        addDialogComponent(new DialogComponentColumnFilter2(m_usedCols, FriedmanTestNodeModel.PORT_IN_DATA));

        createNewTab("Advanced Settings");

        final List<String> nanStrategies = Stream.of(NaNStrategy.values()).map(Enum::name).collect(Collectors.toList());
        // we can not support missing values removal - this breaks the table integrity
        nanStrategies.remove("REMOVED");
        final DialogComponentStringSelection nanComponent = new DialogComponentStringSelection(
            FriedmanTestNodeModel.createSettingsModelNANStrategy(), "Missing Values Strategy", nanStrategies);
        addDialogComponent(nanComponent);

        final List<String> tieStrategies =
                Stream.of(TiesStrategy.values()).map(Enum::name).collect(Collectors.toList());
        final SettingsModelString tiesStrategyModel = FriedmanTestNodeModel.createSettingsModelTiesStrategy();
        final DialogComponentStringSelection tieComponent =
                new DialogComponentStringSelection(tiesStrategyModel, "Ties Strategy", tieStrategies);
        addDialogComponent(tieComponent);

        createNewGroup("Seed for RANDOM Ties Strategy");

        final SettingsModelBoolean useCustomSeed =
                FriedmanTestNodeModel.createSettingsModelUseRandomSeed(tiesStrategyModel);
        final SettingsModelLong seedModel =
                FriedmanTestNodeModel.createSettingsModelSeed(useCustomSeed, tiesStrategyModel);
        final DialogComponentButton drawSeed = new DialogComponentButton("New Seed");

        enableSeedGroup(tiesStrategyModel, useCustomSeed, seedModel, drawSeed);

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(useCustomSeed, "Use Random Seed"));
        addDialogComponent(new DialogComponentNumber(seedModel, "Seed", 1));
        addDialogComponent(drawSeed);
        setHorizontalPlacement(false);

        seedModel.setEnabled(useCustomSeed.getBooleanValue());
        tiesStrategyModel
        .addChangeListener(c -> enableSeedGroup(tiesStrategyModel, useCustomSeed, seedModel, drawSeed));
        useCustomSeed.addChangeListener(c -> enableCustomSeed(tiesStrategyModel, useCustomSeed, seedModel, drawSeed));
        drawSeed.addActionListener(c -> seedModel.setLongValue((long)(((Math.random() * 2.0) - 1) * Long.MAX_VALUE)));
    }

    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        FriedmanTestNodeModel.checkUsedColumns(m_usedCols, m_tableSpec);
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        m_tableSpec = specs[FriedmanTestNodeModel.PORT_IN_DATA];
        final int k = m_tableSpec.getNumColumns();
        if (k < 3) {
            throw new NotConfigurableException(
                "Not enough data columns available (" + k + "), please provide a data table with more than 2.");
        }
    }

    private static void enableCustomSeed(final SettingsModelString tiesStrategyModel,
        final SettingsModelBoolean useCustomSeed, final SettingsModelLong seedModel,
        final DialogComponentButton drawSeed) {
        final boolean customSeed = useCustomSeed.getBooleanValue()
                && tiesStrategyModel.getStringValue().equals(TiesStrategy.RANDOM.toString());
        seedModel.setEnabled(customSeed);
        drawSeed.getComponentPanel().getComponent(0).setEnabled(customSeed);
    }

    private static void enableSeedGroup(final SettingsModelString tiesStrategyModel,
        final SettingsModelBoolean useCustomSeed, final SettingsModelLong seedModel,
        final DialogComponentButton drawSeed) {
        useCustomSeed.setEnabled(tiesStrategyModel.getStringValue().equals(TiesStrategy.RANDOM.toString()));
        enableCustomSeed(tiesStrategyModel, useCustomSeed, seedModel, drawSeed);
    }
}
