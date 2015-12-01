/*
 *
 *  *
 *  *  * Copyright 2015 Michael Kulikov
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package ru.adios.budgeter.core;

import android.support.annotation.IdRes;
import android.text.Editable;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.OffsetTime;

import java.math.BigDecimal;

import javax.annotation.concurrent.Immutable;

import ru.adios.budgeter.Constants;
import ru.adios.budgeter.DateTimeUtils;
import ru.adios.budgeter.adapters.DecoratingAdapter;
import ru.adios.budgeter.adapters.NullableAdapter;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.widgets.DateEditView;
import ru.adios.budgeter.widgets.TimeEditView;

/**
 * Created by Michail Kulikov
 * 11/10/15
 */
@Immutable
public final class Feedbacking {

    public static void textViewFeedback(String text, TextView textView) {
        final CharSequence innerText = textView.getText();
        if (((innerText == null || innerText.length() == 0) && text != null && text.length() > 0)
                || (innerText != null && innerText.length() > 0 && !innerText.toString().equals(text))) {
            if (text == null || text.isEmpty()) {
                if (textView instanceof EditText) {
                    ((EditText)textView).getText().clear();
                } else {
                    textView.setText("");
                }
            } else {
                if (innerText instanceof Editable) {
                    setTextToEditable(text, (Editable) innerText);
                } else {
                    textView.setText(text);
                }
            }

            textView.invalidate();
        }
    }

    public static void decimalTextViewFeedback(BigDecimal decimal, TextView textView) {
        if (decimal == null) {
            if (textView.length() > 0) {
                if (textView instanceof EditText) {
                    ((EditText)textView).getText().clear();
                } else {
                    textView.setText("");
                }
                textView.invalidate();
            }
        } else {
            final String decimalText = decimal.toPlainString();
            final CharSequence text = textView.getText();
            if (!decimalText.equals(text.toString())) {
                if (text instanceof Editable) {
                    setTextToEditable(decimalText, (Editable) text);
                } else {
                    textView.setText(decimalText);
                }
                textView.invalidate();
            }
        }
    }

    private static void setTextToEditable(String textNew, Editable ediText) {
        final int l = ediText.length();
        final int dl = textNew.length();
        if (dl <= l) {
            ediText.replace(0, dl, textNew, 0, dl);
            if (dl != l) {
                ediText.delete(dl, l);
            }
        } else {
            ediText.replace(0, l, textNew, 0, l);
            ediText.append(textNew, l, dl);
        }
    }

    public static void currenciesSpinnerFeedback(CurrencyUnit unit, Spinner spinner) {
        final SpinnerAdapter adapter = spinner.getAdapter();
        if (unit == null) {
            if (adapter instanceof NullableAdapter) {
                ((NullableAdapter) adapter).setNullSelection(spinner);
            }
        } else {
            final Object selectedItem = spinner.getSelectedItem();
            if (selectedItem == null || !selectedItem.toString().equals(unit.getCode())) {
                int pos = Constants.getCurrencyDropdownPosition(unit);
                spinner.setSelection((adapter instanceof DecoratingAdapter) ? ((DecoratingAdapter) adapter).decoratedPositionToDecorators(pos) : pos, true);
                spinner.invalidate();
            }
        }
    }

    public static void nullableArraySpinnerFeedback(Object object, Spinner spinnerView) {
        final SpinnerAdapter adapter = spinnerView.getAdapter();

        boolean extract = false;
        final Object selectedItem = spinnerView.getSelectedItem();
        if (adapter instanceof NullableAdapter) {
            if ((selectedItem == null && object != null) || (selectedItem != null && !selectedItem.equals(object))) {
                if (object == null) {
                    ((NullableAdapter) adapter).setNullSelection(spinnerView); // nothing selected
                } else {
                    extract = true;
                }
            }
        } else if (object != null && !object.equals(selectedItem)) {
            extract = true;
        }

        if (extract) {
            spinnerView.setSelection(extractPosition(adapter, object), true);
        }
    }

    private static int extractPosition(Adapter adapter, Object object) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(object)) {
                return i;
            }
        }
        return -1;
    }

    public static void dateTimeFeedback(OffsetDateTime dateTime, DateEditView dateEditView, TimeEditView timeEditView) {
        if (dateTime != null) {
            dateFeedback(dateTime, dateEditView, true);
            timeFeedback(dateTime.toOffsetTime(), timeEditView);
        }
    }

    public static void dateFeedback(UtcDay day, DateEditView dateEditView) {
        if (day != null) {
            dateFeedback(day.inner, dateEditView, false);
        }
    }

    public static void dateFeedback(OffsetDateTime date, DateEditView dateEditView, boolean cutTime) {
        if (date != null) {
            if (cutTime) {
                date = DateTimeUtils.cutTime(date);
            }

            if (!date.equals(dateEditView.getDate())) {
                dateEditView.setDate(date);
                dateEditView.invalidate();
            }
        }
    }

    public static void timeFeedback(OffsetTime time, TimeEditView timeEditView) {
        if (time != null && !time.equals(timeEditView.getTime())) {
            timeEditView.setTime(time);
            timeEditView.invalidate();
        }
    }

    public static void radioGroupFeedback(Enum e, RadioGroup radioGroup) {
        if (e != null) {
            final int ord = e.ordinal();

            @IdRes
            final int checked = radioGroup.getCheckedRadioButtonId();
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                @IdRes
                final int id = radioGroup.getChildAt(i).getId();

                if (id == checked && i == ord) {
                    return;
                }

                if (i == ord) {
                    radioGroup.check(id);
                    radioGroup.invalidate();
                    return;
                }
            }
        }
    }


    private Feedbacking() {}

}
