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
package org.knime.base.node.stats.testing.friedman;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.stats.StatsUtil;

/**
 * This is the model implementation of FriedmanTest, as on
 * https://en.wikipedia.org/w/index.php?title=Friedman_test&oldid=829231943
 *
 * @author Lukas Siedentop, University of Konstanz
 */
public class FriedmanTestNodeModel extends NodeModel {

    static final int PORT_IN_DATA = 0;

    private final SettingsModelDoubleBounded m_alpha = createSettingsModelAlpha();

    private final SettingsModelColumnFilter2 m_usedCols = createSettingsModelCols();

    private final SettingsModelString m_nanStrategy = createSettingsModelNANStrategy();

    private final SettingsModelString m_tieStrategy = createSettingsModelTiesStrategy();

    private final SettingsModelBoolean m_useRandomSeed = createSettingsModelUseRandomSeed(m_tieStrategy);

    private final SettingsModelLong m_seed = createSettingsModelSeed(m_useRandomSeed, m_tieStrategy);

    /**
     * Constructor for the node model.
     */
    protected FriedmanTestNodeModel() {
        super(1, 1);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final NodeProgressMonitor progMon = exec.getProgressMonitor();
        int progCnt = 0;

        final BufferedDataTable data = inData[PORT_IN_DATA];

        final BufferedDataContainer outContainer = exec.createDataContainer(createOutputSpec());

        // check preconditions
        final String[] cols = m_usedCols.applyTo(data.getDataTableSpec()).getIncludes();
        final int[] relevantColumnIdc =
            Arrays.stream(cols).mapToInt(c -> data.getDataTableSpec().findColumnIndex(c)).toArray();

        final int k = cols.length; // treatments
        final long n = data.size(); // patients
        final int df = k - 1; // degrees of freedom

        if (n == 0) {
            outContainer.close();
            return new BufferedDataTable[]{outContainer.getTable()};
        }

        if (n <= 15) {
            setWarningMessage("The resulting test statistic Q has a Chi-squared probability "
                + "distribution only for more than 15 rows (is: " + n + ").");
        }

        /*
         * 1. calculate ranked table
         * 2. calculate column mean and total mean
         * 3. calculate friedman number q
         * 4. calculate chi-square statistics
         *
         * Note that now there is a different, less demonstrative version
         * of the algorithm on wikipedia, which complies more with various textbooks.
         * This shorter version implies a check and correction for ties in the data,
         * which needs to be done before. The previously described algorithm directly
         * accounts for ties and is implemented here.
         */

        // calculate mean of whole ranked table and mean of each ranked column
        final NaturalRanking ranker;

        if (m_tieStrategy.getStringValue().equals("RANDOM")) {
            final Well19937c randomGenerator = new Well19937c();
            if (m_useRandomSeed.getBooleanValue()) {
                randomGenerator.setSeed(m_seed.getLongValue());
            }
            ranker = new NaturalRanking(NaNStrategy.valueOf(m_nanStrategy.getStringValue()), randomGenerator);
        } else {
            ranker = new NaturalRanking(NaNStrategy.valueOf(m_nanStrategy.getStringValue()),
                TiesStrategy.valueOf(m_tieStrategy.getStringValue()));
        }

        final double[] columnMean = new double[k];

        final BufferedDataContainer temp = exec.createDataContainer(createTempSpec(k));

        for (final DataRow row : data) {
            progMon.setProgress(progCnt / (2.0 * data.size()));
            progCnt++;

            // loop over relevant column indices, convert datarow to double array to chuck into ranker.
            double[] ranked = new double[k];

            for (int i = 0; i < relevantColumnIdc.length; i++) {
                final DataCell cell = row.getCell(relevantColumnIdc[i]);
                if (cell.isMissing()) {
                    ranked[i] = Double.NaN;
                } else {
                    ranked[i] = ((DoubleValue)cell).getDoubleValue();
                }
            }

            ranked = ranker.rank(ranked);

            // sum the ranks and save in temp table
            final List<DataCell> cells = new ArrayList<>(k);
            for (int i = 0; i < k; i++) {
                columnMean[i] += ranked[i];
                cells.add(new DoubleCell(ranked[i]));
            }

            temp.addRowToTable(new DefaultRow(row.getKey(), cells));
        }
        temp.close();

        // Calculate the means.
        // The total mean is always (k+1)/2 if there are no ties - else this depends on the ties strategy.
        double totalMean = 0;
        for (int i = 0; i < k; i++) {
            columnMean[i] /= n;
            totalMean += columnMean[i];
        }
        totalMean /= k;

        // Friedman number, to be compared to the chi sq distribution
        double sst = 0; // Measure of Aggregate Group Differences
        double sse = 0; //

        for (final DataRow row : temp.getTable()) {
            progMon.setProgress(progCnt / (2.0 * data.size()));
            progCnt++;

            for (int i = 0; i < k; i++) {
                sse += ((((DoubleValue)row.getCell(i)).getDoubleValue() - totalMean)
                    * (((DoubleValue)row.getCell(i)).getDoubleValue() - totalMean));
            }
        }

        for (final double cMean : columnMean) {
            sst += ((cMean - totalMean) * (cMean - totalMean));
        }

        sst *= n;
        sse /= ((double)n * (double)df);

        final double q = sst / sse;

        // Chi squared calculation
        final ChiSquaredDistribution chiDist = new ChiSquaredDistribution(df);
        final double chi2 = chiDist.inverseCumulativeProbability(1 - m_alpha.getDoubleValue());
        final double pval = 1 - chiDist.cumulativeProbability(q);

        final List<DataCell> cells = new ArrayList<>(4);
        cells.add(BooleanCellFactory.create(pval < m_alpha.getDoubleValue()));
        cells.add(new DoubleCell(q));
        cells.add(new DoubleCell(chi2));
        cells.add(new DoubleCell(pval));

        final RowKey key = new RowKey("Friedman (" + String.join(", ", cols) + ")");
        final DataRow outRow = new DefaultRow(key, cells);
        outContainer.addRowToTable(outRow);

        // once we are done, we close the container and return its table
        outContainer.close();
        return new BufferedDataTable[]{outContainer.getTable()};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final int k = checkUsedColumns(m_usedCols, inSpecs[PORT_IN_DATA]);
        if (k <= 4) {
            setWarningMessage("The resulting test statistic Q has a Chi-squared probability "
                + "distribution only for more than 4 columns (is: " + k + ").");
        }

        return new DataTableSpec[]{createOutputSpec()};
    }

