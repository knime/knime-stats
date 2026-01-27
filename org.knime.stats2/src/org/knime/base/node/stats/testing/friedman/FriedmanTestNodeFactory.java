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
 */
package org.knime.base.node.stats.testing.friedman;

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
 * <code>NodeFactory</code> for the "FriedmanTest" Node.
 *
 *
 * @author Lukas Siedentop, University of Konstanz
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class FriedmanTestNodeFactory
extends NodeFactory<FriedmanTestNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public FriedmanTestNodeModel createNodeModel() {
        return new FriedmanTestNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FriedmanTestNodeModel> createNodeView(final int viewIndex,
        final FriedmanTestNodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "Friedman Test";
    private static final String NODE_ICON = "./friedman_test.png";
    private static final String SHORT_DESCRIPTION = """
            States whether three or more samples show significant statistical difference in their location
                parameters
            """;
    private static final String FULL_DESCRIPTION = """
            <p>The Friedman test is used to detect any difference between subjects under test measured variously
                multiple times. More precisely, this non-parametric test states whether there is a significant
                difference in the location parameters of <i>k</i> statistical samples (&gt;= 3, <i>columns</i>
                <i>candidates</i>, <i>treatments</i>, <i>subject</i>), measured <i>n</i> times (<i>rows</i>,
                <i>blocks</i>, <i>participants</i>, <i>measures</i>), or not. The data in each row is ranked, based on
                which a resulting test statistic <i>Q</i> is calculated.</p> <p>If <i>n</i> &gt; 15 or <i>k</i> &gt; 4,
                the test statistic <i>Q</i> can be approximated to be Χ<sup>2</sup> distributed. With the given
                significance level α, a corresponding <i>p</i>-value (null hypothesis H<sub>0</sub>: there is no
                difference of the location parameters in the samples, alternative hypothesis H<sub>A</sub>: the samples
                in the columns have different location parameters) can be given.</p> <p>Please refer also to the <a
                href="https://en.wikipedia.org/wiki/Friedman_test">Wikipedia description of the Friedman Test</a>.</p>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input data", """
                The table from which to test samples.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Evaluation", """
                Friedman test evaluation.
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, FriedmanTestNodeParameters.class);
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
            FriedmanTestNodeParameters.class,
            null,
            NodeType.Manipulator,
            List.of(),
            null
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, FriedmanTestNodeParameters.class));
    }


}

