package org.knime.base.node.stats.shapirowilk2;

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
 * <code>NodeFactory</code> for the "ShapiroWilk2" Node.
 *
 * @author Alexander Fillbrunn
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class ShapiroWilk2NodeFactory
        extends NodeFactory<ShapiroWilk2NodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public ShapiroWilk2NodeModel createNodeModel() {
        return new ShapiroWilk2NodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ShapiroWilk2NodeModel> createNodeView(final int viewIndex,
            final ShapiroWilk2NodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Shapiro-Wilk Test";
    private static final String NODE_ICON = "./shapiro.png";
    private static final String SHORT_DESCRIPTION = """
            This node performs a Shapiro-Wilk test.
            """;
    private static final String FULL_DESCRIPTION = """
            <p>The Shapiro-Wilk test tests if a sample comes from a normally distributed population. The test is
                biased by sample size, so it may yield statistically significant results for any large sample. </p> <p>
                This node is applicable for 3 to 5000 samples, but a bias may begin to occur with more than 50
                samples.</p> <p>More information can be found at <a
                href="https://en.wikipedia.org/wiki/Shapiro–Wilk_test">Shapiro–Wilk test</a> on Wikipedia.</p> <p>
                <b>Hypotheses:</b><br/> H<sub>0</sub>: sample comes from a normally distributed population.<br />
                H<sub>A</sub>: sample does not originate from a normally distributed population. </p>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Samples", """
                Input table with one or more numerical columns.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Results", """
                Output table with the Shapiro-Wilk test statistic, p-Value, and acceptance/rejection of H<sub>0</sub>.
                """)
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
        return new DefaultNodeDialog(SettingsType.MODEL, ShapiroWilk2NodeParameters.class);
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
            ShapiroWilk2NodeParameters.class,
            null,
            NodeType.Manipulator,
            List.of(),
            null
        );
    }

    /**
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ShapiroWilk2NodeParameters.class));
    }

}

