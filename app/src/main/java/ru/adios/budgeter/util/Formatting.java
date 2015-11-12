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

package ru.adios.budgeter.util;

import android.content.res.Resources;

import org.joda.money.Money;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.TemporalAccessor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

import java8.util.Optional;
import ru.adios.budgeter.DateTimeUtils;
import ru.adios.budgeter.api.UtcDay;

/**
 * Created by Michail Kulikov
 * 9/25/15
 */
@Immutable
public final class Formatting {

    private static final DateTimeFormatter DATE_TIME_RUS_SHORT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static final DateTimeFormatter DATE_RUS_SHORT = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.####");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^(?!0[0-9])[0-9]*\\.?[0-9]*$");

    public static boolean isDecimal(String str) {
        return str.length() > 0 && DECIMAL_PATTERN.matcher(str).matches();
    }

    public static String toStringRusDateTimeShort(TemporalAccessor dateTime) {
        return DATE_TIME_RUS_SHORT.format(dateTime);
    }

    public static String toStringRusDay(UtcDay day) {
        return DATE_RUS_SHORT.format(DateTimeUtils.convertToCurrentZone(day.inner));
    }

    public static String toStringExchangeRate(BigDecimal rate) {
        return DECIMAL_FORMAT.format(rate);
    }

    public static String toStringMoneyUsingText(Money money) {
        return money.toString();
    }

    public static String toStringMoneyUsingSign(Money money, Resources resources) {
        final String formatted;
        final String amountStr = money.getAmount().toPlainString();

        final Optional<CurrencySymbols.SymInfo> infoRef = CurrencySymbols.getSymbolicInfo(money.getCurrencyUnit(), resources);

        if (infoRef.isPresent()) {
            final CurrencySymbols.SymInfo symInfo = infoRef.get();
            final StringBuilder sb = new StringBuilder(amountStr.length() + symInfo.sym.length() + 1);
            if (symInfo.afterAmount) {
                sb.append(amountStr).append(symInfo.sym);
            } else {
                sb.append(symInfo.sym).append(amountStr);
            }
            formatted = sb.toString();
        } else {
            formatted = amountStr;
        }

        return formatted;
    }

    private Formatting() {}

}
