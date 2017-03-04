package com.example.andrea.tabsactionbar.chat;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.example.andrea.tabsactionbar.MessageTypes;
import com.example.andrea.tabsactionbar.R;
import com.example.andrea.tabsactionbar.SampleService;
import com.example.andrea.tabsactionbar.chat.messages.ChatMessage;
import com.example.andrea.tabsactionbar.chat.messages.RegistrationRequest;

import java.util.ArrayList;

public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = "ConversationActivity";

    /*
     * This is the key used to pass the recipient's email address when starting this activity with
     * an intent.
     */
    public static final String RECIPIENT_EMAIL_KEY = "recipientEmailKey";
    /*
     * This is the key used to pass the user's own email address when starting this activity with
     * an intent.
     */
    public static final String USER_EMAIL_KEY = "userEmailKey";
	/*
	 * This is the key used to pass the user's own full name when starting this activity with an
	 * intent.
	 */
	public static final String USER_FULL_NAME_KEY = "userFullNameKey";
	/*
	 * This is the key used to pass the recipient's full name when starting this activity with an
	 * intent.
	 */
	public static final String RECIPIENT_FULL_NAME = "recipientFullName";

	/* The email address of the recipient */
    private String recipientEmail;
	/* The full name of the recipient */
	private String recipientFullName;
    /* My own email address */
    private String userEmail;
	/* My own full name */
	private String userFullName;

    /* This is the list of messages ordered by timestamp and the message adapter*/
    ArrayList<ChatMessage> messageList;
	ChatListAdapter messageAdapter;

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
            registerConversation();
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
            Log.i(TAG, "Message received");
            switch (msg.what) {
	            case MessageTypes.CHAT_MESSAGE:
		            Log.w(TAG, "ChatMessage received");
		            ChatMessage chatMessage = (ChatMessage) msg.obj;
		            //TODO check if it is inserted at the end of the message list
		            messageList.add(chatMessage);
					messageAdapter.notifyDataSetChanged();
		            break;

	            case SampleService.MESSAGE_SENT_NOTIFICATION:
		            Log.w(TAG, "MessageSentNotification received");
		            /*
		             *This message is sent by the service to this activity to update the message
		             * list.
		             */
		            messageAdapter.notifyDataSetChanged();
		            break;

	            case MessageTypes.REGISTRATION_RESPONSE:
		            Log.w(TAG, "RegistrationResponse received");
		            break;

                default:
                    Log.e(TAG, "Received unknow message");
            }
            super.handleMessage(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	    Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ListView l = (ListView)findViewById(R.id.chat_list);
        l.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        l.setStackFromBottom(true);

	    /* Retriving emailAddress passed within intent */
	    Intent mIntent = getIntent();
	    recipientEmail = mIntent.getStringExtra(RECIPIENT_EMAIL_KEY);
	    recipientFullName = mIntent.getStringExtra(RECIPIENT_FULL_NAME);
	    userEmail = mIntent.getStringExtra(USER_EMAIL_KEY);
	    userFullName = mIntent.getStringExtra(USER_FULL_NAME_KEY);

	    Log.i(TAG, "userEmail: " + userEmail + " recipientEmail: " + recipientEmail + " userFullName: " + userFullName + " recipientFullName: " + recipientFullName);

        mMessenger = new Messenger(new IncomingHandler());

        /* Retriving all the message for the recipient "recipientEmail" */
        ConversationsDbHelper mDbHelper = new ConversationsDbHelper(this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String query = "SELECT receiver, receiverName, sender, senderName, payload, ts " +
                        "FROM message " +
                        "WHERE (receiver=\"" + userEmail + "\" AND sender=\"" + recipientEmail + "\") " +
                        "OR (receiver=\"" + recipientEmail + "\" AND sender=\"" + userEmail + "\") " +
                        "ORDER BY ts ASC";

        Cursor cursor = db.rawQuery(query, null);
        messageList = new ArrayList<>();
        while (cursor.moveToNext()) {
            int recipientColumnIndex = cursor.getColumnIndex("receiver");
	        int recipientNameColumnIndex = cursor.getColumnIndex("receiverName");
            int payloadColumnIndex = cursor.getColumnIndex("payload");
            int senderColumnIndex = cursor.getColumnIndex("sender");
	        int senderNameColumnIndex= cursor.getColumnIndex("senderName");
            int timestampColumnIndex = cursor.getColumnIndex("ts");
            if (recipientColumnIndex == -1 || payloadColumnIndex == -1 || senderColumnIndex == -1 ||
		            recipientNameColumnIndex == -1 || senderNameColumnIndex == -1 ||
		            timestampColumnIndex == -1) {
                Log.e(TAG, "error while reading local conversation db");
                break;
            }
            String recipient = cursor.getString(recipientColumnIndex);
	        String recipientName = cursor.getString(recipientNameColumnIndex);
            String payload = cursor.getString(payloadColumnIndex);
            String sender = cursor.getString(senderColumnIndex);
	        String senderName = cursor.getString(senderNameColumnIndex);
            long ts = cursor.getLong(timestampColumnIndex);
            Log.i(TAG, "messages = " + sender + " " + recipient + " " +  payload + " " + ts);
            messageList.add(new ChatMessage(sender, senderName, recipient, recipientName, payload, ts));
        }
        messageAdapter = new ChatListAdapter(getApplicationContext(), messageList, userEmail);
        //Log.d(TAG, Integer.toString(adapter.getCount()));
        ListView listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(messageAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

        /* Binding to the message service.
         * This is completed in an asynchronous fashion, the connection changes listener is
         * mConnection.
         */
        Intent connectionIntent = new Intent(this, SampleService.class);
        bindService(connectionIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
	    Log.v(TAG, "onPause");
        if (bound) {
            unregisterConversation();
            unbindService(mConnection);
            mService = null;
        }
        super.onPause();
    }

    /* This is called when the user push the "Send" button.
     * It sends the message to the service which then sends it to the server.
     * The service also saves it in the local message db.
     */
    public void sendMessage(View view) {
        EditText editText = (EditText) findViewById(R.id.input_message_text);
        String messageText = editText.getText().toString();
        if (messageText.equalsIgnoreCase("")) {
	        Log.w(TAG, "Trying to send empty message");
            return;
        }

        ChatMessage message = new ChatMessage(userEmail, userFullName, recipientEmail, recipientFullName, messageText);
	    //TODO check if this adds the message at the end of messageList
	    messageList.add(message);
	    Message serviceMsg = Message.obtain(null, MessageTypes.CHAT_MESSAGE, message);
		serviceMsg.replyTo = mMessenger;
	    try {
		    /* When the service receives this message, it saves it in the local db (so that
		     * ConversationActivity retrives every message sent on this chat).
		     */
		    mService.send(serviceMsg);
	    } catch (RemoteException re) {
		    Log.e(TAG, "unable to send unregistration");
            /* Service crashed. Nothing to do */
	    }

	    editText.setText("");
    }

    /**
     * This method register this activity to the service.
     * After the registration has been performed, it asks the service to register the user in the
     * directory server.
     */
    private void registerConversation() {
        /* Activity registration */
        Message registration = Message.obtain(null, SampleService.CLIENT_REGISTRATION);
        registration.arg1 = SampleService.CHAT_ACTIVITY;
	    /* We pass the name of the recipient so that the server is aware of it */
	    registration.obj = recipientEmail;
        registration.replyTo = mMessenger;

        try {
            mService.send(registration);
        } catch (RemoteException re) {
            /* Service has crashed, display an error */
            Log.e(TAG, "Unable to send client registration to service");
        }

	    /* requesting new messages */
	    /* Activity registration */
	    Message serverRegistration = Message.obtain(null, MessageTypes.REGISTRATION_REQUEST);
	    serverRegistration.replyTo = mMessenger;
	    serverRegistration.obj = new RegistrationRequest(userEmail, userFullName, 0);
	    try {
		    mService.send(serverRegistration);
	    } catch (RemoteException re) {
            /* Service has crashed, display an error */
		    Log.e(TAG, "Unable to ask for server registration");
	    }
    }

    /**
     * Unregister from the service
     */
    private void unregisterConversation() {
        Message unregistration = Message.obtain(null, SampleService.CLIENT_UNREGISTRATION);
        unregistration.arg1 = SampleService.CHAT_ACTIVITY;
        unregistration.replyTo = mMessenger;

        try {
            mService.send(unregistration);
        } catch (RemoteException re) {
            Log.e(TAG, "unable to send unregistration");
            /* Service crashed. Nothing to do */
        }
    }
}
