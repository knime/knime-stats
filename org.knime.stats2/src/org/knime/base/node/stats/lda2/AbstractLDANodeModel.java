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
 *   Oct 29, 2018 (lukass): created
 */
package org.knime.base.node.stats.lda2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.UniqueNameGenerator;

/**
 *
 * Provides basic functions for an LDA Node such as autoconfigure.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractLDANodeModel extends NodeModel {

    static final int PORT_IN_DATA = 0;

    /**
     * The configuration key for the number of dimensions.
     */
    private static final String K_CFG = "k";

    /**
     * The configuration key for the used columns.
     */
    private static final String USED_COLS_CFG = "used_columns";

    /**
     * The configuration key for the class column.
     */
    private static final String CLASS_COL_CFG = "class_column";

    /**
     * Creates a settings model for the used columns.
     *
     * @return the settings model
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createUsedColsSettingsModel() {
        return new SettingsModelColumnFilter2(USED_COLS_CFG, org.knime.core.data.DoubleValue.class);
    }

    /**
     * Creates a settings model for the class column.
     *
     * @return the settings model
     */
    static SettingsModelString createClassColSettingsModel() {
        return new SettingsModelString(CLASS_COL_CFG, null);
    }

    /**
     * Creates a settings model for k.
     *
     * @return the settings model
     */
    static SettingsModelInteger createKSettingsModel() {
        return new SettingsModelInteger(K_CFG, 1);
    }

    /**
     * Settings model for the used columns.
     */
    private final SettingsModelColumnFilter2 m_usedCols = createUsedColsSettingsModel();

    /**
     * Settings model for the class column. Must have a calculated domain and contain at least two distinct classes to
     * make the LDA function properly.
     */
    final SettingsModelString m_classColumn = createClassColSettingsModel();

    /**
     * Settings model for the dimension to reduce to.
     */
    final SettingsModelInteger m_k = createKSettingsModel();

    /**
     * Indices where to find the used columns in the table.
     */
    int[] m_indices;

    /**
     * The index where to find the class column.
     */
    int m_classColIdx;

    /**
     * The names of the used columns.
     */
    String[] m_usedColumnNames;

    /**
     * Constructs a body for an LDA Node with a BufferedDataTable as the inPort.
     *
     * @param outPortTypes
     */
    AbstractLDANodeModel(final PortType[] outPortTypes) {
        super(new PortType[]{BufferedDataTable.TYPE}, outPortTypes);
    }

    /**
     * Constructs a body for an LDA Node.
     *
     * @param outPortTypes
     */
    AbstractLDANodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // clear members
        m_indices = null;
        m_classColIdx = -1;
        m_usedColumnNames = null;
    }

    /**
     * Validates and returns the input data.
     *
     * @param inData
     * @param exec
     * @return the input as a BufferedDataTable
     * @throws IllegalArgumentException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        if (!(inData[PORT_IN_DATA] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as input expected");
        }

        final BufferedDataTable inTable = (BufferedDataTable)inData[PORT_IN_DATA];
        if (inTable.size() == 0) {
            throw new InvalidSettingsException("Cannot produce an LDA model for an empty table.");
        }

        return doExecute(inTable, exec);
    }

    /**
     * Will be called after execute, which prepared the data. Calculates the resulting LDA.
     *
     * @param inTable
     * @param exec
     * @return The created PortObject[].
     * @throws IllegalArgumentException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     *
     */
    protected abstract PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException;

    /**
     * Tries to find a valid class column and one column that can be projected down from. Also finds the indices of the
     * used Columns, w/o the class column index if it should be contained.
     *
     * @param inSpecs
     * @return the inSpec that can further be used
     * @throws InvalidSettingsException
     */
    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (!(inSpecs[PORT_IN_DATA] instanceof DataTableSpec)) {
            throw new IllegalArgumentException("Datatable as input expected");
        }

        final DataTableSpec inSpec = (DataTableSpec)inSpecs[PORT_IN_DATA];

        // find valid column if needed and it exists
        if (m_classColumn.getStringValue() == null) {
            boolean isAnyDomainCalculated = false;
            boolean isAnyDomainNominal = false;

            for (final String col : inSpec.getColumnNames()) {
                final DataColumnSpec columnSpec = inSpec.getColumnSpec(col);
                final DataColumnDomain domain = columnSpec.getDomain();
                isAnyDomainCalculated |= domain.hasValues();
                isAnyDomainNominal |= columnSpec.getType().isCompatible(NominalValue.class);
                if (columnSpec.getType().isCompatible(NominalValue.class) && domain.hasValues()
                    && (domain.getValues().size() > 2)) {
                    m_classColumn.setStringValue(col);
                    setWarningMessage("Auto-selected column \"" + col + "\" as the class column.");
                    break;
                }
            }
            if (m_classColumn.getStringValue() == null) {
                final StringBuilder stb = new StringBuilder("Could not auto-configure the class column: ");
                if (!isAnyDomainCalculated && isAnyDomainNominal) {
                    stb.append("The column domains were not calculated by default."
                        + " Please use the \"Domain Calculator\" node.");
                } else if (!isAnyDomainNominal) {
                    stb.append("The table does not provide a nominal value column.");
                } else {
                    stb.append("The table does not provide a nominal value column"
                        + " and the column domains were not calculated by"
                        + " default. Please use the \"Domain Calculator\" node.");
                }
                throw new InvalidSettingsException(stb.toString());
            }
        }

        m_classColIdx = inSpec.findColumnIndex(m_classColumn.getStringValue());

        // m_usedCols has all valid columns included per default. Exclude the class column, though
        m_usedColumnNames =
            ArrayUtils.removeElement(m_usedCols.applyTo(inSpec).getIncludes(), m_classColumn.getStringValue());

        // check number of columns and get the used column indices
        final int numIncludedColumns = m_usedColumnNames.length;
        if (numIncludedColumns == 0) {
            throw new InvalidSettingsException("No column selected.");
        }

        m_indices = new int[numIncludedColumns];
        for (int i = 0; i < numIncludedColumns; i++) {
            final int colIdx = inSpec.findColumnIndex(m_usedColumnNames[i]);
            if (colIdx != m_classColIdx) {
                m_indices[i] = colIdx;
            }
        }

        // Sanity check settings even though dialog checks, in case any flow variables went bad.
        calcBoundedMaxDim(inSpec, m_classColumn.getStringValue(), m_indices.length, m_k.getIntValue());

        return doConfigure(inSpec);
    }

    /**
     * Additional configuration that will be done after the call of configure(), i.e. after auto-configuration of the
     * class column and one column to project down from. Should then serve as
     * {@link NodeModel#configure(DataTableSpec[])}.
     *
     * @param inSpec
     * @return The portobjectspec as the output of this node will produce
     * @throws InvalidSettingsException
     */
    protected abstract PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        m_k.saveSettingsTo(settings);
        m_classColumn.saveSettingsTo(settings);
        m_usedCols.saveSettingsTo(settings);
        saveAdditionalSettingsTo(settings);
    }

    /**
     * Save additional settings, called after {@link #saveSettingsTo(NodeSettingsWO)}.
     *
     * @param settings
     */
    @SuppressWarnings("javadoc")
    protected abstract void saveAdditionalSettingsTo(final NodeSettingsWO settings);

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_k.loadSettingsFrom(settings);
        m_classColumn.loadSettingsFrom(settings);
        m_usedCols.loadSettingsFrom(settings);
        loadAdditionalValidatedSettingsFrom(settings);
    }

    /**
     * Load additional settings, called after {@link #loadValidatedSettingsFrom(NodeSettingsRO)}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    @SuppressWarnings("javadoc")
    protected abstract void loadAdditionalValidatedSettingsFrom(final NodeSettingsRO settings)
        throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_k.validateSettings(settings);
        m_classColumn.validateSettings(settings);
        m_usedCols.validateSettings(settings);
        validateAdditionalSettings(settings);
    }

    /**
     * Validate additional settings, called after {@link #validateSettings(NodeSettingsRO)}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    @SuppressWarnings("javadoc")
    protected abstract void validateAdditionalSettings(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * Create a column rearranger that applies the LDA, if given
     *
     * @param inSpec the inspec of the table
     * @param lda the transformation or null if called from configure
     * @param k number of dimensions to reduce to (number of rows in w)
     * @param removeUsedCols whether to remove the input data
     * @param usedColumnNames the names of the used columns, needed for removal
     * @return
     */
    static ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final LDA2 lda, final int k,
        final boolean removeUsedCols, final String[] usedColumnNames) {
        // use the columnrearranger to exclude the used columns if checked
        final ColumnRearranger cr = new ColumnRearranger(inSpec);

        if (removeUsedCols) {
            cr.remove(usedColumnNames);
        }

        // check that none of the newly put columns is already existing
        final UniqueNameGenerator ung = new UniqueNameGenerator(cr.createSpec());

        final DataColumnSpec[] specs = new DataColumnSpec[k];
        for (int i = 0; i < k; i++) {
            specs[i] = ung.newColumn("Projected dimension " + i, DoubleCell.TYPE);
        }

        cr.append(new AbstractCellFactory(true, specs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                try {
                    return lda.getProjection(row);
                } catch (final InvalidSettingsException e) {
                    return null;
                }
            }
        });

        return cr;
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