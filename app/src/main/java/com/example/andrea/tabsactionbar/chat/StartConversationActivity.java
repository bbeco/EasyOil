package com.example.andrea.tabsactionbar.chat;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.andrea.tabsactionbar.MessageTypes;
import com.example.andrea.tabsactionbar.SampleService;

import com.example.andrea.tabsactionbar.R;
import com.google.android.gms.nearby.messages.internal.MessageType;

public class StartConversationActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String TAG = "StartConversation";

    /* true if this activity is currently bound to the service, false otherwise */
    private boolean bound = false;

    /** This is the messenger used to communicate with the service */
    private Messenger mService = null;

    /** This is used to receive messages from the service */
    private Messenger mMessenger = null;

    /* */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = new Messenger(iBinder);
            bound = true;
            Log.i(TAG, "Bound to SampleService");
            registerClient();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            bound = false;
            Log.i(TAG, "Unbound to SampleService");
        }
    };

    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Message received: " + msg.arg1);
            switch (msg.what) {
                default:
                    //TODO
            }
            super.handleMessage(msg);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_conversation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.new_message);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        /* Creating this app's messenger so that the service can communicate with it */
        mMessenger = new Messenger(new IncomingHandler());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        /* Binding to the message service.
         * This is completed in an asynchronous fashion, the connection changes listener is
         * mConnection.
         */
        Intent connectionIntent = new Intent(this, SampleService.class);
        bindService(connectionIntent, mConnection, BIND_AUTO_CREATE);
    }


    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        if (bound) {
            unregisterClient();
            unbindService(mConnection);
            mService = null;
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.new_conversation_menu, menu);

        /** Retrieving the SearchView item */
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView =
                (SearchView) MenuItemCompat.getActionView(searchItem);

        /** setting up SearchView's listener */
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.action_search:
                Log.i(TAG, "selected \"search\" option");
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    /**
     * Called when the user submits the query. This could be due to a key press on the
     * keyboard or due to pressing a submit button.
     * The listener can override the standard behavior by returning true
     * to indicate that it has handled the submit request. Otherwise return false to
     * let the SearchView handle the submission by launching any associated intent.
     *
     * @param query the query text that is to be submitted
     * @return true if the query has been handled by the listener, false to let the
     * SearchView perform the default action.
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        SearchUserRequest msg = new SearchUserRequest();
        msg.name = query;
        Message message = new Message();
        message.obj = msg;
        message.what = MessageTypes.SEARCH_USER_REQUEST;
        try {
            mService.send(message);
        } catch (RemoteException re) {
            Log.e(TAG, re.getMessage());
        }
        return false;
    }

    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     * @return false if the SearchView should perform the default action of showing any
     * suggestions if available, true if the action was handled by the listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    /**
     * This method register this activity to the service.
     * After the registration has been performed, it asks the service to register the user in the
     * directory server.
     */
    private void registerClient() {
        /* Activity registration */
        Message registration = Message.obtain(null, SampleService.CLIENT_REGISTRATION);
        registration.arg1 = SampleService.START_CONVERSATION_ACTIVITY;
        registration.replyTo = mMessenger;

        try {
            mService.send(registration);
        } catch (RemoteException re) {
            /* Service has crashed, display an error */
            Log.e(TAG, "Unable to send client registration to service");
        }

        /* User registration */
        Message registrationRequest = Message.obtain(null, MessageTypes.REGISTRATION_REQUEST);
        registrationRequest.replyTo = mMessenger;

        try {
            mService.send(registrationRequest);
            Log.i(TAG, "registration command sent");
        } catch (RemoteException re) {
            /* Service has crashed. Nothing to do here */
            Log.e(TAG, "unable to send registration command");
        }
    }

    /**
     * Unregister from the service
     */
    private void unregisterClient() {
        Message unregistration = Message.obtain(null, SampleService.CLIENT_UNREGISTRATION);
        unregistration.arg1 = SampleService.START_CONVERSATION_ACTIVITY;
        unregistration.replyTo = mMessenger;

        try {
            mService.send(unregistration);
        } catch (RemoteException re) {
            Log.e(TAG, "unable to send unregistration");
            /* Service crashed. Nothing to do */
        }
    }
}

