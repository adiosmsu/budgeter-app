package ru.adios.budgeter;

import ru.adios.budgeter.inmemrepo.InnerMemoryAccounter;
import ru.adios.budgeter.inmemrepo.Schema;

/**
 * Created by Michail Kulikov
 * 9/24/15
 */
public final class Constants {

    public static final RatesDelegatingBackgroundService CURRENCIES_EXCHANGE_SERVICE =
            new RatesDelegatingBackgroundService(
                    new CurrenciesExchangeService(
                            new TransactionalSupport() {
                                @Override
                                public void runWithTransaction(Runnable runnable) {
                                    runnable.run();
                                }
                            },
                            Schema.CURRENCY_RATES,
                            new InnerMemoryAccounter(),
                            Schema.TREASURY,
                            ExchangeRatesLoader.createBtcLoader(Schema.TREASURY),
                            ExchangeRatesLoader.createCbrLoader(Schema.TREASURY)
                    )
            );

    private Constants() {}

}
