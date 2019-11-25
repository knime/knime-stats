package org.knime.base.node.stats.shapirowilk2;

import java.awt.Color;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;

/**
 * <code>NodeDialog</code> for the "ShapiroWilk2" Node.
 *
 * @author Alexander Fillbrunn
 */
public class ShapiroWilk2NodeDialog extends DefaultNodeSettingsPane {

    private static final int INPUT_WIDTH = 5;

    private final SettingsModelColumnFilter2 m_usedCols = ShapiroWilk2NodeModel.createSettingsModelCols();

    private final SettingsModelDoubleBounded m_alpha = ShapiroWilk2NodeModel.createSettingsModelAlpha();

    private final DialogComponentLabel m_invalidAlphaLabel = new DialogComponentLabel("");

    private DataTableSpec m_tableSpec;

    /**
     * New pane for configuring the node.
     */
    protected ShapiroWilk2NodeDialog() {
        JPanel panel = m_invalidAlphaLabel.getComponentPanel();
        synchronized (panel.getTreeLock()) {
            panel.getComponent(0).setForeground(Color.RED);
        }
        m_alpha.addChangeListener(e -> {
            try {
                ShapiroWilk2NodeModel.checkAlphaRange(m_alpha.getDoubleValue());
                m_invalidAlphaLabel.setText("");
            } catch (InvalidSettingsException ex) {
                m_invalidAlphaLabel.setText(ex.getMessage());
            }
        });
        addDialogComponent(new DialogComponentNumber(m_alpha, "Significance level \u03B1", 0.01, INPUT_WIDTH));
        addDialogComponent(m_invalidAlphaLabel);
        final DialogComponentBoolean shapFrancia = new DialogComponentBoolean(
            ShapiroWilk2NodeModel.createShapiroFranciaSettingsModel(), "Use Shapiro-Francia for leptokurtic samples");
        addDialogComponent(new DialogComponentColumnFilter2(m_usedCols, ShapiroWilk2NodeModel.PORT_IN_DATA));
        addDialogComponent(shapFrancia);
    }

    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        ShapiroWilk2NodeModel.checkUsedColumns(m_usedCols, m_tableSpec);
        ShapiroWilk2NodeModel.checkAlphaRange(m_alpha.getDoubleValue());
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_tableSpec = specs[ShapiroWilk2NodeModel.PORT_IN_DATA];
        final int k = m_tableSpec.getNumColumns();
        if (k < 1) {
            throw new NotConfigurableException(
                "Not enough data columns available (" + k + "), please provide a data table with at least 1.");
        }
    }
}
