package org.knime.base.node.stats.lda2.compute;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.mine.pca.EigenValue;
import org.knime.base.node.mine.pca.PCAModelPortObject;
import org.knime.base.node.mine.pca.PCAModelPortObjectSpec;
import org.knime.base.node.stats.lda2.AbstractLDANodeModel;
import org.knime.base.node.stats.lda2.algorithm.LDA2;
import org.knime.base.node.stats.lda2.algorithm.LDAUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This is the model implementation of the LDA Learner.
 *
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDAComputeNodeModel extends AbstractLDANodeModel {

    /**
     *
     */
    private static final String INTRA_CLASS_SCATTER_MATRIX = "Intra class scatter matrix";

    /**
     *
     */
    private static final String INTER_CLASS_SCATTER_MATRIX = "Inter class scatter matrix";

    /**
     * Constructor for the node model.
     */
    LDAComputeNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE,
            PCAModelPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidSettingsException
     * @throws IllegalArgumentException
     * @throws CanceledExecutionException
     */
    @Override
    protected PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        final LDA2 lda = new LDA2(m_indices, m_computeSettings.getFailOnMissingsModel().getBooleanValue());
        lda.calculateTransformationMatrix(exec.createSubExecutionContext(0.9), inTable, m_classColIdx);

        // return the spectral decomposition and the models PortObject
        return new PortObject[]{
            createScatterTable(exec, INTRA_CLASS_SCATTER_MATRIX, m_usedColumnNames, lda.getIntraScatterMatrix()),
            createScatterTable(exec, INTER_CLASS_SCATTER_MATRIX, m_usedColumnNames, lda.getInterScatterMatrix()),
            createDecompositionTable(exec.createSubExecutionContext(0.1), lda), createModelPortObject(lda)};
    }

    /**
     * Create table spec for output of the LDA spectral decomposition.
     *
     * @param columnNames names of the input columns
     * @return table spec (first col for eigenvalues, others for components of eigenvectors)
     */
    private static DataTableSpec createDecompositionTableSpec(final String[] columnNames) {
        final DataColumnSpecCreator eigenvalueCol = new DataColumnSpecCreator("eigenvalue", DoubleCell.TYPE);

        final DataColumnSpec[] colsSpecs = new DataColumnSpec[columnNames.length + 1];
        colsSpecs[0] = eigenvalueCol.createSpec();

        for (int i = 1; i < colsSpecs.length; i++) {
            colsSpecs[i] = new DataColumnSpecCreator(columnNames[i - 1], DoubleCell.TYPE).createSpec();
        }
        return new DataTableSpec("spectral decomposition", colsSpecs);
    }

    /**
     * Returns the transformation matrix as a DataTable.
     *
     * @param exec Execution context
     * @return The transformation matrix as a DataTable.
     * @throws CanceledExecutionException if the execution was user canceled.
     */
    private BufferedDataTable createDecompositionTable(final ExecutionContext exec, final LDA2 lda)
        throws CanceledExecutionException {
        final List<EigenValue> sortedEV = lda.getEigenvalues();

        final DataTableSpec outSpec = createDecompositionTableSpec(m_usedColumnNames);
        final BufferedDataContainer result = exec.createDataContainer(outSpec);
        final int k = lda.getMaxDim();
        for (int i = 0; i < k; i++) {
            exec.checkCanceled();
            exec.setProgress((double)i / k, "Adding Eigenvalue-Eigenvector pair " + i + "/" + k + ".");

            final EigenValue ev = sortedEV.get(i);
            final DataCell[] values = new DataCell[sortedEV.size() + 1];
            values[0] = new DoubleCell(ev.getValue());
            final double[] vector = ev.getEigenVector();
            for (int j = 0; j < vector.length; j++) {
                values[j + 1] = new DoubleCell(vector[j]);
            }
            result.addRowToTable(new DefaultRow(new RowKey(i + ". eigenvector"), values));
        }
        result.close();
        return result.getTable();
    }

    private static DataTableSpec createScatterTableSpec(final String tableName, final String[] columnNames) {
        final DataColumnSpec[] colSpecs = Arrays.stream(columnNames)
            .map(s -> new DataColumnSpecCreator(s, DoubleCell.TYPE).createSpec()).toArray(DataColumnSpec[]::new);
        return new DataTableSpec(tableName, colSpecs);
    }

    private static BufferedDataTable createScatterTable(final ExecutionContext exec, final String tableName, final String[] columnNames,
        final RealMatrix matrix) throws CanceledExecutionException {
        final DataTableSpec spec = createScatterTableSpec(tableName, columnNames);
        final BufferedDataContainer scatterTable = exec.createDataContainer(spec);
        final double nRow = matrix.getRowDimension();
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            exec.checkCanceled();
            exec.setProgress(i / nRow, "Adding row " + i + "/" + nRow + " to " + tableName);
            final DataCell[] vals =
                Arrays.stream(matrix.getRow(i)).mapToObj(d -> new DoubleCell(d)).toArray(DoubleCell[]::new);
            scatterTable.addRowToTable(new DefaultRow(new RowKey(columnNames[i]), vals));
        }
        scatterTable.close();
        return scatterTable.getTable();
    }

    /**
     * Create the PortObject for this projection.
     *
     * @return the PortObject which can be applied via the ProjectionApply Node
     */
    PCAModelPortObject createModelPortObject(final LDA2 lda) {
        RealMatrix w = lda.getTransformationMatrix();
        if (w == null) {
            throw new IllegalStateException(
                "Can't create port object: The transformation matrix has not been calculated");
        }
        return new PCAModelPortObject(w.getData(),
            lda.getEigenvalues().stream().limit(w.getColumnDimension()).mapToDouble(e -> e.getValue()).toArray(),
            m_usedColumnNames, new double[w.getRowDimension()], LDAUtils.LDA_COL_PREFIX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new PortObjectSpec[]{createScatterTableSpec(INTRA_CLASS_SCATTER_MATRIX, m_usedColumnNames),
            createScatterTableSpec(INTER_CLASS_SCATTER_MATRIX, m_usedColumnNames),
            createDecompositionTableSpec(m_usedColumnNames),
            new PCAModelPortObjectSpec(m_usedColumnNames, LDAUtils.LDA_COL_PREFIX)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadAdditionalValidatedSettingsFrom(final NodeSettingsRO settings) {
        // nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateAdditionalSettings(final NodeSettingsRO settings) {
        // nothing to do
    }
}
