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
package org.knime.base.node.stats.transformation.lda2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.base.node.stats.transformation.lda2.settings.LDAComputeSettings;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 *
 * Provides basic functions for an LDA Node such as autoconfigure.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractLDANodeModel extends NodeModel {

    /**
     * The data in-port index.
     */
    public static final int DATA_IN_PORT = 0;

    /**
     * The missing nominal value column exception text.
     */
    private static final String MISSING_NOM_VAL_COL_EXECPTION = "The table does not contain a nominal value column.";

    /**
     * The compute settings.
     */
    protected final LDAComputeSettings m_computeSettings = new LDAComputeSettings();

    /**
     * Indices where to find the used columns in the table.
     */
    protected int[] m_indices;

    /**
     * The index where to find the class column.
     */
    protected int m_classColIdx;

    /**
     * The names of the used columns.
     */
    protected String[] m_usedColumnNames;

    /**
     * Constructs a body for an LDA Node with a BufferedDataTable as the inPort.
     *
     * @param outPortTypes
     */
    protected AbstractLDANodeModel(final PortType[] outPortTypes) {
        super(new PortType[]{BufferedDataTable.TYPE}, outPortTypes);
    }

    /**
     * Constructs a body for an LDA Node.
     *
     * @param inPortTypes the in-port types
     * @param outPortTypes the out-port types
     */
    protected AbstractLDANodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    @Override
    protected void reset() {
        // clear members
        m_indices = null;
        m_classColIdx = -1;
        m_usedColumnNames = null;
    }

    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        if (!(inData[DATA_IN_PORT] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as input expected");
        }

        final BufferedDataTable inTable = (BufferedDataTable)inData[DATA_IN_PORT];
        if (inTable.size() == 0) {
            throw new InvalidSettingsException("Cannot produce an LDA model for an empty table.");
        }

        return doExecute(inTable, exec);
    }

    /**
     * Will be called after execute, which prepared the data. Calculates the resulting LDA.
     *
     * @param inTable the input table
     * @param exec the execution context
     * @return the created port objects
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
        if (!(inSpecs[DATA_IN_PORT] instanceof DataTableSpec)) {
            throw new IllegalArgumentException("Datatable as input expected");
        }

        final DataTableSpec inSpec = (DataTableSpec)inSpecs[DATA_IN_PORT];

        // find valid column if needed and it exists
        if (m_computeSettings.getClassModel().getStringValue() == null) {
            for (final String col : inSpec.getColumnNames()) {
                final DataColumnSpec columnSpec = inSpec.getColumnSpec(col);
                if (columnSpec.getType().isCompatible(NominalValue.class)) {
                    m_computeSettings.getClassModel().setStringValue(col);
                    setWarningMessage("Auto-selected column \"" + col + "\" as the class column.");
                    break;
                }
            }
            if (m_computeSettings.getClassModel().getStringValue() == null) {
                throw new InvalidSettingsException(MISSING_NOM_VAL_COL_EXECPTION);
            }
        }

        m_classColIdx = inSpec.findColumnIndex(m_computeSettings.getClassModel().getStringValue());

        // m_usedCols has all valid columns included per default. Exclude the class column, though
        m_usedColumnNames = ArrayUtils.removeElement(m_computeSettings.getUsedColsModel().applyTo(inSpec).getIncludes(),
            m_computeSettings.getClassModel().getStringValue());

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

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_computeSettings.saveSettingsTo(settings);
    }


    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeSettings.loadValidatedSettingsFrom(settings);
    }


    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeSettings.validateSettings(settings);
    }


    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

}