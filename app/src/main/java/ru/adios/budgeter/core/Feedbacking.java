package ru.adios.budgeter.core;

import android.support.annotation.IdRes;
import android.text.Editable;
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
import ru.adios.budgeter.adapters.HintedArrayAdapter;
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
        if (unit == null) {
            final SpinnerAdapter adapter = spinner.getAdapter();
            if (adapter instanceof HintedArrayAdapter) {
                spinner.setSelection(adapter.getCount());
                spinner.invalidate();
            }
        } else if (!spinner.getSelectedItem().toString().equals(unit.getCode())) {
            spinner.setSelection(Constants.getCurrencyDropdownPosition(unit), true);
            spinner.invalidate();
        }
    }

    public static <T> void hintedArraySpinnerFeedback(T object, Spinner spinnerView) {
        final SpinnerAdapter adapter = spinnerView.getAdapter();

        int pos = -1;
        final Object selectedItem = spinnerView.getSelectedItem();
        if (adapter instanceof HintedArrayAdapter) {
            Object innerItem;
            if (selectedItem == null || (innerItem = ((HintedArrayAdapter.ObjectContainer) selectedItem).getObject()) == null || !innerItem.equals(object)) {
                @SuppressWarnings("unchecked")
                final HintedArrayAdapter<T> hintedArrayAdapter = (HintedArrayAdapter) adapter;
                if (object == null) {
                    spinnerView.setSelection(adapter.getCount()); // nothing selected => display hint
                } else {
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (hintedArrayAdapter.getItem(i).getObject().equals(object)) {
                            pos = i;
                            break;
                        }
                    }
                }
            }
        } else if (object != null && !object.equals(selectedItem)) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(object)) {
                    pos = i;
                    break;
                }
            }
        }

        if (pos >= 0) {
            spinnerView.setSelection(pos, true);
            spinnerView.invalidate();
        }
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
