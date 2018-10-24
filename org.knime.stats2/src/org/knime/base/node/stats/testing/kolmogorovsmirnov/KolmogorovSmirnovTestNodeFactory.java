package org.knime.base.node.stats.testing.kolmogorovsmirnov;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "KolmogorovSmirnovTest" Node.
 *
 *
 * @author Kevin Kress, Knime GmbH, Konstanz
 */
public final class KolmogorovSmirnovTestNodeFactory extends NodeFactory<KolmogorovSmirnovTestNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public KolmogorovSmirnovTestNodeModel createNodeModel() {
        return new KolmogorovSmirnovTestNodeModel();
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
    public NodeView<KolmogorovSmirnovTestNodeModel> createNodeView(final int viewIndex,
        final KolmogorovSmirnovTestNodeModel nodeModel) {
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
        return new KolmogorovSmirnovTestNodeDialog();
    }

}
