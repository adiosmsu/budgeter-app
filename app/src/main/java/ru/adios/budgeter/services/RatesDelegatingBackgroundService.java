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

package ru.adios.budgeter.services;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import java8.util.Optional;
import java8.util.function.Consumer;
import ru.adios.budgeter.CurrenciesExchangeService;
import ru.adios.budgeter.api.CurrencyRatesRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.ConversionRate;

/**
 * Designed for usage from UI thread.
 *
 * Created by Michail Kulikov
 * 9/26/15
 */
public final class RatesDelegatingBackgroundService implements CurrencyRatesRepository {

    @UiThread
    public interface Callback {

        void onGetConversionMultiplierBidirectional(Optional<BigDecimal> result);

        void onGetConversionMultiplierWithIntermediate(Optional<BigDecimal> result);

        void onGetLatestOptionalConversionMultiplierBidirectional(Optional<BigDecimal> result);

        void onGetLatestConversionMultiplierWithIntermediate(@Nullable BigDecimal result);

        void onProcessAllPostponedEvents();

        void onGetLatestOptionalConversionMultiplier(Optional<BigDecimal> result);

        void onGetConversionMultiplier(Optional<BigDecimal> result);

        void onGetConversionMultiplierStraight(Optional<BigDecimal> result);

        void onGetLatestConversionMultiplier(@Nullable BigDecimal result);

    }

    private static final Logger logger = LoggerFactory.getLogger(RatesDelegatingBackgroundService.class);

    private final CurrenciesExchangeService exchangeService;
    private final TreeSet<Callback> callbacksSet = new TreeSet<>();

    public RatesDelegatingBackgroundService(CurrenciesExchangeService delegate) {
        exchangeService = delegate;
    }

    @WorkerThread
    public CurrenciesExchangeService getExchangeService() {
        return exchangeService;
    }

    @UiThread
    public void addCallback(Callback callback) {
        callbacksSet.add(callback);
    }

    @UiThread
    public void removeCallback(Callback callback) {
        callbacksSet.remove(callback);
    }

    @UiThread
    public void runWithTransaction(final ImmutableList<Runnable> runnables) {
        exchangeService.runWithTransaction(runnables);
    }

    @Override
    @WorkerThread
    public Optional<ConversionRate> getById(Long id) {
        return exchangeService.getById(id);
    }

    @Override
    @WorkerThread
    public Long currentSeqValue() {
        return exchangeService.currentSeqValue();
    }

    @Override
    @WorkerThread
    public ImmutableSet<Long> getIndexedForDay(UtcDay day) {
        return exchangeService.getIndexedForDay(day);
    }

