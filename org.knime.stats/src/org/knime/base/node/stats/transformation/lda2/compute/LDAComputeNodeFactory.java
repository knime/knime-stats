package org.knime.base.node.stats.transformation.lda2.compute;

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
 * <code>NodeFactory</code> for the "Linear Discriminant Analysis Compute" node.
 *
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class LDAComputeNodeFactory extends NodeFactory<LDAComputeNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public LDAComputeNodeModel createNodeModel() {
        return new LDAComputeNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<LDAComputeNodeModel> createNodeView(final int viewIndex, final LDAComputeNodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "Linear Discriminant Analysis Compute";

    private static final String NODE_ICON = "./lda_compute-icon.png";

    private static final String SHORT_DESCRIPTION = """
            This node computes a transformation model using linear discriminant analysis.
            """;

    private static final String FULL_DESCRIPTION = """
            This node performs <a href="http://en.wikipedia.org/wiki/Linear_discriminant_analysis">Linear
                Discriminant Analysis (LDA)</a> which is a dimensionality reduction technique. It takes class
                information into account in order to project the data into a space in which classes are well separated.
                The results are similar to <a
                href="https://en.wikipedia.org/wiki/Principal_component_analysis">Principle Component Analysis (PCA)</a>
                and may be used in subsequent classification.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input data", """
                Input table containing numeric columns and one column with class information.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Intra-class scatter matrix", """
                The intra-class scatter matrix.
                """),
            fixedPort("Inter-class scatter matrix", """
                The inter-class scatter matrix.
                """),
            fixedPort("Spectral decomposition", """
                Table containing the spectral decomposition. Rows are in descending order according to eigenvalues
                (first column).
                """),
            fixedPort("Transformation model", """
                Model holding the LDA transformation used by the <i>Linear Discriminant Analysis Apply</i> node to apply
                the transformation to, e.g. another validation set.
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
        return new DefaultNodeDialog(SettingsType.MODEL, LDAComputeNodeParameters.class);
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
            LDAComputeNodeParameters.class,
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, LDAComputeNodeParameters.class));
    }

}
