package com.example.andrea.tabsactionbar.chat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.andrea.tabsactionbar.MessageTypes;
import com.example.andrea.tabsactionbar.SampleService;

import com.example.andrea.tabsactionbar.R;
import com.google.android.gms.nearby.messages.internal.MessageType;

import java.util.ArrayList;

public class StartConversationActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String TAG = "StartConversation";

    private String userEmail = "andrea";

    /*
     * This is the structure for an entry in the array list associated to the list view of old
     * conversations.
     */
    protected class ConversationItem {
        /* The name of this conversation's recipient */
        public String recipient;
        /* The last message sent in this conversation */
        public String lastMessage;
        /* Who sent the last message in this conversation */
        public String lastMessageOwner;

        public ConversationItem(String recipient, String lastMessage, String lastMessageOwner) {
            this.recipient = recipient;
            this.lastMessage = lastMessage;
            this.lastMessageOwner = lastMessageOwner;
        }
    }

    /* This is the viewHolder to improve views reuse within the conversation list */
    public class ViewHolder {
        public TextView recipient;
        public TextView lastMessage;
    }

    /* This is the onClickListener used when clicking on a conversation */
    public class OnConversationClickListener implements View.OnClickListener {

        private final String recipientEmail;

        public OnConversationClickListener(String recipientEmail) {
            this.recipientEmail = recipientEmail;
        }
        @Override
        public void onClick(View view) {
            Log.v(TAG, "Starting conversation with " + recipientEmail);
            Intent i = new Intent(getApplicationContext(), ConversationActivity.class);
            i.putExtra(ConversationActivity.RECIPIENT_EMAIL_KEY, recipientEmail);
            i.putExtra(ConversationActivity.USER_EMAIL_KEY, userEmail);
            startActivity(i);
        }
    }

    /*
     * This is the conversationAdapter used to render the list view for the old conversations
     */
    protected class ConversationAdapter extends ArrayAdapter<ConversationItem> {
        public ConversationAdapter(Context context, ArrayList<ConversationItem> conversation) {
            super(context, 0, conversation);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ConversationItem c = getItem(position);
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.conversation_item, parent, false);
                viewHolder.recipient = (TextView) convertView.findViewById(R.id.recipient_text);
                viewHolder.lastMessage = (TextView) convertView.findViewById(R.id.last_message_text);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.recipient.setText(c.recipient);
            viewHolder.lastMessage.setText(c.lastMessageOwner + ": " + c.lastMessage);
            convertView.setOnClickListener(new OnConversationClickListener(c.recipient));
            return convertView;
        }
    }

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
            registerStartConversation();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            bound = false;
            Log.i(TAG, "Unbound to SampleService");
        }
    };

    /* This is the handler for the UI Thread */
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

        /* Retriving the list of old conversations and populating the list view */
        ConversationsDbHelper mDbHelper = new ConversationsDbHelper(this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT c.receiver AS receiver, c.sender AS sender, c.payload AS payload FROM conversation c JOIN (SELECT receiver, MAX(ts) AS maxTs FROM conversation GROUP BY receiver) groupedc ON c.receiver = groupedc.receiver AND c.ts = maxTs WHERE c.receiver <> \"" + userEmail + "\"", null);
        ArrayList<ConversationItem> oldConversation = new ArrayList<>();
        while (cursor.moveToNext()) {
            int recipientColumnIndex = cursor.getColumnIndex("receiver");
            int payloadColumnIndex = cursor.getColumnIndex("payload");
            int ownerColumnIndex = cursor.getColumnIndex("sender");
            if (recipientColumnIndex == -1 || payloadColumnIndex == -1 || ownerColumnIndex == -1) {
                Log.e(TAG, "error while reading local conversation db");
                break;
            }
            String recipient = cursor.getString(recipientColumnIndex);
            String payload = cursor.getString(payloadColumnIndex);
            String owner = cursor.getString(ownerColumnIndex);
            Log.i(TAG, recipient + " " + owner + " " + payload);
            oldConversation.add(new ConversationItem(recipient, payload, owner));
        }
        cursor.close();
        Log.i(TAG, Integer.toString(oldConversation.size()) + " conversations found");
        ConversationAdapter adapter = new ConversationAdapter(this, oldConversation);
        ListView listView = (ListView) findViewById(R.id.conversation_list);
        listView.setAdapter(adapter);
    }


    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        if (bound) {
            unregisterStartConversation();
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
    private void registerStartConversation() {
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
    }

    /**
     * Unregister from the service
     */
    private void unregisterStartConversation() {
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

