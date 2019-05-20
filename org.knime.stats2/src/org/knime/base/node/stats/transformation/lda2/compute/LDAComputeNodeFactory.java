package org.knime.base.node.stats.transformation.lda2.compute;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Linear Discriminant Analysis Compute" node.
 *
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public final class LDAComputeNodeFactory extends NodeFactory<LDAComputeNodeModel> {

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

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new LDAComputeNodeDialog();
    }
}
