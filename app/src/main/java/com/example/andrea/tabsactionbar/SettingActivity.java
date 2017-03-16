package com.example.andrea.tabsactionbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class SettingActivity extends PreferenceActivity {

    private static final String TAG = "SettingActivity";
    public static final String USER_EMAIL_KEY = "UserEmailKey";
    private Messenger mService;
    private boolean bound;

	/* Notifications alarm */
	private AlarmReceiver alarmReceiver = new AlarmReceiver();

	/* Settings' preference file */
	private static final String PREF_FILE_NAME = "EasyOilSettingsFile";

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
    }

    @Override
    public void onResume() {
	    super.onResume();
	    Log.i(TAG, "onResume");

	    /* Binding to service */
	    Intent s = new Intent(this, SampleService.class);
	    bindService(s, mServiceConnection, Context.BIND_AUTO_CREATE);

	    /* Retriving the last saved value for notification setting */
	/*    SharedPreferences notificationConfig = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
	    notificationSetting = notificationConfig.getBoolean(NOTIFICATION_CONFIG_KEY, false); //if no save is found, default is false
	    Log.i(TAG, "notificationSetting: " + notificationSetting);
	    CheckBox checkBoxNotification = (CheckBox) findViewById(R.id.checkbox_notifications);
	    checkBoxNotification.setChecked(notificationSetting);*/
    }

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
/*	    SharedPreferences notificationConfig = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
	    SharedPreferences.Editor editor = notificationConfig.edit();
	    editor.putBoolean(NOTIFICATION_CONFIG_KEY, notificationSetting);
	    editor.apply();*/
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
            Preference prf_path = findPreference("pathReset");
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
            Preference prf_conv = findPreference("convReset");
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
     /*       Button btnReset = (Button) findViewById(R.id.button3);
            btnReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(TAG,"Reset home/work positions");
                    Intent myInt =  getIntent();
                    String userEmail = myInt.getStringExtra(USER_EMAIL_KEY);
                    CommuteRequest creq = new CommuteRequest(null,null,null,null,userEmail,false);
                    Message msg = Message.obtain(null,MessageTypes.COMMUTE_REQUEST);
                    msg.obj = creq;
                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });*/
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Unbound");
            mService = null;
            bound = false;
        }
    };

    /* This is called automatically when the user change the checkbox value for notifications */
	public void onCheckboxClicked(View view) {
		// Is the view now checked?
		boolean checked = ((CheckBox) view).isChecked();

		/* We have one checkbox only, no need for checking id */
		if (checked) {
			Log.i(TAG, "Enabling notifications");
			alarmReceiver.setAlarm(this);
			notificationSetting = true;
		} else {
			Log.i(TAG, "Disabling notifications");
			alarmReceiver.cancelAlarm(this);
			notificationSetting = false;
		}

	}

	/**
	 * Called when the user presses a button
	 *
	 * @param view
	 */
//	@Override
/*	public void onClick(View view) {
		int btnId = view.getId();
		switch (btnId) {
			case R.id.btn_clear_conversation_cache:
				Message clearCacheMsg = Message.obtain(null, SampleService.CLEAR_CONVERSATION_CACHE);
				try {
					mService.send(clearCacheMsg);
				} catch (RemoteException e) {
					Log.e(TAG, "Unable to clear cache");
					e.printStackTrace();
				}
				Toast toast = Toast.makeText(getApplicationContext(),"Conversations deleted",Toast.LENGTH_SHORT);
				toast.show();
				break;
		}
	}*/
}
