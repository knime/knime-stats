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
 * -------------------------------------------------------------------
 *
 * History
 *   04.10.2006 (uwe): created
 */

package org.knime.base.node.stats.lda2.port;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * Port model object transporting the Linear Discriminant Analysis (LDA) transformation.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public final class LDAModelPortObject extends AbstractSimplePortObject {
    /**
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.7
     */
    public static final class Serializer extends AbstractSimplePortObjectSerializer<LDAModelPortObject> {
    }

    private static final String W_ROW_KEYPREFIX = "transformation_matrix_row_";

    private static final String W_KEY = "transformation_matrix";

    private static final String COLUMN_NAMES_KEY = "column_names";

    /**
     * Define port type of objects of this class when used as PortObjects.
     */
    @SuppressWarnings("hiding")
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(LDAModelPortObject.class);

    private String[] m_inputColumnNames;

    private RealMatrix m_w;

    /**
     * empty constructor.
     */
    public LDAModelPortObject() {
        // nothing to do
    }

    /**
     * construct port model object with values.
     *
     * @param inputColumnNames names of input columns
     * @param w the transformation matrix
     */
    public LDAModelPortObject(final String[] inputColumnNames, final RealMatrix w) {
        m_inputColumnNames = inputColumnNames;
        m_w = w;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        m_inputColumnNames = model.getStringArray(COLUMN_NAMES_KEY);

        final ModelContentRO mc = model.getModelContent(W_KEY);
        final int numRows = mc.getChildCount();
        final double[][] w = new double[numRows][];
        for (int i = 0; i < numRows; i++) {
            w[i] = mc.getDoubleArray(W_ROW_KEYPREFIX + i);
        }
        m_w = MatrixUtils.createRealMatrix(w);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec) throws CanceledExecutionException {
        model.addStringArray(COLUMN_NAMES_KEY, m_inputColumnNames);
        final ModelContentWO mc = model.addModelContent(W_KEY);
        final RealMatrix w = m_w;
        for (int i = 0; i < w.getRowDimension(); i++) {
            mc.addDoubleArray(W_ROW_KEYPREFIX + i, w.getRow(i));
        }
    }

    /**
     * Returns the transformation matrix.
     *
     * @return the transformation matrix
     */
    public RealMatrix getTransformationMatrix() {
        return m_w;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LDAModelPortObjectSpec getSpec() {
        return new LDAModelPortObjectSpec(m_inputColumnNames, m_w.getRowDimension());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        final StringBuilder stb = new StringBuilder("<html>LDA transformation matrix with ");
        stb.append(getSummary());
        stb.append(":<br><table>");
        // give the eigenvectors as well
        // start at -1 to include a header row
        for (int i = -1; i < m_w.getRowDimension(); i++) {
            stb.append("<tr>");
            for (int j = 0; j < m_w.getColumnDimension(); j++) {
                if (i == -1) {
                    stb.append("<th>");
                    stb.append(j + 1);
                    stb.append(". eigenvector");
                } else {
                    stb.append("<td>");
                    stb.append(m_w.getEntry(i, j));
                }
            }
        }
        stb.append("</table></html>");

        final JLabel label = new JLabel(stb.toString());
        label.setName("LDA port");
        return new JComponent[]{label};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        final int numEVs = m_w.getColumnDimension();
        return numEVs + " eigenvector" + (numEVs == 1 ? "" : "s");
    }
}
