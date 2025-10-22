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
 *   Feb 21, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.algorithms.outlier.listeners.NumericOutlierWarning;
import org.knime.base.algorithms.outlier.listeners.NumericOutlierWarningListener;
import org.knime.base.algorithms.outlier.options.NumericOutliersDetectionOption;
import org.knime.base.algorithms.outlier.options.NumericOutliersReplacementStrategy;
import org.knime.base.algorithms.outlier.options.NumericOutliersTreatmentOption;
import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 * The algorithm to treat outliers based on the permitted intervals provided.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class NumericOutliersReviser {

    /**
     * The supported data types. The data types are restricted, since we have to create new cells, see
     * {@link #getTreatedCell(DataCell, double)}, of the same type as the input column.
     */
    static final Set<DataType> SUPPORTED_DATA_TYPES =
        new HashSet<DataType>(Arrays.asList(new DataType[]{LongCell.TYPE, IntCell.TYPE, DoubleCell.TYPE}));

    /** The supported data values. */
    public static final List<Class<? extends DataValue>> SUPPORTED_DATA_VALUES =
            List.of(LongValue.class, IntValue.class, DoubleValue.class);

    /** The illegal cell type exception. */
    private static final String ILLEGAL_CELL_TYPE_EXCEPTION =
        "Only cells of type double, integer, and long are supported";

    /** Outlier treatment and output generation routine message. */
    private static final String TREATMENT_MSG = "Treating outliers and generating output";

    /** Empty table warning text. */
    private static final String EMPTY_TABLE_WARNING = "Node created an empty data table";

    /** The outlier treatment option. */
    private final NumericOutliersTreatmentOption m_treatment;

    /** The outlier replacement strategy. */
    private final NumericOutliersReplacementStrategy m_repStrategy;

    /** The outlier detection option. */
    private final NumericOutliersDetectionOption m_detectionOption;

    /** List of listeners receiving warning messages. */
    private final List<NumericOutlierWarningListener> m_listeners;

    /** The outlier column names. */
    private String[] m_outlierColNames;

    /** The table storing the summary. */
    private BufferedDataTable m_summaryTable;

    /** Tells whether the domains of the outlier columns have to be updated after the computation or not. */
    private final boolean m_updateDomain;

    /** Responsible to update the domain. */
    private NumericOutliersDomainsUpdater m_domainUpdater;

    /** The summary internals. */
    private SummaryInternals m_summaryInterals;

    /**
     * Builder of the OutlierReviser.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static final class Builder {

        // Optional parameters
        /** The outlier treatment option. */
        private NumericOutliersTreatmentOption m_treatment = NumericOutliersTreatmentOption.REPLACE;

        /** The outlier replacement strategy. */
        private NumericOutliersReplacementStrategy m_repStrategy = NumericOutliersReplacementStrategy.INTERVAL_BOUNDARY;

        /** The outlier detection option. */
        private NumericOutliersDetectionOption m_detectionOption = NumericOutliersDetectionOption.ALL;

        /** Tells whether the domains of the outlier columns have to be update after the computation or not. */
        private boolean m_updateDomain = false;

        /**
         * Constructort.
         *
         */
        public Builder() {
        }

        /**
         * Defines how outlier have to be treated, see {@link NumericOutliersTreatmentOption}.
         *
         * @param treatment the treatment option to be used
         * @return the builder itself
         */
        public Builder setTreatmentOption(final NumericOutliersTreatmentOption treatment) {
            m_treatment = treatment;
            return this;
        }

        /**
         * Defines the outlier replacement strategy, see {@link NumericOutliersReplacementStrategy}.
         *
         * @param repStrategy the replacement strategy
         * @return the builder itself
         */
        public Builder setReplacementStrategy(final NumericOutliersReplacementStrategy repStrategy) {
            m_repStrategy = repStrategy;
            return this;
        }

        /**
         * Defines the outlier detection option, see {@link NumericOutliersDetectionOption}
         *
         * @param detectionOption the detection option
         * @return the builder itself
         */
        public Builder setDetectionOption(final NumericOutliersDetectionOption detectionOption) {
            m_detectionOption = detectionOption;
            return this;
        }

        /**
         * Sets the domain policy flag.
         *
         * @param resetDomain the domain policy
         * @return the builder itself
         */
        public Builder updateDomain(final boolean resetDomain) {
            m_updateDomain = resetDomain;
            return this;
        }

        /**
         * Constructs the outlier reviser using the settings provided by the builder.
         *
         * @return the outlier reviser using the settings provided by the builder
         */
        public NumericOutliersReviser build() {
            return new NumericOutliersReviser(this);
        }
    }

    /**
     * Constructor.
     *
     * @param b the builder
     */
    private NumericOutliersReviser(final Builder b) {
        m_treatment = b.m_treatment;
        m_repStrategy = b.m_repStrategy;
        m_detectionOption = b.m_detectionOption;
        m_updateDomain = b.m_updateDomain;
        m_listeners = new LinkedList<NumericOutlierWarningListener>();
    }

    /**
     * Tells whether or not the provided {@link DataType} is supported.
     *
     * @param type the data type to be evaluated
     * @return {@code True} if the provided {@link DataType} is supported
     */
    public static boolean supports(final DataType type) {
        return SUPPORTED_DATA_TYPES.contains(type);
    }

    /**
     * Adds the given listener.
     *
     * @param listener the listener to add
     */
    public void addListener(final NumericOutlierWarningListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * Returns the spec of the outlier free data table.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the outlier free data table
     */
    public static DataTableSpec getOutTableSpec(final DataTableSpec inSpec) {
        return inSpec;
    }

    /**
     * Returns the data table storing the permitted intervals and additional information about member counts.
     *
     * @return the data table storing the summary
     */
    public BufferedDataTable getSummaryTable() {
        return m_summaryTable;
    }

    /**
     * Returns the spec of the table storing the permitted intervals and additional information about member counts.
     *
     * @param inSpec the in spec
     * @param groupColNames the group column names
     *
     * @return the spec of the data table storing the summary
     */
    public static DataTableSpec getSummaryTableSpec(final DataTableSpec inSpec, final String[] groupColNames) {
        return NumericOutliersSummaryTable.getSpec(inSpec, groupColNames);
    }

    /**
     * Returns the summary internals. Only set if in streamable mode.
     *
     * @return the summary internals
     */
    public SummaryInternals getSummaryInternals() {
        return m_summaryInterals;
    }

    /**
     * Returns the outlier treatment option.
     *
     * @return the outlier treatment option
     */
    NumericOutliersTreatmentOption getTreatmentOption() {
        return m_treatment;
    }

    /**
     * Returns the outlier replacement strategy.
     *
     * @return the outlier replacement strategy
     */
    NumericOutliersReplacementStrategy getReplacementStrategy() {
        return m_repStrategy;
    }

    /**
     * Returns the outlier detection restriction option.
     *
     * @return the outlier detection option
     */
    NumericOutliersDetectionOption getRestrictionOption() {
        return m_detectionOption;
    }

    /**
     * Returns the update domain flag.
     *
     * @return the update domain flag
     */
    boolean updateDomain() {
        return m_updateDomain;
    }

    /**
     * Removes/Retains all rows from the input table that contain outliers. Additionally, the outlier and group related
     * counts, and the new domains are calculated.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param outlierModel the model storing the permitted intervals
     * @return returns the data table whose outliers have been treated
     * @throws Exception any exception to indicate an error, cancelation.
     */
    public BufferedDataTable treatOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final NumericOutliersModel outlierModel) throws Exception {
        final BufferedDataTableRowOutput out =
            new BufferedDataTableRowOutput(exec.createDataContainer(getOutTableSpec(in.getDataTableSpec())));

        // treat the outliers
        treatOutliers(exec, new DataTableRowInput(in), out, outlierModel, in.size(), false);

        // store the result
        final BufferedDataTable outTable;

        // update the domain if necessary. This cannot be done if we are in streaming mode
        if (updateDomain()) {
            outTable = m_domainUpdater.updateDomain(exec, out.getDataTable());
            m_domainUpdater = null;
        } else {
            outTable = out.getDataTable();
        }

        // set empty table message only if not both tables are empty
        if (outTable.size() == 0 && m_summaryTable.size() > 0) {
            warnListeners(EMPTY_TABLE_WARNING);
        }

        // return the table
        return outTable;

    }

    /**
     * Clears the input data table of its outliers according to the defined outlier treatment settings.
     *
     * <p>
     * Given that outliers have to be replaced, each of the cells containing an outlier is either replaced by an missing
     * value or set to value of the closest value within the permitted interval. Otherwise all rows containing an
     * outlier are removed from the input data table.
     * </p>
     *
     * @param exec the execution context
     * @param in the row input whose outliers have to be treated
     * @param out the row output whose outliers have been treated
     * @param outlierModel the model storing the permitted intervals
     * @throws Exception any exception to indicate an error, cancelation.
     */
    public void treatOutliers(final ExecutionContext exec, final RowInput in, final RowOutput out,
        final NumericOutliersModel outlierModel) throws Exception {
        treatOutliers(exec, in, out, outlierModel, -1, true);
    }

    /**
     * Clears the input data table of its outliers according to the defined outlier treatment settings.
     *
     * <p>
     * Given that outliers have to be replaced, each of the cells containing an outlier is either replaced by an missing
     * value or set to value of the closest value within the permitted interval. Otherwise all rows containing an
     * outlier are removed from the input data table.
     * </p>
     * <p>
     * If the node is executed in streaming mode instead of the summary table a summary internals object will be
     * created.
     * </p>
     *
     * @param exec the execution context
     * @param in the row input whose outliers have to be treated
     * @param out the row output whose outliers have been treated
     * @param outlierModel the model storing the permitted intervals
     * @param the row count of the row input
     * @param inStreamingMode tells whether this method is executed in streaming mode, or not
     * @throws Exception any exception to indicate an error, cancelation.
     */
    private void treatOutliers(final ExecutionContext exec, final RowInput in, final RowOutput out,
        final NumericOutliersModel outlierModel, final long rowCount, final boolean inStreamingMode) throws Exception {
        // check the outlier column type compatibility
        checkOutlierCompatibility(in.getDataTableSpec(), outlierModel.getOutlierColNames());

        // start the treatment step
        exec.setMessage(TREATMENT_MSG);

        // set the outlier column names
        m_outlierColNames = outlierModel.getOutlierColNames();

        // counters for the number of non-missing values and outliers contained in each outlier column respective
        // the different groups
        final MemberCounter outlierRepCounter = new MemberCounter();
        final MemberCounter memberCounter = new MemberCounter();
        final MemberCounter missingGroupsCounter = new MemberCounter();

        // the progress
        double treatmentProgress = 0.9;

        if (inStreamingMode) {
            // set the summary internals
            m_summaryInterals = new SummaryInternals(in.getDataTableSpec(), outlierModel, memberCounter,
                outlierRepCounter, missingGroupsCounter);
            addListener(m_summaryInterals);
            treatmentProgress = 1;
        }

        // the domains updater
        m_domainUpdater = new NumericOutliersDomainsUpdater();

        // treat the outliers with respect to the selected treatment option
        if (m_treatment == NumericOutliersTreatmentOption.REPLACE) {
            // replaces outliers according to the set replacement strategy
            replaceOutliers(exec.createSubExecutionContext(treatmentProgress), in, out, outlierModel, memberCounter,
                outlierRepCounter, missingGroupsCounter);
        } else {
            // we remove/retain all columns containing at least one outlier
            treatRows(exec.createSubExecutionContext(treatmentProgress), in, out, outlierModel, rowCount, memberCounter,
                outlierRepCounter, missingGroupsCounter);
        }

        if (!inStreamingMode) {
            // set the summary table
            m_summaryTable = NumericOutliersSummaryTable.getTable(exec.createSubExecutionContext(1 - treatmentProgress),
                in.getDataTableSpec(), outlierModel, memberCounter, outlierRepCounter, missingGroupsCounter);
        }
        // cleare some memory
        m_outlierColNames = null;
    }

    /**
     * Checks if all outlier column types are supported.
     *
     * @param inSpec the input data table spec
     * @param outlierColNames the outlier column names
     */
    private static void checkOutlierCompatibility(final DataTableSpec inSpec, final String[] outlierColNames) {
        if (!Arrays.stream(outlierColNames)//
            .map(c -> inSpec.getColumnSpec(c).getType())//
            .allMatch(NumericOutliersReviser::supports)) {
            throw new IllegalArgumentException(ILLEGAL_CELL_TYPE_EXCEPTION);
        }
    }

    /**
     * Replaces outliers found in the row input according to the selected replacement option. Additionally, the outlier
     * replacement counts and new domains are calculated.
     *
     * @param exec the execution context
     * @param in the row input whose outliers have to be treated
     * @param out the row output whose outliers have been treated
     * @param outlierModel the model storing the permitted intervals
     * @param memberCounter the member counter
     * @param outlierRepCounter the outlier replacement counter
     * @param missingGroupsCounter the missing groups counter
     * @throws Exception any exception to indicate an error, cancelation
     */
    private void replaceOutliers(final ExecutionContext exec, final RowInput in, final RowOutput out,
        final NumericOutliersModel outlierModel, final MemberCounter memberCounter,
        final MemberCounter outlierRepCounter, final MemberCounter missingGroupsCounter) throws Exception {
        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // the in table spec
        final DataTableSpec inSpec = in.getDataTableSpec();

        // create column re-arranger to overwrite cells corresponding to outliers
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);

        // store the positions where the outlier column names can be found in the input table
        final int[] outlierIndices = calculateOutlierIndicies(inSpec);

        final DataColumnSpec[] outlierSpecs = new DataColumnSpec[noOutliers];
        for (int i = 0; i < noOutliers; i++) {
            outlierSpecs[i] = inSpec.getColumnSpec(outlierIndices[i]);
        }
        // values are copied anyways by the re-arranger so there is no need to
        // create new instances for each row
        final DataCell[] treatedVals = new DataCell[noOutliers];

        final AbstractCellFactory fac = new AbstractCellFactory(true, outlierSpecs) {

            @Override
            public DataCell[] getCells(final DataRow row) {
                final GroupKey key = outlierModel.getKey(row, inSpec);
                final Map<String, double[]> colsMap = outlierModel.getGroupIntervals(key);
                for (int i = 0; i < noOutliers; i++) {
                    final DataCell curCell = row.getCell(outlierIndices[i]);
                    final DataCell treatedCell;
                    final String outlierColName = m_outlierColNames[i];
                    if (!curCell.isMissing()) {
                        // if the key exists treat the value otherwise we process an unkown group
                        if (colsMap != null) {
                            // increment the member counter
                            memberCounter.incrementMemberCount(outlierColName, key);
                            // treat the value of the cell if its a outlier
                            treatedCell =
                                treatCellValue(colsMap.get(outlierColName), outlierSpecs[i].getType(), curCell);
                        } else {
                            missingGroupsCounter.incrementMemberCount(outlierColName, key);
                            treatedCell = curCell;
                        }
                    } else {
                        treatedCell = curCell;
                    }
                    // if we changed the value this is an outlier
                    if (!treatedCell.equals(curCell)) {
                        outlierRepCounter.incrementMemberCount(outlierColName, key);
                    }
                    // update the domain if necessary
                    if (m_updateDomain && !treatedCell.isMissing()) {
                        m_domainUpdater.updateDomain(outlierColName, ((DoubleValue)treatedCell).getDoubleValue());
                    }
                    treatedVals[i] = treatedCell;
                }
                return treatedVals;
            }

        };
        // replace the outlier columns by their updated versions
        colRearranger.replace(fac, outlierIndices);

        // stream it
        colRearranger.createStreamableFunction().runFinal(new PortInput[]{in}, new PortOutput[]{out}, exec);

        exec.setProgress(1);
    }

    /**
     * Calculates the positions where the outlier columns can be found in the input table.
     *
     * @param inSpec the input table spec
     * @return the positions of the outlier columns w.r.t. the input spec
     */
    private int[] calculateOutlierIndicies(final DataTableSpec inSpec) {
        final int[] outlierIndices = new int[m_outlierColNames.length];
        for (int i = 0; i < m_outlierColNames.length; i++) {
            outlierIndices[i] = inSpec.findColumnIndex(m_outlierColNames[i]);
        }
        return outlierIndices;
    }

    /**
     * If necessary the value/type of the data cell is modified in accordance with the selected outlier replacement
     * strategy.
     *
     * @param interval the permitted interval
     * @param colType the data type of the column storing the cell
     * @param cell the the current data cell
     * @return the new data cell after replacing its value if necessary
     */
    private DataCell treatCellValue(final double[] interval, final DataType colType, final DataCell cell) {
        // the model might not have learned anything about this key
        if (interval == null) {
            return cell;
        }

        // If the cell is of type long casting it to double value can cause precision problems, e.g.:
        // final long a = -5307351023624618796l;
        // final long b = (long)((double)a);
        // > a : -5307351023624618796
        // > b : -5307351023624619008
        // Since the permitted interval is a double array this conversion is necessary (keep in mind that it is
        // also deterministic). However, if we do not change the value, i.e., the cell is not an outlier, we
        // return the original cell otherwise we have to create a new cell (see #getTreatedCell).
        // In the latter case we cannot overcome the precision problem. Anyway, the intervals were also
        // calculated by casting the long value to double!
        double val = ((DoubleValue)cell).getDoubleValue();

        // treat cell according the selected replacement strategy
        if (m_repStrategy == NumericOutliersReplacementStrategy.MISSING) {
            return isOutlier(interval, val) ? DataType.getMissingCell() : cell;
        }

        if (colType.equals(DoubleCell.TYPE)) {
            // sets to the lower interval bound if necessary
            if (m_detectionOption == NumericOutliersDetectionOption.LOWER_BOUND
                || m_detectionOption == NumericOutliersDetectionOption.ALL) {
                val = Math.max(val, interval[0]);
            }
            // sets to the higher interval bound if necessary
            if (m_detectionOption == NumericOutliersDetectionOption.UPPER_BOUND
                || m_detectionOption == NumericOutliersDetectionOption.ALL) {
                val = Math.min(val, interval[1]);
            }
        } else {
            // sets to the lower interval bound if necessary
            // to the smallest integer inside the permitted interval
            if (m_detectionOption == NumericOutliersDetectionOption.LOWER_BOUND
                || m_detectionOption == NumericOutliersDetectionOption.ALL) {
                val = Math.max(val, Math.ceil(interval[0]));
            }
            // sets to the higher interval bound if necessary
            // to the largest integer inside the permitted interval
            if (m_detectionOption == NumericOutliersDetectionOption.UPPER_BOUND
                || m_detectionOption == NumericOutliersDetectionOption.ALL) {
                val = Math.min(val, Math.floor(interval[1]));
            }
            // return the proper DataCell
        }
        return getTreatedCell(colType, cell, val);
    }

    /**
     * Returns the treated cell.
     *
     * @param colType the data type of the column storing the cell
     * @param cell the data cell
     * @param val the new value of this cell
     * @return a new data cell if the cell's value is different from the new value
     */
    private static DataCell getTreatedCell(final DataType colType, final DataCell cell, final double val) {
        final double prevVal = ((DoubleValue)cell).getDoubleValue();
        if (prevVal == val) {
            return cell;
        } else if (colType.equals(DoubleCell.TYPE)) {
            return DoubleCellFactory.create(val);
        } else if (colType.equals(LongCell.TYPE)) {
            return LongCellFactory.create((long)val);
        } else if (colType.equals(IntCell.TYPE)) {
            return IntCellFactory.create((int)val);
        }
        throw new IllegalArgumentException(ILLEGAL_CELL_TYPE_EXCEPTION);
    }

    /**
     * Checks w.r.t. the selected detection option if the value is an outlier or not.
     *
     * @param interval the permitted interval
     * @param val the value to be tested
     * @return {@code True} if the value is an outlier
     */
    private boolean isOutlier(final double[] interval, final double val) {
        if (val < interval[0] && (m_detectionOption == NumericOutliersDetectionOption.LOWER_BOUND
            || m_detectionOption == NumericOutliersDetectionOption.ALL)) {
            return true;
        }
        if (val > interval[1] && (m_detectionOption == NumericOutliersDetectionOption.UPPER_BOUND
            || m_detectionOption == NumericOutliersDetectionOption.ALL)) {
            return true;
        }
        return false;
    }

    /**
     * Removes/Retains all rows from the row input that contain outliers. Additionally, the outlier and group related
     * counts, and the new domains are calculated.
     *
     * @param exec the execution context
     * @param in the row input whose outliers have to be treated
     * @param out the row output whose outliers have been treated
     * @param permIntervalsModel the model storing the permitted intervals
     * @param rowCount the row count of the row input
     * @param memberCounter the member counter
     * @param outlierRepCounter the outlier replacement counter
     * @param missingGroupsCounter the missing groups counter
     * @throws CanceledExecutionException if the user has canceled the execution
     * @throws InterruptedException if canceled
     */
    private void treatRows(final ExecutionContext exec, final RowInput in, final RowOutput out,
        final NumericOutliersModel permIntervalsModel, final long rowCount, final MemberCounter memberCounter,
        final MemberCounter outlierRepCounter, final MemberCounter missingGroupsCounter)
        throws CanceledExecutionException, InterruptedException {
        // the in spec
        final DataTableSpec inSpec = in.getDataTableSpec();

        // store the positions where the outlier column names can be found in the input table
        final int[] outlierIndices = calculateOutlierIndicies(inSpec);

        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        final double divisor = rowCount;
        long rowCounter = 1;

        // for each row test if it contains an outlier
        DataRow row;
        while ((row = in.poll()) != null) {
            exec.checkCanceled();
            if (rowCount > 0) {
                final long rowCounterLong = rowCounter++; // 'final' due to access in lambda expression
                exec.setProgress(rowCounterLong / divisor,
                    () -> "Testing row " + rowCounterLong + " of " + rowCount + " for outliers");
            }
            // get the group key of the currently processed row
            final GroupKey key = permIntervalsModel.getKey(row, inSpec);
            //get the map holding the permitted intervals for the given groups key
            Map<String, double[]> colsMap = permIntervalsModel.getGroupIntervals(key);
            boolean outlierFreeRow = true;
            for (int i = 0; i < noOutliers; i++) {
                final DataCell cell = row.getCell(outlierIndices[i]);
                final String outlierColName = m_outlierColNames[i];
                // if the key is existent check the rows, otherwise increment the missing group counters
                if (colsMap != null) {
                    final double[] interval = colsMap.get(outlierColName);
                    if (!cell.isMissing()) {
                        // increment the member counter
                        memberCounter.incrementMemberCount(outlierColName, key);
                        final double val = ((DoubleValue)cell).getDoubleValue();
                        // the model might not have learned anything about this key - outlier column combination
                        if (interval != null && isOutlier(interval, val)) {
                            outlierFreeRow = false;
                            // increment the outlier counter
                            outlierRepCounter.incrementMemberCount(outlierColName, key);
                        }
                    }
                } else {
                    if (!cell.isMissing()) {
                        missingGroupsCounter.incrementMemberCount(outlierColName, key);
                    }
                }
            }
            if ((outlierFreeRow && m_treatment == NumericOutliersTreatmentOption.FILTER)
                || (!outlierFreeRow && m_treatment == NumericOutliersTreatmentOption.RETAIN)) {
                out.push(row);
                // update the domain if necessary
                if (m_updateDomain) {
                    DataCell cell;
                    for (int i = 0; i < noOutliers; i++) {
                        if (!(cell = row.getCell(outlierIndices[i])).isMissing()) {
                            m_domainUpdater.updateDomain(m_outlierColNames[i], ((DoubleValue)cell).getDoubleValue());
                        }
                    }
                }
            }
        }
        out.close();
    }

    /**
     * Informs the listeners that a problem occured.
     *
     * @param msg the warning message
     */
    private void warnListeners(final String msg) {
        final NumericOutlierWarning warning = new NumericOutlierWarning(msg);
        // warn all listeners
        m_listeners.forEach(l -> l.warning(warning));
    }

    /**
     * Class wrapping the functionality to update domain bounds
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private final class NumericOutliersDomainsUpdater {

        /** Map containing the min and max values for the column domains to update. */
        private final Map<String, double[]> m_domainsMap;

        private NumericOutliersDomainsUpdater() {
            m_domainsMap = new HashMap<String, double[]>();
        }

        /**
         * Update the domain for the given columns.
         *
         * @param exec the execution context
         * @param data the data table whose domains have to be reset
         * @return the data table after reseting the domains
         */
        private BufferedDataTable updateDomain(final ExecutionContext exec, final BufferedDataTable data) {
            DataTableSpec spec = data.getSpec();
            final DataColumnSpec[] domainSpecs = new DataColumnSpec[spec.getNumColumns()];
            for (int i = 0; i < spec.getNumColumns(); i++) {
                final DataColumnSpec columnSpec = spec.getColumnSpec(i);
                if (m_domainsMap.containsKey(columnSpec.getName())) {
                    domainSpecs[i] = updateDomainSpec(columnSpec, m_domainsMap.get(columnSpec.getName()));
                } else {
                    domainSpecs[i] = columnSpec;
                }
            }
            return exec.createSpecReplacerTable(data, new DataTableSpec(spec.getName(), domainSpecs));
        }

        /**
         * Updates the domain of the input spec.
         *
         * @param inSpec the spec to be updated
         * @param domainVals the min and max value of the input spec column
         * @return the updated spec
         */
        private DataColumnSpec updateDomainSpec(final DataColumnSpec inSpec, final double[] domainVals) {
            DataColumnSpecCreator specCreator = new DataColumnSpecCreator(inSpec);
            DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(inSpec.getDomain());
            DataCell[] domainBounds = createBoundCells(inSpec.getType(), domainVals[0], domainVals[1]);
            domainCreator.setLowerBound(domainBounds[0]);
            domainCreator.setUpperBound(domainBounds[1]);
            specCreator.setDomain(domainCreator.createDomain());
            return specCreator.createSpec();
        }

        /**
         * Creates two data cells of the proper type holding storing the given domain.
         *
         * @param type the type of the cell to create
         * @param lowerBound the lower bound of the domain
         * @param upperBound the upper bound of the domain
         * @return cells of the proper storing the given value
         */
        private DataCell[] createBoundCells(final DataType type, final double lowerBound, final double upperBound) {
            if (type == DoubleCell.TYPE) {
                return new DataCell[]{DoubleCellFactory.create(lowerBound), DoubleCellFactory.create(upperBound)};
            }
            // for int and long type use floor of the lower bound and ceil of the upper bound
            if (type == LongCell.TYPE) {
                return new DataCell[]{LongCellFactory.create((long)Math.floor(lowerBound)),
                    LongCellFactory.create((long)Math.ceil(upperBound))};
            }
            // it must be a int cell
            return new DataCell[]{IntCellFactory.create((int)Math.floor(lowerBound)),
                IntCellFactory.create((int)Math.ceil(upperBound))};
        }

        /**
         * Updates the domain for the respective column.
         *
         * @param colName the outlier column name
         * @param val the value
         */
        synchronized private void updateDomain(final String colName, final double val) {
            if (!m_domainsMap.containsKey(colName)) {
                m_domainsMap.put(colName, new double[]{val, val});
            }
            final double[] domainVals = m_domainsMap.get(colName);
            domainVals[0] = Math.min(domainVals[0], val);
            domainVals[1] = Math.max(domainVals[1], val);
        }
    }

    /**
     * Class responsible for creating the outlier summary table.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private static final class NumericOutliersSummaryTable {

        /** The name of the outlier column. */
        private static final String OUTLIER_COL_NAME = "Outlier column";

        /** The name of the outlier replacement count column. */
        private static final String REPLACEMENT_COUNT = "Outlier count";

        /** The name of the member count column. */
        private static final String MEMBER_COUNT = "Member count";

        /** The name of the column storing the upper bound. */
        private static final String UPPER_BOUND = "Upper bound";

        /** The name of the column storing the lower bound. */
        private static final String LOWER_BOUND = "Lower bound";

        /** The number of additional columns in the table. */
        private static final int NUM_ADD_COLUMNS = 5;

        /**
         * Returns the spec of the table storing the permitted intervals and additional information about member counts.
         *
         * @param inSpec the in table spec
         * @param groupColNames the group column names
         * @return the spec of the table storing the permitted intervals and additional information about member counts
         */
        static DataTableSpec getSpec(final DataTableSpec inSpec, final String[] groupColNames) {
            // init the specs
            final DataColumnSpec[] specs = new DataColumnSpec[NUM_ADD_COLUMNS + groupColNames.length];

            int pos = 0;

            // first column is the outlier column name
            specs[pos++] = new DataColumnSpecCreator(OUTLIER_COL_NAME, StringCell.TYPE).createSpec();

            // add for each group column name an additional column
            for (final String groupColName : groupColNames) {
                specs[pos++] = inSpec.getColumnSpec(groupColName);
            }

            // add the counter and bound columns
            specs[pos++] = new DataColumnSpecCreator(MEMBER_COUNT, IntCell.TYPE).createSpec();
            specs[pos++] = new DataColumnSpecCreator(REPLACEMENT_COUNT, IntCell.TYPE).createSpec();
            specs[pos++] = new DataColumnSpecCreator(LOWER_BOUND, DoubleCell.TYPE).createSpec();
            specs[pos++] = new DataColumnSpecCreator(UPPER_BOUND, DoubleCell.TYPE).createSpec();

            // return the spec
            return new DataTableSpec(specs);
        }

        /**
         * Returns of the data table storing the permitted intervals and additional information about member counts.
         *
         * @param exec the execution context
         * @param inSpec the in spec
         * @param outlierModel the outlier model
         * @param memberCounter the member counter
         * @param outlierRepCounter the outlier replacement counter
         * @param missingGroupsCounter the missing groups counter
         * @return the data table storing the permitted intervals and additional information about member counts.
         * @throws CanceledExecutionException if the user has canceled the execution
         * @throws InterruptedException if canceled
         */
        private static BufferedDataTable getTable(final ExecutionContext exec, final DataTableSpec inSpec,
            final NumericOutliersModel outlierModel, final MemberCounter memberCounter,
            final MemberCounter outlierRepCounter, final MemberCounter missingGroupsCounter)
            throws CanceledExecutionException, InterruptedException {
            // create the data container storing the table

            final DataTableSpec outSpec = getSpec(inSpec, outlierModel.getGroupColNames());
            final BufferedDataTableRowOutput rowOutputTable =
                new BufferedDataTableRowOutput(exec.createDataContainer(outSpec));
            writeTable(exec, outSpec.getNumColumns(), rowOutputTable, outlierModel, memberCounter, outlierRepCounter,
                missingGroupsCounter);
            return rowOutputTable.getDataTable();
        }

        /**
         * Write the data table storing the permitted intervals and additional information about member counts.
         *
         * @param exec the execution context
         * @param numCols the number of columns
         * @param rowOutputTable
         * @param outlierModel the outlier model
         * @param memberCounter the member counter
         * @param outlierRepCounter the outlier replacement counter
         * @param missingGroups the missing groups counter
         * @throws CanceledExecutionException if the user has canceled the execution
         * @throws InterruptedException if canceled
         *
         */
        private static void writeTable(final ExecutionContext exec, final int numCols, final RowOutput rowOutputTable,
            final NumericOutliersModel outlierModel, final MemberCounter memberCounter,
            final MemberCounter outlierRepCounter, final MemberCounter missingGroups)
            throws CanceledExecutionException, InterruptedException {
            // create the array storing the rows
            final DataCell[] row = new DataCell[numCols];

            int rowCount = 0;

            // the missing group keys
            Set<GroupKey> missingGroupKeys = missingGroups.getGroupKeys();

            // numerics used for the progress update
            final long outlierCount = outlierModel.getOutlierColNames().length;
            final double divisor = outlierCount;
            int colCount = 0;

            // write the rows
            for (final String outlierColName : outlierModel.getOutlierColNames()) {
                exec.checkCanceled();
                row[0] = StringCellFactory.create(outlierColName);
                for (Entry<GroupKey, Map<String, double[]>> entry : outlierModel.getEntries()) {
                    final GroupKey key = entry.getKey();
                    final double[] permInterval = entry.getValue().get(outlierColName);
                    addRow(rowOutputTable, rowCount++, row, key, outlierColName, memberCounter, outlierRepCounter,
                        permInterval);
                }
                if (missingGroupKeys.size() != 0) {
                    for (final GroupKey key : missingGroupKeys) {
                        addRow(rowOutputTable, rowCount++, row, key, outlierColName, missingGroups, outlierRepCounter,
                            null);
                    }
                }
                final int count = ++colCount;
                exec.setProgress(count / divisor, () -> "Writing summary for column " + count + " of " + outlierCount);
            }
            // close the container and return the data table
            rowOutputTable.close();

        }

        /**
         * Adds the row to the row output.
         *
         * @param rowOutput the row output
         * @param rowCount the row count
         * @param row the data cell row
         * @param key the groups key
         * @param outlierColName the outlier column name
         * @param memberCounter the member counter
         * @param outlierRepCounter the outlier replacement counter
         * @param permInterval the permitted interval
         * @throws InterruptedException if canceled
         */
        private static void addRow(final RowOutput rowOutput, final int rowCount, final DataCell[] row,
            final GroupKey key, final String outlierColName, final MemberCounter memberCounter,
            final MemberCounter outlierRepCounter, final double[] permInterval) throws InterruptedException {
            int pos = 1;
            for (final DataCell gVal : key.getGroupVals()) {
                row[pos++] = gVal;
            }
            row[pos++] = memberCounter.getCount(outlierColName, key);
            row[pos++] = outlierRepCounter.getCount(outlierColName, key);
            if (permInterval != null) {
                row[pos++] = DoubleCellFactory.create(permInterval[0]);
                row[pos++] = DoubleCellFactory.create(permInterval[1]);
            } else {
                row[pos++] = DataType.getMissingCell();
                row[pos++] = DataType.getMissingCell();
            }
            rowOutput.push(new DefaultRow("Row" + rowCount, row));
        }

    }

    /**
     * Streamable operator internals object storign the summary table information.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static final class SummaryInternals extends StreamableOperatorInternals
        implements NumericOutlierWarningListener {

        /** The warnings key. */
        private static final String WARNINGS_KEY = "warnings";

        /** The data table spec key. */
        private static final String NUM_COL_KEY = "column-count";

        /** The outlier model settings key. */
        private static final String MODEL_KEY = "model";

        /** The member counter settings key. */
        private static final String MEMBER_KEY = "member-counter";

        /** The outlier replacement counter settings key. */
        private static final String REP_KEY = "replacement-counter";

        /** The missing groups counter settings key. */
        private static final String MISSING_GROUPS_KEY = "missing-groups-counter";

        /** The data table in spec. */
        private int m_numCols;

        /** The outlier model. */
        private NumericOutliersModel m_outlierModel;

        /** The member counter. */
        private MemberCounter m_memberCounter;

        /** The outlier replacement counter. */
        private MemberCounter m_outlierRepCounter;

        /** The missing groups counter. */
        private MemberCounter m_missingGroupsCounter;

        /** List containing all warnings. */
        private Set<String> m_warnings;

        /** Empty constructor used by the stream framework. */
        public SummaryInternals() {

        }

        /**
         * Constructor.
         *
         * @param inSpec the in spec
         * @param outlierModel the outlier model
         * @param memberCounter the member counter
         * @param outlierRepCounter the outlier replacement counter
         * @param missingGroupsCounter the missing groups counter
         */
        private SummaryInternals(final DataTableSpec inSpec, final NumericOutliersModel outlierModel,
            final MemberCounter memberCounter, final MemberCounter outlierRepCounter,
            final MemberCounter missingGroups) {
            super();
            m_numCols = getSummaryTableSpec(inSpec, outlierModel.getGroupColNames()).getNumColumns();
            m_outlierModel = outlierModel;
            m_memberCounter = memberCounter;
            m_outlierRepCounter = outlierRepCounter;
            m_missingGroupsCounter = missingGroups;
            m_warnings = new LinkedHashSet<String>();
        }

        /**
         * Creates a summary internals object that merges all information from the provided summary internals.
         *
         * @param internals the internals to merge
         * @return the merged summary internals
         */
        private static SummaryInternals mergeInternals(final SummaryInternals[] internals) {
            final SummaryInternals sumInt = new SummaryInternals();
            sumInt.m_numCols = internals[0].m_numCols;
            sumInt.m_outlierModel = internals[0].m_outlierModel;
            sumInt.m_memberCounter = MemberCounter.merge(Arrays.stream(internals)//
                .map(i -> i.m_memberCounter)//
                .toArray(MemberCounter[]::new));
            sumInt.m_outlierRepCounter = MemberCounter.merge(Arrays.stream(internals)//
                .map(i -> i.m_outlierRepCounter)//
                .toArray(MemberCounter[]::new));
            sumInt.m_missingGroupsCounter = MemberCounter.merge(Arrays.stream(internals)//
                .map(i -> i.m_missingGroupsCounter)//
                .toArray(MemberCounter[]::new));
            sumInt.m_warnings = Arrays.stream(internals)//
                .map(i -> i.m_warnings)//
                .flatMap(Set::stream)//
                .collect(Collectors.toCollection(LinkedHashSet<String>::new));
            return sumInt;
        }

        /**
         * Returns the received warnings
         *
         * @return the received warnings
         */
        public Set<String> getWarnings() {
            return m_warnings;
        }

        /**
         * Write the data table storing the permitted intervals and additional information about member counts.
         *
         * @param exec the execution context
         * @param out the row output to write to
         * @throws CanceledExecutionException if the user has canceled the execution
         * @throws InterruptedException if canceled
         */
        public void writeTable(final ExecutionContext exec, final RowOutput out)
            throws CanceledExecutionException, InterruptedException {
            NumericOutliersSummaryTable.writeTable(exec, m_numCols, out, m_outlierModel, m_memberCounter,
                m_outlierRepCounter, m_missingGroupsCounter);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void load(final DataInputStream input) throws IOException {
            final ModelContentRO model = ModelContent.loadFromXML(input);
            try {
                m_numCols = model.getInt(NUM_COL_KEY);
                m_warnings = Arrays.stream(model.getStringArray(WARNINGS_KEY))//
                    .collect(Collectors.toCollection(LinkedHashSet<String>::new));
                m_outlierModel = NumericOutliersModel.loadInstance(model.getModelContent(MODEL_KEY));
                m_memberCounter = MemberCounter.loadInstance(model.getModelContent(MEMBER_KEY));
                m_outlierRepCounter = MemberCounter.loadInstance(model.getModelContent(REP_KEY));
                m_missingGroupsCounter = MemberCounter.loadInstance(model.getModelContent(MISSING_GROUPS_KEY));
            } catch (InvalidSettingsException e) {
                throw new IOException(e.getMessage());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void save(final DataOutputStream output) throws IOException {
            final ModelContent model = new ModelContent(getClass().getSimpleName());
            model.addInt(NUM_COL_KEY, m_numCols);
            model.addStringArray(WARNINGS_KEY, m_warnings.stream().toArray(String[]::new));
            m_outlierModel.saveModel(model.addModelContent(MODEL_KEY));
            m_memberCounter.saveModel(model.addModelContent(MEMBER_KEY));
            m_outlierRepCounter.saveModel(model.addModelContent(REP_KEY));
            m_missingGroupsCounter.saveModel(model.addModelContent(MISSING_GROUPS_KEY));
            model.saveToXML(output);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void warning(final NumericOutlierWarning warning) {
            m_warnings.add(warning.getMessage());
        }

    }

    /**
     * Class to merge several summaries into one.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static final class SummaryMerger extends MergeOperator {

        /**
         * Constructor.
         */
        public SummaryMerger() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isHierarchical() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
            return SummaryInternals.mergeInternals(Arrays.stream(operators)//
                .map(o -> (SummaryInternals)o)//
                .toArray(SummaryInternals[]::new));
        }

    }

}