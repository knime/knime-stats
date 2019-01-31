package org.knime.base.node.stats.lda2.compute;

import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.mine.pca.EigenValue;
import org.knime.base.node.stats.lda2.AbstractLDANodeModel;
import org.knime.base.node.stats.lda2.algorithm.LDA2;
import org.knime.base.node.stats.lda2.algorithm.LDAUtils;
import org.knime.base.node.stats.lda2.port.LDAModelPortObject;
import org.knime.base.node.stats.lda2.port.LDAModelPortObjectSpec;
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
     * Constructor for the node model.
     */
    LDAComputeNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, LDAModelPortObject.TYPE});
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
        final LDA2 lda = new LDA2(m_indices);
        lda.calculateTransformationMatrix(exec.createSubExecutionContext(0.9), inTable, m_classColIdx);

        // return the spectral decomposition and the models PortObject
        return new PortObject[]{createDecompositionTable(exec.createSubExecutionContext(0.1), lda),
            createLDAModelPortObject(lda)};
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

    /**
     * Create the PortObject for this projection.
     *
     * @return the PortObject which can be applied via the ProjectionApply Node
     */
    LDAModelPortObject createLDAModelPortObject(final LDA2 lda) {
        RealMatrix w = lda.getTransformationMatrix();
        if (w == null) {
            throw new IllegalStateException(
                "Can't create port object: The transformation matrix has not been calculated");
        }

        return new LDAModelPortObject(m_usedColumnNames, w);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new PortObjectSpec[]{createDecompositionTableSpec(m_usedColumnNames), new LDAModelPortObjectSpec(
            m_usedColumnNames,
            LDAUtils.calcPositiveMaxDim(inSpec, m_computeSettings.getClassModel().getStringValue(), m_indices.length))};
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
