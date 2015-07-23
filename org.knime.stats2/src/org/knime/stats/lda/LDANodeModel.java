package org.knime.stats.lda;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.base.node.mine.pca.PCAModelPortObject;
import org.knime.base.node.mine.pca.PCAModelPortObjectSpec;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * This is the model implementation of PMMLToJavascriptCompiler.
 *
 *
 * @author Alexander Fillbrunn
 */
public class LDANodeModel extends NodeModel {

    /**
     * The configuration key for the number of dimensions.
     */
    static final String K_CFG = "k";

    /**
     * The configuration key for the used columns.
     */
    static final String USED_COLS_CFG = "usedCols";

    /**
     * The configuration key for the class column.
     */
    static final String CLASS_COL_CFG = "classCols";

    /**
     * Constructor for the node model.
     */
    protected LDANodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE, PCAModelPortObject.TYPE});
    }

    /**
     * Creates a settings model for k.
     * @return the settings model
     */
    public static SettingsModelInteger createKSettingsModel() {
        return new SettingsModelInteger(K_CFG, 1);
    }

    /**
     * Creates a settings model for the used columns.
     * @return the settings model
     */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createUsedColsSettingsModel() {
        return new SettingsModelColumnFilter2(USED_COLS_CFG,
                                              org.knime.core.data.DoubleValue.class);
    }

    /**
     * Creates a settings model for the class column.
     * @return the settings model
     */
    public static SettingsModelColumnName createClassColSettingsModel() {
        return new SettingsModelColumnName(CLASS_COL_CFG, null);
    }

    private SettingsModelInteger m_k = createKSettingsModel();

    private SettingsModelColumnFilter2 m_usedCols = createUsedColsSettingsModel();

    private SettingsModelColumnName m_classColumn = createClassColSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getDataTableSpec();
        FilterResult res = m_usedCols.applyTo(inSpec);

        if (m_classColumn.getColumnName() == null) {
            for (String col : inSpec.getColumnNames()) {
                if (inSpec.getColumnSpec(col).getType().isCompatible(NominalValue.class)) {
                    m_classColumn.setStringValue(col);
                    setWarningMessage("No class column set. Using " + col + ".");
                    break;
                }
            }
        }

        if (m_k.getIntValue() > res.getIncludes().length) {
            throw new InvalidSettingsException("The number of dimensions to project to "
                    + "cannot be larger than the number of input columns.");
        }

        int[] indices = new int[res.getIncludes().length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = inSpec.findColumnIndex(res.getIncludes()[i]);
        }
        int classCol = inSpec.findColumnIndex(m_classColumn.getColumnName());

        DataContainer dc = exec.createDataContainer(createSpec(inSpec));

        PCAModelPortObject pca;
        if (inTable.getRowCount() != 0) {
            LDA lda = new LDA(inTable, indices, classCol, m_k.getIntValue());
            lda.calculateTransformationMatrix(exec.createSilentSubExecutionContext(0.7));
            double[] center = new double[res.getIncludes().length];
            Arrays.fill(center, 0);
            pca = new PCAModelPortObject(lda.getEigenvectors(), lda.getEigenvalues(), res.getIncludes(), center);
            for (DataRow row : inTable) {
                DoubleCell[] p = lda.getProjection(row);
                DataCell[] output = new DataCell[row.getNumCells() + p.length];
                for (int i = 0; i < row.getNumCells(); i++) {
                    output[i] = row.getCell(i);
                }
                for (int i = 0; i < p.length; i++) {
                    output[i + row.getNumCells()] = p[i];
                }
                dc.addRowToTable(new DefaultRow(row.getKey(), output));
            }
        } else {
            throw new InvalidSettingsException("Cannot produce an LDA model for an empty table.");
        }
        dc.close();


        return new PortObject[]{(BufferedDataTable)dc.getTable(), pca};
    }

    private DataTableSpec createSpec(final DataTableSpec inSpec) {
        DataTableSpecCreator appendedSpecCreator = new DataTableSpecCreator(inSpec);
        for (int i = 0; i < m_k.getIntValue(); i++) {
            appendedSpecCreator.addColumns(new DataColumnSpecCreator("axis" + Integer.toString(i),
                                                                     DoubleCell.TYPE).createSpec());
        }
        return appendedSpecCreator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];

        DataColumnSpec classSpec = inSpec.getColumnSpec(m_classColumn.getColumnName());
        if (classSpec != null && classSpec.getDomain().getValues() != null) {
            int num = classSpec.getDomain().getValues().size() - 1;
            if (m_k.getIntValue() > num) {
                throw new InvalidSettingsException("The number of dimensions to project to "
                        + "cannot be larger than the number of input columns.");
            }
        }

        FilterResult res = m_usedCols.applyTo(inSpec);
        return new PortObjectSpec[]{createSpec(inSpec), new PCAModelPortObjectSpec(res.getIncludes())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_k.saveSettingsTo(settings);
        m_classColumn.saveSettingsTo(settings);
        m_usedCols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_k.loadSettingsFrom(settings);
        m_classColumn.loadSettingsFrom(settings);
        m_usedCols.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_k.validateSettings(settings);
        m_classColumn.validateSettings(settings);
        m_usedCols.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

