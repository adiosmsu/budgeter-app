package ru.adios.budgeter.util;

import android.support.annotation.IdRes;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioGroup;
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

    public interface HintedLinker extends Linker {

        void link(HintedArrayAdapter.ObjectContainer data);

    }

    public interface NumberLinker extends Linker {

        void link(Number data);

    }

    public interface ArbitraryLinker extends Linker {

        void link(Object data);

    }

    /**
     * For initializing activity.
     * @param activity element activity
     * @param view field view
     * @param linker linker callback
     */
    public static void addLink(final CoreElementActivity activity, final View view, final Linker linker) {
        if (view instanceof DateEditView) {
            final DateEditView dateEditView = (DateEditView) view;

            dateEditView.addTextChangedListener(new EmptyTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (activity.isFeedbackCommencing()) {
                        return;
                    }

                    if (s != null && s.length() > 0) {
                        if (linker instanceof ArbitraryLinker) {
                            ((ArbitraryLinker) linker).link(dateEditView.formatText(s));
                            activity.coreFeedback();
                        } else {
                            linkViewValueWithCore(s.toString(), linker, activity);
                        }
                        dateEditView.invalidate();
                    }
                }
            });
        } else if (view instanceof TimeEditView) {
            final TimeEditView timeEditView = (TimeEditView) view;

            timeEditView.addTextChangedListener(new EmptyTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (activity.isFeedbackCommencing()) {
                        return;
                    }

                    if (s != null && s.length() > 0) {
                        if (linker instanceof ArbitraryLinker) {
                            ((ArbitraryLinker) linker).link(timeEditView.formatText(s));
                            activity.coreFeedback();
                        } else {
                            linkViewValueWithCore(s.toString(), linker, activity);
                        }
                        timeEditView.invalidate();
                    }
                }
            });
        } else if (view instanceof TextView) {
            ((TextView) view).addTextChangedListener(new EmptyTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (activity.isFeedbackCommencing())
                        return;

                    if (s != null && s.length() > 0) {
                        linkViewValueWithCore(s.toString(), linker, activity);
                        view.invalidate();
                    }
                }
            });
        } else if (view instanceof Spinner) {
            final Spinner sp = (Spinner) view;
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (activity.isFeedbackCommencing())
                        return;

                    if (sp.getAdapter().getCount() > position) {
                        linkViewValueWithCore(parent.getItemAtPosition(position), linker, activity);
                        sp.invalidate();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else if (view instanceof RadioGroup) {
            final RadioGroup radioGroup = (RadioGroup) view;
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                    if (activity.isFeedbackCommencing())
                        return;

                    for (int i = 0; i < group.getChildCount(); i++) {
                        if (group.getChildAt(i).getId() == checkedId) {
                            linkViewValueWithCore(i, linker, activity);
                            radioGroup.invalidate();
                            return;
                        }
                    }
                }
            });
        }
    }

    private static void linkViewValueWithCore(Object o, Linker linker, CoreElementActivity activity) {
        if (linker instanceof DecimalLinker) {
            final String decStr = getStringFromObject(o);
            if (decStr.length() > 0) {
                try {
                    ((DecimalLinker) linker).link(new BigDecimal(decStr));
                    activity.coreFeedback();
                } catch (NumberFormatException ignore) {}
            }
        } else if (linker instanceof CurrencyLinker) {
            if (o instanceof CurrencyUnit) {
                ((CurrencyLinker) linker).link((CurrencyUnit) o);
                activity.coreFeedback();
            } else {
                final String decStr = getStringFromObject(o);
                if (decStr.length() > 0) {
                    try {
                        ((CurrencyLinker) linker).link(CurrencyUnit.of(decStr.toUpperCase()));
                        activity.coreFeedback();
                    } catch (IllegalCurrencyException ignore) {
                    }
                }
            }
        } else if (linker instanceof TextLinker) {
            ((TextLinker) linker).link(getStringFromObject(o));
            activity.coreFeedback();
        } else if (linker instanceof HintedLinker && o instanceof HintedArrayAdapter.ObjectContainer) {
            ((HintedLinker) linker).link((HintedArrayAdapter.ObjectContainer) o);
            activity.coreFeedback();
        } else if (linker instanceof NumberLinker) {
            final NumberLinker nl = (NumberLinker) linker;
            if (o instanceof Number) {
                nl.link((Number) o);
                activity.coreFeedback();
            } else {
                final String str = getStringFromObject(o);
                if (str.length() > 0) {
                    try {
                        nl.link(Long.valueOf(str));
                        activity.coreFeedback();
                    } catch (NumberFormatException ignore) {}
                }
            }
        } else if (linker instanceof ArbitraryLinker) {
            ((ArbitraryLinker) linker).link(o);
        }
    }

    private static String getStringFromObject(Object o) {
        return o instanceof String ? (String) o : o.toString();
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
