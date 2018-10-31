package org.knime.base.node.stats.testing.kolmogorovsmirnov;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * {@link NodeDialog} for the "KolmogorovSmirnovTest" Node.
 *
 * @author Kevin Kress, Knime GmbH, Konstanz
 */
final class KolmogorovSmirnovTestNodeDialog extends DefaultNodeSettingsPane {

    private static final int INPUT_WIDTH = 5;

    private final SettingsModelBoolean exactP = KolmogorovSmirnovTestNodeModel.createSettingsModelExactP();

    private final SettingsModelDoubleBounded tolerance = KolmogorovSmirnovTestNodeModel.createSettingsModelTolerance();

    private final SettingsModelIntegerBounded iterations =
        KolmogorovSmirnovTestNodeModel.createSettingsModelIterations();

    /**
     * New pane for configuring KolmogorovSmirnovTest node dialog.
     */
    protected KolmogorovSmirnovTestNodeDialog() {
        super();
        addDialogComponent(new DialogComponentNumber(KolmogorovSmirnovTestNodeModel.createSettingsModelAlpha(),
            "Significance level alpha", 0.01, INPUT_WIDTH));

        @SuppressWarnings("unchecked")
        final DialogComponentColumnNameSelection testCol1 = new DialogComponentColumnNameSelection(
            KolmogorovSmirnovTestNodeModel.createSettingsModelCol(KolmogorovSmirnovTestNodeModel.CFGKEY_COLUMN1),
            "First test column", KolmogorovSmirnovTestNodeModel.PORT_IN_DATA, true,
            org.knime.core.data.DoubleValue.class);
        @SuppressWarnings("unchecked")
        final DialogComponentColumnNameSelection testCol2 = new DialogComponentColumnNameSelection(
            KolmogorovSmirnovTestNodeModel.createSettingsModelCol(KolmogorovSmirnovTestNodeModel.CFGKEY_COLUMN2),
            "Second test column", KolmogorovSmirnovTestNodeModel.PORT_IN_DATA, true,
            org.knime.core.data.DoubleValue.class);

        addDialogComponent(testCol1);
        addDialogComponent(testCol2);

        createNewTab("Advanced Settings");

        final List<String> nanStrategies = Arrays.asList(KolmogorovSmirnovTestNodeModel.NAN_STRATEGY_REMOVED,
            KolmogorovSmirnovTestNodeModel.NAN_STRATEGY_FAILED);
        final DialogComponentStringSelection nanComponent = new DialogComponentStringSelection(
            KolmogorovSmirnovTestNodeModel.createSettingsModelNANStrategy(), "Missing values strategy", nanStrategies);
        addDialogComponent(nanComponent);

        final DialogComponentBoolean exactPComponent =
            new DialogComponentBoolean(exactP, "Exact p-value (Computationally expensive)");
        exactP.addChangeListener(e -> {
            if (exactP.getBooleanValue()) {
                tolerance.setEnabled(false);
                iterations.setEnabled(false);
            } else {
                tolerance.setEnabled(true);
                iterations.setEnabled(true);
            }
        });

        addDialogComponent(exactPComponent);

        createNewGroup("Settings for approximation of p-value");

        final DialogComponentNumber cauchyCriterionComponent =
            new DialogComponentNumber(tolerance, "Cauchy criterion", 1E-7, 8);
        addDialogComponent(cauchyCriterionComponent);

        final DialogComponentNumber iterationsComponent =
            new DialogComponentNumber(iterations, "Max number of iterations", 10000);
        addDialogComponent(iterationsComponent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        final DataTableSpec inSpec = specs[KolmogorovSmirnovTestNodeModel.PORT_IN_DATA];
        final int k = inSpec.getNumColumns();
        if (k < 2) {
            throw new NotConfigurableException(
                "Not enough data columns available (" + k + "), please provide a data table with at least 2.");
        }
    }
}
