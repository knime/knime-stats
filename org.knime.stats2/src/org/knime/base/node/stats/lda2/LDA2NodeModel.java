package org.knime.base.node.stats.lda2;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This is the model implementation of the LDA Node.
 *
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDA2NodeModel extends AbstractLDANodeModel {

    /**
     * The configuration key for the number of dimensions.
     */
    private static final String K_CFG = "k";

    /**
     * The configuration key whether to remove the used columns.
     */
    private static final String REMOVE_USED_COLS_CFG = "remove_used_columns";

    /**
     * Settings model for the dimension to reduce to.
     */
    final SettingsModelInteger m_k = createKSettingsModel();

    /**
     * Creates a settings model for k.
     *
     * @return the settings model
     */
    static SettingsModelInteger createKSettingsModel() {
        return new SettingsModelInteger(K_CFG, 1);
    }

    /**
     * Constructor for the node model.
     */
    LDA2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Creates a settings model whether to remove the used columns.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createRemoveUsedColsSettingsModel() {
        return new SettingsModelBoolean(REMOVE_USED_COLS_CFG, false);
    }

    private final SettingsModelBoolean m_removeUsedCols = createRemoveUsedColsSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
            throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        final DataTableSpec inSpec = inTable.getDataTableSpec();

        final LDA2 lda = new LDA2(m_indices);
        lda.calculateTransformationMatrix(exec.createSubExecutionContext(0.5), inTable, m_k.getIntValue(),
            m_classColIdx);

        final ColumnRearranger cr = createColumnRearranger(inSpec, lda, m_k.getIntValue(),
            m_removeUsedCols.getBooleanValue(), m_usedColumnNames);

        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec.createSubProgress(0.5));
        return new PortObject[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        // Sanity check settings even though dialog checks, in case any flow variables went bad.
        calcBoundedMaxDim(inSpec, m_classColumn.getStringValue(), m_indices.length, m_k.getIntValue());
        return new PortObjectSpec[]{createColumnRearranger(inSpec, null, m_k.getIntValue(),
            m_removeUsedCols.getBooleanValue(), m_usedColumnNames).createSpec()};
    }

    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
        m_k.saveSettingsTo(settings);
        m_removeUsedCols.saveSettingsTo(settings);
    }

    @Override
    protected void loadAdditionalValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_k.loadSettingsFrom(settings);
        m_removeUsedCols.loadSettingsFrom(settings);
    }

    @Override
    protected void validateAdditionalSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_k.validateSettings(settings);
        m_removeUsedCols.validateSettings(settings);
    }

    /**
     * Calculates the maximum possible dimension to reduce to and checks that the current selected dimensions are not
     * out of bounds.
     */
    private static final int calcBoundedMaxDim(final DataTableSpec inSpec, final String classColName,
        final int numSelectedColumns, final int selectedDims) throws InvalidSettingsException {
        final int maxDim = calcPositiveMaxDim(inSpec, classColName, numSelectedColumns);

        if (selectedDims <= 0) {
            throw new InvalidSettingsException(
                "The number of dimensions to project to must be a positive integer larger than 0, " + selectedDims
                    + " is invalid.");
        } else if (selectedDims > maxDim) {
            // classSpec & domain must be valid as checked in the beginning
            final DataColumnSpec classSpec = inSpec.getColumnSpec(classColName);
            final int selectedClasses = classSpec.getDomain().getValues().size();
            throw new InvalidSettingsException(
                LDA2.createTooHighDimWarning(selectedDims, maxDim, selectedClasses, numSelectedColumns, classColName));
        }

        return maxDim;
    }


    /**
     * Calculates the maximum possible dimension that can be reduced to from the current settings and checks that it is
     * not less than 1.
     */
    private static final int calcPositiveMaxDim(final DataTableSpec inSpec, final String classColName,
        final int numSelectedColumns) throws InvalidSettingsException {
        // get the selected Classes, columns and calculate maxDim - much like updateSettings() in the dialog
        final DataColumnSpec classSpec = inSpec.getColumnSpec(classColName);
        if (classSpec == null) {
            throw new InvalidSettingsException("The selected class column \"" + classColName + "\" does not exist.");
        }

        // initialize to flag value indicating an uncalculated domain
        final int selectedClasses = classSpec.getDomain().hasValues() ? classSpec.getDomain().getValues().size() : -1;

        final int maxDim = Math.min((selectedClasses - 1), numSelectedColumns);

        if (maxDim <= 0) {
            throw new InvalidSettingsException(
                LDA2.createMaxDimZeroWarning(selectedClasses, numSelectedColumns, classColName));
        }

        return maxDim;
    }

}
