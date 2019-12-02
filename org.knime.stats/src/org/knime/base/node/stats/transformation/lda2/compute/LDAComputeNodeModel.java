package org.knime.base.node.stats.transformation.lda2.compute;

import java.util.Arrays;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.data.statistics.TransformationMatrix;
import org.knime.base.node.mine.transformation.port.TransformationPortObject;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec.TransformationType;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.base.node.stats.transformation.lda2.AbstractLDANodeModel;
import org.knime.base.node.stats.transformation.lda2.algorithm.LDA2;
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

    /** The intra class scatter matrix table name. */
    private static final String INTRA_CLASS_SCATTER_MATRIX = "Intra class scatter matrix";

    /** The inter class scatter matrix table name. */
    private static final String INTER_CLASS_SCATTER_MATRIX = "Inter class scatter matrix";

    /**
     * Constructor for the node model.
     */
    LDAComputeNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE,
            TransformationPortObject.TYPE});
    }

    @Override
    protected PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        final LDA2 lda = new LDA2(m_indices, m_computeSettings.getFailOnMissingsModel().getBooleanValue());
        lda.calculateTransformationMatrix(exec.createSubExecutionContext(0.9), inTable, m_classColIdx);

        // return the spectral decomposition and the models PortObject
        return new PortObject[]{
            createScatterTable(exec, INTRA_CLASS_SCATTER_MATRIX, m_usedColumnNames, lda.getIntraScatterMatrix()),
            createScatterTable(exec, INTER_CLASS_SCATTER_MATRIX, m_usedColumnNames, lda.getInterScatterMatrix()),
            TransformationUtils.createEigenDecompositionTable(exec.createSubExecutionContext(0.1),
                lda.getTransformationMatrix(), m_usedColumnNames),
            createModelPortObject(lda.getTransformationMatrix())};
    }

    private static DataTableSpec createScatterTableSpec(final String tableName, final String[] columnNames) {
        final DataColumnSpec[] colSpecs = Arrays.stream(columnNames)
            .map(s -> new DataColumnSpecCreator(s, DoubleCell.TYPE).createSpec()).toArray(DataColumnSpec[]::new);
        return new DataTableSpec(tableName, colSpecs);
    }

    private static BufferedDataTable createScatterTable(final ExecutionContext exec, final String tableName,
        final String[] columnNames, final RealMatrix matrix) throws CanceledExecutionException {
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

    TransformationPortObject createModelPortObject(final TransformationMatrix transMtx) {
        if (transMtx == null) {
            throw new IllegalStateException(
                "Can't create port object: The transformation matrix has not been calculated");
        }
        return new TransformationPortObject(TransformationType.LDA, transMtx, m_usedColumnNames);
    }

    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new PortObjectSpec[]{createScatterTableSpec(INTRA_CLASS_SCATTER_MATRIX, m_usedColumnNames),
            createScatterTableSpec(INTER_CLASS_SCATTER_MATRIX, m_usedColumnNames),
            TransformationUtils.createDecompositionTableSpec(m_usedColumnNames),
            new TransformationPortObjectSpec(TransformationType.LDA, m_usedColumnNames, m_usedColumnNames.length)};
    }

}
