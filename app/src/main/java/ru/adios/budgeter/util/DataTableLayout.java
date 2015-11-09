package ru.adios.budgeter.util;

import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import java8.util.Optional;
import java8.util.function.Consumer;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.OrderedField;
import ru.adios.budgeter.api.RepoOption;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Created by Michail Kulikov
 * 11/5/15
 */
@NotThreadSafe
public class DataTableLayout extends TableLayout {

    @ColorInt
    private static final int BORDERS_COLOR = 0xFF4CAC45;
    private static final int MAX_ROW_CAPACITY = 8;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MIN_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 100;

    private DataStore dataStore;
    private int rowsPerSet;
    private int count;

    private Optional<Consumer<DataTableLayout>> listener = Optional.empty();
    private Optional<String> tableName = Optional.empty();
    private Optional<OrderResolver> orderResolver = Optional.empty();
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int currentPage;
    private OptLimit pageLimit;
    private Optional<OrderBy> orderBy = Optional.empty();
    private boolean tablePopulated = false;

    private Optional<TableRow[]> columnsRow;
    private Optional<LinearLayout> titleView = Optional.empty();
    private LinearLayout footer;
    private Button pressedButton;
    private int itemsPerInnerRow;
    private int itemsInFirstRow;
    private int headerOffset;
    private int knownWidth;
    private TextView selectedColumn;
    private boolean insidePageSize = false;
    private Optional<List<Integer>> spinnerContents = Optional.empty();
    private Spinner pageSizeSpinner;
    private final OnClickListener buttonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            pressedButton.setPressed(false);
            pressedButton.setClickable(true);
            pressedButton.invalidate();
            final Button thisBtn = (Button) v;
            thisBtn.setClickable(false);
            thisBtn.setPressed(true);
            pressedButton = thisBtn;
            turnToPage(Integer.valueOf(thisBtn.getText().toString()));
            thisBtn.invalidate();
        }
    };
    private final OnClickListener rowOrderListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (orderResolver.isPresent()) {
                final TextView tv = (TextView) v;
                if (tv.isSelected()) {
                    orderBy = Optional.of(orderBy.get().flipOrder());
                } else {
                    final Optional<OrderBy> possibleOrder = orderResolver.get().byColumnName(tv.getText().toString(), Order.DESC);
                    if (!possibleOrder.isPresent()) {
                        return;
                    }
                    orderBy = Optional.of(possibleOrder.get());
                    selectOrderByColumn(tv);
                }
                clearContents();
                loadTableProcedures();
            }
        }
    };

    private int dp1;
    private int dp2;
    private int dp3;
    private int dp4;
    private int dp5;

    public DataTableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DataTableLayout(Context context, int rowsPerSet, DataStore dataStore) {
        super(context);
        checkArgument(rowsPerSet >= 1, "rowsPerSet must be positive");
        checkArgument(dataStore != null, "dataStore is null");
        this.rowsPerSet = rowsPerSet;
        this.dataStore = dataStore;
        setId(ElementsIdProvider.getNextId());
        init();
    }

    public DataTableLayout(Context context, DataStore dataStore) {
        this(context, 1, dataStore);
    }

    public void start() {
        if (!tablePopulated) {
            if (getChildCount() == 0) {
                populateFraming(dataStore.getDataHeaders());
            }
            loadDataAndPopulateTable();
        }
    }

    public void repopulate() {
        if (tablePopulated) {
            clearContents();
        } else {
            removeAllViews();
            populateFraming(dataStore.getDataHeaders());
        }
        loadDataAndPopulateTable();
    }

    public void setTableName(String tableName) {
        this.tableName = Optional.of(tableName);
    }

    public void enableUserOrdering(OrderResolver orderResolver) {
        checkArgument(orderResolver != null, "orderResolver is null");
        this.orderResolver = Optional.of(orderResolver);
        if (getChildCount() > 0) {
            final TableRow columnsRow = (TableRow) getChildAt(2);
            for (int i = 0; i < columnsRow.getChildCount(); i++) {
                final TextView col = (TextView) columnsRow.getChildAt(i);
                final String text = col.getText().toString();

                if (!orderBy.isPresent()) {
                    final Optional<OrderBy> possibleOrder = orderResolver.byColumnName(text, Order.DESC);
                    if (!possibleOrder.isPresent()) {
                        continue;
                    }
                    orderBy = Optional.of(possibleOrder.get());
                    selectOrderByColumn(col);
                    if (tablePopulated) {
                        clearContents();
                        loadTableProcedures();
                    }
                    return;
                }

                if (text.equals(orderResolver.byField(orderBy.get().field).orElse(null))) {
                    if (!col.isSelected()) {
                        selectOrderByColumn(col);
                        col.invalidate();
                    }
                    break;
                }
            }
        }
    }

    public void disableUserOrdering() {
        orderResolver = Optional.empty();
        if (getChildCount() > 0) {
            final TableRow columnsRow = (TableRow) getChildAt(2);
            for (int i = 0; i < columnsRow.getChildCount(); i++) {
                final View childAt = columnsRow.getChildAt(i);
                if (childAt.isSelected()) {
                    childAt.setSelected(false);
                    childAt.invalidate();
                    break;
                }
            }
        }
    }

    public void setOnDataLoadedListener(Consumer<DataTableLayout> listener) {
        this.listener = Optional.ofNullable(listener);
    }

    public void setPageSize(int pageSize) {
        if (insidePageSize) {
            return;
        }
        checkArgument(pageSize > 0, "Page size must be positive");

        insidePageSize = true;
        try {
            if (pageSize == this.pageSize) {
                return;
            }

            this.pageSize = pageSize;

            if (spinnerContents.isPresent()) {
                final List<Integer> contents = spinnerContents.get();
                if (!contents.contains(pageSize)) {
                    contents.add(pageSize);
                    Collections.sort(contents);
                    pageSizeSpinner.setSelection(contents.indexOf(pageSize));
                }
            }

            int page = 1;
            if (pageLimit != null && pageLimit.offset > 0) {
                page = pageLimit.offset / pageSize + 1;
            }
            turnToPage(page);

            if (footer != null) {
                footer.removeAllViews();
                populateFooter();
            }
        } finally {
            insidePageSize = false;
        }
    }

    public void setOrderBy(OrderBy orderBy) {
        checkState(!orderResolver.isPresent() || orderBy != null, "User ordering enabled, cannot set no ordering at all");

        if ((this.orderBy.isPresent() && !this.orderBy.get().equals(orderBy)) || (!this.orderBy.isPresent() && orderBy != null)) {
            if (orderResolver.isPresent() && columnsRow.isPresent()) {
                final TableRow[] colsRow = columnsRow.get();

                outer: for (final TableRow r : colsRow) {
                    for (int j = 0; j < r.getChildCount(); j++) {
                        final TextView col = (TextView) r.getChildAt(j);

                        if (col.getText().toString().equals(orderResolver.get().byField(orderBy.field).orElse(null))) {
                            if (!col.isSelected()) {
                                selectOrderByColumn(col);
                            }
                            break outer;
                        }
                    }
                }
            }

            this.orderBy = Optional.ofNullable(orderBy);

            if (tablePopulated) {
                clearContents();
                loadTableProcedures();
            }
        }
    }

    private void init() {
        setWillNotDraw(false);

        if (!isInEditMode()) {
            final int setSize = dataStore.getDataHeaders().size();
            checkArgument(setSize >= rowsPerSet, "Data store returned set size less than rowsPerSet provided in constructor");
            itemsPerInnerRow = setSize / rowsPerSet;
            checkArgument(itemsPerInnerRow < MAX_ROW_CAPACITY, "Row appears to be too large: %s", itemsPerInnerRow);
            itemsInFirstRow = setSize % rowsPerSet + itemsPerInnerRow;
            checkArgument(itemsInFirstRow < MAX_ROW_CAPACITY, "First row appears to be too large: %s, while others are %s", itemsInFirstRow, itemsPerInnerRow);
        }
    }

    private void loadDataAndPopulateTable() {
        new AsyncTask<DataStore, Void, Integer>() {
            @Override
            protected Integer doInBackground(DataStore... params) {
                return params[0].count();
            }

            @Override
            protected void onPostExecute(Integer c) {
                count = c;
                if (c == 0) {
                    insertNoDataRow();
                    tablePopulated = true;
                    if (listener.isPresent()) {
                        listener.get().accept(DataTableLayout.this);
                    }
                    invalidate();
                    return;
                }

                turnToPage(1);
                if (c > pageSize) {
                    final Context context = getContext();

                    if (tableName.isPresent()) {
                        pageSizeSpinner = new Spinner(context, Spinner.MODE_DROPDOWN);
                        pageSizeSpinner.setMinimumWidth(UiUtils.dpAsPixels(context, 70));
                        pageSizeSpinner.setId(ElementsIdProvider.getNextId());
                        final List<Integer> contents = getPageSpinnerContents();
                        spinnerContents = Optional.of(contents);
                        pageSizeSpinner.setAdapter(new ArrayAdapter<>(
                                context,
                                android.R.layout.simple_spinner_item,
                                android.R.id.text1,
                                contents
                        ));
                        pageSizeSpinner.setSelection(contents.indexOf(pageSize));
                        pageSizeSpinner.setLayoutParams(new LinearLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 2f));
                        pageSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                setPageSize((Integer) parent.getAdapter().getItem(position));
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                        pageSizeSpinner.setVisibility(VISIBLE);

                        if (!titleView.isPresent()) {
                            addTitleRowWithSeparator(context, 10f, 8f);
                        } else {
                            titleView.get().setWeightSum(10f);
                            titleView.get()
                                    .getChildAt(0)
                                    .setLayoutParams(
                                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 8f)
                                    );
                        }
                        titleView.get().addView(pageSizeSpinner, 0);
                    }
                    final TableRow.LayoutParams fp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                    fp.span = itemsPerInnerRow;
                    footer.setLayoutParams(fp);
                    footer.setVisibility(VISIBLE);
                    populateFooter();
                }
                loadTableProcedures();
            }
        }.execute(dataStore);
    }

    private List<Integer> getPageSpinnerContents() {
        final ArrayList<Integer> list = new ArrayList<>(11);
        int curValue = 0, circle = 0, additive = MIN_PAGE_SIZE, min = Math.min(MAX_PAGE_SIZE, count);
        boolean foundCurPSize = false;

        while (curValue < min) {
            if (circle++ == 3) {
                additive *= 2;
                circle = 0;
            }
            final int c = curValue += additive;
            if (c == pageSize) {
                foundCurPSize = true;
            }
            list.add(c);
        }

        if (!foundCurPSize) {
            list.add(pageSize);
            Collections.sort(list);
        }

        return list;
    }

    private void turnToPage(int numPage) {
        checkArgument(numPage > 0, "page number must be positive");
        currentPage = numPage;
        pageLimit = numPage == 1
                ? OptLimit.createLimit(pageSize)
                : OptLimit.create(pageSize, pageSize * (numPage - 1));
        if (tablePopulated) {
            clearContents();
            loadTableProcedures();
        }
    }

    private void clearContents() {
        for (int i = getChildCount() - 4; i >= headerOffset; i--) {
            removeViewAt(i);
        }
        tablePopulated = false;
    }

    private void loadTableProcedures() {
        new AsyncTask<DataStore, Void, List<Iterable<String>>>() {
            @Override
            protected List<Iterable<String>> doInBackground(DataStore... params) {
                return orderBy.isPresent()
                        ? params[0].loadData(pageLimit, orderBy.get())
                        : params[0].loadData(pageLimit);
            }

            @Override
            protected void onPostExecute(List<Iterable<String>> iterables) {
                int rowId = headerOffset;
                for (final Iterable<String> dataSet : iterables) {
                    rowId = addDataRow(dataSet, rowId, Optional.<Consumer<TextView>>empty());
                }
                tablePopulated = true;
                if (listener.isPresent()) {
                    listener.get().accept(DataTableLayout.this);
                }
                invalidate();
            }
        }.execute(dataStore);
    }

    private void populateFraming(List<String> headers) {
        final Context context = getContext();

        addView(constructRowSeparator(1));
        headerOffset++;

        if (tableName.isPresent()) {
            addTitleRowWithSeparator(context, 1f, 1f);
        }

        if (orderResolver.isPresent() && !orderBy.isPresent()) {
            for (final String h : headers) {
                final Optional<OrderBy> possibleOrder = orderResolver.get().byColumnName(h, Order.DESC);
                if (possibleOrder.isPresent()) {
                    orderBy = Optional.of(possibleOrder.get());
                    break;
                }
            }
        }

        final int curHdrOff = headerOffset + 1;
        headerOffset = addDataRow(headers, headerOffset, Optional.<Consumer<TextView>>of(new Consumer<TextView>() {
            @Override
            public void accept(TextView textView) {
                if (orderResolver.isPresent() && textView.getText().toString().equals(orderResolver.get().byField(orderBy.get().field).orElse(null))) {
                    selectOrderByColumn(textView);
                }
                textView.setOnClickListener(rowOrderListener);
            }
        }));
        final int colArrLength = headerOffset - curHdrOff;
        final TableRow[] rArr = new TableRow[colArrLength];
        for (int i = 0; i < colArrLength; i++) {
            rArr[i] = (TableRow) getChildAt(curHdrOff + i);
        }
        columnsRow = Optional.of(rArr);

        addView(constructRowSeparator(1));
        final TableRow footerRow = constructRow(context, 1f);
        footer = new LinearLayout(context);
        final TableRow.LayoutParams fp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 0, 1f);
        fp.span = itemsPerInnerRow;
        footer.setLayoutParams(fp);
        footer.setBackgroundResource(R.drawable.cell_shape);
        footer.setOrientation(HORIZONTAL);
        footer.setVisibility(GONE);
        footerRow.addView(footer);
        addView(footerRow);
        addView(constructRowSeparator(1));
    }

    private void addTitleRowWithSeparator(Context context, float layoutWeightSum, float titleViewWeight) {
        final TableRow tableNameRow = new TableRow(context);
        tableNameRow.setId(ElementsIdProvider.getNextId());
        tableNameRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tableNameRow.setWeightSum(1f);

        titleView = Optional.of(getCenteredRowText(context, tableName.get(), layoutWeightSum, true, titleViewWeight));
        tableNameRow.addView(titleView.get());
        addView(tableNameRow, 1);
        addView(constructRowSeparator(1), 2);
        headerOffset += 2;
    }

    private void selectOrderByColumn(TextView col) {
        if (selectedColumn != null) {
            selectedColumn.setSelected(false);
            selectedColumn.invalidate();
        }
        col.setSelected(true);
        selectedColumn = col;
    }

    private LinearLayout getCenteredRowText(Context context, String text, float layoutWeightSum, boolean largeText, float textViewWeight) {
        final LinearLayout inner = new LinearLayout(context);
        final TableRow.LayoutParams innerParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        innerParams.span = itemsPerInnerRow;
        inner.setLayoutParams(innerParams);
        inner.setWeightSum(layoutWeightSum);
        inner.setBackgroundResource(R.drawable.cell_shape);
        final TextView nameTextView = new TextView(context);
        nameTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, textViewWeight));
        nameTextView.setTextAppearance(context, largeText ? android.R.style.TextAppearance_Large : android.R.style.TextAppearance_Medium);
        nameTextView.setText(text);
        nameTextView.setGravity(Gravity.CENTER);
        inner.addView(nameTextView);
        return inner;
    }

    private void insertNoDataRow() {
        final Context context = getContext();
        final TableRow noDataRow = constructRow(context, 1f);
        noDataRow.addView(getCenteredRowText(context, getResources().getString(R.string.data_table_no_rows), 1f, false, 1f));
        addView(noDataRow, headerOffset);
    }

    private int addDataRow(Iterable<String> dataSet, int rowId, Optional<Consumer<TextView>> optional) {
        final Context context = getContext();

        if (rowsPerSet > 1) {
            addView(constructRowSeparator(rowsPerSet), rowId++);
        }

        int i = 0;
        boolean firstInner = true, fistRow = true;
        TableRow currentRow = constructRow(context, itemsInFirstRow * 2f);
        for (final String str : dataSet) {
            if (i > 0 && (fistRow ? i % itemsInFirstRow == 0 : i % itemsPerInnerRow == 0)) {
                i = 0;
                fistRow = false;
                addView(currentRow, rowId++);
                currentRow = constructRow(context, itemsPerInnerRow * 2f);
                firstInner = true;
            }

            final TextView textView;
            if (firstInner) {
                textView = createSpyingColumnForTableRow(str, rowId, 2f, context);
                firstInner = false;
            } else {
                textView = createColumnForTableRow(str, 2f, context);
            }
            final Optional<Integer> maxWidth = dataStore.getMaxWidthForData(i);
            if (maxWidth.isPresent()) {
                textView.setMaxWidth(UiUtils.dpAsPixels(context, maxWidth.get()));
            }

            if (optional.isPresent()) {
                optional.get().accept(textView);
            }

            currentRow.addView(textView);

            i++;
        }

        addView(currentRow, rowId++);

        return rowId;
    }

    private TextView createColumnForTableRow(String text, float weight, Context context) {
        final TextView view = new TextView(context);
        populateColumn(view, context, weight);
        view.setText(text);
        return view;
    }

    private TextView createSpyingColumnForTableRow(String text, final int rowId, final float weight, Context context) {
        final SpyingTextView view = new SpyingTextView(context);
        populateColumn(view, context, weight);
        view.heightCatch = true;
        view.setHeightRunnable(new Runnable() {
            @Override
            public void run() {
                final TableRow row = (TableRow) getChildAt(rowId);
                final int childCount = row.getChildCount();

                int maxHeight = 0;
                for (int i = 0; i < childCount; i++) {
                    final TextView childAt = (TextView) row.getChildAt(i);
                    maxHeight = Math.max(maxHeight, childAt.getHeight());
                }

                for (int i = 0; i < childCount; i++) {
                    final TextView childAt = (TextView) row.getChildAt(i);
                    childAt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, maxHeight, weight));
                }

                invalidate();
            }
        });
        view.setText(text);
        return view;
    }

    private void populateColumn(TextView view, Context context, float weight) {
        view.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, weight));
        view.setId(ElementsIdProvider.getNextId());
        view.setBackground(ContextCompat.getDrawable(context, R.drawable.cell_shape));
        final int fiveDp = getDpAsPixels(5, context);
        view.setPadding(fiveDp, fiveDp, fiveDp, fiveDp);
        view.setTextAppearance(context, android.R.style.TextAppearance_Small);
    }

    private TableRow constructRow(Context context, float weightSum) {
        final TableRow row = new TableRow(context);
        row.setId(ElementsIdProvider.getNextId());
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        row.setWeightSum(weightSum);
        return row;
    }

    private View constructRowSeparator(int heightDp) {
        final Context context = getContext();
        final View view = new View(context);
        view.setBackgroundColor(BORDERS_COLOR);
        view.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, getDpAsPixels(heightDp, context)));
        return view;
    }

    private void populateFooter() {
        if (knownWidth > 0 && footer != null && footer.getVisibility() == VISIBLE && footer.getChildCount() == 0) {
            final int maxButtons = knownWidth / UiUtils.dpAsPixels(getContext(), 20);
            final int numPages = count / pageSize + 1;
            final int numButtons = Math.min(maxButtons, numPages);

            if (numButtons > 1) {
                for (int i = -numButtons; i <= numButtons; i++) {
                    int page = currentPage + i;
                    if (page < 1 || page > numPages) {
                        continue;
                    }
                    footer.addView(createButtonForFooter(page, page == currentPage));
                }
            } else {
                footer.addView(createButtonForFooter(1, true));
            }
            footer.invalidate();
        }
    }

    private Button createButtonForFooter(int pageNum, boolean pressed) {
        final Button button = new Button(getContext());
        button.setText(String.valueOf(pageNum));
        if (pressed) {
            button.setClickable(false);
            button.setPressed(true);
            pressedButton = button;
        }
        button.setOnClickListener(buttonListener);
        return button;
    }

    private int getDpAsPixels(int dps, Context context) {
        switch (dps) {
            case 1:
                if (dp1 == 0) {
                    dp1 = UiUtils.dpAsPixels(context, dps);
                }
                return dp1;
            case 2:
                if (dp2 == 0) {
                    dp2 = UiUtils.dpAsPixels(context, dps);
                }
                return dp2;
            case 3:
                if (dp3 == 0) {
                    dp3 = UiUtils.dpAsPixels(context, dps);
                }
                return dp3;
            case 4:
                if (dp4 == 0) {
                    dp4 = UiUtils.dpAsPixels(context, dps);
                }
                return dp4;
            case 5:
                if (dp5 == 0) {
                    dp5 = UiUtils.dpAsPixels(context, dps);
                }
                return dp5;
            default:
                return UiUtils.dpAsPixels(context, dps);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dp1 = 0;
        dp2 = 0;
        dp3 = 0;
        dp4 = 0;
        dp5 = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        knownWidth = getWidth();
        populateFooter();
        super.onDraw(canvas);
    }

    public interface DataStore {

        /**
         * For execution in a separate thread.
         * @param options optional pagination for table to request.
         * @return data in string form.
         */
        List<Iterable<String>> loadData(RepoOption... options);

        /**
         * For execution in a separate thread.
         * @return count of total data records.
         */
        int count();

        List<String> getDataHeaders();

        Optional<Integer> getMaxWidthForData(int index);

    }

    public interface OrderResolver {

        Optional<OrderBy> byColumnName(String columnName, Order order);

        Optional<String> byField(OrderedField field);

    }

}
