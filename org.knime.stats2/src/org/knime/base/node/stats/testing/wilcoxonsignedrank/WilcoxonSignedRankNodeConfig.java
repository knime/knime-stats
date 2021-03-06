package org.knime.base.node.stats.testing.wilcoxonsignedrank;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Patrick Winter, University of Konstanz
 */
class WilcoxonSignedRankNodeConfig {

    private static final String FIRST_COLUMNS_CFG = "firstColumns";

    private static final String SECOND_COLUMNS_CFG = "secondColumns";

    private static final String[] FIRST_COLUMNS_DEFAULT = new String[0];

    private static final String[] SECOND_COLUMNS_DEFAULT = new String[0];

    private String[] m_firstColumns = FIRST_COLUMNS_DEFAULT;

    private String[] m_secondColumns = SECOND_COLUMNS_DEFAULT;

    public String[] getFirstColumns() {
        return m_firstColumns;
    }

    public void setFirstColumns(final String[] firstColumns) {
        m_firstColumns = firstColumns;
    }

    public String[] getSecondColumns() {
        return m_secondColumns;
    }

    public void setSecondColumns(final String[] secondColumns) {
        m_secondColumns = secondColumns;
    }

    public void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_firstColumns = settings.getStringArray(FIRST_COLUMNS_CFG);
        m_secondColumns = settings.getStringArray(SECOND_COLUMNS_CFG);
    }

    public void loadInDialog(final NodeSettingsRO settings) {
        m_firstColumns = settings.getStringArray(FIRST_COLUMNS_CFG, FIRST_COLUMNS_DEFAULT);
        m_secondColumns = settings.getStringArray(SECOND_COLUMNS_CFG, SECOND_COLUMNS_DEFAULT);
    }

    public void save(final NodeSettingsWO settings) {
        settings.addStringArray(FIRST_COLUMNS_CFG, m_firstColumns);
        settings.addStringArray(SECOND_COLUMNS_CFG, m_secondColumns);
    }

}
