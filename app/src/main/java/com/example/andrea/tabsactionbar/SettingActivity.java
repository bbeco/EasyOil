package com.example.andrea.tabsactionbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SettingActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingActivity";
    public static final String RESET_PATH_KEY = "path_reset";
    public static final String CLEAR_CONV_KEY = "conv_reset";
    public static final String DIST_KM_KEY = "dist_pref";
    public static final String FUEL_KEY = "fuel_pref";
    public static final String CHECK_BOX_KEY = "checkbox_pref";
    public static final String PREFERENCE_SETTING = "PreferenceSetting";

    private Messenger mService;
    private boolean bound;
    SharedPreferences prf;

	/* Notifications alarm */
	private AlarmReceiver alarmReceiver = new AlarmReceiver();

	/* key to retrieve notification setting in preference file */
	private static final String NOTIFICATION_CONFIG_KEY = "NotificationConfigKey";

	/* Notification setting: true = periodic check enabled, false otherwise */
	private boolean notificationSetting;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting);
        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
        root.addView(bar, 0);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        prf = getSharedPreferences(PREFERENCE_SETTING, MODE_WORLD_READABLE);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
	    super.onResume();
	    Log.i(TAG, "onResume");

	    /* Binding to service */
	    Intent s = new Intent(this, SampleService.class);
	    bindService(s, mServiceConnection, Context.BIND_AUTO_CREATE);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);


	    /* Retriving the last saved value for notification setting */
	    notificationSetting = prf.getBoolean(NOTIFICATION_CONFIG_KEY, false); //if no save is found, default is false
	    Log.i(TAG, "notificationSetting: " + notificationSetting);
	    CheckBoxPreference checkBoxNotification = (CheckBoxPreference) findPreference(CHECK_BOX_KEY);
	    checkBoxNotification.setChecked(notificationSetting);
    }
    @SuppressWarnings("deprecation")
    @Override
    public void onPause (){
        Log.i(TAG, "onPause");
        if (bound) {
            unregisterSetting();
            unbindService(mServiceConnection);
        }
        mService = null;
        bound = false;

        /* Saving notifications setting value */
	    SharedPreferences.Editor editor = prf.edit();
	    editor.putBoolean(NOTIFICATION_CONFIG_KEY, notificationSetting);
	    editor.apply();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private void registerSetting() {
        Message registration = Message.obtain(null, SampleService.CLIENT_REGISTRATION);
        registration.arg1 = SampleService.SETTING_ACTIVITY;
        registration.replyTo = null;

        try {
            mService.send(registration);
        } catch (RemoteException re) {
            /* Service has crashed, display an error */
            Log.e(TAG, "Unable to send client registration to service");
        }
    }

    private void unregisterSetting() {
        Message unregistration = Message.obtain(null, SampleService.CLIENT_UNREGISTRATION);
        unregistration.arg1 = SampleService.SETTING_ACTIVITY;
        unregistration.replyTo = null;

        try {
            mService.send(unregistration);
        } catch (RemoteException re) {
            /* Service crashed. Nothing to do */
        }
    }
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @SuppressWarnings("deprecation")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Bound");
            mService = new Messenger(service);
            bound = true;
            registerSetting();
            Preference prf_path = findPreference(RESET_PATH_KEY);
            prf_path.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.i(TAG,"Reset home/work positions");
                    CommuteRequest creq = new CommuteRequest(null,null,null,null,null,false);
                    Message msg = Message.obtain(null,MessageTypes.COMMUTE_REQUEST);
                    msg.obj = creq;
                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return true;
                }

            });
            Preference prf_conv = findPreference(CLEAR_CONV_KEY);
            prf_conv.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Message clearCacheMsg = Message.obtain(null, SampleService.CLEAR_CONVERSATION_CACHE);
                    try {
                        mService.send(clearCacheMsg);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Unable to clear cache");
                        e.printStackTrace();
                    }
                    Toast toast = Toast.makeText(getApplicationContext(),"Conversations deleted",Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
            });
            final CheckBoxPreference checkBox = (CheckBoxPreference) findPreference(CHECK_BOX_KEY);
            checkBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean checked = checkBox.isChecked();

		            /* We have one checkbox only, no need for checking id */
                    if (checked) {
                        Log.i(TAG, "Enabling notifications");
                        alarmReceiver.setAlarm(getApplicationContext());
                        notificationSetting = true;
                    } else {
                        Log.i(TAG, "Disabling notifications");
                        alarmReceiver.cancelAlarm(getApplicationContext());
                        notificationSetting = false;
                    }

                    return false;
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Unbound");
            mService = null;
            bound = false;
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(s.equals(DIST_KM_KEY)){
            String value = sharedPreferences.getString(DIST_KM_KEY,"10");
            Log.v(TAG,"value "+value);
            SharedPreferences.Editor edit = prf.edit();
            edit.putString(DIST_KM_KEY,value);
            edit.apply();
            Log.v(TAG,prf.getString(DIST_KM_KEY,"7"));
        }
        if(s.equals(FUEL_KEY)){
            String value = sharedPreferences.getString(FUEL_KEY,"Oil");
            SharedPreferences.Editor edit = prf.edit();
            edit.putString(FUEL_KEY,value);
            edit.apply();
        }
    }
}
