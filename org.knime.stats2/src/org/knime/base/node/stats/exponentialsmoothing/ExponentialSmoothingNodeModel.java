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
package org.knime.base.node.stats.exponentialsmoothing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of "ExponentialSmoothing", as on https://en.wikipedia.org/wiki/Exponential_smoothing
 *
 * @author Lukas Siedentop, University of Konstanz
 */
public class ExponentialSmoothingNodeModel extends NodeModel {

    static final int PORT_IN_DATA = 0;

    private final SettingsModelDoubleBounded m_alpha = createSettingsModelAlpha();

    private final SettingsModelString m_columnSelection = createSettingsModelColumnSelection();

    /**
     * Constructor for the node model.
     */
    protected ExponentialSmoothingNodeModel() {
        super(1, 1);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final NodeProgressMonitor progMon = exec.getProgressMonitor();
        int progCnt = 0;

        final BufferedDataTable data = inData[PORT_IN_DATA];

        final BufferedDataContainer outContainer = exec.createDataContainer(createOutputSpec());

        int relevantColumnIdx = data.getDataTableSpec().findColumnIndex(m_columnSelection.getStringValue());

        /*
         * Calculate the smoothed value at timestep t via
         * s_t = alpha * x_t + (1 - alpha) * s_t-1
         */

        // TODO: get initial value
        double mean = 0;

        double alpha = m_alpha.getDoubleValue();

        double[] smoothed = new double[(int)data.size()];
        smoothed[0] = mean;

        int t = 1;

        for (final DataRow row : data) {
            progMon.setProgress(progCnt / data.size());
            progCnt++;

            // don't calculate the last one
            if (t >= data.size()) {
                break;
            }

            final DataCell cell = row.getCell(relevantColumnIdx);
            if (!cell.isMissing()) {
                double x_t = ((DoubleValue)cell).getDoubleValue();
                smoothed[t] = alpha * x_t + (1 - alpha) * smoothed[t - 1];
            } else {
                // TODO: missing values strategy?
                smoothed[t] = alpha * mean + (1 - alpha) * smoothed[t - 1];
            }

            final List<DataCell> cells = new ArrayList<>(1);
            cells.add(DoubleCellFactory.create(smoothed[t]));
            final DataRow outRow = new DefaultRow(row.getKey(), cells);
            outContainer.addRowToTable(outRow);

            t++;
        }

        // once we are done, we close the container and return its table
        outContainer.close();
        return new BufferedDataTable[]{outContainer.getTable()};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createOutputSpec()};
    }

    private static DataTableSpec createOutputSpec() {
        final List<DataColumnSpec> allColSpecs = new ArrayList<>(1);
        allColSpecs.add(new DataColumnSpecCreator("Smoothed", DoubleCell.TYPE).createSpec());
        return new DataTableSpec(allColSpecs.toArray(new DataColumnSpec[0]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_alpha.saveSettingsTo(settings);
        m_columnSelection.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alpha.loadSettingsFrom(settings);
        m_columnSelection.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alpha.validateSettings(settings);
        m_columnSelection.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    @Override
    protected void reset() {
        // Nothing to do.
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    /**
     * Creates a settings model for the significance level alpha.
     *
     * @return the settings model
     */
    static SettingsModelDoubleBounded createSettingsModelAlpha() {
        return new SettingsModelDoubleBounded("Alpha", 0.05, 0, 1);
    }

    /**
     * Creates a settings model for the significance level alpha.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelColumnSelection() {
        return new SettingsModelString("Timeseries", "");
    }
}