    private static DataTableSpec createOutputSpec() {
        final List<DataColumnSpec> allColSpecs = new ArrayList<>(4);
        allColSpecs.add(new DataColumnSpecCreator("Reject H0", BooleanCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator("Q", DoubleCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator("Critical ChiSq Value", DoubleCell.TYPE).createSpec());
        allColSpecs.add(StatsUtil.createDataColumnSpec("p-Value", StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));

        return new DataTableSpec(allColSpecs.toArray(new DataColumnSpec[0]));
    }

    private static DataTableSpec createTempSpec(final int k) {
        final List<DataColumnSpec> allColSpecs = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            allColSpecs.add(new DataColumnSpecCreator(Integer.toString(i), DoubleCell.TYPE).createSpec());
        }

        return new DataTableSpec(allColSpecs.toArray(new DataColumnSpec[0]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_alpha.saveSettingsTo(settings);
        m_usedCols.saveSettingsTo(settings);
        m_nanStrategy.saveSettingsTo(settings);
        m_tieStrategy.saveSettingsTo(settings);
        m_useRandomSeed.saveSettingsTo(settings);
        m_seed.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alpha.loadSettingsFrom(settings);
        m_usedCols.loadSettingsFrom(settings);
        m_nanStrategy.loadSettingsFrom(settings);
        m_tieStrategy.loadSettingsFrom(settings);
        m_useRandomSeed.loadSettingsFrom(settings);
        m_seed.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_alpha.validateSettings(settings);
        m_usedCols.validateSettings(settings);
        m_nanStrategy.validateSettings(settings);
        m_tieStrategy.validateSettings(settings);
        m_useRandomSeed.validateSettings(settings);
        m_seed.validateSettings(settings);
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
     * Creates a settings model for the used columns.
     *
     * @return the settings model
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createSettingsModelCols() {
        /*
         * TODO: it'd be nice to give a min-expected and max-expected amount of columns,
         * if this sort of problem occurs more often - see DL Keras Executor and/or Learner
         */
        return new SettingsModelColumnFilter2("Used columns", DoubleValue.class, IntValue.class, LongValue.class);
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
     * Creates a settings model for the NaN-Strategy.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelNANStrategy() {
        return new SettingsModelString("NaN-Strategy", "FAILED");
    }

    /**
     * Creates a settings model for the Ties-Strategy.
     *
     * @return the settings model
     */
    static SettingsModelString createSettingsModelTiesStrategy() {
        return new SettingsModelString("Tie-Strategy", "AVERAGE");
    }

    /**
     * Creates a settings model whether to use a seed.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createSettingsModelUseRandomSeed(final SettingsModelString tiesStrategy) {
        /*
         * TODO: It'd be nice to have a randomSeedDialogComponent, that handles all this logic on its own.
         */
        final SettingsModelBoolean useRandomSeed = new SettingsModelBoolean("Use Random Seed?", false);
        useRandomSeed.setEnabled(tiesStrategy.getStringValue().equals(TiesStrategy.RANDOM.toString()));
        return useRandomSeed;
    }

    /**
     * Creates a settings model for the seed.
     *
     * @return the settings model
     */
    static SettingsModelLong createSettingsModelSeed(final SettingsModelBoolean useSeed,
        final SettingsModelString tiesStrategy) {
        final SettingsModelLong seed = new SettingsModelLong("Seed", 1234567890123L);
        seed.setEnabled(
            tiesStrategy.getStringValue().equals(TiesStrategy.RANDOM.toString()) && useSeed.getBooleanValue());
        return seed;
    }

    static int checkUsedColumns(final SettingsModelColumnFilter2 usedCols, final DataTableSpec tableSpec)
        throws InvalidSettingsException {
        final String[] cols = usedCols.applyTo(tableSpec).getIncludes();
        final int k = cols.length;
        if (k < 3) {
            throw new InvalidSettingsException(
                "Not enough data columns chosen (" + k + "), please choose more than 2.");
        }
        return k;
    }
}
