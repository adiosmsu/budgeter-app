package ru.adios.budgeter.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.UiThread;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import ru.adios.budgeter.util.GeneralUtils;

/**
 * Adapter for Spinner to display a hint.
 *
 * Created by Michail Kulikov
 * 10/8/15
 */
@UiThread
public final class HintedArrayAdapter<T> extends ArrayAdapter<HintedArrayAdapter.ObjectContainer<T>> {

    public static void adaptStringSpinner(Spinner spinner, Context context, String[] values) {
        final ObjectContainer<String>[] objectContainers = switchArrayAndApplyHint(values, spinner.getPrompt().toString());
        final HintedArrayAdapter<String> adapter = new HintedArrayAdapter<>(context, android.R.layout.simple_spinner_item, objectContainers);
        finishAdapting(spinner, adapter);
    }

    public static void adaptStringSpinner(Spinner spinner, Context context, List<String> values) {
        final List<ObjectContainer<String>> objectContainers = switchListAndApplyHint(values, spinner.getPrompt().toString());
        final HintedArrayAdapter<String> adapter = new HintedArrayAdapter<>(context, android.R.layout.simple_spinner_item, objectContainers);
        finishAdapting(spinner, adapter);
    }

    public static <Param> void adaptArbitrarySpinner(Spinner spinner, Context context, Stream<Param> stream) {
        final List<ObjectContainer<Param>> collected = stream.map(new Function<Param, ObjectContainer<Param>>() {
            @Override
            public ObjectContainer<Param> apply(Param param) {
                return new ToStringObjectContainer<>(param);
            }
        }).collect(Collectors.<ObjectContainer<Param>>toList());
        collected.add(new HintContainer<Param>(spinner.getPrompt().toString()));

        final HintedArrayAdapter<Param> adapter = new HintedArrayAdapter<>(context, android.R.layout.simple_spinner_item, collected);
        finishAdapting(spinner, adapter);
    }

    public static <Param> void adaptArbitraryContainedSpinner(Spinner spinner, Context context, List<ObjectContainer<Param>> containers) {
        containers.add(new HintContainer<Param>(spinner.getPrompt().toString()));
        final HintedArrayAdapter<Param> adapter = new HintedArrayAdapter<>(context, android.R.layout.simple_spinner_item, containers);
        finishAdapting(spinner, adapter);
    }

    public static <Param> void adaptArbitrarySpinner(Spinner spinner, Context context, List<Param> values) {
        final HintedArrayAdapter<Param> adapter =
                new HintedArrayAdapter<>(context, android.R.layout.simple_spinner_item, switchArbitraryListAndApplyHint(values, spinner.getPrompt().toString()));
        finishAdapting(spinner, adapter);
    }

    public static <Param> void adaptArbitrarySpinner(Spinner spinner, Context context, Param[] values) {
        final HintedArrayAdapter<Param> adapter =
                new HintedArrayAdapter<>(context, android.R.layout.simple_spinner_item, switchArbitraryArrayAndApplyHint(values, spinner.getPrompt().toString()));
        finishAdapting(spinner, adapter);
    }

