package org.knime.base.node.stats.shapirowilk;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PMMLToJavascriptCompiler" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class ShapiroWilkNodeFactory
        extends NodeFactory<ShapiroWilkNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ShapiroWilkNodeModel createNodeModel() {
        return new ShapiroWilkNodeModel();
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
    public NodeView<ShapiroWilkNodeModel> createNodeView(final int viewIndex,
            final ShapiroWilkNodeModel nodeModel) {
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
        return new ShapiroWilkNodeDialog();
    }

}

