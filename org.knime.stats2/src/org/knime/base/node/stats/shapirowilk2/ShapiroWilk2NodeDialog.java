package org.knime.base.node.stats.shapirowilk2;

import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;

/**
 * <code>NodeDialog</code> for the "PMMLToJavascriptCompiler" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Alexander Fillbrunn
 */
public class ShapiroWilk2NodeDialog extends DefaultNodeSettingsPane {

	private DataTableSpec m_tableSpec;

	private final SettingsModelColumnFilter2 m_usedCols;

	/**
	 * New pane for configuring the node.
	 */
	protected ShapiroWilk2NodeDialog() {

		m_usedCols = ShapiroWilk2NodeModel.createSettingsModelCols();
		final DialogComponentBoolean shapFrancia = new DialogComponentBoolean(
				ShapiroWilk2NodeModel.createShapiroFranciaSettingsModel(),
				"Use Shapiro-Francia for leptokurtic samples");
		final DialogComponentStringSelection sortP = new DialogComponentStringSelection(
				ShapiroWilk2NodeModel.createSortByPValueSettingsModel(), "Sort results by p-Value",
            Arrays.asList(ShapiroWilk2NodeModel.PVALUE_SORT_NOSORTING, ShapiroWilk2NodeModel.PVALUE_SORT_ASCENDING,
                ShapiroWilk2NodeModel.PVALUE_SORT_DESCENDING));
		addDialogComponent(new DialogComponentColumnFilter2(m_usedCols, ShapiroWilk2NodeModel.PORT_IN_DATA));
		addDialogComponent(shapFrancia);
		addDialogComponent(sortP);
	}

	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		super.saveAdditionalSettingsTo(settings);
		ShapiroWilk2NodeModel.checkUsedColumns(m_usedCols, m_tableSpec);
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
