package ru.adios.budgeter;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.UiUtils;

public class SettingsActivity extends FundsAwareMenuActivity {

    private DialogInterface.OnClickListener dbDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        String r = null;
                        try {
                            BundleProvider.getBundle().clearSchema();
                        } catch (RuntimeException ex) {
                            r = "Error: " + ex.getMessage();
                        }
                        return r;
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        final TextView info = (TextView) findViewById(R.id.settings_reset_db_button_info);
                        if (s == null) {
                            info.setTextColor(UiUtils.GREEN_COLOR);
                            info.setText(R.string.button_success);
                            BalancesUiThreadState.instantiate(); // invalidate balances cache
                        } else {
                            info.setTextColor(UiUtils.RED_COLOR);
                            info.setText(s);
                        }
                        info.setVisibility(View.VISIBLE);
                    }
                }.execute();
            }
            dialog.dismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_settings;
    }

    @Override
    @MenuRes
    protected int getMenuResource() {
        return R.menu.menu_settings;
    }

    public void resetDb(View view) {
        findViewById(R.id.settings_reset_db_button_info).setVisibility(View.INVISIBLE);
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.settings_db_reset_dialog_text))
                .setPositiveButton(getString(R.string.common_yes), dbDialogClickListener)
                .setNegativeButton(getString(R.string.common_no), dbDialogClickListener)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
