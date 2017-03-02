package com.example.andrea.tabsactionbar;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.example.andrea.tabsactionbar.chat.ConversationsDbHelper;
import com.example.andrea.tabsactionbar.chat.SearchUserRequest;
import com.example.andrea.tabsactionbar.chat.messages.ChatMessage;
import com.example.andrea.tabsactionbar.chat.messages.RegistrationRequest;
import com.example.andrea.tabsactionbar.chat.messages.RegistrationResponse;
import com.google.android.gms.nearby.messages.internal.MessageType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * The onStartCommand method is called with a timer. The foreground activity calls bindService.
 * So the service can start the listeningthread when the activity is bound to it and make the
 * request for new messages and waiting for the reply sequence action (not parallel).
 */
public class SampleService extends Service {
    private static final String TAG = "SampleService";

    /* true if there are some activities bound to this service */
    private boolean bound;

    /* Currently bound activity's messenger */
    private Messenger boundActivityMessenger;

    /** List of codes for the activities that can bind to this service */
    public static final int MAIN_ACTIVITY = 0;
    public static final int MAPS_ACTIVITY = 1;
    public static final int CHAT_ACTIVITY = 2;
    public static final int START_CONVERSATION_ACTIVITY = 3;
    public static final int COMMUTE_ACTIVITY = 4;
    /** Currently bound activity's code (it is not meaningful if no activity is bound). This must
     * be set to one of the code described above.
     */
    private int boundActivityCode;

	/*
	 * The service knows when we are chatting with somebody, so that it can choose when to create a
	 * notification or not.
	 */
	private String conversationRecipient;

	/* The Sqlite helper is used to get an instance of the sqlite db */
	private ConversationsDbHelper mDbHelper;
	/* Opening the database */
	private SQLiteDatabase conversationsDb;

    private class ListenerThread extends Thread {
        private static final String TAG = "ListeningThread";
        private Socket socket;
        private DataInputStream in;

        ListenerThread(Socket s) throws IOException {
            socket = s;
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        }

        @Override
        public void run() {
            Log.i(TAG, "starting");
            int res;

            while (true) {
                byte[] buffer, tmp;
                tmp = new byte[4096];
                try {
                    res = in.read(tmp);

                    if (res < 0) {
                        break;
                    }

                    buffer = new byte[res];
                    System.arraycopy(tmp, 0, buffer, 0, res);
                    String json = new String(buffer);
                    Log.i(TAG, "incoming message: " + json);

                    try {
                        JSONObject obj = new JSONObject(json);
                        int type = Integer.parseInt(obj.getString("type"));
                        switch (type) {
                            case MessageTypes.REGISTRATION_RESPONSE:
                                /* This carries a list of unread messages */
                                Log.i(TAG, "registration response received");
                                RegistrationResponse response = new RegistrationResponse(json);

                                /* Saving the new messages int the local db */
                                for (ChatMessage msg : response.messages) {
	                                /* The following saves the message and update the timestamp lastTs */
                                    saveChatMessageInDb(msg);
                                }

                                //if the current activity is not the expected one, discard the message and continue
                                if (response.messages.size() > 0 && (!bound || boundActivityCode != CHAT_ACTIVITY)) {
                                    Log.w(TAG, "received new message for an unbound activity");
                                    //TODO handle the message (show notification)
                                    continue;
                                }
                                if(boundActivityCode == SampleService.CHAT_ACTIVITY) {
                                    Message msg = Message.obtain(null, MessageTypes.REGISTRATION_RESPONSE);
                                    boundActivityMessenger.send(msg);
                                }
                                break;

                            case MessageTypes.SEARCH_STATION_RESPONSE:
                                if(!bound || boundActivityCode != MAPS_ACTIVITY){
                                    Log.w(TAG, "received station response for an unbound activity");
                                    continue;
                                }
                                Log.v(TAG,"in the search station response case");
                                if (boundActivityCode == SampleService.MAPS_ACTIVITY) {
                                    SearchOilResponse sor = new SearchOilResponse(json);
                                    Message sorMsg = Message.obtain(null, MessageTypes.SEARCH_STATION_RESPONSE);
                                    sorMsg.obj = sor;
                                    boundActivityMessenger.send(sorMsg);
                                }
                                break;
                            case MessageTypes.COMMUTE_REQUEST:
                                if(boundActivityCode == SampleService.COMMUTE_ACTIVITY){
                                    CommuteRequest creq = new CommuteRequest(json);
                                    Message comreq = Message.obtain(null, MessageTypes.COMMUTE_REQUEST);
                                    comreq.obj = creq;
                                    boundActivityMessenger.send(comreq);
                                }
                                break;
                            case MessageTypes.CHAT_MESSAGE:
	                            /* always save the message in the local db */
	                            ChatMessage chatMessage = new ChatMessage(json);
	                            saveChatMessageInDb(chatMessage);

	                            /*
	                             * Checking if the service must inform the running activity or
	                             * create a notification
	                             */
	                            if (bound && boundActivityCode == CHAT_ACTIVITY && chatMessage.sender.contentEquals(conversationRecipient)) {
		                            Message chatActivityMessage = Message.obtain(null, MessageTypes.CHAT_MESSAGE);
		                            chatActivityMessage.obj = chatMessage;
		                            boundActivityMessenger.send(chatActivityMessage);
	                            } else {
		                            //TODO create a notification
	                            }
	                            break;

                            default:
                                Log.w(TAG, "Incoming message type not recognized");
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "Cannot decode incoming message");
                        e.printStackTrace();
                    } catch (RemoteException e) {
                        Log.w(TAG, "unable to send message back to activity");
                        e.printStackTrace();
                    }

                } catch (IOException ioe) {
                    /* when the socket is closed (by the service's thread) we end up here */
                    Log.i(TAG, "Exception raised by read");
                    break;
                }
            }
            Log.i(TAG, "terminating");
            /* Closing the database */
            mDbHelper.close();
        }
    }

