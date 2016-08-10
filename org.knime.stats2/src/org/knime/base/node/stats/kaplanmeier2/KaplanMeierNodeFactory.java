package org.knime.base.node.stats.kaplanmeier2;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.wizard.WizardNodeFactoryExtension;

/**
 * <code>NodeFactory</code> for the "PMMLToJavascriptCompiler" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class KaplanMeierNodeFactory extends NodeFactory<KaplanMeierNodeModel> implements
        WizardNodeFactoryExtension<KaplanMeierNodeModel, KaplanMeierViewRepresentation, KaplanMeierViewValue> {

    /**
     * {@inheritDoc}
     */
    @Override
    public KaplanMeierNodeModel createNodeModel() {
        return new KaplanMeierNodeModel(getInteractiveViewName());
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

