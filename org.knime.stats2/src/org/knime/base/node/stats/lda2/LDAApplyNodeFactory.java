package org.knime.base.node.stats.lda2;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Linear Discriminant Analysis" Node.
 *
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public final class LDAApplyNodeFactory extends NodeFactory<LDAApplyNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LDAApplyNodeModel createNodeModel() {
        return new LDAApplyNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<LDAApplyNodeModel> createNodeView(final int viewIndex, final LDAApplyNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new LDAApplyNodeDialog();
    }

}
