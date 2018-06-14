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
 */

package org.knime.dl.keras.core.struct.dialog;

import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParsePosition;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.ParameterMember;
import org.scijava.util.Types;

import net.miginfocom.swing.MigLayout;

/**
 * <p>
 * NB: Heavily inspired by work of Curtis Rueden.
 * </p>
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class SwingNumberWidgetFactory implements SwingWidgetFactory<Number> {

    @Override
    public boolean supports(final Member<?> model) {
        // TODO we need to generalize later or make one for double etc
        return Types.isNumber(model.getRawType()) && ((ParameterMember<?>)model).isRequired();
    }

    @Override
    public SwingWidget<Number> create(final Member<Number> model) {
        return new Widget(model);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<Number> implements AdjustmentListener, ChangeListener {

        String SLIDER_STYLE = "slider";

        private JPanel panel;

        private JScrollBar scrollBar;

        private JSlider slider;

        private JSpinner spinner;

        public Widget(final Member<Number> member) {
            super(member);
            getComponent();
        }

        private Number getMin() {
            Number min = toNumber(SwingWidgets.minimum(this), member().getRawType());
            if (min == null) {
                min = org.scijava.util.NumberUtils.getMinimumNumber(member().getRawType());
            }
            return min;
        }

        private Number getMax() {
            Number max = toNumber(SwingWidgets.maximum(this), member().getRawType());
            if (max == null) {
                max = org.scijava.util.NumberUtils.getMaximumNumber(member().getRawType());
            }
            return max;
        }

        @Override
        public JPanel getComponent() {
            if (panel != null)
                return panel;

            panel = new JPanel();
            final MigLayout layout = new MigLayout("fillx,ins 3 0 3 0", "[fill,grow]");
            panel.setLayout(layout);

            Number min = getMin();
            Number max = getMax();

            Number stepSize = toNumber(SwingWidgets.stepSize(this), member().getRawType());
            if (stepSize == null) {
                stepSize = toNumber("1", member().getRawType());
            }

            if (SwingWidgets.isStyle(this, SLIDER_STYLE)) {
                slider = new JSlider();
                slider.setMinorTickSpacing(stepSize.intValue());
                slider.setPaintLabels(true);
                slider.setPaintTicks(true);
                getComponent().add(slider);
                slider.addChangeListener(this);
            }

            // add spinner widget
            final Class<?> type = member().getRawType();
            final Number v = modelValue();
            final Number value = v == null ? getDefaultValue(type) : v;
            final SpinnerNumberModel spinnerModel =
                new SpinnerNumberModelFactory().createModel(value, min, max, stepSize);
            spinner = new JSpinner(spinnerModel);
            fixSpinner(type);
            panel.add(spinner);
            limitWidth(200);
            spinner.addChangeListener(this);

            String format = findDecimalFormat((String)SwingWidgets.stepSize(this), (String)SwingWidgets.maximum(this),
                (String)SwingWidgets.minimum(this));
            spinner.setEditor(new JSpinner.NumberEditor(spinner, format));

            spinner.setValue(modelValue());
            syncSliders();

            return panel;
        }

        private int getNumberOfDecimalPlaces(final String num) {
            if (num == null) {
                return 0;
            }
            if (num.contains("E")) {
                return Math.abs(Integer.parseInt(num.split("E")[1]));
            } else if (num.contains(".")) {
                return num.split("\\.")[1].length();
            } else {
                return 0;
            }
        }

        private String constructDecimalFormat(final int num) {
            return "#" + (num > 0 ? "." : "") + StringUtils.repeat("#", num);
        }

        private String findDecimalFormat(String... nums) {
            int maxNumdecimalPlaces = 0;
            for (String num : nums) {
                int curr = getNumberOfDecimalPlaces(num);
                if (curr > maxNumdecimalPlaces) {
                    maxNumdecimalPlaces = curr;
                }
            }
            return constructDecimalFormat(maxNumdecimalPlaces);
        }

        private String findDecimalFormat(Number num) {
            return findDecimalFormat(num + "");
        }

        private Number getDefaultValue(Class<?> type) {
            if (Types.isByte(type))
                return (byte)0;
            if (Types.isShort(type))
                return (short)0;
            if (Types.isInteger(type))
                return 0;
            if (Types.isLong(type))
                return 0l;
            if (Types.isFloat(type))
                return 0f;
            if (Types.isDouble(type))
                return 0d;
            return null;
        }

        private Number toNumber(Object minimum, Class<?> rawType) {
            final String casted = (String)minimum;
            if (casted == null || casted.isEmpty()) {
                return null;
            }
            try {
                if (Types.isByte(rawType))
                    return NumberUtils.toByte(casted);
                if (Types.isShort(rawType))
                    return NumberUtils.toShort(casted);
                if (Types.isInteger(rawType))
                    return NumberUtils.toInt(casted);
                if (Types.isLong(rawType))
                    return NumberUtils.toLong(casted);
                if (Types.isFloat(rawType))
                    return NumberUtils.toFloat(casted);
                if (Types.isDouble(rawType))
                    return NumberUtils.toDouble(casted);
                return null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // -- AdjustmentListener methods --

        @Override
        public void adjustmentValueChanged(final AdjustmentEvent e) {
            // sync spinner with scroll bar value
            final int value = scrollBar.getValue();
            spinner.setValue(value);
        }

        // -- ChangeListener methods --

        @Override
        public void stateChanged(final ChangeEvent e) {
            final Object source = e.getSource();
            if (source == slider) {
                // sync spinner with slider value
                final int value = slider.getValue();
                spinner.setValue(value);
            } else if (source == spinner) {
                // sync slider and/or scroll bar with spinner value
                syncSliders();
            }
        }

        // -- Helper methods --
        /**
         * Limit component width to a certain maximum. This is a HACK to work around an issue with Double-based spinners
         * that attempt to size themselves very large (presumably to match Double.MAX_VALUE).
         */
        private void limitWidth(final int maxWidth) {
            final Dimension minSize = spinner.getMinimumSize();
            if (minSize.width > maxWidth) {
                minSize.width = maxWidth;
                spinner.setMinimumSize(minSize);
            }
            final Dimension prefSize = spinner.getPreferredSize();
            if (prefSize.width > maxWidth) {
                prefSize.width = maxWidth;
                spinner.setPreferredSize(prefSize);
            }
        }

        /** Improves behavior of the {@link JSpinner} widget. */
        private void fixSpinner(final Class<?> type) {
            fixSpinnerType(type);
        }

        /**
         * Fixes spinners that display {@link BigDecimal} or {@link BigInteger} values. This is a HACK to work around
         * the fact that {@link DecimalFormat#parse(String, ParsePosition)} uses {@link Double} and/or {@link Long} by
         * default, hence losing precision.
         */
        private void fixSpinnerType(final Class<?> type) {
            if (!BigDecimal.class.isAssignableFrom(type) && !BigInteger.class.isAssignableFrom(type)) {
                return;
            }
            final JComponent editor = spinner.getEditor();
            final JSpinner.NumberEditor numberEditor = (JSpinner.NumberEditor)editor;
            final DecimalFormat decimalFormat = numberEditor.getFormat();
            decimalFormat.setParseBigDecimal(true);
        }

        /** Sets slider values to match the spinner. */
        private void syncSliders() {
            if (slider != null) {
                // clamp value within slider bounds
                int value = modelValue().intValue();
                if (value < slider.getMinimum())
                    value = slider.getMinimum();
                else if (value > slider.getMaximum())
                    value = slider.getMaximum();
                slider.removeChangeListener(this);
                slider.setValue(value);
                slider.addChangeListener(this);
            }
            if (scrollBar != null) {
                // clamp value within scroll bar bounds
                int value = modelValue().intValue();
                if (value < scrollBar.getMinimum())
                    value = scrollBar.getMinimum();
                else if (value > scrollBar.getMaximum())
                    value = scrollBar.getMaximum();
                scrollBar.removeAdjustmentListener(this);
                scrollBar.setValue(modelValue().intValue());
                scrollBar.addAdjustmentListener(this);
            }
        }

        private Number modelValue() {
            if (spinner == null) {
                return null;
            }
            return (Number)spinner.getValue();
        }

        @Override
        public void saveTo(MemberWriteInstance<Number> instance) throws InvalidSettingsException {
            instance.set(modelValue());
        }

        @Override
        public void loadFrom(MemberReadInstance<Number> instance, PortObjectSpec[] spec)
            throws InvalidSettingsException {
            Number value = instance.get();

            String oldFormat = findDecimalFormat((String)SwingWidgets.stepSize(this),
                (String)SwingWidgets.maximum(this), (String)SwingWidgets.minimum(this));
            String newFormat = findDecimalFormat(value);

            spinner.setEditor(
                new JSpinner.NumberEditor(spinner, oldFormat.length() > newFormat.length() ? oldFormat : newFormat));

            // Only set the value if it is in bounds, hence ignore defaults that are not in bounds
            if (value.doubleValue() >= getMin().doubleValue() && value.doubleValue() <= getMax().doubleValue()) {
                spinner.setValue(value);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            spinner.setEnabled(enabled);
        }
    }

    class SpinnerNumberModelFactory {

        public SpinnerNumberModel createModel(final Number value, final Number min, final Number max,
            final Number stepSize) {
            final Class<?> c = value.getClass();
            if (BigInteger.class.isAssignableFrom(c)) {
                final BigInteger biValue = (BigInteger)value;
                final BigInteger biMin = (BigInteger)min;
                final BigInteger biMax = (BigInteger)max;
                final BigInteger biStepSize = (BigInteger)stepSize;
                return new SpinnerBigIntegerModel(biValue, biMin, biMax, biStepSize);
            }
            if (BigDecimal.class.isAssignableFrom(c)) {
                final BigDecimal bdValue = (BigDecimal)value;
                final BigDecimal bdMin = (BigDecimal)min;
                final BigDecimal bdMax = (BigDecimal)max;
                final BigDecimal bdStepSize = (BigDecimal)stepSize;
                return new SpinnerBigDecimalModel(bdValue, bdMin, bdMax, bdStepSize);
            }

            @SuppressWarnings("unchecked")
            final Comparable<Number> cMin = (Comparable<Number>)min;
            @SuppressWarnings("unchecked")
            final Comparable<Number> cMax = (Comparable<Number>)max;

            Number res = value;
            final Comparable<Number> cValue = (Comparable<Number>)value;
            if (min != null && cValue.compareTo(min) < 0)
                res = min;
            if (max != null && cValue.compareTo(max) > 0)
                res = max;

            return new SpinnerNumberModel(res, cMin, cMax, stepSize);
        }

        class SpinnerBigIntegerModel extends SpinnerTypedNumberModel<BigInteger> {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public SpinnerBigIntegerModel(final BigInteger value, final Comparable<BigInteger> min,
                final Comparable<BigInteger> max, final BigInteger stepSize) {
                super(BigInteger.class, value, min, max, stepSize);
            }

            // -- SpinnerTypedNumberModel methods --

            @Override
            protected BigInteger stepUp() {
                return getValue().add(getStepSize());
            }

            @Override
            protected BigInteger stepDown() {
                return getValue().subtract(getStepSize());
            }
        }
    }

    class SpinnerBigDecimalModel extends SpinnerTypedNumberModel<BigDecimal> {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public SpinnerBigDecimalModel(final BigDecimal value, final Comparable<BigDecimal> min,
            final Comparable<BigDecimal> max, final BigDecimal stepSize) {
            super(BigDecimal.class, value, min, max, stepSize);
        }

        // -- SpinnerTypedNumberModel methods --

        @Override
        protected BigDecimal stepUp() {
            return getValue().add(getStepSize());
        }

        @Override
        protected BigDecimal stepDown() {
            return getValue().subtract(getStepSize());
        }
    }

    abstract class SpinnerTypedNumberModel<T extends Number> extends SpinnerNumberModel {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        private final Class<T> m_type;

        private T m_value;

        private Comparable<T> m_min;

        private Comparable<T> m_max;

        private T m_stepSize;

        SpinnerTypedNumberModel(final Class<T> type, final T value, final Comparable<T> min, final Comparable<T> max,
            final T stepSize) {
            super(value, min, max, stepSize);
            m_type = type;
            m_value = value;
            m_min = min;
            m_max = max;
            m_stepSize = stepSize;
        }

        // -- SpinnerTypedNumberModel methods --

        protected abstract T stepUp();

        protected abstract T stepDown();

        // -- SpinnerNumberModel methods --

        @Override
        public Comparable<T> getMaximum() {
            return m_max;
        }

        @Override
        public Comparable<T> getMinimum() {
            return m_min;
        }

        @Override
        public T getNextValue() {
            final T newValue = stepUp();
            if (m_max != null && m_max.compareTo(newValue) < 0)
                return null;
            return newValue;
        }

        @Override
        public T getNumber() {
            return m_value;
        }

        @Override
        public T getPreviousValue() {
            final T newValue = stepDown();
            if (m_min != null && m_min.compareTo(newValue) > 0)
                return null;
            return newValue;
        }

        @Override
        public T getStepSize() {
            return m_stepSize;
        }

        @Override
        public T getValue() {
            return m_value;
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void setMaximum(final Comparable maximum) {
            m_max = maximum;
            super.setMaximum(maximum);
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void setMinimum(final Comparable minimum) {
            m_min = minimum;
            super.setMinimum(minimum);
        }

        @Override
        public void setStepSize(final Number stepSize) {
            if (stepSize == null || !m_type.isInstance(stepSize)) {
                throw new IllegalArgumentException("illegal value");
            }
            @SuppressWarnings("unchecked")
            final T typedStepSize = (T)stepSize;
            this.m_stepSize = typedStepSize;
            super.setStepSize(stepSize);
        }

        @Override
        public void setValue(final Object value) {
            if (value == null || !m_type.isInstance(value)) {
                throw new IllegalArgumentException("illegal value");
            }
            @SuppressWarnings("unchecked")
            final T typedValue = (T)value;
            this.m_value = typedValue;
            super.setValue(value);
        }
    }
}
