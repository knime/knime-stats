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
 */
package org.knime.base.node.stats.correlation.rank2;

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
 * Node factory for the Rank Correlation Compute node.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class RankCorrelationCompute2NodeFactory extends NodeFactory<RankCorrelationCompute2NodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public RankCorrelationCompute2NodeModel createNodeModel() {
        return new RankCorrelationCompute2NodeModel();
    }

    @Override
    public NodeView<RankCorrelationCompute2NodeModel> createNodeView(final int viewIndex,
        final RankCorrelationCompute2NodeModel nodeModel) {
        return new RankCorrelationCompute2NodeView(nodeModel);
    }

    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "Rank Correlation";
    private static final String NODE_ICON = "rankcorrelation.png";
    private static final String SHORT_DESCRIPTION = """
            Computes correlation coefficients for pairs of columns, based on the sorting of its values only.
            """;
    private static final String FULL_DESCRIPTION = """
            <p> Calculates for each pair of selected columns a correlation coefficient, i.e. a measure of the
                correlation of the two variables. </p> <p> All measures are based on the rank of the cells. Where the
                rank of a cell value refers to its position in a sorted list of all entries. All correlation can be
                calculated on any kind of DataColumn. However please note that we use the default ordering of the
                values. If there is no ordering defined in the column, a string representation will be used. The node
                uses fractional ranks for equal values. <a
                href="http://en.wikipedia.org/wiki/Spearman%27s_rank_correlation_coefficient"> Spearman's rank
                correlation coefficient </a> is a statistical measure of the strength of a monotonic relationship
                between paired data. Where the monotonic relationship is characterised by a relationship between ordered
                sets that preserves the given order, i.e., either never increases or never decreases as its independent
                variable increases. The value of this measure ranges from -1 (strong negative correlation) to 1 (strong
                positive correlation). A perfect Spearman correlation of +1 or −1 occurs when each of the variables is a
                perfect monotone function of the other. For Spearman's rank correlation coefficient the p-value and
                degrees of freedom are computed. The p-value indicates the probability of an uncorrelated system
                producing a correlation at least as extreme, if the mean of the correlation is zero and it follows a
                t-distribution with <i>df</i> degrees of freedom. <br /> <a
                href="http://en.wikipedia.org/wiki/Goodman_and_Kruskal%27s_gamma"> Goodman and Kruskal's gamma</a> as
                well as <a href="http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient">Kendall's tau
                rank correlation coefficient</a> is used to measure the strength of association between two measured
                quantities. Both are based on the number of concordant and discordant pairs. Kendall's Tau A and Tau B
                coefficients can be considered as standardized forms of Gamma. The difference between Tau A and Tau B is
                that Tau A statistic does not consider tied values, while Tau B makes adjustments for them. By tied
                observations we consider two or more observations having the same value. Both Kruskal's gamma and
                Kendall's Tau A are mostly suitable for square tables, whereas Tau B is most appropriately used for
                rectangular tables. The coefficients must be in the range from −1 (100% negative association, or perfect
                inversion) to +1 (100% positive association, or perfect agreement). A value of zero indicates the
                absence of association. <br /> </p> <p> Rows containing Missing Values will be ignored, not used for the
                calculations. For other behaviors please resolve them before. </p>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Numeric input data", """
                Numeric input data to evaluate
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Correlation measure", """
                Correlation variables, p-values and degrees of freedom.
                """),
            fixedPort("Correlation matrix", """
                Correlation variables in a matrix representation.
                """),
            fixedPort("Correlation model", """
                A model containing the correlation measures. This model is appropriate to be read by the Correlation
                Filter node.
                """),
            fixedPort("Rank table", """
                A table containing the fractional ranks of the columns. Where the rank corresponds to the values
                position in a sorted table.
                """)
    );
    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Correlation Matrix", """
                Squared table view showing the pair-wise correlation values of all columns. The color range varies from
                dark red (strong negative correlation), over white (no correlation) to dark blue (strong positive
                correlation). If a correlation value for a pair of column is not available, the corresponding cell
                contains a missing value (shown as cross in the color view).
                """)
    );

    private static final List<String> KEYWORDS = List.of( //
        "association analysis", //
        "goodmans gamma", //
        "kruskals gamma", //
        "spearman" //
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
        return new DefaultNodeDialog(SettingsType.MODEL, RankCorrelationCompute2NodeParameters.class);
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
            RankCorrelationCompute2NodeParameters.class,
            VIEWS,
            NodeType.Other,
            KEYWORDS,
            null
        );
    }

    /**
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, RankCorrelationCompute2NodeParameters.class));
    }
}
