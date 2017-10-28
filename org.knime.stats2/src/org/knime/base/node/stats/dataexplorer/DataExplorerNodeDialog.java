/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   5 Jul 2017 (albrecht): created
 */
package org.knime.base.node.stats.dataexplorer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @author Anastasia Zhukova, KNIME GmbH, Konstanz, Germany
 */
public class DataExplorerNodeDialog extends NodeDialogPane {
    private static final int TEXT_FIELD_SIZE = 20;

    private final JCheckBox m_showMedianCheckBox;
    private final JCheckBox m_enablePagingCheckBox;
    private final JSpinner m_initialPageSizeSpinner;
    private final JCheckBox m_enablePageSizeChangeCheckBox;
    private final JTextField m_allowedPageSizesField;
    private final JCheckBox m_enableShowAllCheckBox;
    private final JCheckBox m_enableJumpToPageCheckBox;
    private final JCheckBox m_displayFullscreenButtonCheckBox;
    private final JTextField m_titleField;
    private final JTextField m_subtitleField;
    private final JCheckBox m_enableSelectionCheckbox;
    private final JCheckBox m_enableSearchCheckbox;
    private final JCheckBox m_enableSortingCheckBox;
    private final JCheckBox m_enableClearSortButtonCheckBox;
    private final JCheckBox m_enableGlobalNumberFormatCheckbox;
    private final JSpinner m_globalNumberFormatDecimalSpinner;
    private final JSpinner m_displayPreviewRowsSpinner;
    private final JSpinner m_maxNominalValuesSpinner;
    private final JCheckBox m_enableFreqValDisplayCheckbox;
    private final JSpinner m_freqValuesSpinner;

