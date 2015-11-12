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

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java8.util.Optional;
import ru.adios.budgeter.BalanceElementCore;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.Submitter;
import ru.adios.budgeter.api.BudgeterApiException;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.util.Formatting;

/**
 * Created by Michail Kulikov
 * 9/29/15
 */
@Immutable
public final class CoreUtils {

    public static Money getTotalBalance(BalanceElementCore balanceElement, @Nullable Logger logger) {
        Money totalBalance;
        try {
            totalBalance = balanceElement.getTotalBalance();
        } catch (BudgeterApiException ex) {
            if (logger != null) {
                logger.error("Error fetching total balance", ex);
            }
            totalBalance = Money.zero(Units.RUB);
        }
        return totalBalance;
    }

    public static Money addToTotalBalance(@Nonnull Money totalBalance, @Nonnull Money money, @Nonnull final Context context, boolean fromUiThread) {
        final CurrencyUnit moneyUnit = money.getCurrencyUnit();
        final CurrencyUnit totalUnit = totalBalance.getCurrencyUnit();

        if (!moneyUnit.equals(totalUnit)) {
            final Optional<BigDecimal> conversionMultiplier = fromUiThread
                    ? Constants.CURRENCIES_EXCHANGE_SERVICE.getConversionMultiplier(new UtcDay(), moneyUnit, totalUnit)
                    : Constants.CURRENCIES_EXCHANGE_SERVICE.getConversionMultiplierInThisThread(new UtcDay(), moneyUnit, totalUnit);

            if (conversionMultiplier.isPresent()) {
                money = money.convertedTo(totalUnit, conversionMultiplier.get(), RoundingMode.HALF_DOWN);
            } else {
                if (fromUiThread) {
                    makeConversionToast(context, moneyUnit, totalUnit);
                } else {
                    Constants.MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            makeConversionToast(context, moneyUnit, totalUnit);
                        }
                    });
                }
                return totalBalance;
            }
        }

        return totalBalance.plus(money);
    }

    public static String getExtendedAccountString(BalanceAccount account, Resources resources) {
        final Optional<Money> balance = account.getBalance();
        return account.name
                + " ("
                + (balance.isPresent() ? Formatting.toStringMoneyUsingSign(balance.get(), resources) : account.getUnit().toString())
                + ')';
    }

    private static void makeConversionToast(@Nonnull Context context, CurrencyUnit moneyUnit, CurrencyUnit totalUnit) {
        Toast.makeText(context, "Unable to get currencies conversion rate from " + moneyUnit + " to " + totalUnit + " for today!", Toast.LENGTH_LONG).show();
    }

    public static void doSubmitAndStore(Submitter core) {
        core.setTransactional(BundleProvider.getBundle().getTransactionalSupport());
        core.submitAndStoreResult();
        core.setTransactional(null);
    }

    private CoreUtils() {
    }

}