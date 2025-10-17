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
package org.knime.base.node.stats.viz.extended;

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
import org.knime.node.impl.description.ViewDescription;

/**
 * <code>NodeFactory</code> for the "ExtendedStatistics" Node. Calculates statistic moments with their distributions and
 * counts nominal values and their occurrences across all columns.
 *
 * @author Gabor Bakos
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class ExtendedStatisticsNodeFactory extends NodeFactory<ExtendedStatisticsNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public ExtendedStatisticsNodeModel createNodeModel() {
        return new ExtendedStatisticsNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<ExtendedStatisticsNodeModel> createNodeView(final int viewIndex,
        final ExtendedStatisticsNodeModel nodeModel) {
        return new ExtendedStatisticsHTMLNodeView(nodeModel);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Statistics";
    private static final String NODE_ICON = "./extended_statistics.png";
    private static final String SHORT_DESCRIPTION = """
            Calculates statistic moments with their distributions as histograms and counts nominal values and their
                occurrences across all columns.
            """;
    private static final String FULL_DESCRIPTION = """
            This node calculates statistical moments such as minimum, maximum, mean, standard deviation, variance,
                median, overall sum, number of missing values and row count across all numeric columns, and counts all
                nominal values together with their occurrences. The dialog offers two options for choosing the median
                and/or nominal values calculations:
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table", """
                Table from which to compute statistics.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Statistics Table", """
                Table with numeric values.
                """),
            fixedPort("Nominal Histogram Table", """
                Table with all nominal value histograms.
                """),
            fixedPort("Occurrences Table", """
                Table with all nominal values and their counts.
                """)
    );
    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Statistics View", """
                Displays all statistic moments (for all numeric columns), nominal values (for all selected categorical
                columns) and the most frequent/infrequent values from the categorical columns (Top/bottom).
                """)
    );

    private static final List<String> KEYWORDS = List.of( //
            "frequency table", //
            "field summary" //
    );

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
        return new DefaultNodeDialog(SettingsType.MODEL, ExtendedStatisticsNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            ExtendedStatisticsNodeParameters.class,
            VIEWS,
            NodeType.Visualizer,
            KEYWORDS,
            null
        );
    }

    /**
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ExtendedStatisticsNodeParameters.class));
    }
}