    /** reference to the listening thread for incoming messages */
    private ListenerThread mListener;

    /* Worker thread attributes */
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Messenger mMessenger;

    private long lastMessageTs;

    /**
     * Service handler's codes
     */
    /** This one does not start the thread */
    public static final int CHECK_UNREAD_MESSAGES = -1;
    /** Message code to save the activity's messenger in this service after binding*/
    public static final int CLIENT_REGISTRATION = -2;
    /** Message code to remove the activity's messenger from this service before unbinding */
    public static final int CLIENT_UNREGISTRATION = -3;

	/*
	 *This is sent back to the activity by the service so that the activity can upate the message
	 * list
	 */
	public static final int MESSAGE_SENT_NOTIFICATION = -4;


    /* Handler that receives messages from the thread */
    private final class ServiceHandler extends Handler {

        private static final String TAG = "ServiceHandler";

        /* Service's Handler codes */


        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case CHECK_UNREAD_MESSAGES: //register and check for incoming messages
                    Log.i(TAG, "Checking unread messages");
                    if (socket == null) {
                        try {
                            socket = new Socket(HOST, PORT);
                        } catch (IOException ioe) {
                            Log.e(TAG, "Cannot create socket");
                            ioe.printStackTrace();
                            break;
                        }
                        try {
                            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        } catch (IOException ioe) {
                            Log.e(TAG, "Cannot create socket");
                            ioe.printStackTrace();
                            break;
                        }
                    }
					//FIXME create a request with right values
                    RegistrationRequest regMsg = new RegistrationRequest("andrea.beconcini@gmail.com",
                            "Andrea Beconcini", lastMessageTs);
                    String json;
                    try {
                        json = regMsg.toJSONString();
                        out.writeBytes(json);
                        out.flush();

                        /* Receiving replies */
                        byte[] tmp = new byte[4096];
                        byte[] buffer;
                        int res;

                        res = in.read(tmp);
                        if (res < 0) {
                            Log.i(TAG, "Connection closed by server");
                            break;
                        }

                        buffer = new byte[res];
                        System.arraycopy(tmp, 0, buffer, 0, res);
                        String jsonReply = new String(buffer);
                        Log.i(TAG, "read: " + jsonReply);
                        //TODO Add a test to check whether the received message type is correct
                        RegistrationResponse response = new RegistrationResponse(jsonReply);
                        if (response.messages.size() > 0) {
	                        //TODO send a notification
	                        for (ChatMessage m : response.messages) {
		                        Log.i(TAG, "message: " + m);
	                        /* saving messages and updating timestamp */
		                        saveChatMessageInDb(m);
	                        }
                        }

                    } catch (JSONException je) {
                        Log.e(TAG, "Malformed JSON string");
                        je.printStackTrace();
                    } catch (IOException ioe) {
                        Log.e(TAG, "error in read call");
                        ioe.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close socket");
                        e.printStackTrace();
                    }
                    socket = null;
                    break;

