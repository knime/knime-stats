package org.knime.base.node.stats.kaplanmeier;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PMMLToJavascriptCompiler" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class KaplanMeierNodeFactory
        extends NodeFactory<KaplanMeierNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public KaplanMeierNodeModel createNodeModel() {
        return new KaplanMeierNodeModel();
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
    public NodeView<KaplanMeierNodeModel> createNodeView(final int viewIndex,
            final KaplanMeierNodeModel nodeModel) {
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
        return new KaplanMeierNodeDialog();
    }

}

