package org.knime.base.node.stats.contingencytable;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "OddsRatio" Node.
 *
 *
 * @author Oliver Sampson, University of Konstanz
 */
public class ContingencyTableNodeFactory extends NodeFactory<ContingencyTableNodeModel> {

    @Override
    public ContingencyTableNodeModel createNodeModel() {
        return new ContingencyTableNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ContingencyTableNodeModel> createNodeView(final int viewIndex,
        final ContingencyTableNodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ContingencyTableNodeDialog();
    }

}
