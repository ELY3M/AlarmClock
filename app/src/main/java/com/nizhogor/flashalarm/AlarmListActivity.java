package com.nizhogor.flashalarm;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class AlarmListActivity extends ListActivity {

    public static final String TAG = "flashalarm list ";
    private AlarmListAdapter mAdapter;
    private AlarmDBHelper dbHelper = new AlarmDBHelper(this);
    private Context mContext;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateAlarmList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_list);
        mContext = this;
        mAdapter = new AlarmListAdapter(this, dbHelper.getAlarms());
        setListAdapter(mAdapter);
        ActionBar actionBar = getActionBar();
        if (actionBar!=null)
            actionBar.setTitle("Active alarms");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem add = menu.findItem(R.id.action_add_new_alarm);
        add.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        Drawable dr = getResources().getDrawable(R.drawable.add_alarm);
        Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
        Drawable d = new BitmapDrawable(getResources(),
        Bitmap.createScaledBitmap(bitmap, 95, 95, true));
        add.setIcon(d);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_alarm_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_add_new_alarm: {
                startAlarmDetailsActivity(-1);
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void startAlarmDetailsActivity(long id) {

        Intent intent = new Intent(this, AlarmDetailsActivity.class);
        intent.putExtra("id", id);
        startActivityForResult(intent, 0);
    }

    public void updateAlarmList() {
        // if android didn't restore adapter
        if (mAdapter == null) {
            mContext = this;
            mAdapter = new AlarmListAdapter(this, dbHelper.getAlarms());
            setListAdapter(mAdapter);
        }
        mAdapter.setAlarms(dbHelper.getAlarms());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            updateAlarmList();
        } else {
            Log.i(TAG, "------>AlarmListActivity startAlarmDetailsActivity() wasn't successful");
        }
    }

    public void setAlarmEnabled(long id, boolean isEnabled) {

        AlarmModel model = dbHelper.getAlarm(id);
        // when alarm is deleted this is already handled
        if (model != null) {
            model.isEnabled = isEnabled;
            dbHelper.updateAlarm(model);
            AlarmManagerHelper.setAlarms(this, true); //m0d //was true
            Log.i(TAG,  "setAlarmEnabled() isEnabled: " + isEnabled);

            //backup sql
            backupsql();

        }
    }


    public void deleteAlarm(long id) {
        final long alarmId = id;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please confirm")
                .setTitle("Delete alarm?")
                .setCancelable(true)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Delete alarm from DB by id
                        dbHelper.deleteAlarm(alarmId);
                        updateAlarmList();
                        AlarmManagerHelper.setAlarms(mContext, false);
                    }
                }).show();
    }

    @Override
    protected  void onStart(){
        super.onStart();
        Log.i(TAG, "--->registered");
        IntentFilter filter = new IntentFilter("android.intent.action.TIME_SET");
        filter.addAction("com.nizhogor.action.REFRESH_ALARMS_DISPLAY");
        registerReceiver(mMessageReceiver, filter);
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mMessageReceiver);
    }


    void backupsql() {

        try {
            Log.i(TAG,  "backupsql() started");
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "//data//com.nizhogor.flashalarm//databases//alarmclock.db";
                String backupDBPath = "alarmclock.db";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    Log.i(TAG,  "backupsql() ran ok");
                }
            }
        } catch (Exception e) {
            Log.i(TAG,  "backupsql() failed... " + e);
        }

    }


}
