package org.knime.base.node.stats.transformation.lda2.perform;

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
 * <code>NodeFactory</code> for the "Linear Discriminant Analysis" node.
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class LDA2NodeFactory
        extends NodeFactory<LDA2NodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public LDA2NodeModel createNodeModel() {
        return new LDA2NodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<LDA2NodeModel> createNodeView(final int viewIndex,
            final LDA2NodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Linear Discriminant Analysis";
    private static final String NODE_ICON = "./lda.png";

    private static final String SHORT_DESCRIPTION = """
            This node performs a linear discriminant analysis.
            """;

    private static final String FULL_DESCRIPTION = """
            This node performs <a href="http://en.wikipedia.org/wiki/Linear_discriminant_analysis">Linear
                Discriminant Analysis (LDA)</a> which is a dimensionality reduction technique. It takes class
                information into account in order to project the data into a space in which classes are well separated.
                The results are similar to <a
                href="https://en.wikipedia.org/wiki/Principal_component_analysis">Principle Component Analysis (PCA)</a>
                and may be used in subsequent classification. <p>This node is equivalent to using a <i>Linear
                Discriminant Ananlysis Compute</i> node in combination with a <i>Linear Discriminant Analysis Apply</i>
                node. This pattern may be useful when applying a transformation to multiple datasets. </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table to transform", """
                Input table containing numeric columns and one column with class information.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Transformed data", """
                The original data (if not excluded) plus columns for the projected dimensions.
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
        return new DefaultNodeDialog(SettingsType.MODEL, LDA2NodeParameters.class);
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
            LDA2NodeParameters.class,
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, LDA2NodeParameters.class));
    }

}

