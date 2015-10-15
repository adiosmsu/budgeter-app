package ru.adios.budgeter.util;

import android.content.Context;

import java.util.List;

import javax.annotation.Nullable;

/**
 * RequestingAutoCompleteAdapter that supports decorating request text.
 *
 * Created by Michail Kulikov
 * 10/15/15
 */
public final class ModedRequestingAutoCompleteAdapter<T> extends RequestingAutoCompleteAdapter<T> {

    private static final String PERCENT = "%";
    private static final String EMPTY_STR = "";

    public static final RequestDecorator SQL_LIKE_STARTS_WITH_DECORATOR = new StaticRequestDecorator(EMPTY_STR, PERCENT);
    public static final RequestDecorator SQL_LIKE_ENDS_WITH_DECORATOR = new StaticRequestDecorator(PERCENT, EMPTY_STR);
    public static final RequestDecorator SQL_ILIKE_DECORATOR = new StaticRequestDecorator(PERCENT, PERCENT);

    public interface Requester<T> {

        List<T> doActualRequest(String constraint);

    }

    public static final class StaticRequestDecorator extends SimpleRequestDecorator {

        public final String prefix;
        public final String postfix;

        public StaticRequestDecorator(String prefix, String postfix) {
            this.prefix = prefix;
            this.postfix = postfix;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getPostfix() {
            return postfix;
        }

    }

    public abstract static class SimpleRequestDecorator implements RequestDecorator {

        @Override
        public final String transformRequest(String requestText) {
            return requestText;
        }

    }

    public interface RequestDecorator {

        String getPrefix();

        String getPostfix();

        String transformRequest(String requestText);

    }


    private final Requester<T> requester;
    @Nullable
    private final RequestDecorator decorator;

    public ModedRequestingAutoCompleteAdapter(Context context, Requester<T> requester) {
        super(context);
        this.requester = requester;
        decorator = null;
    }

    public ModedRequestingAutoCompleteAdapter(Context context, Requester<T> requester, StringPresenter<T> presenter) {
        super(context, presenter);
        this.requester = requester;
        decorator = null;
    }

    public ModedRequestingAutoCompleteAdapter(Context context, Requester<T> requester, @Nullable RequestDecorator decorator) {
        super(context);
        this.requester = requester;
        this.decorator = decorator;
    }

    public ModedRequestingAutoCompleteAdapter(Context context, Requester<T> requester, StringPresenter<T> presenter, @Nullable RequestDecorator decorator) {
        super(context, presenter);
        this.requester = requester;
        this.decorator = decorator;
    }

    @Override
    protected List<T> doRequest(String constraint) {
        return requester.doActualRequest(transform(constraint));
    }

    private String transform(String constraint) {
        return decorator == null ? constraint : decorator.getPrefix() + decorator.transformRequest(constraint) + decorator.getPostfix();
    }

}
