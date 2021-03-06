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
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;
import org.joda.money.IllegalCurrencyException;

import java.math.BigDecimal;

import javax.annotation.concurrent.Immutable;

import ru.adios.budgeter.util.EmptyTextWatcher;
import ru.adios.budgeter.widgets.DateEditView;
import ru.adios.budgeter.widgets.RefreshingSpinner;
import ru.adios.budgeter.widgets.TimeEditView;

/**
 * Class to link view's listeners to logic core object.
 *
 * Created by adios on 30.09.15.
 */
@Immutable
public final class CoreNotifier {

    public interface Linker {}

    public interface DecimalLinker extends Linker {

        boolean link(BigDecimal data);

    }

    public interface CurrencyLinker extends Linker {

        boolean link(CurrencyUnit data);

    }

    public interface TextLinker extends Linker {

        boolean link(String data);

    }

    public interface NumberLinker extends Linker {

        boolean link(Number data);

    }

    public interface ArbitraryLinker extends Linker {

        boolean link(Object data);

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
                            if (((ArbitraryLinker) linker).link(dateEditView.formatText(s))) {
                                activity.coreFeedback();
                            }
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
                            if (((ArbitraryLinker) linker).link(timeEditView.formatText(s))) {
                                activity.coreFeedback();
                            }
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

                    linkViewValueWithCore(s, linker, activity);
                    view.invalidate();
                }
            });
        } else if (view instanceof AdapterView) {
            final AdapterView sp = (AdapterView) view;
            final boolean isRefSpinner = sp instanceof RefreshingSpinner;
            final AdapterView.OnItemSelectedListener oldListener = isRefSpinner ? ((RefreshingSpinner) sp).getSelectionListener() : sp.getOnItemSelectedListener();
            final AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (activity.isFeedbackCommencing())
                        return;

                    if (oldListener != null) {
                        oldListener.onItemSelected(parent, view, position, id);
                    }

                    final Adapter adapter = sp.getAdapter();
                    if (adapter.getCount() > position) {
                        linkViewValueWithCore(parent.getItemAtPosition(position), linker, activity);
                        sp.invalidate();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };
            if (isRefSpinner) {
                ((RefreshingSpinner) sp).setSelectionListener(listener);
            } else {
                sp.setOnItemSelectedListener(listener);
            }
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

    public static void linkViewValueWithCore(Object o, Linker linker, CoreElementActivity activity) {
        boolean fieldChanged = false;
        if (linker instanceof DecimalLinker) {
            final String decStr = getStringFromObject(o);
            if (decStr.length() > 0) {
                try {
                    fieldChanged = ((DecimalLinker) linker).link(new BigDecimal(decStr));
                } catch (NumberFormatException ignore) {}
            } else {
                fieldChanged = ((DecimalLinker) linker).link(null);
            }
        } else if (linker instanceof CurrencyLinker) {
            if (o instanceof CurrencyUnit) {
                fieldChanged = ((CurrencyLinker) linker).link((CurrencyUnit) o);
            } else {
                if (o == null) {
                    fieldChanged = ((CurrencyLinker) linker).link(null);
                } else {
                    final String decStr = getStringFromObject(o);
                    if (decStr.length() > 0) {
                        try {
                            fieldChanged = ((CurrencyLinker) linker).link(CurrencyUnit.of(decStr.toUpperCase()));
                        } catch (IllegalCurrencyException ignore) {
                        }
                    } else {
                        fieldChanged = ((CurrencyLinker) linker).link(null);
                    }
                }
            }
        } else if (linker instanceof TextLinker) {
            fieldChanged = ((TextLinker) linker).link(getStringFromObject(o));
        } else if (linker instanceof NumberLinker) {
            final NumberLinker nl = (NumberLinker) linker;
            if (o instanceof Number) {
                fieldChanged = nl.link((Number) o);
            } else {
                final String str = getStringFromObject(o);
                if (str.length() > 0) {
                    try {
                        fieldChanged = nl.link(Long.valueOf(str));
                    } catch (NumberFormatException ignore) {}
                }
            }
        } else if (linker instanceof ArbitraryLinker) {
            fieldChanged = ((ArbitraryLinker) linker).link(o);
        }

        if (fieldChanged) {
            activity.coreFeedback();
        }
    }

    private static String getStringFromObject(Object o) {
        return o == null ? "" : (o instanceof String ? (String) o : o.toString());
    }

}
