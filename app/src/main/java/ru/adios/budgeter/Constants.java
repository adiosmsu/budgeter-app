package ru.adios.budgeter;

import android.os.Handler;

import com.google.common.collect.ImmutableMap;

import org.joda.money.CurrencyUnit;

import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.InnerMemoryAccounter;
import ru.adios.budgeter.inmemrepo.Schema;

/**
 * Created by Michail Kulikov
 * 9/24/15
 */
public final class Constants {

    public static final Handler MAIN_HANDLER = new Handler();

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

    public static final Accounter ACCOUNTER = new InnerMemoryAccounter();

    public static final String[] CURRENCIES_DROPDOWN = new String[] {
            Units.RUB.getCode(),
            CurrencyUnit.USD.getCode(),
            CurrencyUnit.EUR.getCode(),
            Units.BTC.getCode()
    };
    private static final ImmutableMap<String, Integer> CUR_DROP_POSITIONS;
    static {
        final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (int i = 0; i < CURRENCIES_DROPDOWN.length; i++) {
            final String str = CURRENCIES_DROPDOWN[i];
            builder.put(str, i);
        }
        CUR_DROP_POSITIONS = builder.build();
    }

    public static int getCurrencyDropdownPosition(CurrencyUnit unit) {
        final Integer pos = CUR_DROP_POSITIONS.get(unit.getCode());
        if (pos == null) {
            throw new IllegalArgumentException("Unit " + unit + " is not from currencies dropdown list");
        }
        return pos;
    }

    private Constants() {}

}