                /*
                 * Register this client to the server and set up a listening thread for message
                 * exchanges.
                 */
                case MessageTypes.REGISTRATION_REQUEST:
                    Log.i(TAG, "registering to remote server and starting listening thread");
                    if (socket == null) {
                        try {
                            socket = new Socket(HOST, PORT);
                        } catch (IOException ioe) {
                            Log.e(TAG, "Cannot create socket");
                            ioe.printStackTrace();
                            break;
                        }
                        try {
                            /* this should not be needed because of the listening thread */
                            //in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        } catch (IOException ioe) {
                            Log.e(TAG, "Cannot create OutputStream");
                            ioe.printStackTrace();
                            break;
                        }

                        try {
                            if (mListener == null) {
                                mListener = new ListenerThread(socket);
                            }
                        } catch (IOException ioe) {
                            Log.e(TAG, "Unable to create listening thread");
                        }
                        mListener.start();

                        /* Sending registration */
                        //TODO make this service automatically create the registration request message
                        if (boundActivityCode == SampleService.CHAT_ACTIVITY) {
                            RegistrationRequest registrationRequest = (RegistrationRequest) msg.obj;
                            Log.e(TAG, msg.obj.toString());
                            registrationRequest.ts = lastMessageTs;
                            try {
                                json = registrationRequest.toJSONString();
                                out.writeBytes(json);
                                out.flush();
                            } catch (JSONException je) {
                                je.printStackTrace();
                            } catch (IOException e) {
                                Log.e(TAG, "unable to send message");
                                e.printStackTrace();
                                break;
                            }
                        }
                    }

                    break;

                /*
                 * Register the messenger for an activitiy. The activity that is registering
                 * puts its own specific code in msg.arg1 so that the service knows which
                 * activity is currenctly bound to it.
                 */
                case CLIENT_REGISTRATION:
                    Log.i(TAG, "Client activity registration. Code: " + msg.arg1);
                    boundActivityMessenger = msg.replyTo;
                    boundActivityCode = msg.arg1;
	                /* Saving the name of the recipient during a conversation */
	                if (boundActivityCode == SampleService.CHAT_ACTIVITY) {
		                conversationRecipient = (String)msg.obj;
	                }
                    break;

                /* Remove the current activity bound */
                case CLIENT_UNREGISTRATION:
                    Log.i(TAG, "Client unregistration");
                    boundActivityMessenger = null;
	                conversationRecipient = null;
                    break;

                /* Search a specific user in the directory server */
                case MessageTypes.SEARCH_USER_REQUEST:
                    SearchUserRequest userReq = (SearchUserRequest) msg.obj;

                    Log.i(TAG, "Searching user " + userReq.name);
                    break;

