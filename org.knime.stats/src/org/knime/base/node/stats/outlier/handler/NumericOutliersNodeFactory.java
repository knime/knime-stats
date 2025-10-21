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
 *   Jan 31, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.stats.outlier.handler;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Factory class of the outlier detector node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class NumericOutliersNodeFactory extends NodeFactory<NumericOutliersNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public NumericOutliersNodeModel createNodeModel() {
        return new NumericOutliersNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<NumericOutliersNodeModel> createNodeView(final int viewIndex,
        final NumericOutliersNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Numeric Outliers";

    private static final String NODE_ICON = "../outlier.png";

    private static final String SHORT_DESCRIPTION = """
            Detects and handles outliers for all numerical columns.
            """;

    private static final String FULL_DESCRIPTION = """
            <p> This node detects and treats the outliers for each of the selected columns individually by means of
                <a href="https://en.wikipedia.org/wiki/Outlier#Tukey's_fences">interquartile range (IQR)</a>. </p> <p>
                To detect the outliers for a given column, the first and third quartile (Q<sub>1</sub>, Q<sub>3</sub>)
                is computed. An observation is flagged an outlier if it lies outside the range R = [Q<sub>1</sub> -
                k(IQR), Q<sub>3</sub> + k(IQR)] with IQR = Q<sub>3</sub> - Q<sub>1</sub> and k &gt;= 0. Setting k = 1.5
                the smallest value in R corresponds, typically, to the lower end of a boxplot's whisker and largest
                value to its upper end. <br /> Providing grouping information allows to detect outliers only within
                their respective groups. </p> <p> If an observation is flagged an outlier, one can either replace it by
                some other value or remove/retain the corresponding row. </p> <p> Missing values contained in the data
                will be ignored, i.e., they will neither be used for the outlier computation nor will they be flagged as
                an outlier. </p>
            """;

    private static final List<PortDescription> INPUT_PORTS =
        List.of(fixedPort("Input data", "Numeric input data to evaluate + optional group information."));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
        fixedPort("Treated table",
            "Data table where outliers were either replaced or rows containing outliers/non-outliers were removed."),
        fixedPort("Summary", """
                Data table holding the number of members, i.e., non-missing values and outliers as well as the lower and
                upper bound for each outlier groups.
                """), //
        fixedPort("Numeric outliers model", """
                Model holding the permitted interval bounds for each outlier group and the outlier treatment
                specifications.
                """));

    /**
     * @since 5.9
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.9
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, NumericOutliersNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), NumericOutliersNodeParameters.class, null,
            NodeType.Manipulator, List.of(), null);
    }

    /**
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, NumericOutliersNodeParameters.class));
    }

}