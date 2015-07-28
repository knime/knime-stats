package org.knime.base.node.stats.testing.wilcoxonmannwhitney;

import org.apache.commons.math3.stat.ranking.NaNStrategy;

/**
 * Used internally to determine the missing value strategy. Reflects possible behaviours of {@link NaNStrategy}.
 *
 * @author Christian Dietz, University of Konstanz
 */
enum MissingValueHandler {
    FAILED("Failed", NaNStrategy.FAILED), FIXED("Fixed", NaNStrategy.FIXED), MAXIMAL("Maximal", NaNStrategy.MAXIMAL),
        MINIMAL("Minimal", NaNStrategy.MINIMAL);

    private String name;

    private NaNStrategy strategy;

    private MissingValueHandler(final String handlerName, final NaNStrategy strat) {
        this.name = handlerName;
        this.strategy = strat;
    }

    /**
     * @return the strategy
     */
    public NaNStrategy getStrategy() {
        return strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }

    public static MissingValueHandler getHandlerByName(final String handlerName) {
        for (MissingValueHandler handler : MissingValueHandler.values()) {
            if (handler.toString().equals(handlerName)) {
                return handler;
            }
        }

        throw new IllegalArgumentException(
            "No handler found. Something went wrong. Please select a valid Missing Value Handler!");
    }

    /**
     * @return names as string array
     */
    public static String[] names() {
        final MissingValueHandler[] values = MissingValueHandler.values();
        final String[] names = new String[values.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = values[i].toString();
        }
        return names;
    }
}