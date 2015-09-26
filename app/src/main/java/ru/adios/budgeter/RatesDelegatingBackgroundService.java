package ru.adios.budgeter;

import android.os.AsyncTask;

import com.google.common.collect.ImmutableList;

import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import java8.util.Optional;
import ru.adios.budgeter.api.CurrencyRatesRepository;
import ru.adios.budgeter.api.UtcDay;

/**
 * Designed for usage from UI thread.
 *
 * Created by Michail Kulikov
 * 9/26/15
 */
@NotThreadSafe
public final class RatesDelegatingBackgroundService implements CurrencyRatesRepository {

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

    public void addCallback(Callback callback) {
        callbacksSet.add(callback);
    }

    public void removeCallback(Callback callback) {
        callbacksSet.remove(callback);
    }

    public void runWithTransaction(final ImmutableList<Runnable> runnables) {
        exchangeService.runWithTransaction(runnables);
    }

    @Override
    public boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return exchangeService.addTodayRate(from, to, rate);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierBidirectional(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        try {
            return getConversionMultiplierBidirectionalTask(day, from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierBidirectional task exception", e);
            return Optional.empty();
        }
    }

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
    public Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        try {
            return getConversionMultiplierWithIntermediateTask(day, from, to, intermediate, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierWithIntermediate task exception", e);
            return Optional.empty();
        }
    }

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
    public Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
        try {
            return getLatestOptionalConversionMultiplierBidirectionalTask(from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getLatestOptionalConversionMultiplierBidirectional task exception", e);
            return Optional.empty();
        }
    }

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
    public BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        try {
            return getLatestConversionMultiplierWithIntermediateTask(from, to, intermediate, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getLatestConversionMultiplierWithIntermediate task exception", e);
            return null;
        }
    }

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

    public void processAllPostponedEvents() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                exchangeService.processAllPostponedEvents();
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
    public boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return exchangeService.addRate(dayUtc, from, to, rate);
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        try {
            return getLatestOptionalConversionMultiplierTask(from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getLatestOptionalConversionMultiplier task exception", e);
            return null;
        }
    }

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
    public boolean isRateStale(CurrencyUnit to) {
        return exchangeService.isRateStale(to);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        try {
            return getConversionMultiplierTask(day, from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplier task exception", e);
            return null;
        }
    }

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
    public Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        try {
            return getConversionMultiplierStraightTask(day, from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierStraight task exception", e);
            return null;
        }
    }

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
    public BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        try {
            return getLatestConversionMultiplierTask(from, to, false).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("getConversionMultiplierStraight task exception", e);
            return null;
        }
    }

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
