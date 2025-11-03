package org.knime.base.node.stats.transformation.lda2.apply;

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
 * <code>NodeFactory</code> for the "Linear Discriminant Analysis" Node.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class LDAApplyNodeFactory extends NodeFactory<LDAApplyNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public LDAApplyNodeModel createNodeModel() {
        return new LDAApplyNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<LDAApplyNodeModel> createNodeView(final int viewIndex, final LDAApplyNodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "Linear Discriminant Analysis Apply";

    private static final String NODE_ICON = "./lda_apply-icon.png";

    private static final String SHORT_DESCRIPTION = """
            This node applies a model resulting from a linear discriminant analysis.
            """;

    private static final String FULL_DESCRIPTION = """
            This node applies a <a href="http://en.wikipedia.org/wiki/Linear_discriminant_analysis">Linear
                Discriminant Analysis (LDA)</a> model to the given input data. This model is most likely the output of a
                <i>Linear Discriminant Analysis Compute</i> node and can be applied to arbitrary data to reduce its
                dimensionality. <p><b>The column names, however, must correspond to those that have been used to compute
                the model.</b></p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Transformation model", """
                The model used to reduce the data dimensionality.
                """),
            fixedPort("Table to transform", """
                Input table containing numeric columns, whose <b>column names match the model</b>.
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
        return new DefaultNodeDialog(SettingsType.MODEL, LDAApplyNodeParameters.class);
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
            LDAApplyNodeParameters.class,
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, LDAApplyNodeParameters.class));
    }


}
