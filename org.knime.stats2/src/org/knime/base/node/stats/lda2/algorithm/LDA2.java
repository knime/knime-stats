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
 *   14.04.2015 (Alexander): created
 */
package org.knime.base.node.stats.lda2.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.base.node.mine.pca.EigenValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * The class that does the math of a Linear Discriminant Analysis (LDA).
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public final class LDA2 {

    private static final String MISSING_CLASS_EXCEPTION =
        "Missing values within the class column are not supported. Please replace them using a \"Missing Value\" node.";

    private static final String MISSING_VALUE_EXCEPTION =
        "Missing values are not supported. Please de-select <Fail if missing values are encountered>.";

    private final int[] m_predVarIndices;

    /** The intra-scatter matrix. */
    private RealMatrix m_sw;

    /** The inter-scatter matrix. */
    private RealMatrix m_sb;

    private RealMatrix m_w;

    private List<EigenValue> m_eigenValues;

    private int m_k = 0;

    private final boolean m_failOnMissings;

    /**
     * Constructor for creation from an existing transformation matrix, used only for prediction.
     *
     * @param colIndices the indices of the columns to use for the prediction
     * @param failOnMissings if {@code true} rows containing missing cells cause an exception
     * @param w the transformation matrix
     */
    public LDA2(final int[] colIndices, final RealMatrix w, final boolean failOnMissings) {
        // variables used for the projection
        m_predVarIndices = colIndices;
        m_w = w;
        m_failOnMissings = failOnMissings;
    }

    /**
     * The constructor for an LDA analysis, used to calculate the transformation matrix prior to prediction.
     *
     * @param colIndices the class column's index, used to calculate the transformation and the prediction.
     * @param failOnMissings if {@code true} rows containing missing cells cause an exception
     * @throws InvalidSettingsException when the table has no data
     */
    public LDA2(final int[] colIndices, final boolean failOnMissings) throws InvalidSettingsException {
        if (colIndices == null || colIndices.length == 0) {
            throw new InvalidSettingsException("No column is given to calculate the transformation matrix.");
        }
        m_predVarIndices = colIndices;
        m_failOnMissings = failOnMissings;
    }

    /**
     * Calculates the projection of the data in the row.
     *
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @return an array of double cells that constitutes the projection of the data.
     * @throws InvalidSettingsException when there are missing values
     */
    public DataCell[] getProjection(final DataRow row) throws InvalidSettingsException {
        return getProjection(row, m_k);
    }

    /**
     * Calculates the projection of the data in the row.
     *
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @param dim the number of dimensions of the projection
     * @return an array of double cells that constitutes the projection of the data.
     * @throws InvalidSettingsException when there are missing values
     */
    public DataCell[] getProjection(final DataRow row, final int dim) throws InvalidSettingsException {
        final Optional<RealVector> rVec = rowToRealVector(row);
        if (!rVec.isPresent()) {
            if (m_failOnMissings) {
                throw new IllegalArgumentException(MISSING_VALUE_EXCEPTION);
            }
            return Stream.generate(() -> DataType.getMissingCell()).limit(dim)
                .toArray(DataCell[]::new);
        }
        return writeCells(calculateProjection(rVec.get()), dim);
    }

    private RealVector calculateProjection(final RealVector rVec) throws InvalidSettingsException {
        if (m_w == null) {
            throw new IllegalStateException("The transformation matrix has not been calculated");
        }
        return m_w.preMultiply(rVec);
    }

    private static DataCell[] writeCells(final RealVector res, final int dim) {
        final DataCell[] cells = new DoubleCell[dim];
        for (int i = 0; i < dim; i++) {
            cells[i] = new DoubleCell(res.getEntry(i));
        }
        return cells;
    }

    /**
     * Returns the maximum number of dimensions to reduce to.
     *
     * @return maximum number of dimensions to reduce to
     */
    public int getMaxDim() {
        if (m_k <= 0) {
            throw new IllegalStateException("Run calculateTransformationMatrix before calling this method");
        }
        return m_k;
    }

    /**
     * @return The transformation matrix or {@code null} if not (yet) calculated.
     */
    public RealMatrix getTransformationMatrix() {
        return m_w;
    }

    /**
     * @return the eigenvalues and -vectors as a sorted list
     */
    public List<EigenValue> getEigenvalues() {
        return m_eigenValues;
    }

    /**
     * Returns the intra class scatter matrix.
     *
     * @return the intra class scatter matrix
     */
    public RealMatrix getIntraScatterMatrix() {
        return m_sw;
    }

    /**
     * Returns the inter class scatter matrix.
     *
     * @return the inter class scatter matrix
     */
    public RealMatrix getInterScatterMatrix() {
        return m_sb;
    }

    /**
     * Object holding information on the predictor variables for a specific class.
     *
     * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
     */
    static final class ClassStats {

        /** The (normalized) vector over the sum of class predictors. */
        private final RealVector m_vec;

        /** Number of entries for this class. */
        private int m_count;

        /** Constructor. */
        ClassStats(final int length) {
            m_vec = new ArrayRealVector(length, 0.0);
            m_count = 0;
        }

        /**
         * Normalizes the sum to get the mean value of the data.
         */
        public void normalize() {
            m_vec.mapDivideToSelf(m_count);
        }

        /**
         * Add data to this class and update the count.
         *
         * @param data Values to add.
         */
        public void add(final RealVector data) {
            m_count++;
            m_vec.combineToSelf(1, 1, data);
        }

        /**
         * Returns either the (normalized) vector of sums over the predictor variables.
         *
         * @return the (normalized) class vector
         */
        public RealVector getVector() {
            return m_vec;
        }

        /**
         * Returns the class count.
         *
         * @return the classe count
         */
        public int getCount() {
            return m_count;
        }
    }

    /**
     * Calculates the class statistics and does some sanity checks.
     *
     * @param exec the execution contex
     * @param inTable the in data table
     * @param k the maximum number of dimensions to reduce to
     * @param classColIndex the class column index
     * @return the individual class statistics
     * @throws CanceledExecutionException - If execution has been canceled
     * @throws InvalidSettingsException - If the table contains less classes then the number of dimensions to reduce to
     */
    private Map<String, ClassStats> calculateClassStats(final ExecutionContext exec, final BufferedDataTable inTable,
        final int k, final int classColIndex) throws CanceledExecutionException, InvalidSettingsException {
        final Map<String, ClassStats> classStats = new HashMap<>();
        // Values for calculating the total mean in the end
        long progressCount = 0;
        final Set<String> classes = new HashSet<>();
        // First calculate the class means and counts and the total mean and count
        for (final DataRow row : inTable) {
            exec.checkCanceled();
            progressCount++;
            exec.setProgress(progressCount / ((double)inTable.size()), "Calculating class stats - Processed row "
                + progressCount + "/" + inTable.size() + " (\"" + row.getKey() + "\")");

            final DataCell cell = row.getCell(classColIndex);
            if (cell.isMissing()) {
                throw new IllegalArgumentException(MISSING_CLASS_EXCEPTION);
            }
            final String cl = cell.toString();

            classes.add(cl);

            final Optional<RealVector> d = rowToRealVector(row);
            if (!d.isPresent()) {
                if (m_failOnMissings) {
                    throw new IllegalArgumentException(MISSING_VALUE_EXCEPTION);
                }
                continue;
            }

            // will be null in the first iteration
            if (!classStats.containsKey(cl)) {
                classStats.put(cl, new ClassStats(m_predVarIndices.length));
            }
            classStats.get(cl).add(d.get());
        }

        if (classStats.size() == 0) {
            throw new IllegalArgumentException("The table contains only rows contain missings data");
        }

        if (k >= classStats.size()) {
            classes.removeAll(classStats.keySet());
            throw new InvalidSettingsException("Not enough distinct classes in the class column \""
                + inTable.getSpec().getColumnSpec(classColIndex).getName() + "\": The data can only be reduced to "
                + (classStats.size() - 1) + " or fewer dimensions."
                + ((classes.size() == 0) ? "" : " Note that all rows for class(es) ("
                    + classes.stream().collect(Collectors.joining(",")) + ") contain missings"));
        }
        return classStats;
    }

    /**
     * Calculates the transformation matrix.
     *
     * @param exec the execution context.
     * @param inTable the input table
     * @param classColIndex the class column index
     * @throws CanceledExecutionException when the execution is cancelled by the user.
     * @throws InvalidSettingsException when the settings are not suitable for the data.
     */
    public void calculateTransformationMatrix(final ExecutionContext exec, final BufferedDataTable inTable,
        final int classColIndex) throws CanceledExecutionException, InvalidSettingsException {
        checkClassColIdx(classColIndex, inTable.getSpec().getNumColumns());
        final Map<String, ClassStats> classStats =
            calculateClassStats(exec.createSubExecutionContext(0.5), inTable, -1, classColIndex);
        m_k = Math.min(classStats.size() - 1, m_predVarIndices.length);
        CheckUtils.checkArgument(m_k > 0, "The class column \"%s\" contains only a single class.",
            classStats.keySet().iterator().next());
        calcTransformationMatrix(exec, inTable, classStats, classColIndex);
    }

    /**
     * Calculates the transformation matrix.
     *
     * @param exec the execution context.
     * @param inTable the input table
     * @param k the number of dimensions to reduce to
     * @param classColIndex the class column index
     * @throws CanceledExecutionException when the execution is cancelled by the user.
     * @throws InvalidSettingsException when the settings are not suitable for the data.
     */
    public void calculateTransformationMatrix(final ExecutionContext exec, final BufferedDataTable inTable, final int k,
        final int classColIndex) throws CanceledExecutionException, InvalidSettingsException {
        checkClassColIdx(classColIndex, inTable.getSpec().getNumColumns());
        CheckUtils.checkSetting(k > 0, "Cannot reduce the number of dimensions to less than one.");
        m_k = k;
        // Map for storing the per-class sum and count to calculate the mean
        final Map<String, ClassStats> classStats =
            calculateClassStats(exec.createSubExecutionContext(0.5), inTable, m_k, classColIndex);

        calcTransformationMatrix(exec, inTable, classStats, classColIndex);
    }

    /**
     * Calculates the transformation matrix.
     *
     * @param exec the execution context.
     * @param inTable the input table
     * @param classStats the class statistics
     * @param classColIndex the class column index
     * @throws CanceledExecutionException when the execution is cancelled by the user.
     * @throws InvalidSettingsException when the settings are not suitable for the data.
     */
    private void calcTransformationMatrix(ExecutionContext exec, final BufferedDataTable inTable,
        final Map<String, ClassStats> classStats, final int classColIndex)
        throws InvalidSettingsException, CanceledExecutionException {
        // calculate the mean
        final RealVector totalMean = new ArrayRealVector(m_predVarIndices.length, 0.0);
        long nonMissingCnt = 0;

        for (final Map.Entry<String, ClassStats> entry : classStats.entrySet()) {
            final int count = entry.getValue().getCount();
            if (count < m_predVarIndices.length) {
                throw new InvalidSettingsException(
                    "The size of the smallest group must be larger than the number of predictor variables ("
                        + m_predVarIndices.length + "). Class \"" + entry.getKey() + "\" has only " + count
                        + " non-missing instance" + (count == 1 ? "." : "s.")
                        + " Please reduce the number of selected predictor variables.");
            }
            totalMean.combineToSelf(1, 1, entry.getValue().getVector());
            nonMissingCnt += entry.getValue().getCount();

            // finally, normalize the class stats, i.e., calculate the means
            entry.getValue().normalize();
        }

        // normalize, i.e., calculate the mean
        totalMean.mapDivideToSelf(nonMissingCnt);

        // Calculate the inter-class scatter matrix (this is an nrPred x nrPred covariance-matrix)
        long progressCount = 0;
        exec = exec.createSubExecutionContext(0.5);
        for (final DataRow row : inTable) {
            exec.checkCanceled();
            progressCount++;
            exec.setProgress(progressCount / ((double)inTable.size()),
                "Calculating inter-class scatter matrix - Processed row " + progressCount + "/" + inTable.size()
                    + " (\"" + row.getKey() + "\").");

            // subtract mean from data
            final Optional<RealVector> rVec = rowToRealVector(row);
            if (!rVec.isPresent()) {
                // no need to check throw exception since this would have been triggered already during #calcClassStats
                if (!m_failOnMissings) {
                    continue;
                }
            }
            final RealVector v =
                rVec.get().combineToSelf(1, -1, classStats.get(row.getCell(classColIndex).toString()).getVector());

            // make it to a matrix: do the outer product
            final RealMatrix si = v.outerProduct(v);

            m_sw = (m_sw != null) ? m_sw.add(si) : si;
        }

        // Calculate the intra-class scatter matrix (nrPred x nrPred covariance-matrix)
        progressCount = 0;
        exec = exec.createSubExecutionContext(0.1);
        for (final Map.Entry<String, ClassStats> entry : classStats.entrySet()) {
            exec.checkCanceled();
            progressCount++;
            exec.setProgress(progressCount / ((double)inTable.size()),
                "Calculating intra-class scatter matrix - Processed class " + progressCount + "/" + classStats.size()
                    + " (\"" + entry.getKey() + "\").");

            // subtract total mean from class mean and build the scatter matrix
            final RealVector v = entry.getValue().getVector().combineToSelf(1, -1, totalMean);
            final RealMatrix s = v.outerProduct(v).scalarMultiply(entry.getValue().getCount());

            m_sb = (m_sb != null) ? m_sb.add(s) : s;
        }

        // check for reasonable inter-class scatter matrix and throw reasonable error (AP-6588)
        final DecompositionSolver solver = new LUDecomposition(m_sw).getSolver();
        if (!solver.isNonSingular()) {
            throw new InvalidSettingsException(
                "Cannot invert the inter-class scatter matrix as it is singular. Most likely, two input columns are"
                    + " linearly dependent, i.e. differ only by a constant factor.");
        }

        // Now extract eigenvalues of sw^-1 * sb
        final RealMatrix mat = solver.getInverse().multiply(m_sb);
        final EigenDecomposition ed = new EigenDecomposition(mat);
        final double[] eigenValues = ed.getRealEigenvalues();
        final double[][] eigenVectors = new double[m_predVarIndices.length][eigenValues.length];
        for (int i = 0; i < eigenValues.length; i++) {
            final double[] vec = ed.getEigenvector(i).mapMultiply(1.0 / ed.getEigenvector(i).getNorm()).toArray();
            for (int j = 0; j < m_predVarIndices.length; j++) {
                eigenVectors[j][i] = vec[j];
            }
        }

        // rescale to comply with python
        m_sb = m_sb.scalarMultiply(1.0 / nonMissingCnt);
        m_sw = m_sw.scalarMultiply(1.0 / nonMissingCnt);

        m_eigenValues = EigenValue.createSortedList(eigenVectors, eigenValues);

        // Create the transformation matrix.
        final double[][] w = new double[m_k][];
        for (int i = 0; i < m_k; i++) {
            final double[] vec = m_eigenValues.get(i).getEigenVector();
            w[i] = vec;
        }

        m_w = MatrixUtils.createRealMatrix(w).transpose();
    }

    /**
     * Sanity check for the class column index.
     *
     * @param classColIndex the class column index
     * @param numCols the total number of columns
     * @throws InvalidSettingsException - If the index is 0
     */
    private static void checkClassColIdx(final int classColIndex, final int numCols) throws InvalidSettingsException {
        CheckUtils.checkSetting(classColIndex >= 0 && classColIndex < numCols, "No valid class column is given.");
    }

    /**
     * Extracts the predictor variables from a data row and stores them in a {@link RealVector}.
     *
     * @param row the data row
     * @return the predictor variables of that row
     */
    private Optional<RealVector> rowToRealVector(final DataRow row) {
        final RealVector d = new ArrayRealVector(m_predVarIndices.length);
        for (int c = 0; c < m_predVarIndices.length; c++) {
            final DataCell cell = row.getCell(m_predVarIndices[c]);
            if (cell.isMissing()) {
                return Optional.empty();
            }
            d.setEntry(c, ((DoubleValue)cell).getDoubleValue());
        }
        return Optional.of(d);
    }

}