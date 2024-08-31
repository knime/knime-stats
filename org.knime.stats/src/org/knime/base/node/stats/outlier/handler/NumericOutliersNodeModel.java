/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Jan 31, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.stats.outlier.handler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.algorithms.outlier.NumericOutliers;
import org.knime.base.algorithms.outlier.NumericOutliersPortObject;
import org.knime.base.algorithms.outlier.listeners.NumericOutlierWarning;
import org.knime.base.algorithms.outlier.listeners.NumericOutlierWarningListener;
import org.knime.base.algorithms.outlier.options.NumericOutliersDetectionOption;
import org.knime.base.algorithms.outlier.options.NumericOutliersReplacementStrategy;
import org.knime.base.algorithms.outlier.options.NumericOutliersTreatmentOption;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;

/**
 * Model to identify outliers based on interquartile ranges.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class NumericOutliersNodeModel extends NodeModel implements NumericOutlierWarningListener {

    /** Invalid input exception text. */
    private static final String INVALID_INPUT_EXCEPTION = "Input does not contain numerical columns";

    /** Missing outlier column exception text. */
    private static final String MISSING_OUTLIER_COLUMN_EXCEPTION = "Please include at least one numerical column";

    /** Scalar exception text. */
    private static final String SCALAR_EXCEPTION = "The IQR scalar has to be greater than or equal 0.";

    /** Config key of the columns defining the groups. */
    static final String CFG_GROUP_COLS = "groups-list";

    /** Config key of the (outlier)-columns to process. */
    static final String CFG_OUTLIER_COLS = "outlier-list";

    /** Config key for the apply to groups setting. */
    private static final String CFG_USE_GROUPS = "use-groups";

    /** Config key of the iqr scalar. */
    private static final String CFG_SCALAR_PAR = "iqr-scalar";

    /** Config key of the memory policy. */
    private static final String CFG_MEM_POLICY = "memory-policy";

    /** Config key of the estimation type used for in-memory computation. */
    private static final String CFG_ESTIMATION_TYPE = "estimation-type";

    /** Config key of the outlier treatment. */
    private static final String CFG_OUTLIER_TREATMENT = "outlier-treatment";

    /** Config key of the outlier replacement strategy. */
    private static final String CFG_OUTLIER_REPLACEMENT = "replacement-strategy";

    /** Config key of the outlier detection option. */
    private static final String CFG_DETECTION_OPTION = "detection-option";

    /** Config key of the domain policy. */
    private static final String CFG_DOMAIN_POLICY = "update-domain";

    /** Config key of the quartiles algorithm setting. */
    private static final String CFG_HEURISTIC = "use-heuristic";

    /** Default estimation type used to calculate the quartiles. */
    private static final EstimationType DEFAULT_ESTIMATION_TYPE = EstimationType.R_6;

    /** Default scalar to scale the interquartile range */
    private static final double DEFAULT_SCALAR = 1.5d;

    /** Default domain policy. */
    private static final boolean DEFAULT_DOMAIN_POLICY = false;

    /** Default memory policy */
    private static final boolean DEFAULT_MEM_POLICY = false;

    /** Default quartiles algorithm setting. */
    private static final boolean HEURISTIC_DEFAULT = false;

    /** Settings model of the selected groups. */
    private SettingsModelColumnFilter2 m_groupSettings;

    /** Settings model of the columns to check for outliers. */
    private SettingsModelColumnFilter2 m_outlierSettings;

    /** Settings model indicating whether the algorithm should be executed in or out of memory. */
    private SettingsModelBoolean m_memorySetting;

    /** Settings model holding information on how the quartiles are calculated if the algorithm is running in-memory. */
    private SettingsModelString m_estimationSettings;

    /** Settings model holding the information on the outlier treatment. */
    private SettingsModelString m_outlierTreatmentSettings;

    /** Settings model holding the information on the outlier detection options. */
    private SettingsModelString m_detectionSettings;

    /** Settings model holding the information on the outlier replacement strategy. */
    private SettingsModelString m_outlierReplacementSettings;

    /** Settings model holding the factor to scale the interquartile range. */
    private SettingsModelDouble m_scalarModel;

    /** Settings model indicating whether the algorithm has to use the provided groups information. */
    private SettingsModelBoolean m_useGroupsSetting;

    /** Settings model indicating whether the algorithm has to update the domain of the output table, or not. */
    private SettingsModelBoolean m_domainSetting;

    /** Settings model indiciting how the quartiles have to be calculated. */
    private SettingsModelString m_heuristicSetting;

    /** Init the outlier detector node model with one input and output. */
    NumericOutliersNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE, NumericOutliersPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable in = (BufferedDataTable)inData[0];

        final NumericOutliers outDet = createOutlierDetector(in.getDataTableSpec());

        outDet.execute(in, exec);

        return new PortObject[]{outDet.getOutTable(), outDet.getSummaryTable(), outDet.getOutlierPort()};
    }

    /**
     * Create the outlier detector instance for the given settings.
     *
     * @param inSpec the input data table spec
     * @return an instance of outlier detector
     * @throws InvalidSettingsException if there is no corresponding enum type
     */
    private NumericOutliers createOutlierDetector(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new NumericOutliers.Builder(getOutlierColNames(inSpec))//
            .addWarningListener(this)//
            .calcInMemory(m_memorySetting.getBooleanValue())//
            .setEstimationType(EstimationType.valueOf(m_estimationSettings.getStringValue()))//
            .setGroupColumnNames(getGroupColNames(inSpec))//
            .setIQRMultiplier(m_scalarModel.getDoubleValue())//
            .setReplacementStrategy(
                NumericOutliersReplacementStrategy.getEnum(m_outlierReplacementSettings.getStringValue()))//
            .setTreatmentOption(NumericOutliersTreatmentOption.getEnum(m_outlierTreatmentSettings.getStringValue()))//
            .setDetectionOption(NumericOutliersDetectionOption.getEnum(m_detectionSettings.getStringValue()))//
            .useHeuristic(Boolean.parseBoolean(m_heuristicSetting.getStringValue()))//
            .updateDomain(m_domainSetting.getBooleanValue())//
            .build();
    }

    /**
     * Returns the outlier column names.
     *
     * @param inSpec the input data table spec
     * @return the outlier column names
     */
    private String[] getOutlierColNames(final DataTableSpec inSpec) {
        return m_outlierSettings.applyTo(inSpec).getIncludes();
    }

    /**
     * Convenience method returning the group column names stored in a list. If no group columns are selected an empty
     * list is returned.
     *
     * @param inSpec the input data table spec
     * @return array of group column names
     */
    private String[] getGroupColNames(final DataTableSpec inSpec) {
        final String[] groupColNames;
        if (m_useGroupsSetting.getBooleanValue()) {
            final List<String> outliers = Arrays.stream(getOutlierColNames(inSpec)).collect(Collectors.toList());
            // remove columns for which the outliers have to be computed
            groupColNames = Arrays.stream(m_groupSettings.applyTo(inSpec).getIncludes())
                .filter(s -> !outliers.contains(s)).toArray(String[]::new);
        } else {
            groupColNames = new String[]{};
        }
        return groupColNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // check if the table contains any row holding numerical values
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        if (!inSpec.stream()//
            .map(DataColumnSpec::getType)//
            .anyMatch(NumericOutliers::supports)) {
            throw new InvalidSettingsException(INVALID_INPUT_EXCEPTION);
        }

        // check and initialize the groups settings model
        if (m_groupSettings == null) {
            m_groupSettings = createGroupFilterModel();
            // don't add anything to the include list during auto-configure
            m_groupSettings.loadDefaults(inSpec, new InputFilter<DataColumnSpec>() {

                @Override
                public boolean include(final DataColumnSpec name) {
                    return false;
                }
            }, true);
        }
        String[] includes;

        // check and initialize the outlier settings model
        if (m_outlierSettings == null) {
            m_outlierSettings = createOutlierFilterModel();
            m_outlierSettings.loadDefaults(inSpec);
            includes = m_outlierSettings.applyTo(inSpec).getIncludes();
            if (includes.length > 0) {
                setWarningMessage("Auto configuration: Outlier Selection uses all suitable columns (in total "
                    + includes.length + ").");
            }
        }
        includes = m_outlierSettings.applyTo(inSpec).getIncludes();
        if (includes.length == 0) {
            throw new InvalidSettingsException(MISSING_OUTLIER_COLUMN_EXCEPTION);
        }

        // initialize the remaining settings models if necessary
        init();

        // test if flow variables violate settings related to enums
        try {
            EstimationType.valueOf(m_estimationSettings.getStringValue());
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage());
        }

        // check if the heuristic settings is configured via flow variables
        if (!m_heuristicSetting.getStringValue().equals(String.valueOf(true))
            && !m_heuristicSetting.getStringValue().equals(String.valueOf(false))) {
            throw new InvalidSettingsException("The selected <Quartile calculation> has to be " + String.valueOf(true)
                + " or " + String.valueOf(false));
        }

        NumericOutliersTreatmentOption.getEnum(m_outlierTreatmentSettings.getStringValue());
        NumericOutliersDetectionOption.getEnum(m_detectionSettings.getStringValue());
        NumericOutliersReplacementStrategy.getEnum(m_outlierReplacementSettings.getStringValue());

        // test if IQR scalar is < 0
        if (m_scalarModel.getDoubleValue() < 0) {
            throw new InvalidSettingsException(SCALAR_EXCEPTION);
        }

        // return the output spec
        final String[] outlierColNames = getOutlierColNames(inSpec);
        final String[] groupColNames = getGroupColNames(inSpec);

        return new PortObjectSpec[]{NumericOutliers.getOutTableSpec(inSpec),
            NumericOutliers.getSummaryTableSpec(inSpec, groupColNames),
            NumericOutliers.getOutlierPortSpec(inSpec, groupColNames, outlierColNames)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_estimationSettings != null) {
            m_estimationSettings.saveSettingsTo(settings);
        }
        if (m_groupSettings != null) {
            m_groupSettings.saveSettingsTo(settings);
        }
        if (m_memorySetting != null) {
            m_memorySetting.saveSettingsTo(settings);
        }
        if (m_outlierSettings != null) {
            m_outlierSettings.saveSettingsTo(settings);
        }
        if (m_scalarModel != null) {
            m_scalarModel.saveSettingsTo(settings);
        }
        if (m_useGroupsSetting != null) {
            m_useGroupsSetting.saveSettingsTo(settings);
        }
        if (m_outlierReplacementSettings != null) {
            m_outlierReplacementSettings.saveSettingsTo(settings);
        }
        if (m_outlierTreatmentSettings != null) {
            m_outlierTreatmentSettings.saveSettingsTo(settings);
        }
        if (m_detectionSettings != null) {
            m_detectionSettings.saveSettingsTo(settings);
        }
        if (m_domainSetting != null) {
            m_domainSetting.saveSettingsTo(settings);
        }
        if (m_heuristicSetting != null) {
            m_heuristicSetting.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel model : getSettings()) {
            model.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel model : getSettings()) {
            model.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * Creates not yet initialized settings and returns an array storing all them.
     *
     * @return array holding all used settings models.
     */
    private SettingsModel[] getSettings() {
        init();
        return new SettingsModel[]{m_groupSettings, m_outlierSettings, m_estimationSettings, m_scalarModel,
            m_memorySetting, m_useGroupsSetting, m_outlierReplacementSettings, m_outlierTreatmentSettings,
            m_detectionSettings, m_domainSetting, m_heuristicSetting};
    }

    /**
     * Creates all non-initialized settings.
     */
    private void init() {
        if (m_groupSettings == null) {
            m_groupSettings = createGroupFilterModel();
        }
        if (m_outlierSettings == null) {
            m_outlierSettings = createOutlierFilterModel();
        }
        if (m_estimationSettings == null) {
            m_estimationSettings = createEstimationModel();
        }
        if (m_scalarModel == null) {
            m_scalarModel = createScalarModel();
        }
        if (m_memorySetting == null) {
            m_memorySetting = createMemoryModel();
        }
        if (m_useGroupsSetting == null) {
            m_useGroupsSetting = createUseGroupsModel();
        }
        if (m_outlierReplacementSettings == null) {
            m_outlierReplacementSettings = createOutlierReplacementModel();
        }
        if (m_outlierTreatmentSettings == null) {
            m_outlierTreatmentSettings = createOutlierTreatmentModel();
        }
        if (m_detectionSettings == null) {
            m_detectionSettings = createOutlierDetectionModel();
        }
        if (m_domainSetting == null) {
            m_domainSetting = createDomainModel();
        }
        if (m_heuristicSetting == null) {
            m_heuristicSetting = createHeuristicModel();
        }
    }

    /**
     * Returns the settings model holding the factor to scale the IQR.
     *
     * @return the IQR scalar settings model
     */
    public static SettingsModelDouble createScalarModel() {
        return new SettingsModelDoubleBounded(CFG_SCALAR_PAR, DEFAULT_SCALAR, 0, Double.MAX_VALUE);
    }

    /**
     * Returns the settings model holding the selected outliers (restricted to numerical columns).
     *
     * @return the outlier settings model
     */
    public static SettingsModelColumnFilter2 createOutlierFilterModel() {
        return new SettingsModelColumnFilter2(CFG_OUTLIER_COLS, new InputFilter<DataColumnSpec>() {

            @Override
            public boolean include(final DataColumnSpec spec) {
                return NumericOutliers.supports(spec.getType());
            }

        }, NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
    }

    /**
     * Returns the settings model holding the selected groups.
     *
     * @return the groups settings model
     */
    public static SettingsModelColumnFilter2 createGroupFilterModel() {
        return new SettingsModelColumnFilter2(CFG_GROUP_COLS);
    }

    /**
     * Returns the settings model indicating whether the algorithm should be executed in or out of memory.
     *
     * @return the memory settings model
     */
    public static SettingsModelBoolean createMemoryModel() {
        return new SettingsModelBoolean(CFG_MEM_POLICY, DEFAULT_MEM_POLICY);
    }

    /**
     * Returns the settings model holding information on how the quartiles are calculated if the algorithm is running
     * in-memory.
     *
     * @return the estimation type settings model
     */
    public static SettingsModelString createEstimationModel() {
        return new SettingsModelString(CFG_ESTIMATION_TYPE, DEFAULT_ESTIMATION_TYPE.name());
    }

    /**
     * Returns the settings model telling whether to apply the algorithm to the selected groups or not.
     *
     * @return the use groups settings model
     */
    public static SettingsModelBoolean createUseGroupsModel() {
        return new SettingsModelBoolean(CFG_USE_GROUPS, false);
    }

    /**
     * Returns the settings model informing about the treatment of outliers (replace or filter).
     *
     * @return the outlier treatment settings model
     */
    public static SettingsModelString createOutlierTreatmentModel() {
        return new SettingsModelString(CFG_OUTLIER_TREATMENT, NumericOutliersTreatmentOption.values()[0].toString());
    }

    /**
     * Returns the settings model informing about the selected replacement strategy (Missings or IQR).
     *
     * @return the outlier replacement settings model
     */
    public static SettingsModelString createOutlierReplacementModel() {
        return new SettingsModelString(CFG_OUTLIER_REPLACEMENT,
            NumericOutliersReplacementStrategy.values()[0].toString());
    }

    public static SettingsModelString createOutlierDetectionModel() {
        return new SettingsModelString(CFG_DETECTION_OPTION, NumericOutliersDetectionOption.values()[0].toString());
    }

    /**
     * Returns the settings model informing about the selected domain policy.
     *
     * @return the domain policy settings model
     */
    public static SettingsModelBoolean createDomainModel() {
        return new SettingsModelBoolean(CFG_DOMAIN_POLICY, DEFAULT_DOMAIN_POLICY);
    }

    /**
     * Returns the settings model informing about the selected heuristic option.
     *
     * @return the heuristic settings model
     */
    public static SettingsModelString createHeuristicModel() {
        return new SettingsModelString(CFG_HEURISTIC, String.valueOf(HEURISTIC_DEFAULT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warning(final NumericOutlierWarning warning) {
        setWarningMessage(warning.getMessage());
    }

}