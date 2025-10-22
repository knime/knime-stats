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
 * History
 *   Feb 15, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier.options;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.widget.choices.Label;

/**
 * Enum encoding the outlier treatment.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public enum NumericOutliersTreatmentOption {

        /** Indicates that the outliers have to be replaced. */
        @Label(value = "Replace outlier values",
                description = "Allows to replace outliers based on the selected \"Replacement strategy\".")
        REPLACE("Replace outlier values"),

        /** Indicates that rows containing outliers have to be removed. */
        @Label(value = "Remove outlier rows", description = "Removes all rows from the input data that contain in any "
            + "of the selected columns at least one outlier.")
        FILTER("Remove outlier rows"),

        /** Indicates that only rows containing outliers have to be retained. */
        @Label(value = "Retain non-outlier rows", description = "Retains only those rows of the input data that "
            + "contain at least one outlier in any of the selected columns.")
        RETAIN("Remove non-outlier rows");

    /** Missing name exception. */
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";

    /** IllegalArgumentException prefix. */
    private static final String ARGUMENT_EXCEPTION_PREFIX = "No NumericOutliersTreatmentOption constant with name: ";

    private final String m_name;

    NumericOutliersTreatmentOption(final String name) {
        m_name = name;
    }

    @Override
    public String toString() {
        return m_name;
    }

    /**
     * Returns the enum for a given String
     *
     * @param name enum name
     * @return the enum
     * @throws InvalidSettingsException if the given name is not associated with an
     *             {@link NumericOutliersTreatmentOption} value
     */
    public static NumericOutliersTreatmentOption getEnum(final String name) throws InvalidSettingsException {
        if (name == null) {
            throw new InvalidSettingsException(NAME_MUST_NOT_BE_NULL);
        }
        return Arrays.stream(values()).filter(t -> t.m_name.equals(name)).findFirst()
            .orElseThrow(() -> new InvalidSettingsException(ARGUMENT_EXCEPTION_PREFIX + name));
    }

}