                case MessageTypes.SEARCH_STATION_REQUEST:
                    try {
                        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                        SearchOilRequest req = (SearchOilRequest) msg.obj;
                        String jsonReq = req.toJSONString();
                        out.writeBytes(jsonReq);
                        out.flush();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MessageTypes.MODIFY_REQUEST:
                    try {
                        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        ModifyRequest mreq = (ModifyRequest) msg.obj;
                        String jsonMreq = mreq.toJSONString();
                        out.writeBytes(jsonMreq);
                        out.flush();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MessageTypes.COMMUTE_REQUEST:
                    try {
                        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        CommuteRequest creq = (CommuteRequest)msg.obj;
                        Log.i(TAG,creq.toString());
                        String jsonCreq = creq.toJSONString();
                        Log.i(TAG,jsonCreq);
                        out.writeBytes(jsonCreq);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                /* Sending a new chat message (and storing it in the local db) */
                case MessageTypes.CHAT_MESSAGE:
	                Log.i(TAG, "received chat message");
	                if (socket == null) {
		                Log.e(TAG, "socket is null");
	                }
	                try {
		                if (out == null) {
			                Log.w(TAG, "output buffer was null");
			                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		                }
		                ChatMessage chatMessage = (ChatMessage) msg.obj;
		                String jsonChatMessage = chatMessage.toJSONString();
		                out.writeBytes(jsonChatMessage);
		                out.flush();
		                saveChatMessageInDb(chatMessage);

		                /* notifying activity message has been sent */
		                Message replyMessage = Message.obtain();
		                replyMessage.what = MESSAGE_SENT_NOTIFICATION;
		                replyMessage.replyTo = mMessenger;
		                boundActivityMessenger.send(replyMessage);
	                } catch (IOException e) {
		                Log.e(TAG, "Unable to send chat message");
		                e.printStackTrace();
	                } catch (JSONException e) {
		                Log.e(TAG, "Unable to convert chat message to json");
		                e.printStackTrace();
	                } catch (RemoteException e) {
		                Log.e(TAG, "Unable to notify activity message has been sent");
		                e.printStackTrace();
	                }
	                break;

                default:
                    Log.i(TAG, "Unable to deal with incoming task");
                    super.handleMessage(msg);
            }

            if (!bound) {
                stopSelf();
            }
        }
    }

    /** Connection information */
    private static final String HOST = "192.168.1.135";
    private static final int PORT = 1234;
    Socket socket = null;
    DataOutputStream out = null;
    DataInputStream in = null;

    /* Preference file name used to save the last received message ts */
    private static final String PREF_FILE_NAME = "com.example.andrea.tabsactionbar.saved_ts";
    /* This is the key used for saving the last message ts in the PREF_FILE_NAME file */
    private static final String LAST_MESSAGE_TS_KEY = "savedTs";

    public SampleService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we want not to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mMessenger = new Messenger(mServiceHandler);

        /* Retriving the last saved value for the last message ts */
        SharedPreferences savedTs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        lastMessageTs = savedTs.getLong(LAST_MESSAGE_TS_KEY, 0); //if no save is found, default is 0

	    /* Initializing connection with local database */
	    mDbHelper = new ConversationsDbHelper(getApplicationContext());
	    conversationsDb = mDbHelper.getWritableDatabase();
    }

    /*
     * This method is executed by the main thread of the application it belongs to.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        // The service is starting, due to a call to startService()
        if (bound) {
            /* Nothing to do when the alarm goes off. The activity is managing the service. */
            return START_NOT_STICKY;
        }

        /* registering to the server */
        Message msg = mServiceHandler.obtainMessage();
        msg.what = CHECK_UNREAD_MESSAGES;
        mServiceHandler.sendMessage(msg);
        return START_NOT_STICKY;
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        bound = true;
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        // All clients have unbound with unbindService()
        bound = false;

        /* freeing resources */
        if (socket != null) {
            try {
                socket.close();
                mListener.join();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close listening thread's socket");
                e.printStackTrace();
            } catch (InterruptedException e) {
                Log.e(TAG, "Unable to wait for listening thread to stop");
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");

        /* saving last message ts */
        SharedPreferences savedTs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = savedTs.edit();
        editor.putLong(LAST_MESSAGE_TS_KEY, lastMessageTs);
        editor.apply();
    }

	/**
	 * This method saves a chat message in the local sqlite database
	 * @param chatMessage The message to be saved
	 */
	protected void saveChatMessageInDb(ChatMessage chatMessage) {
		ContentValues value = new ContentValues();
		value.put(ConversationsDbHelper.ChatMessageEntry.SENDER, chatMessage.sender);
		value.put(ConversationsDbHelper.ChatMessageEntry.RECEIVER, chatMessage.recipient);
		value.put(ConversationsDbHelper.ChatMessageEntry.PAYLOAD, chatMessage.payload);
		value.put(ConversationsDbHelper.ChatMessageEntry.TIMESTAMP, chatMessage.ts);
		if (chatMessage.ts > lastMessageTs) {
			lastMessageTs = chatMessage.ts;
		}
		conversationsDb.insert(ConversationsDbHelper.ChatMessageEntry.TABLE_NAME, null, value);
	}
}
