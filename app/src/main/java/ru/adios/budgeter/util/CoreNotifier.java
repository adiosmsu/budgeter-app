package ru.adios.budgeter.util;

import android.support.annotation.IdRes;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;
import org.joda.money.IllegalCurrencyException;

import java.math.BigDecimal;

import ru.adios.budgeter.CoreElementActivity;

/**
 * Class to link view's listeners to logic core object.
 *
 * Created by adios on 30.09.15.
 */
public final class CoreNotifier {

    public interface Linker {}

    public interface DecimalLinker extends Linker {

        void link(BigDecimal data);

    }

    public interface CurrencyLinker extends Linker {

        void link(CurrencyUnit data);

    }

    public interface TextLinker extends Linker {

        void link(String data);

    }

    /**
     * For initializing activity.
     * @param activity element activity
     * @param view field view
     * @param linker linker callback
     */
    public static void addLink(final CoreElementActivity activity, View view, final Linker linker) {
        if (view instanceof TextView) {
            ((TextView) view).addTextChangedListener(new EmptyTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (activity.isFeedbackCommencing())
                        return;

                    final int l = s.length();
                    if (l > 0) {
                        final char[] dest = new char[l];
                        s.getChars(0, l, dest, 0);
                        linkViewValueWithCore(new String(dest), linker, activity);
                    }
                }
            });
        } else if (view instanceof Spinner) {
            ((Spinner) view).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    linkViewValueWithCore(parent.getItemAtPosition(position).toString(), linker, activity);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private static void linkViewValueWithCore(String decStr, Linker linker, CoreElementActivity activity) {
        if (linker instanceof DecimalLinker) {
            if (decStr.length() > 0) {
                try {
                    ((DecimalLinker) linker).link(new BigDecimal(decStr));
                    activity.coreFeedback();
                } catch (NumberFormatException ignore) {
                }
            }
        } else if (linker instanceof CurrencyLinker) {
            if (decStr.length() > 0) {
                try {
                    ((CurrencyLinker) linker).link(CurrencyUnit.of(decStr.toUpperCase()));
                    activity.coreFeedback();
                } catch (IllegalCurrencyException ignore) {
                }
            }
        } else if (linker instanceof TextLinker) {
            ((TextLinker) linker).link(decStr);
            activity.coreFeedback();
        }
    }

    /**
     * For fully inflated activity.
     * @param activity element activity
     * @param viewId field view id
     * @param linker linker callback
     */
    public static void addLink(CoreElementActivity activity, @IdRes int viewId, Linker linker) {
        addLink(activity, activity.findViewById(viewId), linker);
    }

}