    @Override
    @WorkerThread
    public boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return exchangeService.addTodayRate(from, to, rate);
    }

    @Override
    @UiThread
    public Optional<BigDecimal> getConversionMultiplierBidirectional(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        try {
            return getConversionMultiplierBidirectionalTask(day, from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierBidirectional task exception", e);
            return Optional.empty();
        }
    }

    @UiThread
    public void doGetConversionMultiplierBidirectional(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        getConversionMultiplierBidirectionalTask(day, from, to, true).execute();
    }

    private AsyncTask<Void, Void, Optional<BigDecimal>> getConversionMultiplierBidirectionalTask(final UtcDay day,
                                                                                                 final CurrencyUnit from,
                                                                                                 final CurrencyUnit to,
                                                                                                 final boolean useCallbacks) {
        return new AsyncTask<Void, Void, Optional<BigDecimal>>() {
            @Override
            protected Optional<BigDecimal> doInBackground(Void... params) {
                return exchangeService.getConversionMultiplierBidirectional(day, from, to);
            }

            @Override
            protected void onPostExecute(Optional<BigDecimal> result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetConversionMultiplierBidirectional(result);
                    }
                }
            }
        };
    }


    @Override
    @UiThread
    public Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        try {
            return getConversionMultiplierWithIntermediateTask(day, from, to, intermediate, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierWithIntermediate task exception", e);
            return Optional.empty();
        }
    }

    @UiThread
    public void doGetConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        getConversionMultiplierWithIntermediateTask(day, from, to, intermediate, true).execute();
    }

    private AsyncTask<Void, Void, Optional<BigDecimal>> getConversionMultiplierWithIntermediateTask(final UtcDay day,
                                                                                                    final CurrencyUnit from,
                                                                                                    final CurrencyUnit to,
                                                                                                    final CurrencyUnit intermediate,
                                                                                                    final boolean useCallbacks) {
        return new AsyncTask<Void, Void, Optional<BigDecimal>>() {
            @Override
            protected Optional<BigDecimal> doInBackground(Void... params) {
                return exchangeService.getConversionMultiplierWithIntermediate(day, from, to, intermediate);
            }

            @Override
            protected void onPostExecute(Optional<BigDecimal> result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetConversionMultiplierWithIntermediate(result);
                    }
                }
            }
        };
    }

    @Override
    @UiThread
    public Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
        try {
            return getLatestOptionalConversionMultiplierBidirectionalTask(from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getLatestOptionalConversionMultiplierBidirectional task exception", e);
            return Optional.empty();
        }
    }

    @UiThread
    public void doGetLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
        getLatestOptionalConversionMultiplierBidirectionalTask(from, to, true).execute();
    }

    private AsyncTask<Void, Void, Optional<BigDecimal>> getLatestOptionalConversionMultiplierBidirectionalTask(final CurrencyUnit from,
                                                                                                               final CurrencyUnit to,
                                                                                                               final boolean useCallbacks) {
        return new AsyncTask<Void, Void, Optional<BigDecimal>>() {
            @Override
            protected Optional<BigDecimal> doInBackground(Void... params) {
                return exchangeService.getLatestOptionalConversionMultiplierBidirectional(from, to);
            }

            @Override
            protected void onPostExecute(Optional<BigDecimal> result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetLatestOptionalConversionMultiplierBidirectional(result);
                    }
                }
            }
        };
    }

    @Nullable
    @Override
    @UiThread
    public BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        try {
            return getLatestConversionMultiplierWithIntermediateTask(from, to, intermediate, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getLatestConversionMultiplierWithIntermediate task exception", e);
            return null;
        }
    }

    @UiThread
    public void doGetLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        getLatestConversionMultiplierWithIntermediateTask(from, to, intermediate, true).execute();
    }

    private AsyncTask<Void, Void, BigDecimal> getLatestConversionMultiplierWithIntermediateTask(final CurrencyUnit from,
                                                                                                final CurrencyUnit to,
                                                                                                final CurrencyUnit intermediate,
                                                                                                final boolean useCallbacks) {
        return new AsyncTask<Void, Void, BigDecimal>() {
            @Override
            protected BigDecimal doInBackground(Void... params) {
                try {
                    return exchangeService.getLatestConversionMultiplierWithIntermediate(from, to, intermediate);
                } catch (RuntimeException ex) {
                    logger.error("Unable to get latest conversion multiplier with intermediate (" + from + ',' + to + ',' + intermediate + ')', ex);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(@Nullable BigDecimal result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetLatestConversionMultiplierWithIntermediate(result);
                    }
                }
            }
        };
    }

    @UiThread
    public void processAllPostponedEvents() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                exchangeService.processAllPostponedEvents(Optional.<Consumer<Integer>>empty(), Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                for (final Callback c : callbacksSet) {
                    c.onProcessAllPostponedEvents();
                }
            }
        }.execute();
    }

    @Override
    @WorkerThread
    public boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return exchangeService.addRate(dayUtc, from, to, rate);
    }

    @Override
    @UiThread
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        try {
            return getLatestOptionalConversionMultiplierTask(from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getLatestOptionalConversionMultiplier task exception", e);
            return Optional.empty();
        }
    }

    @UiThread
    public void doGetLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        getLatestOptionalConversionMultiplierTask(from, to, true).execute();
    }

    private AsyncTask<Void, Void, Optional<BigDecimal>> getLatestOptionalConversionMultiplierTask(final CurrencyUnit from, final CurrencyUnit to, final boolean useCallbacks) {
        return new AsyncTask<Void, Void, Optional<BigDecimal>>() {
            @Override
            protected Optional<BigDecimal> doInBackground(Void... params) {
                return exchangeService.getLatestOptionalConversionMultiplier(from, to);
            }

            @Override
            protected void onPostExecute(Optional<BigDecimal> result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetLatestOptionalConversionMultiplier(result);
                    }
                }
            }
        };
    }

    @Override
    @WorkerThread
    public boolean isRateStale(CurrencyUnit to) {
        return exchangeService.isRateStale(to);
    }

    @Override
    @UiThread
    public Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        try {
            return getConversionMultiplierTask(day, from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplier task exception", e);
            return Optional.empty();
        }
    }

    @WorkerThread
    public Optional<BigDecimal> getConversionMultiplierInThisThread(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return exchangeService.getConversionMultiplier(day, from, to);
    }

    @UiThread
    public void doGetConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        getConversionMultiplierTask(day, from, to, true).execute();
    }

    private AsyncTask<Void, Void, Optional<BigDecimal>> getConversionMultiplierTask(final UtcDay day, final CurrencyUnit from, final CurrencyUnit to, final boolean useCallbacks) {
        return new AsyncTask<Void, Void, Optional<BigDecimal>>() {
            @Override
            protected Optional<BigDecimal> doInBackground(Void... params) {
                return exchangeService.getConversionMultiplier(day, from, to);
            }

            @Override
            protected void onPostExecute(Optional<BigDecimal> result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetConversionMultiplier(result);
                    }
                }
            }
        };
    }

    @Override
    @UiThread
    public Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        try {
            return getConversionMultiplierStraightTask(day, from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierStraight task exception", e);
            return Optional.empty();
        }
    }

    @UiThread
    public void doGetConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        getConversionMultiplierStraightTask(day, from, to, true).execute();
    }

    private AsyncTask<Void, Void, Optional<BigDecimal>> getConversionMultiplierStraightTask(final UtcDay day, final CurrencyUnit from, final CurrencyUnit to, final boolean useCallbacks) {
        return new AsyncTask<Void, Void, Optional<BigDecimal>>() {
            @Override
            protected Optional<BigDecimal> doInBackground(Void... params) {
                return exchangeService.getConversionMultiplierStraight(day, from, to);
            }

            @Override
            protected void onPostExecute(Optional<BigDecimal> result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetConversionMultiplierStraight(result);
                    }
                }
            }
        };
    }

    @Override
    @Nullable
    @UiThread
    public BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        try {
            return getLatestConversionMultiplierTask(from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierStraight task exception", e);
            return null;
        }
    }

    @UiThread
    public void doGetLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        getLatestConversionMultiplierTask(from, to, true).execute();
    }

    private AsyncTask<Void, Void, BigDecimal> getLatestConversionMultiplierTask(final CurrencyUnit from, final CurrencyUnit to, final boolean useCallbacks) {
        return new AsyncTask<Void, Void, BigDecimal>() {
            @Override
            protected BigDecimal doInBackground(Void... params) {
                try {
                    return exchangeService.getLatestConversionMultiplier(from, to);
                } catch (RuntimeException ex) {
                    logger.error("Unable to get latest conversion multiplier (" + from + ',' + to + ')', ex);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(@Nullable BigDecimal result) {
                if (useCallbacks) {
                    for (final Callback c : callbacksSet) {
                        c.onGetLatestConversionMultiplier(result);
                    }
                }
            }
        };
    }

    @UiThread
    public static abstract class EmptyCallback implements Callback {
        @Override
        public void onGetConversionMultiplierBidirectional(Optional<BigDecimal> result) {}
        @Override
        public void onGetConversionMultiplierWithIntermediate(Optional<BigDecimal> result) {}
        @Override
        public void onGetLatestOptionalConversionMultiplierBidirectional(Optional<BigDecimal> result) {}
        @Override
        public void onGetLatestConversionMultiplierWithIntermediate(@Nullable BigDecimal result) {}
        @Override
        public void onProcessAllPostponedEvents() {}
        @Override
        public void onGetLatestOptionalConversionMultiplier(Optional<BigDecimal> result) {}
        @Override
        public void onGetConversionMultiplier(Optional<BigDecimal> result) {}
        @Override
        public void onGetConversionMultiplierStraight(Optional<BigDecimal> result) {}
        @Override
        public void onGetLatestConversionMultiplier(@Nullable BigDecimal result) {}
    }

}
