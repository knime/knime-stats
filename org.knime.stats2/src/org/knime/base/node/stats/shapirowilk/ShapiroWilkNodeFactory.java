package org.knime.base.node.stats.shapirowilk;

import org.knime.base.node.stats.shapirowilk2.ShapiroWilk2NodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * @deprecated Use the {@link ShapiroWilk2NodeFactory} instead.
 * <code>NodeFactory</code> for the "ShapiroWilk" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
@Deprecated
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

