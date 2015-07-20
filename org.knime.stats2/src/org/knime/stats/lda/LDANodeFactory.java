package org.knime.stats.lda;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PMMLToJavascriptCompiler" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class LDANodeFactory
        extends NodeFactory<LDANodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LDANodeModel createNodeModel() {
        return new LDANodeModel();
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
    public NodeView<LDANodeModel> createNodeView(final int viewIndex,
            final LDANodeModel nodeModel) {
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
        return new LDANodeDialog();
    }

}

