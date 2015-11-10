package ru.adios.budgeter.adapters;

import android.content.res.Resources;
import android.support.annotation.UiThread;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.core.CoreUtils;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
@UiThread
public final class BalanceAccountContainer extends CachingHintedContainer<BalanceAccount> {

    public static Factory getFactory(Resources resources) {
        final Factory factory = FACTORY_REF.get();
        if (factory == null) {
            synchronized (BalanceAccountContainer.class) {
                FACTORY_REF.compareAndSet(null, new Factory(resources));
            }
            return FACTORY_REF.get();
        }

        return factory;
    }

    private static final AtomicReference<Factory> FACTORY_REF = new AtomicReference<>(null);


    private final Resources resources;

    public BalanceAccountContainer(BalanceAccount account, Resources resources) {
        super(account);
        this.resources = resources;
    }

    @Nonnull
    protected String calculateToString() {
        final BalanceAccount account = getObject();
        return CoreUtils.getExtendedAccountString(account, resources);
    }

    public static final class Factory implements HintedArrayAdapter.ContainerFactory<BalanceAccount> {

        private final Resources resources;

        private Factory(Resources resources) {
            this.resources = resources;
        }

        @Override
        public HintedArrayAdapter.ObjectContainer<BalanceAccount> create(BalanceAccount account) {
            return new BalanceAccountContainer(account, resources);
        }

    }

}
