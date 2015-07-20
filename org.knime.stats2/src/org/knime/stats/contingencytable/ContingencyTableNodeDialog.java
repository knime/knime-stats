package org.knime.stats.contingencytable;

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
public class ContingencyTableNodeDialog extends DefaultNodeSettingsPane {

    private static final int INPUT_WIDTH = 5;

    private SettingsModelString m_columnX = ContingencyTableNodeModel.createSettingsModelColumnSelectorX();

    private SettingsModelString m_columnY = ContingencyTableNodeModel.createSettingsModelColumnSelectorY();

    private SettingsModelString m_valueX = ContingencyTableNodeModel.createSettingsModelValueSelectorX();

    private SettingsModelString m_valueY = ContingencyTableNodeModel.createSettingsModelValueSelectorY();

    private Map<String, List<String>> m_values = null;

    private DialogComponentColumnNameSelection m_dcnsX = null;

    private DialogComponentStringSelection m_dcssX = null;

    private DialogComponentColumnNameSelection m_dcnsY = null;

    private DialogComponentStringSelection m_dcssY = null;

    /**
     * Constructor for the dialog.
     */
    @SuppressWarnings("unchecked")
    protected ContingencyTableNodeDialog() {
        super();

        m_dcnsX =
            new DialogComponentColumnNameSelection(m_columnX, "Column X", ContingencyTableNodeModel.PORT_IN_DATA,
                StringValue.class);
        m_dcnsY =
            new DialogComponentColumnNameSelection(m_columnY, "Column Y", ContingencyTableNodeModel.PORT_IN_DATA,
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



        addDialogComponent(new DialogComponentNumber(ContingencyTableNodeModel.createSettingsModelConfidenceLevel(),
            "Confidence Level", ContingencyTableNodeModel.CONFIDENCE_LEVEL_STEPSIZE, INPUT_WIDTH));

        addDialogComponent(new DialogComponentNumber(ContingencyTableNodeModel.createSettingsModelLaplaceCorrection(),
            "Laplace Correction", ContingencyTableNodeModel.LAPLACE_CORRECTION_DEFAULT, INPUT_WIDTH));

    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        m_values = new HashMap<String, List<String>>();

        for (DataColumnSpec dcSpec : specs[ContingencyTableNodeModel.PORT_IN_DATA]) {
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
