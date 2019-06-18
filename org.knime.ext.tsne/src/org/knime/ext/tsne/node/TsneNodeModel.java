/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 */
package org.knime.ext.tsne.node;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelSeed;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;

import com.jujutsu.tsne.FastTSne;
import com.jujutsu.tsne.Progress;
import com.jujutsu.tsne.TSne;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne.ThreadingExceptionHandler;
import com.jujutsu.utils.TSneUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TsneNodeModel extends NodeModel {

    private static final String OUTPUT_PREFIX = "t-SNE dimension ";

    private static final int DATA_IN_PORT = 0;

    static SettingsModelDoubleBounded createThetaModel() {
        return new SettingsModelDoubleBounded("theta", 0.5, 0, 1);
    }

    static SettingsModelIntegerBounded createOutputDimensionsModel() {
        return new SettingsModelIntegerBounded("outputDimensions", 2, 1, Integer.MAX_VALUE);
    }

    static SettingsModelDoubleBounded createPerplexityModel() {
        return new SettingsModelDoubleBounded("perplexity", 30, 1e-5, Double.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createIterationsModel() {
        return new SettingsModelIntegerBounded("iterations", 1000, 1, Integer.MAX_VALUE);
    }

    static SettingsModelSeed createSeedModel() {
        return new SettingsModelSeed("seed", System.currentTimeMillis(), false);
    }

    static SettingsModelIntegerBounded createNumberOfThreadsModel() {
        return new SettingsModelIntegerBounded("numberOfThreads", Runtime.getRuntime().availableProcessors(), 1,
            Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createFeaturesModel() {
        // TODO support more data types
        return new SettingsModelColumnFilter2("features", DoubleValue.class);
    }

    static SettingsModelBoolean createRemoveOriginalColumnsModel() {
        return new SettingsModelBoolean("removeOriginalColumns", false);
    }

    static SettingsModelBoolean createFailOnMissingValuesModel() {
        return new SettingsModelBoolean("failOnMissingValues", false);
    }

    private final SettingsModelDoubleBounded m_theta = createThetaModel();

    private final SettingsModelIntegerBounded m_outputDimensions = createOutputDimensionsModel();

    private final SettingsModelDoubleBounded m_perplexity = createPerplexityModel();

    private final SettingsModelIntegerBounded m_iterations = createIterationsModel();

    private final SettingsModelColumnFilter2 m_features = createFeaturesModel();

    private final SettingsModelBoolean m_removeOriginalColumns = createRemoveOriginalColumnsModel();

    private final SettingsModelBoolean m_failOnMissingValues = createFailOnMissingValuesModel();

    private final SettingsModelSeed m_seed = createSeedModel();

    private final SettingsModelIntegerBounded m_numThreads = createNumberOfThreadsModel();

    TsneNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final int inputDims = m_features.applyTo(inSpecs[0]).getIncludes().length;
        final int outputDims = m_outputDimensions.getIntValue();
        CheckUtils.checkSetting(inputDims >= outputDims,
            "The number of output dimensions (%s) must not exceed the number of input dimensions (%s).", outputDims,
            inputDims);
        // TODO support distance matrices
        return new DataTableSpec[]{
            createColumnRearranger(inSpecs[DATA_IN_PORT], Collections.emptyList(), null).createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec inputSpec, final List<Integer> missingIndices,
        final double[][] embedding) {
        final ColumnRearranger cr = new ColumnRearranger(inputSpec);
        if (m_removeOriginalColumns.getBooleanValue()) {
            cr.remove(m_features.applyTo(inputSpec).getIncludes());
        }
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(cr.createSpec());
        cr.append(new EmbeddingCellFactory(embedding, createSpecs(nameGen), missingIndices));
        return cr;
    }

    private DataColumnSpec[] createSpecs(final UniqueNameGenerator nameGen) {
        final int outDims = m_outputDimensions.getIntValue();
        final DataColumnSpec[] specs = new DataColumnSpec[outDims];
        for (int i = 0; i < outDims; i++) {
            specs[i] = nameGen.newColumn(OUTPUT_PREFIX + i, DoubleCell.TYPE);
        }
        return specs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable table = inData[DATA_IN_PORT];
        CheckUtils.checkSetting(table.size() > 2,
            "The table must have at least 2 rows in order to calculate an embedding.");
        final DataTableSpec tableSpec = table.getDataTableSpec();
        final BufferedDataTable filtered = filterTable(table, exec.createSilentSubExecutionContext(0));
        try {
            final TsneData data = new TsneData(filtered, m_failOnMissingValues.getBooleanValue());
            final List<Integer> missingIndices = data.getMissingIndices();
            if (!missingIndices.isEmpty()) {
                setMissingValuesWarning(missingIndices.size());
            }
            final double[][] embedding = learnEmbedding(data.getData(), exec);
            final ColumnRearranger cr = createColumnRearranger(tableSpec, missingIndices, embedding);
            return new BufferedDataTable[]{
                exec.createColumnRearrangeTable(table, cr, exec.createSilentSubProgress(0.0))};
        } catch (OutOfMemoryError oome) {
            throw new OutOfMemoryError("Couldn't calculate t-SNE because not enough memory is available.");
        }
    }

    private void setMissingValuesWarning(final int numMissingValues) {
        final StringBuilder sb = new StringBuilder();
        sb.append(numMissingValues).append(" row");
        if (numMissingValues > 1) {
            sb.append("s were ");
        } else {
            sb.append(" was ");
        }
        boolean isSingular = numMissingValues == 1;
        final String rowSuffix = isSingular ? "" : "s";
        final String verb = isSingular ? "was" : "were";
        final String personalPronoun = isSingular ? "it" : "they";
        setWarningMessage(String.format("%s row%s %s ignored because %s contained missing values.", numMissingValues,
            rowSuffix, verb, personalPronoun));
    }

    private BufferedDataTable filterTable(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        final DataTableSpec tableSpec = table.getDataTableSpec();
        final ColumnRearranger cr = new ColumnRearranger(tableSpec);
        cr.keepOnly(m_features.applyTo(tableSpec).getIncludes());
        return exec.createColumnRearrangeTable(table, cr, exec);
    }

    private double[][] learnEmbedding(final double[][] data, final ExecutionMonitor monitor) throws Exception {
        final int iterations = m_iterations.getIntValue();
        monitor.checkCanceled();
        monitor.setMessage("Start learning");
        // TSNE will reuse this matrix internally (or create it if we don't provide it)

        final double theta = m_theta.getDoubleValue();
        final double perplexity = m_perplexity.getDoubleValue();
        final double maxPerplexity = (data.length - 1) / 3.0;
        CheckUtils.checkSetting(perplexity <= maxPerplexity, "For your data the perplexity must be at most %s.",
            maxPerplexity);
        TSneConfiguration config = TSneUtils.buildConfig(data, m_outputDimensions.getIntValue(), data[0].length,
            perplexity, iterations, false, theta, false);
        final KnimeProgress progress = new KnimeProgress(monitor);
        config.setRandom(new Random(m_seed.getIsActive() ? m_seed.getLongValue() : System.currentTimeMillis()));
        final TSne tsne;

        if (theta == 0) {
            tsne = new FastTSne(progress);
        } else {
            final int numThreads = m_numThreads.getIntValue();
            tsne = numThreads == 1 ? new BHTSne(progress)
                : new ParallelBHTsne(progress, numThreads, KnimeThreadingExceptionHandler.INSTANCE);
        }
        Future<double[][]> tsneFuture = KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(() -> tsne.tsne(config));

        try {
            return tsneFuture.get();
        } catch (InterruptedException e) {
            tsne.abort();
            throw new CanceledExecutionException();
        }
    }

    private enum KnimeThreadingExceptionHandler implements ThreadingExceptionHandler {
            INSTANCE;
        @Override
        public void handleInterruptedException(final InterruptedException exception, List<Future<Double>> futures) {
            if (futures != null) {
                futures.stream().filter(f -> !f.isDone()).forEach(f -> f.cancel(true));
            }
        }

        @Override
        public void handleExecutionException(final ExecutionException exception, List<Future<Double>> futures) {
            handleAnyException(exception, futures);
        }

        private static void handleAnyException(final Exception exception, List<Future<Double>> futures) {
            if (futures != null) {
                futures.stream().filter(f -> !f.isDone()).forEach(f -> f.cancel(true));
            }
            throw new IllegalStateException("Calculation could not be completed", exception);
        }
    }

    private static class KnimeProgress implements Progress {

        private final ExecutionMonitor m_monitor;

        KnimeProgress(final ExecutionMonitor monitor) {
            m_monitor = monitor;
        }

        @Override
        public void setProgress(double arg0) {
            m_monitor.setProgress(arg0);
        }

        @Override
        public void log(String arg0, Object... arg1) {
            m_monitor.setMessage(String.format(arg0.replaceAll("\n", " "), arg1));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_iterations.saveSettingsTo(settings);
        m_perplexity.saveSettingsTo(settings);
        m_theta.saveSettingsTo(settings);
        m_outputDimensions.saveSettingsTo(settings);
        m_features.saveSettingsTo(settings);
        m_removeOriginalColumns.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
        m_seed.saveSettingsTo(settings);
        m_numThreads.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_iterations.validateSettings(settings);
        m_perplexity.validateSettings(settings);
        m_theta.validateSettings(settings);
        m_outputDimensions.validateSettings(settings);
        m_features.validateSettings(settings);
        m_removeOriginalColumns.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
        m_seed.validateSettings(settings);
        m_numThreads.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_iterations.loadSettingsFrom(settings);
        m_perplexity.loadSettingsFrom(settings);
        m_theta.loadSettingsFrom(settings);
        m_outputDimensions.loadSettingsFrom(settings);
        m_features.loadSettingsFrom(settings);
        m_removeOriginalColumns.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
        m_seed.loadSettingsFrom(settings);
        m_numThreads.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    private static class EmbeddingCellFactory extends AbstractCellFactory {

        private final double[][] m_embedding;

        private final int m_nCols;

        private final DataCell[] m_missingCells;

        private final Iterator<Integer> m_missingIterator;

        private int m_nextMissingIdx;

        private int m_embeddingIdx = 0;

        private int m_rowIdx = -1;

        EmbeddingCellFactory(final double[][] embedding, final DataColumnSpec[] specs,
            final Iterable<Integer> missings) {
            super(false, specs);
            m_embedding = embedding;
            m_nCols = specs.length;
            final MissingCell missingCell = new MissingCell("Missing value in the input of t-SNE.");
            m_missingCells = new DataCell[m_nCols];
            Arrays.fill(m_missingCells, missingCell);
            m_missingIterator = missings.iterator();
            advanceMissingPointer();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] getCells(final DataRow row) {
            m_rowIdx++;
            if (m_rowIdx == m_nextMissingIdx) {
                advanceMissingPointer();
                return m_missingCells;
            }
            final DataCell[] cells = new DataCell[m_nCols];
            for (int i = 0; i < m_nCols; i++) {
                cells[i] = new DoubleCell(m_embedding[m_embeddingIdx][i]);
            }
            m_embeddingIdx++;
            return cells;
        }

        private void advanceMissingPointer() {
            m_nextMissingIdx = m_missingIterator.hasNext() ? m_missingIterator.next() : -1;
        }

    }

}