    private static void finishAdapting(Spinner spinner, HintedArrayAdapter adapter) {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getCount());
    }

    public interface ContainerFactory<Obj> {

        ObjectContainer<Obj> create(Obj obj);

    }

    public interface ObjectContainer<Obj> {

        Obj getObject();

        @Override
        String toString();

    }

    public static final class ToStringObjectContainer<Obj> implements ObjectContainer<Obj> {

        public final Obj o;

        public ToStringObjectContainer(Obj o) {
            this.o = o;
        }

        @Override
        public Obj getObject() {
            return o;
        }

        @Override
        public String toString() {
            return o.toString();
        }

    }

    public static final class StringContainer implements ObjectContainer<String> {

        public final String s;

        public StringContainer(String s) {
            this.s = s;
        }

        @Override
        public String getObject() {
            return s;
        }

        @Override
        public String toString() {
            return s;
        }

    }


    public HintedArrayAdapter(Context context, @LayoutRes int resource, HintedArrayAdapter.ObjectContainer<T>[] values, String hint) {
        super(context, resource, GeneralUtils.arrayPlusValue(values, new HintContainer<T>(hint)));
    }

    public HintedArrayAdapter(Context context, @LayoutRes int resource, List<HintedArrayAdapter.ObjectContainer<T>> values, String hint) {
        super(context, resource, GeneralUtils.listPlusValueAsArray(values, new HintContainer<T>(hint)));
    }

    private HintedArrayAdapter(Context context, @LayoutRes int resource, HintedArrayAdapter.ObjectContainer<T>[] stringsWithHintAttached) {
        super(context, resource, stringsWithHintAttached);
    }
    private HintedArrayAdapter(Context context, @LayoutRes int resource, List<HintedArrayAdapter.ObjectContainer<T>> stringsWithHintAttached) {
        super(context, resource, stringsWithHintAttached);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View v = super.getView(position, convertView, parent);

        if (position == getCount()) {
            final TextView textView = (TextView) v.findViewById(android.R.id.text1);
            textView.setText("");
            textView.setHint(getItem(getCount()).toString()); //"Hint to be displayed"
        }

        return v;
    }

    @Override
    public int getCount() {
        return super.getCount() - 1; // you don't display last item. It is used as a hint.
    }

    @Override
    public void add(ObjectContainer<T> object) {
        super.insert(object, getCount()); // insertion must be aware of hidden last element
    }

    @Override
    public void addAll(Collection<? extends ObjectContainer<T>> collection) {
        for (final ObjectContainer<T> o : collection) {
            add(o);
        }
    }

    @Override
    public void addAll(ObjectContainer... items) {
        for (ObjectContainer o : items) {
            //noinspection unchecked
            add(o);
        }
    }

    @Override
    public void insert(ObjectContainer<T> object, int index) {
        final int sc = super.getCount();
        if (index == sc) {
            index = sc - 1;
        }
        super.insert(object, index);
    }

    @Override
    public void remove(ObjectContainer<T> object) {
        super.remove(object);
    }

    private static ObjectContainer<String>[] switchArrayAndApplyHint(String[] array, String hint) {
        final int l = array.length;
        final ObjectContainer[] objectContainers = new ObjectContainer[l + 1];
        for (int i = 0; i < l; i++) {
            objectContainers[i] = new StringContainer(array[i]);
        }
        objectContainers[l] = new HintContainer(hint);
        //noinspection unchecked
        return objectContainers;
    }
    private static List<ObjectContainer<String>> switchListAndApplyHint(List<String> list, String hint) {
        final int s = list.size();
        final ArrayList<ObjectContainer<String>> containers = new ArrayList<>(s + 1);
        for (final String str : list) {
            containers.add(new StringContainer(str));
        }
        containers.add(new HintContainer<String>(hint));
        return containers;
    }
    private static <Param> ObjectContainer<Param>[] switchArbitraryArrayAndApplyHint(Param[] array, String hint) {
        final int l = array.length;
        final ObjectContainer[] objectContainers = new ObjectContainer[l + 1];
        for (int i = 0; i < l; i++) {
            objectContainers[i] = new ToStringObjectContainer<>(array[i]);
        }
        objectContainers[l] = new HintContainer<>(hint);
        //noinspection unchecked
        return objectContainers;
    }
    private static <Param> List<ObjectContainer<Param>> switchArbitraryListAndApplyHint(List<Param> list, String hint) {
        final int s = list.size();
        final ArrayList<ObjectContainer<Param>> containers = new ArrayList<>(s + 1);
        for (final Param obj : list) {
            containers.add(new ToStringObjectContainer<>(obj));
        }
        containers.add(new HintContainer<Param>(hint));
        return containers;
    }

    private static final class HintContainer<Obj> implements ObjectContainer<Obj> {

        private final String s;

        private HintContainer(String s) {
            this.s = s;
        }

        @Override
        public Obj getObject() {
            return null;
        }

        @Override
        public String toString() {
            return s;
        }

    }

}
