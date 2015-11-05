package ru.adios.budgeter.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

import java8.util.Optional;
import java8.util.function.Consumer;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.RepoOption;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by Michail Kulikov
 * 11/5/15
 */
public class DataTableLayout extends TableLayout {

    @ColorInt
    private static final int BORDERS_COLOR = 0xFF4CAC45;
    private static final int MAX_ROW_CAPACITY = 8;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private DataStore dataStore;
    private int rowsPerSet;

    private Consumer<DataTableLayout> listener;
    private String tableName;
    private OptLimit pageLimit;
    private OrderBy orderBy;

    private Drawable cellsBackground;
    private int setSize;
    private int itemsPerInnerRow;
    private int itemsInLastRow;
    private int headerOffset;

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
        if (pageLimit == null) {
            setLimit(DEFAULT_PAGE_SIZE);
        }
        if (getChildCount() == 0) {
            populateFraming(dataStore.getDataHeaders());
        }
        loadDataAndPopulateTable();
    }

    public void repopulate() {
        removeAllViews();
        populateFraming(dataStore.getDataHeaders());
        invalidate();
        loadDataAndPopulateTable();
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setOnDataLoadedListener(Consumer<DataTableLayout> listener) {
        this.listener = listener;
    }

    public void setPageSize(int pageSize) {
        setLimit(pageSize);
    }

    public void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
    }

    private void setLimit(int pageSize) {
        pageLimit = OptLimit.createLimit(pageSize);
    }

    private void init() {
        cellsBackground = ContextCompat.getDrawable(getContext(), R.drawable.cell_shape);

        if (!isInEditMode()) {
            setSize = dataStore.getDataHeaders().size();
            itemsPerInnerRow = setSize / rowsPerSet;
            checkArgument(itemsPerInnerRow < MAX_ROW_CAPACITY, "Row appears to be too large: %s", MAX_ROW_CAPACITY);
            itemsInLastRow = setSize % rowsPerSet;
            if (itemsInLastRow == 0) {
                itemsInLastRow = itemsPerInnerRow;
            }
        }
    }

    private void loadDataAndPopulateTable() {
        new AsyncTask<DataStore, Void, List<Iterable<String>>>() {
            @Override
            protected List<Iterable<String>> doInBackground(DataStore... params) {
                return orderBy != null
                        ? params[0].loadData(pageLimit, orderBy)
                        : params[0].loadData(pageLimit);
            }

            @Override
            protected void onPostExecute(List<Iterable<String>> iterables) {
                int rowId = headerOffset;
                for (final Iterable<String> dataSet : iterables) {
                    rowId = addDataRow(dataSet, rowId);
                }
                listener.accept(DataTableLayout.this);
                invalidate();
            }
        }.execute(dataStore);
    }

    private void populateFraming(List<String> headers) {
        final Context context = getContext();

        addView(getRowSeparator(1));
        headerOffset++;

        if (tableName != null) {
            final TableRow tableNameRow = new TableRow(context);
            tableNameRow.setId(ElementsIdProvider.getNextId());
            tableNameRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            tableNameRow.setWeightSum(1f);
            final LinearLayout inner = new LinearLayout(context);
            final TableRow.LayoutParams innerParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f);
            innerParams.span = itemsPerInnerRow;
            inner.setLayoutParams(innerParams);
            inner.setBackground(cellsBackground);
            final TextView nameTextView = new TextView(context);
            nameTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            nameTextView.setTextAppearance(context, android.R.style.TextAppearance_Large);
            nameTextView.setText(tableName);
            nameTextView.setGravity(Gravity.CENTER);
            inner.addView(nameTextView);
            tableNameRow.addView(inner);
            addView(tableNameRow);
            headerOffset++;
        }

        headerOffset = addDataRow(headers, headerOffset);

        addView(getRowSeparator(1));
    }

    private int addDataRow(Iterable<String> dataSet, int rowId) {
        final Context context = getContext();
        final float lastColumnWeight = 2f * (itemsPerInnerRow - itemsInLastRow + 1);

        if (rowsPerSet > 1) {
            addView(getRowSeparator(rowsPerSet));
            rowId++;
        }

        int i = 0;
        boolean firstInner = true;
        TableRow currentRow = constructRow(context);
        for (final String str : dataSet) {
            if (i > 0 && i % itemsPerInnerRow == 0) {
                addView(currentRow, rowId++);
                currentRow = constructRow(context);
                firstInner = true;
            }

            final float weight = i < setSize - 1
                    ? 2f
                    : lastColumnWeight;
            final TextView textView;
            if (firstInner) {
                textView = createSpyingColumnForTableRow(str, rowId, weight, lastColumnWeight, context);
                firstInner = false;
            } else {
                textView = createColumnForTableRow(str, weight, context);
            }
            final Optional<Integer> maxWidth = dataStore.getMaxWidthForData(i);
            if (maxWidth.isPresent()) {
                textView.setMaxWidth(UiUtils.dpAsPixels(context, maxWidth.get()));
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

    private TextView createSpyingColumnForTableRow(String text, final int rowId, final float weight, final float lcw, Context context) {
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
                    childAt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, maxHeight, i < childCount - 1 ? weight : lcw));
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

    private TableRow constructRow(Context context) {
        final TableRow row = new TableRow(context);
        row.setId(ElementsIdProvider.getNextId());
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        row.setWeightSum(itemsPerInnerRow * 2);
        return row;
    }

    private View getRowSeparator(int heightDp) {
        final Context context = getContext();
        final View view = new View(context);
        view.setBackgroundColor(BORDERS_COLOR);
        view.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, getDpAsPixels(heightDp, context)));
        return view;
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
                return dp1;
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

    public interface DataStore {

        /**
         * For execution in a separate thread.
         * @param options optional pagination for table to request.
         * @return data in string form.
         */
        List<Iterable<String>> loadData(RepoOption... options);

        List<String> getDataHeaders();

        Optional<Integer> getMaxWidthForData(int index);

    }

}
