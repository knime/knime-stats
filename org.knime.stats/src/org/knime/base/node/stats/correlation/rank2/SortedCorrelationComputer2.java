/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.stats.correlation.rank2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.TDistribution;
import org.knime.base.node.preproc.correlation.CorrelationUtils.CorrelationResult;
import org.knime.base.node.preproc.correlation.compute2.CorrelationComputer2;
import org.knime.base.node.preproc.correlation.compute2.PValueAlternative;
import org.knime.base.util.HalfDoubleMatrix;
import org.knime.base.util.HalfIntMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

/**
 * Calculates pairwise correlation values for a table.
 *
 * @author Iris Adae, University of Konstanz
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class SortedCorrelationComputer2 {

    private SortedCorrelationComputer2() {
        // Utility class
    }

    /**
     * This class is used to generate the ranks.
     *
     * @author Iris Adae, University of Konstanz
     */
    private static class SortablePair {

        // the data cell
        private DataCell m_dc;

        private int m_i;

        // Rank is double as we need to be able to cover duplicate values
        private double m_rankValue;

        SortablePair(final int i, final DataCell dc) {
            m_i = i;
            m_dc = dc;
        }

        /**
         * @return the m_dc
         */
        protected DataCell getDataCell() {
            return m_dc;
        }

        protected int getOrignalOrder() {
            return m_i;
        }

        protected double getRank() {
            return m_rankValue;
        }

        protected void setRank(final double rank) {
            m_rankValue = rank;
        }

    }

    /**
     * The Ranks are always only calculated for one column at a time. This might be slower but is decreasing the amount
     * of necessary memory.
     *
     * @param bdt the data table to convert
     * @param colIndex the column to rank
     * @param exec the execution context to report progress to
     * @return a Buffered Data Table where column colIndex is replaced with a numerical column containing its rank
     * @throws CanceledExecutionException
     */
    private static BufferedDataTable getRanks(final BufferedDataTable bdt, final int colIndex,
        final ExecutionContext exec) throws CanceledExecutionException {

        final DataValueComparator colComparators =
            bdt.getDataTableSpec().getColumnSpec(colIndex).getType().getComparator();

        LinkedList<SortablePair> myList = new LinkedList<>();
        // read the data
        int counter = 0;
        for (DataRow row : bdt) {
            DataCell dCell = row.getCell(colIndex);
            myList.add(new SortablePair(counter++, dCell));
            exec.checkCanceled();
        }
        exec.setProgress(0.2);

        // sort the data by value
        Collections.sort(myList, (dr1, dr2) -> {

            if (dr1 == dr2) {
                return 0;
            }
            if (dr1 == null) {
                return 1;
            }
            if (dr2 == null) {
                return -1;
            }
            return colComparators.compare(dr1.getDataCell(), dr2.getDataCell());
        });
        exec.setProgress(0.4);

        // check for duplicates and adjust their rank
        counter = 1;
        DataCell lastCell = null;
        HashMap<DataCell, Double> duplicateValues = new HashMap<>();
        int nrOfDups = 0;
        for (SortablePair p : myList) {
            exec.checkCanceled();
            double rank = 1.0 * counter++;
            DataCell currentCell = p.getDataCell();
            if (lastCell != null) {
                // init last cell
                if (colComparators.compare(lastCell, currentCell) == 0) {
                    if (duplicateValues.containsKey(lastCell)) {
                        nrOfDups++;
                        duplicateValues.put(lastCell,
                            ((duplicateValues.get(lastCell) * (nrOfDups - 1) + rank) / nrOfDups));
                    } else {
                        duplicateValues.put(lastCell, (rank - 0.5));
                        nrOfDups = 2;
                    }
                } else {
                    nrOfDups = 0;
                }
            }
            lastCell = p.getDataCell();
            p.setRank(rank);
        }
        exec.setProgress(0.6);

        // resolve duplicates
        if (duplicateValues.size() > 0) {
            // change the duplicates
            for (SortablePair p : myList) {
                exec.checkCanceled();
                Double d = duplicateValues.get(p.getDataCell());
                if (d != null) {
                    p.setRank(d);

                }
            }
        }
        exec.setProgress(0.8);

        // sort the data by counter backwards
        Collections.sort(myList, (dr1, dr2) -> {
            if (dr1 == dr2) {
                return 0;
            }
            if (dr1 == null) {
                return 1;
            }
            if (dr2 == null) {
                return -1;
            }
            return dr1.getOrignalOrder() - dr2.getOrignalOrder();
        });

        return replace(bdt, colIndex, myList, exec.createSubExecutionContext(0.2));
    }

    /**
     * Replaces the values of the column colIndex in the Table bdt, with the values from mylist.
     *
     * @param bdt original data table
     * @param colIndex column to replace
     * @param myList new values
     * @return the original data table where the defined column is replaced by the values from mylist
     * @throws CanceledExecutionException if canceled by user.
     */
    private static BufferedDataTable replace(final BufferedDataTable bdt, final int colIndex,
        final LinkedList<SortablePair> myList, final ExecutionContext exec) throws CanceledExecutionException {
        // Create ColumnRearranger
        ColumnRearranger c = new ColumnRearranger(bdt.getDataTableSpec());

        // Spec of the new Counter Column
        DataColumnSpec newColSpec =
            new DataColumnSpecCreator(bdt.getDataTableSpec().getColumnSpec(colIndex).getName(), DoubleCell.TYPE)
                .createSpec();

        final Iterator<SortablePair> it = myList.iterator();

        // Fill the cells of the new column
        CellFactory factory = new SingleCellFactory(newColSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                if (it.hasNext()) {
                    return new DoubleCell(it.next().getRank());
                }
                return DataType.getMissingCell();
            }
        };

        // Append Column
        c.replace(factory, colIndex);

        return exec.createColumnRearrangeTable(bdt, c, exec);
    }

    /**
     * This methods calculate the ranks of all column in the data table.
     *
     * @param table the original data table
     * @param exec execution context for progress reprot
     * @throws CanceledExecutionException if canceled by user.
     */
    static BufferedDataTable calculateRank(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        BufferedDataTable rank = table;
        int nrofColumns = table.getDataTableSpec().getNumColumns();

        for (int i = 0; i < table.getDataTableSpec().getNumColumns(); i++) {
            exec.setMessage("Ranking column " + table.getDataTableSpec().getColumnNames()[i]);
            exec.checkCanceled();
            rank = getRanks(rank, i, exec.createSubExecutionContext(1.0 / nrofColumns));
        }
        return rank;
    }

    /**
     * Calculates the Spearmans rank for all pairs of Data table columns based on previously calculated ranks.
     *
     * @param exec the Execution context.
     * @param pValueAlternative
     * @return the output matrix to be turned into the output model
     * @throws CanceledExecutionException if canceled by users
     */
    static CorrelationResult calculateSpearman(final BufferedDataTable rank, final ExecutionMonitor exec,
        final PValueAlternative pValueAlternative) throws CanceledExecutionException {
        // the ranking must have been calculated before
        assert (rank != null);
        final CorrelationComputer2 calculator = new CorrelationComputer2(rank.getDataTableSpec(), 0);

        // Calculate statistics on the table
        exec.setMessage("Calculating table statistics");
        final ExecutionMonitor execStep1 = exec.createSubProgress(0.5);
        calculator.calculateStatistics(rank, execStep1);
        execStep1.setProgress(1.0);

        // Calculate the correlation
        exec.setMessage("Calculating correlation values");
        final ExecutionMonitor execStep2 = exec.createSubProgress(0.5);
        final CorrelationResult calculateOutput =
            calculator.calculateOutput(rank, execStep2, PValueAlternative.TWO_SIDED);
        final HalfDoubleMatrix corrMatrix = calculateOutput.getCorrelationMatrix();
        final HalfIntMatrix dofMatrix = calculateOutput.getDegreesOfFreedomMatrix();
        final HalfDoubleMatrix pValMatrix =
            calculateSpearmanCorrelationPValue(pValueAlternative, corrMatrix, dofMatrix);

        return new CorrelationResult(corrMatrix, pValMatrix, dofMatrix);
    }

    /** Calculates the p-values for a matrix of correlation results */
    private static HalfDoubleMatrix calculateSpearmanCorrelationPValue(final PValueAlternative pValueAlternative,
        final HalfDoubleMatrix corrMatrix, final HalfIntMatrix dofMatrix) {
        final HalfDoubleMatrix pValMatrix = new HalfDoubleMatrix(corrMatrix.getRowCount(), false);
        for (int i = 0; i < corrMatrix.getRowCount(); i++) {
            for (int j = i + 1; j < corrMatrix.getRowCount(); j++) {
                final double corr = corrMatrix.get(i, j);
                final int dof = dofMatrix.get(i, j);
                final double pVal = calculateSpearmanCorrelationPValue(pValueAlternative, corr, dof);
                pValMatrix.set(i, j, pVal);
            }
        }
        return pValMatrix;
    }

    /** Calculates the p-value for one correlation result */
    private static double calculateSpearmanCorrelationPValue(final PValueAlternative pValueAlternative,
        final double corr, final int dof) {
        if (dof > 0 && !Double.isNaN(corr)) {
            // See https://github.com/scipy/scipy/blob/v1.3.1/scipy/stats/stats.py#L3613-L3764
            final TDistribution tDistr = new TDistribution(null, dof);
            final double t = corr * Math.sqrt((dof / ((corr + 1.0) * (1.0 - corr))));
            if (Double.isNaN(t)) {
                // We divided by 0 -> correlation is about 1
                return 0;
            } else {
                switch (pValueAlternative) {
                    case TWO_SIDED:
                        return 2 * (1 - tDistr.cumulativeProbability(Math.abs(t)));

                    case LESS:
                        return tDistr.cumulativeProbability(t);

                    case GREATER:
                        return 1 - tDistr.cumulativeProbability(t);

                    default:
                        // Cannot happen
                        return Double.NaN;
                }
            }
        } else {
            return Double.NaN;
        }
    }

    /**
     * Calculates the kendall rank for all pairs of Data table columns based on previously calculated ranks.
     *
     * @param exec the Execution context.
     * @param corrType the type of correlation used, as defined in CorrelationComputeNodeModel
     * @return the output matrix to be turned into the output model
     * @throws CanceledExecutionException if canceled by users
     */
    static HalfDoubleMatrix calculateKendallInMemory(final BufferedDataTable rankTable, final String corrType,
        final ExecutionMonitor exec) throws CanceledExecutionException {

        // the ranking must have been calculated before
        assert (rankTable != null);
        final int coCount = rankTable.getDataTableSpec().getNumColumns();
        final int rowCount = rankTable.getRowCount();

        double[][] rank = new double[rowCount][coCount];
        int c = 0;
        for (DataRow row : rankTable) {
            for (int k = 0; k < coCount; k++) {
                rank[c][k] = ((DoubleValue)row.getCell(k)).getDoubleValue();
            }
            c++;
        }

        final HalfDoubleMatrix nominatorMatrix = new HalfDoubleMatrix(coCount, /*includeDiagonal=*/false);

        double[][] cMatrix = new double[coCount][coCount];
        double[][] dMatrix = new double[coCount][coCount];
        double[][] txMatrix = new double[coCount][coCount];
        double[][] tyMatrix = new double[coCount][coCount];

        for (int rowIn1 = 0; rowIn1 < rowCount; rowIn1++) {
            for (int rowIn2 = 0; rowIn2 < rowCount; rowIn2++) {
                exec.checkCanceled();
                for (int i = 0; i < coCount; i++) {
                    final double x1 = rank[rowIn1][i];
                    final double x2 = rank[rowIn2][i];
                    for (int j = 0; j < coCount; j++) {
                        final double y1 = rank[rowIn1][j];
                        final double y2 = rank[rowIn2][j];
                        if (x1 < x2 && y1 < y2) { // values are concordant
                            cMatrix[i][j]++;
                        } else if (x1 < x2 && y1 > y2) { // values are discordant
                            dMatrix[i][j]++;
                        } else if (x1 != x2 && y1 == y2) { // values are bounded in y
                            tyMatrix[i][j]++;
                        } else if (x1 == x2 && y1 != y2) { // values are bounded in x
                            txMatrix[i][j]++;
                        } else if (x1 == x2 && y1 == y2) { // values are bounded in x and y
                            // txyMatrix[i][j]++; // no measure need this count
                        }
                    }
                }
            }

            exec.checkCanceled();
            exec.setProgress(0.95 * rowIn1 / rowCount, String.format("Calculating - %d/%d ", rowIn1, rowCount));
        }

        // the calculation of the matrix will be much more time intensive, so we only assign 5%
        // of the execution time to the final calculation of

        if (corrType.equals(RankCorrelationCompute2NodeModel.CFG_KENDALLA)) {
            double nrOfRows = rankTable.size();
            // kendalls Tau a
            double divisor = (nrOfRows * (nrOfRows - 1.0)) * 0.5;
            for (int i = 0; i < coCount; i++) {
                for (int j = i + 1; j < coCount; j++) {
                    nominatorMatrix.set(i, j, (cMatrix[i][j] - dMatrix[i][j]) / divisor);
                }
                exec.setProgress(0.05 * i / coCount, "Calculating correlations");
            }

        } else if (corrType.equals(RankCorrelationCompute2NodeModel.CFG_KENDALLB)) {
            double n0 = rowCount * (rowCount - 1) * 0.5;

            // kendalls Tau b
            for (int i = 0; i < coCount; i++) {
                for (int j = i + 1; j < coCount; j++) {
                    // we divide tx and ty by 2, as each of the pairs was counted twice
                    double tiesX = txMatrix[i][j];
                    double tiesY = tyMatrix[i][j];
                    double n1 = tiesX * 0.5;
                    double n2 = tiesY * 0.5;
                    double div = Math.sqrt((n0 - n1) * (n0 - n2));
                    nominatorMatrix.set(i, j, (cMatrix[i][j] - dMatrix[i][j]) / div);

                    // P-Values:
                    // See https://github.com/scipy/scipy/blob/v1.3.1/scipy/stats/stats.py#L3861-L4052
                    // https://www.rdocumentation.org/packages/stats/versions/3.6.1/source

                }
                exec.setProgress(0.05 * i / coCount, "Calculating correlations");
            }

        } else if (corrType.equals(RankCorrelationCompute2NodeModel.CFG_KRUSKALAL)) {
            // Kruskals Gamma
            for (int i = 0; i < coCount; i++) {
                for (int j = i + 1; j < coCount; j++) {
                    final double concordances = cMatrix[i][j];
                    final double discordances = dMatrix[i][j];
                    nominatorMatrix.set(i, j, (concordances - discordances) / (concordances + discordances));

                    // P-Values:
                    // See https://es.mathworks.com/matlabcentral/mlc-downloads/downloads/submissions/42645/versions/2/previews/gkgammatst.m/index.html
                }
                exec.setProgress(0.05 * i / coCount, "Calculating correlations");

            }

        }

        return nominatorMatrix;
    }
}
