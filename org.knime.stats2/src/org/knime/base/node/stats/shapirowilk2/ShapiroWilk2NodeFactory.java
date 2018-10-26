package org.knime.base.node.stats.shapirowilk2;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PMMLToJavascriptCompiler" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class ShapiroWilk2NodeFactory
        extends NodeFactory<ShapiroWilk2NodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ShapiroWilk2NodeModel createNodeModel() {
        return new ShapiroWilk2NodeModel();
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
    public NodeView<ShapiroWilk2NodeModel> createNodeView(final int viewIndex,
            final ShapiroWilk2NodeModel nodeModel) {
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
        return new ShapiroWilk2NodeDialog();
    }

}

