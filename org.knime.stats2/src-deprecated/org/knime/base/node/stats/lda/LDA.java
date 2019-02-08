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
package org.knime.base.node.stats.lda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.stats.lda2.algorithm.LDA2;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Alexander Fillbrunn
 * @deprecated use {@link LDA2} instead
 */
@Deprecated
public class LDA {

    private int m_k;

    private BufferedDataTable m_data;

    private int[] m_indices;

    private int m_classCol;

    private RealMatrix m_w;

    private double[][] m_eigenvectors;

    private double[] m_eigenvalues;

    /**
     * Constructor for LDA.
     * @param data the data to use
     * @param colIndices the indices of the columns to use for the analysis
     * @param classColIndex the class column's index
     * @param k the number of dimensions to reduce to
     * @throws InvalidSettingsException when the table has no data
     */
    public LDA(final BufferedDataTable data, final int[] colIndices, final int classColIndex, final int k)
            throws InvalidSettingsException {
        if (data.getRowCount() == 0) {
            throw new InvalidSettingsException("The given data does not contain any rows.");
        }
        m_data = data;
        m_indices = colIndices;
        m_classCol = classColIndex;
        m_k = k;
    }

    /**
     * Calculates the projection of the data in the row.
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @return an array of double cells that constitutes the projection of the data.
     * @throws InvalidSettingsException when there are missing values
     */
    public DoubleCell[] getProjection(final DataRow row) throws InvalidSettingsException {
        if (m_w == null) {
            throw new IllegalStateException("The transformation matrix has not been calculated");
        }
        RealMatrix x = rowToMatrix(row);
        RealMatrix y = m_w.multiply(x);
        double[] p = y.getColumn(0);
        DoubleCell[] cells = new DoubleCell[p.length];
        for (int i = 0; i < p.length; i++) {
            cells[i] = new DoubleCell(p[i]);
        }
        return cells;
    }

    /**
     * @return the eigenvectors
     */
    public double[][] getEigenvectors() {
        return m_eigenvectors;
    }

    /**
     * @return the eigenvalues
     */
    public double[] getEigenvalues() {
        return m_eigenvalues;
    }

    /**
     * Calculates the transformation matrix for the projection of data into the smaller space.
     * @param exec the execution context.
     * @throws CanceledExecutionException when the execution is cancelled by the user.
     * @throws InvalidSettingsException when the settings are not suitable for the data.
     */
    public void calculateTransformationMatrix(final ExecutionContext exec)
            throws CanceledExecutionException, InvalidSettingsException {

        if (m_k == 0) {
            throw new InvalidSettingsException("0 is not a valid value for the number of dimensions to reduce to.");
        }

        // Maps for storing the per-class sum and count to calculate the mean in the end
        Map<String, RealMatrix> classSums = new HashMap<String, RealMatrix>();
        Map<String, Integer> classCounts = new HashMap<String, Integer>();

        // Values for calculating the total mean in the end
        RealMatrix totalMean;
        int totalCount = 0;
        double max = m_data.getRowCount() * 2;

        // First calculate the class means and counts and the total mean and count
        for (DataRow row : m_data) {
            exec.checkCanceled();

            DataCell cell = row.getCell(m_classCol);
            if (cell.isMissing()) {
                throw new InvalidSettingsException("Missing values are not supported. "
                    + "Please replace them using a Missing Value node.");
            }
            String cl = cell.toString();

            RealMatrix d = rowToMatrix(row);
            Integer count = classCounts.get(cl);
            RealMatrix sum = classSums.get(cl);

            if (sum == null) {
                sum = d;
                count = 1;
            } else {
                sum = sum.add(d);
                count += 1;
            }
            totalCount++;
            classSums.put(cl, sum);
            classCounts.put(cl, count);
            exec.setProgress(max / totalCount);
        }

        if (m_k >= classCounts.size()) {
            throw new InvalidSettingsException("The data can only be reduced to "
                                        + (classCounts.size() - 1) + " or fewer dimensions.");
        }

        RealMatrix totalSum = null;
        for (String key : classSums.keySet()) {
            RealMatrix sum = classSums.get(key);
            classSums.put(key, sum.scalarMultiply(1.0 / classCounts.get(key)));
            if (totalSum == null) {
                totalSum = sum;
            } else {
                totalSum = totalSum.add(sum);
            }
        }

        if (totalSum == null) {
            throw new InvalidSettingsException("The table contains no classes");
        }

        totalMean = totalSum.scalarMultiply(1.0 / totalCount);

        // Calculate the within- and between-class scatter matrices
        RealMatrix sw = null;
        // Reset totalCount to report the progress correctly
        for (DataRow row : m_data) {
            String cl = row.getCell(m_classCol).toString();
            RealMatrix m = classSums.get(cl);
            RealMatrix v = rowToMatrix(row).subtract(m);
            RealMatrix si = v.multiply(v.transpose());
            if (sw == null) {
                sw = si;
            } else {
                sw = sw.add(si);
            }
            totalCount++;
            exec.setProgress(max / totalCount);
        }
        RealMatrix sb = null;
        for (String key : classSums.keySet()) {
            RealMatrix m = classSums.get(key).subtract(totalMean);
            RealMatrix s = m.multiply(m.transpose()).scalarMultiply(classCounts.get(key));
            if (sb == null) {
                sb = s;
            } else {
                sb = sb.add(s);
            }
        }

        // Now extract eigenvalues of sw^-1 * sb
        RealMatrix mat = new LUDecomposition(sw).getSolver().getInverse().multiply(sb);
        EigenDecomposition ed = new EigenDecomposition(mat);
        double[] ev = ed.getRealEigenvalues();
        ArrayList<Double> eigenvalues = new ArrayList<Double>();
        for (double d : ev) {
            eigenvalues.add(d);
        }

        // Remember indices before sorting so we can access the correct eigenvectors later
        IdentityHashMap<Double, Integer> originalIndices
        = new IdentityHashMap<Double, Integer>();
        for (int i = 0; i < eigenvalues.size(); i++) {
            originalIndices.put(eigenvalues.get(i), i);
        }

        // Sort the eigenvalues and select only the best ones
        Collections.sort(eigenvalues);
        //List<Double> bestK = eigenvalues.subList(eigenvalues.size() - m_k, eigenvalues.size());

        double[][] w = new double[m_k][];
        m_eigenvectors = new double[m_indices.length][eigenvalues.size()];
        m_eigenvalues = new double[eigenvalues.size()];

        for (int i = 0; i < eigenvalues.size(); i++) {
            double[] vec = ed.getEigenvector(originalIndices.get(eigenvalues.get(
                eigenvalues.size() - i - 1))).toArray();
            m_eigenvalues[i] = eigenvalues.get(eigenvalues.size() - i - 1);
            if (i < m_k) {
                w[i] = vec;
            }
            for (int j = 0; j < m_indices.length; j++) {
                m_eigenvectors[j][i] = vec[j];
            }
        }
        // Create the transformation matrix
        m_w = MatrixUtils.createRealMatrix(w);
    }

    private RealMatrix rowToMatrix(final DataRow row) throws InvalidSettingsException {
        double[] d = new double[m_indices.length];
        for (int c = 0; c < m_indices.length; c++) {
            DataCell cell = row.getCell(m_indices[c]);
            if (cell.isMissing()) {
                throw new InvalidSettingsException("Missing values are not supported. "
                    + "Please replace them using a Missing Value node.");
            }
            d[c] = ((DoubleValue)cell).getDoubleValue();
        }
        return new Array2DRowRealMatrix(d);
    }
}
