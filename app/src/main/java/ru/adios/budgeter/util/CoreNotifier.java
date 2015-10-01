package ru.adios.budgeter.util;

import android.support.annotation.IdRes;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;
import org.joda.money.IllegalCurrencyException;

import java.math.BigDecimal;

/**
 * Class to link view's listeners to logic core object.
 *
 * Created by adios on 30.09.15.
 */
public final class CoreNotifier {

    private interface Linker {}

    public interface DecimalLinker extends Linker {

        void link(BigDecimal data);

    }

    public interface CurrencyLinker extends Linker {

        void link(CurrencyUnit data);

    }

    public interface TextLinker extends Linker {

        void link(String data);

    }

    public static void addLink(View view, final Linker linker) {
        if (view instanceof TextView) {
            final TextView tv = (TextView) view;
            tv.addTextChangedListener(new EmptyTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    final int l = s.length();
                    if (l > 0) {
                        final char[] dest = new char[l];
                        s.getChars(0, l, dest, 0);
                        final String decStr = new String(dest);

                        if (linker instanceof DecimalLinker) {
                            if (decStr.length() > 0) {
                                try {
                                    ((DecimalLinker) linker).link(new BigDecimal(decStr));
                                } catch (NumberFormatException ignore) {}
                            }
                        } else if (linker instanceof CurrencyLinker) {
                            if (decStr.length() > 0) {
                                try {
                                    ((CurrencyLinker) linker).link(CurrencyUnit.of(decStr.toUpperCase()));
                                } catch (IllegalCurrencyException ignore) {}
                            }
                        } else if (linker instanceof TextLinker) {
                            ((TextLinker) linker).link(decStr);
                        }
                    }
                }
            });
        }
    }

    public static void addLink(FragmentActivity activity, @IdRes int viewId, Linker linker) {
        addLink(activity.findViewById(viewId), linker);
    }

}
