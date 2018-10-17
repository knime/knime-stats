package org.knime.base.node.stats.testing.kolmogorovsmirnov;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;

/**
 * <code>NodeDialog</code> for the "KolmogorovSmirnovTest" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author
 */
public class KolmogorovSmirnovTestNodeDialog extends DefaultNodeSettingsPane {


    private static final int INPUT_WIDTH = 5;

    /**
     * New pane for configuring KolmogorovSmirnovTest node dialog.
     */
    @SuppressWarnings("unchecked")
    protected KolmogorovSmirnovTestNodeDialog() {
        super();
        addDialogComponent(new DialogComponentNumber(KolmogorovSmirnovTestNodeModel.createSettingsModelAlpha(),
            "Significance level alpha", 0.01, INPUT_WIDTH));

        DialogComponentColumnNameSelection testCol1 = new DialogComponentColumnNameSelection(
            KolmogorovSmirnovTestNodeModel.createSettingsModelCol1(),
          "First test column", KolmogorovSmirnovTestNodeModel.PORT_IN_DATA, true, org.knime.core.data.DoubleValue.class);
        DialogComponentColumnNameSelection testCol2 = new DialogComponentColumnNameSelection(
            KolmogorovSmirnovTestNodeModel.createSettingsModelCol2(),
          "Second test column", KolmogorovSmirnovTestNodeModel.PORT_IN_DATA, true, org.knime.core.data.DoubleValue.class);

        addDialogComponent(testCol1);
        addDialogComponent(testCol2);

        createNewTab("Advanced Settings");

        final List<String> nanStrategies = new ArrayList<String>();
        nanStrategies.add("REMOVED");
        nanStrategies.add("FAILED");
        final DialogComponentStringSelection nanComponent = new DialogComponentStringSelection(
            KolmogorovSmirnovTestNodeModel.createSettingsModelNANStrategy(), "Missing values strategy", nanStrategies);
        addDialogComponent(nanComponent);

        final DialogComponentBoolean exactPComponent = new DialogComponentBoolean(
            KolmogorovSmirnovTestNodeModel.createSettingsModelExactP(), "Exact p-value");
        addDialogComponent(exactPComponent);

        createNewGroup("Settings for approximation of p-value");

        final DialogComponentNumber cauchyCriterionComponent = new DialogComponentNumber(
            KolmogorovSmirnovTestNodeModel.createSettingsModelTolerance(), "Cauchy criterion", 1E-7, 8);
        addDialogComponent(cauchyCriterionComponent);

        final DialogComponentNumber iterationsComponent = new DialogComponentNumber(
            KolmogorovSmirnovTestNodeModel.createSettingsModelIterations(), "Max number of iterations", 10000);
        addDialogComponent(iterationsComponent);
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        DataTableSpec inSpec = specs[KolmogorovSmirnovTestNodeModel.PORT_IN_DATA];
        final int k = inSpec.getNumColumns();
        if (k < 2) {
            throw new NotConfigurableException(
                "Not enough data columns available (" + k + "), please provide a data table with more than 2.");
        }
    }
}

