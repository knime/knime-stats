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
 *   Jan 7, 2019 (lukass): created
 */
package org.knime.base.node.stats.transformation.lda2.util;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * Utils for the LDA Nodes.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public final class LDAUtils {

    /** Constructor. */
    private LDAUtils() {
        // nothing to do
    }

    /**
     * Creates an explanatory warning why the currently selected dimension is zero. Gives reason whether the selected
     * classes are too low or the selected columns.
     *
     * @param selectedClasses Number of classes in the currently selected class column
     * @param selectedColumns Number of currently selected columns.
     * @param classColName The name of the currently selected class column.
     * @return the explanatory warning message.
     */
    public static final String createMaxDimZeroWarning(final int selectedClasses, final int selectedColumns,
        final String classColName) {
        final StringBuilder stb = new StringBuilder("The maximum allowed dimension is 0.");

        if (selectedClasses == 0) {
            stb.append(" There are only missing values in the selected class column \"" + classColName
                + "\". Please provide a class column with at least two distinct values.");
        } else if (selectedClasses == 1) {
            stb.append(" There is only one distinct value in the selected class column \"" + classColName
                + "\". Please provide a class column with at least two distinct values.");
        }

        if (selectedColumns == 0) {
            if ((selectedClasses - 1) <= 0) {
                stb.append(" Also, t");
            } else {
                stb.append(" T");
            }
            stb.append("here are no columns selected. Please select at least one.");
        }

        return stb.toString();
    }

    /**
     * Creates an explanatory warning why the currently selected dimension is too high compared to the maximum allowed
     * dimension. Gives reason whether the selected classes are too low or the selected columns.
     *
     * @param currentDim Currently selected number of dimensions to project to.
     * @param maximumDim Maximum allowed dimensions.
     * @param selectedClasses Number of classes in the currently selected class column
     * @param selectedColumns Number of currently selected columns.
     * @param classColName The name of the currently selected class column.
     * @return the explanatory warning message.
     */
    public static final String createTooHighDimWarning(final int currentDim, final int maximumDim,
        final int selectedClasses, final int selectedColumns, final String classColName) {

        final StringBuilder stb = new StringBuilder(createTooHighDimBaseWarning(currentDim, maximumDim));

        if (maximumDim == (selectedClasses - 1)) {
            stb.append(" The selected class column \"" + classColName + "\" only features " + selectedClasses
                + " distinct value" + (selectedClasses == 1 ? "" : "s"));
            if (maximumDim != selectedColumns) {
                stb.append(".");
            }
        }

        if (maximumDim == selectedColumns) {
            if (maximumDim == (selectedClasses - 1)) {
                stb.append(" and o");
            } else {
                stb.append(" O");
            }
            stb.append("nly " + selectedColumns + " column" + (selectedColumns == 1 ? " is" : "s are") + " selected.");
        }
        return stb.toString();
    }

    /**
     * Warning if the selected dimensions are too high, without explanation.
     *
     * @param currentDim the current dimension
     * @param maximumDim the maximum dimension
     * @return the too high dim warning
     */
    public static final String createTooHighDimBaseWarning(final int currentDim, final int maximumDim) {
        return "The current number of selected dimensions (" + currentDim
            + ") is higher than the maximum allowed value of " + maximumDim + ".";
    }

    /**
     * Calculates the maximum possible dimension that can be reduced to from the current settings and checks that it is
     * not less than 1.
     *
     * @param inSpec the input table spec
     * @param classColName the class column name
     * @param numSelectedColumns the number of selected columns
     * @return the maximum number of dimensions to reduce to
     * @throws InvalidSettingsException - If the number of dimensions to reduce to is zero
     */
    public static final int calcPositiveMaxDim(final DataTableSpec inSpec, final String classColName,
        final int numSelectedColumns) throws InvalidSettingsException {
        // get the selected Classes, columns and calculate maxDim - much like updateSettings() in the dialog
        final DataColumnSpec classSpec = inSpec.getColumnSpec(classColName);
        if (classSpec == null) {
            throw new InvalidSettingsException("The selected class column \"" + classColName + "\" does not exist.");
        }

        // initialize to flag value indicating an uncalculated domain
        final int selectedClasses =
            classSpec.getDomain().hasValues() ? classSpec.getDomain().getValues().size() : Integer.MAX_VALUE;

        final int maxDim = Math.min((selectedClasses - 1), numSelectedColumns);

        if (maxDim < 1) {
            throw new InvalidSettingsException(
                createMaxDimZeroWarning(selectedClasses, numSelectedColumns, classColName));
        }

        return maxDim;
    }

}
