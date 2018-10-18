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
package org.knime.base.node.stats.testing.proportion;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of ProportionTest.
 *
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public class ProportionTestNodeModel extends NodeModel {

    final NodeLogger LOGGER = NodeLogger.getLogger(ProportionTestNodeModel.class);

    static final int PORT_IN_DATA = 0;

    static final String ERRORMESSAGE = "provide a \"StringValue\" or \"BooleanValue\" column  with at "
        + "least two distinct values. If a column has more than 60 distinct values,"
        + " use the \"Domain Calculator\" node.";

    private final SettingsModelDoubleBounded m_alphaModel = createSettingsModelAlpha();

    private final SettingsModelDoubleBounded m_h0Model = createSettingsModelNullHypothesis();

    private final SettingsModelString m_hAModel = createSettingsModelAlternativeHypothesis();

    private final SettingsModelString m_categoryColumnModel = createSettingsModelCategoryColumn();

    private final SettingsModelString m_categoryModel = createSettingsModelCategory();

    private final SettingsModelBoolean m_sampleProportionStdErrorModel = createSettingsModelSampleProportionStdError();

    /**
     * Constructor for the node model.
     */
    protected ProportionTestNodeModel() {
        super(1, 1);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final BufferedDataTable data = inData[PORT_IN_DATA];
        if (data.size() <= 2) {
            throw new InvalidSettingsException("Not enough data rows in the table (" + data.size() + ").");
        }

        final BufferedDataContainer outContainer = exec.createDataContainer(createOutputSpec());

        final int catColIdx = data.getDataTableSpec().findColumnIndex(m_categoryColumnModel.getStringValue());
        if (catColIdx == -1) {
            throw new InvalidSettingsException("Category column not found. Please reconfigure the node.");
        }

        /*
         * 1. Count the occurrences (count) of the chosen value in the column and the total number of
         * valid occurrences (nobs)
         * 2. Calculate the z-score via zScore = ((count/nobs)-p0)/sqrt(p0*(1-p0)/nobs) with the null hypothesis p0
         * 3. As zScore should be normally distributed, calculate the p-Value
         * 4. With the given significance level, check whether the null hypothesis is to be rejected.
         *
         * See e.g. https://onlinecourses.science.psu.edu/statprogram/reviews/statistical-concepts/proportions
         * for further explanation.
         */

        double progCnt = 0;
        int nobs = 0;
        int count = 0;
        int numMissing = 0;
        final boolean isBoolean = data.getSpec().getColumnSpec(catColIdx).getType().isCompatible(BooleanValue.class);

        for (final DataRow row : data) {
            final DataCell cell = row.getCell(catColIdx);
            // be sure that its not a missing value
            if (!cell.isMissing()) {
                nobs++;
                if (isBoolean) {
                    final String cellValue = ((BooleanValue)cell).getBooleanValue() ? "true" : "false";
                    if (cellValue.contentEquals(m_categoryModel.getStringValue())) {
                        count++;
                    }
                } else if (((StringValue)cell).getStringValue().contentEquals(m_categoryModel.getStringValue())) {
                    count++;
                }
            } else {
                numMissing++;
            }
            progCnt++;
            exec.setProgress(progCnt / data.size());
        }

        if (nobs < 2) {
            throw new InvalidSettingsException("The column '" + m_categoryColumnModel.getStringValue()
                + "' has not enough non-missing entries (" + nobs + ").");
        }

        if (numMissing > 0) {
            setWarningMessage("Skipped " + numMissing + " row" + (numMissing == 1 ? "" : "s") + " with missing value"
                + (numMissing == 1 ? "" : "s") + "!");
        }

        final AlternativeHypothesis hA = AlternativeHypothesis.valueOf(m_hAModel.getStringValue());
        final DataCell[] cells = proportionTest(count, nobs, m_h0Model.getDoubleValue(), hA,
            m_alphaModel.getDoubleValue(), m_sampleProportionStdErrorModel.getBooleanValue());

        final RowKey rowKey = new RowKey("Proportion Test '" + m_categoryModel.getStringValue() + "'*("
            + m_categoryColumnModel.getStringValue() + ")");
        final DataRow statsRow = new DefaultRow(rowKey, cells);
        outContainer.addRowToTable(statsRow);

        outContainer.close();
        return new BufferedDataTable[]{outContainer.getTable()};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // Auto detect category column, if needed
        if (!inSpecs[0].containsName(m_categoryColumnModel.getStringValue())) {
            // Choose the first String or Boolean column that has two or more distinct values.
            for (final DataColumnSpec col : inSpecs[0]) {
                final Set<DataCell> values = col.getDomain().getValues();
                if ((values != null) && !values.isEmpty() && (values.size() > 1)) {
                    m_categoryColumnModel.setStringValue(col.getName());

                    final String defaultCategory = values.iterator().next().toString();
                    m_categoryModel.setStringValue(defaultCategory);

                    LOGGER.warn("Auto detection suggested category \"" + defaultCategory + "\" of column \""
                        + m_categoryColumnModel.getStringValue() + "\".");
                    break;
                }
            }

            // could not find a fitting column
            if (!inSpecs[0].containsName(m_categoryColumnModel.getStringValue())) {
                throw new InvalidSettingsException("No compatible column in spec available: " + ERRORMESSAGE);
            }
        }

        // Auto detect category, if needed
        final Set<DataCell> values =
            inSpecs[0].getColumnSpec(m_categoryColumnModel.getStringValue()).getDomain().getValues();
        if ((values == null) || (values.size() < 2)) {
            throw new InvalidSettingsException(
                "No compatible category in column \"" + m_categoryColumnModel.getStringValue() + "\": " + ERRORMESSAGE);
        }
        boolean isContained = false;
        for (final DataCell value : values) {
            if (!value.isMissing()) {
                String cellValue = null;
                if (value.getType().isCompatible(BooleanValue.class)) {
                    cellValue = ((BooleanValue)value).getBooleanValue() ? "true" : "false";
                } else {
                    cellValue = ((StringValue)value).getStringValue();
                }
                if (cellValue.equals(m_categoryModel.getStringValue())) {
                    isContained = true;
                    break;
                }
            }
        }
        if (!isContained) {
            final String defaultCategory = values.iterator().next().toString();
            m_categoryModel.setStringValue(defaultCategory);
            LOGGER.warn("No category specified: auto detection suggested category \"" + defaultCategory + "\".");
        }

        return new DataTableSpec[]{createOutputSpec()};
    }

    private static DataCell[] proportionTest(final double count, final double nobs, final double p0,
        final AlternativeHypothesis hA, final double alpha, final boolean sampleProportionStdError) {

        final double pHat = count / nobs;

        // denominator: standard error
        double stdError = 0;
        if (sampleProportionStdError) {
            stdError = Math.sqrt((pHat * (1 - pHat)) / nobs);
        } else {
            stdError = Math.sqrt((p0 * (1 - p0)) / nobs);
        }

        // actual formula for zstat
        final double zScore = (pHat - p0) / stdError;

        // zScore should be normal distributed; thus calculate the p-Value from there
        // As another distribution could be meant, the symmetry of the normal distribution is not taken advantage of.
        double pval = 0;
        final NormalDistribution normDist = new NormalDistribution(0, 1);

        switch (hA) {
            case GREATER_THAN:
                pval = 1 - normDist.cumulativeProbability(zScore);
                break;
            case LESS_THAN:
                // zScore might be negative already
                pval = normDist.cumulativeProbability(zScore);
                break;
            case TWO_SIDED:
                pval = normDist.cumulativeProbability(-zScore) + (1 - normDist.cumulativeProbability(zScore));
                break;
        }

        // (mis-)used as a container to be able to return different data types
        return new DataCell[]{new IntCell((int)count), new IntCell((int)nobs), new DoubleCell(pHat),
            BooleanCellFactory.create(pval < alpha), new DoubleCell(zScore), new DoubleCell(pval)};
    }

    private static DataTableSpec createOutputSpec() {
        final DataColumnSpec[] allColSpecs =
            new DataColumnSpec[]{new DataColumnSpecCreator("Count", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Nobs", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Proportion", DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Reject H0", BooleanCell.TYPE).createSpec(),
                new DataColumnSpecCreator("z-Score", DoubleCell.TYPE).createSpec(),
                StatsUtil.createPValueColumnSpec()};

        return new DataTableSpec(allColSpecs);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_alphaModel.saveSettingsTo(settings);
        m_h0Model.saveSettingsTo(settings);
        m_hAModel.saveSettingsTo(settings);
        m_categoryColumnModel.saveSettingsTo(settings);
        m_categoryModel.saveSettingsTo(settings);
        m_sampleProportionStdErrorModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alphaModel.loadSettingsFrom(settings);
        m_h0Model.loadSettingsFrom(settings);
        m_hAModel.loadSettingsFrom(settings);
        m_categoryColumnModel.loadSettingsFrom(settings);
        m_categoryModel.loadSettingsFrom(settings);
        m_sampleProportionStdErrorModel.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alphaModel.validateSettings(settings);
        m_h0Model.validateSettings(settings);
        m_hAModel.validateSettings(settings);
        m_categoryColumnModel.validateSettings(settings);
        m_categoryModel.validateSettings(settings);
        m_sampleProportionStdErrorModel.validateSettings(settings);
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
     * Creates a settings model for the null hypothesis.
     *
     * @return the settings model
     */
    static SettingsModelDoubleBounded createSettingsModelNullHypothesis() {
        return new SettingsModelDoubleBounded("H_0", 0.5, 0, 1);
    }

    /**
     * Creates a settings model for the alternative hypothesis.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelAlternativeHypothesis() {
        return new SettingsModelString("H_A", "GREATER_THAN");
    }

    /**
     * Creates a settings model for the category column.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelCategoryColumn() {
        return new SettingsModelString("Category column", "");
    }

    /**
     * Creates a settings model for the category.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelCategory() {
        return new SettingsModelString("Category", "");
        // When the default is set to null, the component gets a white background?!
        //return new SettingsModelString("Category", null);
    }

    /**
     * Creates a settings model for how to calculate the standard error.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createSettingsModelSampleProportionStdError() {
        return new SettingsModelBoolean("Use sample proportion", false);
    }
}