    /** Creates a new dialog instance */
    public DataExplorerNodeDialog() {
        m_showMedianCheckBox = new JCheckBox("Show median (computationally expensive)");
        m_enablePagingCheckBox = new JCheckBox("Enable pagination");
        m_enablePagingCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                enablePagingFields();
            }
        });
        m_initialPageSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        m_enablePageSizeChangeCheckBox = new JCheckBox("Enable page size change control");
        m_enablePageSizeChangeCheckBox.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                enablePagingFields();
            }
        });
        m_allowedPageSizesField = new JTextField(20);
        m_enableShowAllCheckBox = new JCheckBox("Add \"All\" option to page sizes");
        m_enableJumpToPageCheckBox = new JCheckBox("Display field to jump to a page directly");
        m_displayFullscreenButtonCheckBox = new JCheckBox("Display fullscreen button");
        m_titleField = new JTextField(TEXT_FIELD_SIZE);
        m_subtitleField = new JTextField(TEXT_FIELD_SIZE);
        m_enableSelectionCheckbox = new JCheckBox("Enable selection");
        m_enableSearchCheckbox = new JCheckBox("Enable searching");
        m_enableSearchCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                enableSearchFields();
            }
        });
        m_enableSortingCheckBox = new JCheckBox("Enable sorting on columns");
        m_enableSortingCheckBox.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                enableSortingFields();
            }
        });
        m_enableClearSortButtonCheckBox = new JCheckBox("Enable 'Clear Sorting' button");
        m_enableGlobalNumberFormatCheckbox = new JCheckBox("Enable global number format (double cells)");
        m_enableGlobalNumberFormatCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                enableFormatterFields();
            }
        });
        m_globalNumberFormatDecimalSpinner = new JSpinner(new SpinnerNumberModel(2, 0, null, 1));
        m_displayPreviewRowsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        m_maxNominalValuesSpinner = new JSpinner(new SpinnerNumberModel(100, 1,null,5));
        m_enableFreqValDisplayCheckbox = new JCheckBox("Show most frequent/infrequent values");
        m_enableFreqValDisplayCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                enableFreqValues();
            }
        });
        m_freqValuesSpinner = new JSpinner(new SpinnerNumberModel(5, 1, null, 1));

        addTab("Options", initOptions());
        addTab("Table", initTable());
    }

    private JPanel initOptions() {
        JPanel generalPanel = new JPanel(new GridBagLayout());
        generalPanel.setBorder(new TitledBorder("General Options"));
        GridBagConstraints gbcG = createConfiguredGridBagConstraints();
        gbcG.gridwidth = 2;
        gbcG.fill = GridBagConstraints.HORIZONTAL;
        generalPanel.add(m_enableFreqValDisplayCheckbox, gbcG);
        gbcG.gridy++;
        gbcG.gridwidth = 1;
        generalPanel.add(new JLabel("Number of most freq./infreq. values: "), gbcG);
        gbcG.gridx++;

        m_freqValuesSpinner.setPreferredSize(new Dimension(100, TEXT_FIELD_SIZE));
        generalPanel.add(m_freqValuesSpinner, gbcG);
        gbcG.gridwidth = 2;
        gbcG.gridx = 0;
        //generalPanel.add(m_displayFullscreenButtonCheckBox, gbcG);

        gbcG.gridy++;
        generalPanel.add(m_showMedianCheckBox, gbcG);

        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setBorder(new TitledBorder("Titles"));
        GridBagConstraints gbcT = createConfiguredGridBagConstraints();
        titlePanel.add(new JLabel("Title: "), gbcT);
        gbcT.gridx++;
        titlePanel.add(m_titleField, gbcT);
        gbcT.gridx = 0;
        gbcT.gridy++;
        titlePanel.add(new JLabel("Subtitle: "), gbcT);
        gbcT.gridx++;
        titlePanel.add(m_subtitleField, gbcT);

        JPanel numberPanel = new JPanel(new GridBagLayout());
        numberPanel.setBorder(new TitledBorder("Number Formatter"));
        GridBagConstraints gbcN = createConfiguredGridBagConstraints();
        gbcN.gridwidth = 2;
        numberPanel.add(m_enableGlobalNumberFormatCheckbox, gbcN);
        gbcN.gridy++;
        gbcN.gridwidth = 1;
        numberPanel.add(new JLabel("Decimal places: "), gbcN);
        gbcN.gridx++;
        m_globalNumberFormatDecimalSpinner.setPreferredSize(new Dimension(100, TEXT_FIELD_SIZE));
        numberPanel.add(m_globalNumberFormatDecimalSpinner, gbcN);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createConfiguredGridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(generalPanel, gbc);
        gbc.gridy++;
        panel.add(titlePanel, gbc);
        gbc.gridy++;
        panel.add(numberPanel, gbc);
        return panel;
    }

    private JPanel initTable() {
        JPanel pagingPanel = new JPanel(new GridBagLayout());
        pagingPanel.setBorder(new TitledBorder("Paging"));
        GridBagConstraints gbcP = createConfiguredGridBagConstraints();
        gbcP.gridwidth = 2;
        pagingPanel.add(m_enablePagingCheckBox, gbcP);
        gbcP.gridy++;
        gbcP.gridwidth = 1;
        pagingPanel.add(new JLabel("Initial page size: "), gbcP);
        gbcP.gridx++;
        m_initialPageSizeSpinner.setPreferredSize(new Dimension(100, TEXT_FIELD_SIZE));
        pagingPanel.add(m_initialPageSizeSpinner, gbcP);
        gbcP.gridx = 0;
        gbcP.gridy++;
        pagingPanel.add(m_enablePageSizeChangeCheckBox, gbcP);
        gbcP.gridx = 0;
        gbcP.gridy++;
        pagingPanel.add(new JLabel("Selectable page sizes: "), gbcP);
        gbcP.gridx++;
        pagingPanel.add(m_allowedPageSizesField, gbcP);
        gbcP.gridx = 0;
        gbcP.gridy++;
        gbcP.gridwidth = 2;
        pagingPanel.add(m_enableShowAllCheckBox, gbcP);

        gbcP.gridy++;
        gbcP.gridwidth = 1;
        pagingPanel.add(new JLabel("Number of rows for data preview: "), gbcP);
        gbcP.gridx++;
        m_displayPreviewRowsSpinner.setPreferredSize(new Dimension(100, TEXT_FIELD_SIZE));
        pagingPanel.add(m_displayPreviewRowsSpinner, gbcP);
        //gbcP.gridy++;
        //pagingPanel.add(m_enableJumpToPageCheckBox, gbcP);

        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setBorder(new TitledBorder("Selection"));
        GridBagConstraints gbcS = createConfiguredGridBagConstraints();
        selectionPanel.add(m_enableSelectionCheckbox, gbcS);

        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(new TitledBorder("Searching / Filtering"));
        GridBagConstraints gbcSe = createConfiguredGridBagConstraints();
        searchPanel.add(m_enableSearchCheckbox, gbcSe);

        JPanel sortingPanel = new JPanel(new GridBagLayout());
        sortingPanel.setBorder(new TitledBorder("Sorting"));
        GridBagConstraints gbcSo = createConfiguredGridBagConstraints();
        sortingPanel.add(m_enableSortingCheckBox, gbcSo);
        gbcSo.gridx++;
        sortingPanel.add(m_enableClearSortButtonCheckBox, gbcSo);

        JPanel nominalPanel = new JPanel(new GridBagLayout());
        nominalPanel.setBorder(new TitledBorder("Nominal Values"));
        GridBagConstraints gbcNo = createConfiguredGridBagConstraints();
        nominalPanel.add(new JLabel("Max number of nominal values"), gbcNo);
        gbcNo.gridx++;
        m_maxNominalValuesSpinner.setPreferredSize(new Dimension(100, TEXT_FIELD_SIZE));
        nominalPanel.add(m_maxNominalValuesSpinner, gbcNo);


        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createConfiguredGridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(pagingPanel, gbc);
        gbc.gridy++;
        panel.add(selectionPanel, gbc);
        gbc.gridy++;
        panel.add(searchPanel, gbc);
        gbc.gridy++;
        panel.add(sortingPanel, gbc);
        gbc.gridy++;
        panel.add(nominalPanel, gbc);
        return panel;
    }

    private GridBagConstraints createConfiguredGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
    }

    private String getAllowedPageSizesString(final int[] sizes) {
        if (sizes.length < 1) {
            return "";
        }
        StringBuilder builder = new StringBuilder(String.valueOf(sizes[0]));
        for (int i = 1; i < sizes.length; i++) {
            builder.append(", ");
            builder.append(sizes[i]);
        }
        return builder.toString();
    }

    private int[] getAllowedPageSizes() throws InvalidSettingsException {
        String[] sizesArray = m_allowedPageSizesField.getText().split(",");
        int[] allowedPageSizes = new int[sizesArray.length];
        try {
            for (int i = 0; i < sizesArray.length; i++) {
                allowedPageSizes[i] = Integer.parseInt(sizesArray[i].trim());
            }
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
        return allowedPageSizes;
    }

    private void enableFreqValues() {
        boolean enableFreqVal = m_enableFreqValDisplayCheckbox.isSelected();
        m_freqValuesSpinner.setEnabled(enableFreqVal);

    }

    private void enablePagingFields() {
        boolean enableGlobal = m_enablePagingCheckBox.isSelected();
        boolean enableSizeChange = m_enablePageSizeChangeCheckBox.isSelected();
        m_initialPageSizeSpinner.setEnabled(enableGlobal);
        m_enablePageSizeChangeCheckBox.setEnabled(enableGlobal);
        m_allowedPageSizesField.setEnabled(enableGlobal && enableSizeChange);
        m_enableShowAllCheckBox.setEnabled(enableGlobal && enableSizeChange);
        m_enableJumpToPageCheckBox.setEnabled(enableGlobal);
    }

    private void enableFormatterFields() {
        boolean enableNumberFormat = m_enableGlobalNumberFormatCheckbox.isSelected();
        m_globalNumberFormatDecimalSpinner.setEnabled(enableNumberFormat);
    }

    private void enableSortingFields() {
        m_enableClearSortButtonCheckBox.setEnabled(m_enableSortingCheckBox.isSelected());
    }

    private void enableSearchFields() {
        /* nothing so far */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        DataExplorerConfig config = new DataExplorerConfig();
        config.setShowMedian(m_showMedianCheckBox.isSelected());
        config.setEnablePaging(m_enablePagingCheckBox.isSelected());
        config.setInitialPageSize((Integer)m_initialPageSizeSpinner.getValue());
        config.setEnablePageSizeChange(m_enablePageSizeChangeCheckBox.isSelected());
        config.setAllowedPageSizes(getAllowedPageSizes());
        config.setPageSizeShowAll(m_enableShowAllCheckBox.isSelected());
        config.setEnableJumpToPage(m_enableJumpToPageCheckBox.isSelected());
        config.setDisplayFullscreenButton(m_displayFullscreenButtonCheckBox.isSelected());
        config.setTitle(m_titleField.getText());
        config.setSubtitle(m_subtitleField.getText());
        config.setEnableSelection(m_enableSelectionCheckbox.isSelected());
        config.setEnableSorting(m_enableSortingCheckBox.isSelected());
        config.setEnableClearSortButton(m_enableClearSortButtonCheckBox.isSelected());
        config.setEnableSearching(m_enableSearchCheckbox.isSelected());
        config.setEnableGlobalNumberFormat(m_enableGlobalNumberFormatCheckbox.isSelected());
        config.setGlobalNumberFormatDecimals((Integer)m_globalNumberFormatDecimalSpinner.getValue());
        config.setdisplayRowNumber((Integer)m_displayPreviewRowsSpinner.getValue());
        config.setMaxNominalValues((Integer)m_maxNominalValuesSpinner.getValue());
        config.setEnableFreqValDisplay(m_enableFreqValDisplayCheckbox.isSelected());
        config.setFreqValues((Integer)m_freqValuesSpinner.getValue());
        config.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        DataExplorerConfig config = new DataExplorerConfig();
        DataTableSpec inSpec = (DataTableSpec)specs[0];
        config.loadSettingsForDialog(settings, inSpec);
        m_showMedianCheckBox.setSelected(config.getShowMedian());
        m_enablePagingCheckBox.setSelected(config.getEnablePaging());
        m_initialPageSizeSpinner.setValue(config.getInitialPageSize());
        m_enablePageSizeChangeCheckBox.setSelected(config.getEnablePageSizeChange());
        m_allowedPageSizesField.setText(getAllowedPageSizesString(config.getAllowedPageSizes()));
        m_enableShowAllCheckBox.setSelected(config.getPageSizeShowAll());
        m_enableJumpToPageCheckBox.setSelected(config.getEnableJumpToPage());
        m_displayFullscreenButtonCheckBox.setSelected(config.getDisplayFullscreenButton());
        m_titleField.setText(config.getTitle());
        m_subtitleField.setText(config.getSubtitle());
        m_enableSelectionCheckbox.setSelected(config.getEnableSelection());
        m_enableSearchCheckbox.setSelected(config.getEnableSearching());
        m_enableSortingCheckBox.setSelected(config.getEnableSorting());
        m_enableClearSortButtonCheckBox.setSelected(config.getEnableClearSortButton());
        m_enableGlobalNumberFormatCheckbox.setSelected(config.getEnableGlobalNumberFormat());
        m_globalNumberFormatDecimalSpinner.setValue(config.getGlobalNumberFormatDecimals());
        m_displayPreviewRowsSpinner.setValue(config.getDisplayRowNumber());
        m_maxNominalValuesSpinner.setValue(config.getMaxNominalValues());
        m_enableFreqValDisplayCheckbox.setSelected(config.getEnableFreqValDisplay());
        m_freqValuesSpinner.setValue(config.getFreqValues());
        enablePagingFields();
        enableSearchFields();
        enableFormatterFields();
        enableSortingFields();
        enableFreqValues();
    }

}
