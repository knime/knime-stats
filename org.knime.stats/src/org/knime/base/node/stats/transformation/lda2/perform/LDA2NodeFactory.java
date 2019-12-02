package org.knime.base.node.stats.transformation.lda2.perform;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Linear Discriminant Analysis" node.
 *
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public final class LDA2NodeFactory
        extends NodeFactory<LDA2NodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LDA2NodeModel createNodeModel() {
        return new LDA2NodeModel();
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
    public NodeView<LDA2NodeModel> createNodeView(final int viewIndex,
            final LDA2NodeModel nodeModel) {
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
        return new LDA2NodeDialog();
    }

}

