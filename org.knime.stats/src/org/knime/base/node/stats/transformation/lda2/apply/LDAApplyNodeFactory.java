package org.knime.base.node.stats.transformation.lda2.apply;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Linear Discriminant Analysis" Node.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public final class LDAApplyNodeFactory extends NodeFactory<LDAApplyNodeModel> {

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

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new LDAApplyNodeDialog();
    }

}
