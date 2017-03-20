package com.example.andrea.tabsactionbar;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.andrea.tabsactionbar.chat.ConversationActivity;
import com.example.andrea.tabsactionbar.chat.ConversationsDbHelper;
import com.example.andrea.tabsactionbar.chat.StartConversationActivity;
import com.example.andrea.tabsactionbar.chat.messages.ChatMessage;
import com.example.andrea.tabsactionbar.chat.messages.RegistrationRequest;
import com.example.andrea.tabsactionbar.chat.messages.RegistrationResponse;
import com.example.andrea.tabsactionbar.chat.messages.SearchUserRequest;
import com.example.andrea.tabsactionbar.chat.messages.SearchUserResponse;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The onStartCommand method is called with a timer. The foreground activity calls bindService.
 * So the service can start the listeningthread when the activity is bound to it and make the
 * request for new messages and waiting for the reply sequence action (not parallel).
 */
public class SampleService extends IntentService {
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
    public static final int HTTP_REQUEST = 6;
	public static final int CHAT_SEARCH_ACTIVITY = 7;
    public static final int SETTING_ACTIVITY = 8;

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

	/** user information */
	private String userEmail;
	private String userFullName;

	public SampleService() {
		super("EasyOilSampleService");
	}

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
                tmp = new byte[8192];
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

	                            /* Saving the new messages int the local db.
                                 *
                                 * Suppress notification only when all the incoming messages are
								 * from the same source and we are chatting with him.
								 */
	                            boolean suppressNotification = bound &&
			                            boundActivityCode == CHAT_ACTIVITY;
                                for (ChatMessage msg : response.messages) {
	                                /* The following saves the message and update the timestamp lastTs */
                                    saveChatMessageInDb(msg);
	                                if (suppressNotification && !msg.sender.contentEquals(conversationRecipient)) {
		                                suppressNotification = false;
	                                }
                                }

                                if (!suppressNotification && response.messages.size() > 0) {
                                    displayNotification("New messages");
	                                if (bound && boundActivityCode == START_CONVERSATION_ACTIVITY) {
		                                Message msg = Message.obtain(null, MessageTypes.REGISTRATION_RESPONSE);
		                                boundActivityMessenger.send(msg);
	                                }
                                } else if (response.messages.size() > 0) {
                                    Message msg = Message.obtain(null, MessageTypes.REGISTRATION_RESPONSE);
                                    boundActivityMessenger.send(msg);
                                }
                                break;

	                        case MessageTypes.SEARCH_USER_RESPONSE:
	                        	if (!bound || boundActivityCode != CHAT_SEARCH_ACTIVITY) {
			                        Log.w(TAG, "Received SearchUserResponse for an unbound activity");
			                        continue;
		                        }
		                        Log.i(TAG, "SearchUserResponse received");
		                        SearchUserResponse searchUserResponse = new SearchUserResponse(json);
		                        Message msgSearchUserResponse = Message.obtain(null, MessageTypes.SEARCH_USER_RESPONSE);
		                        msgSearchUserResponse.obj = searchUserResponse;
		                        boundActivityMessenger.send(msgSearchUserResponse);
	                        	break;

                            case MessageTypes.SEARCH_STATION_RESPONSE:
                            	Log.d(TAG, "SearchStationResponse received");
                                if(!bound || (boundActivityCode != MAPS_ACTIVITY && boundActivityCode!=COMMUTE_ACTIVITY)){
                                    Log.w(TAG, "received station response for an unbound activity");
                                    continue;
                                }
                                Log.v(TAG,"in the search station response case");
                                if (boundActivityCode == MAPS_ACTIVITY || boundActivityCode == COMMUTE_ACTIVITY) {
                                    SearchOilResponse sor = new SearchOilResponse(json);
                                    Message sorMsg = Message.obtain(null, MessageTypes.SEARCH_STATION_RESPONSE);
                                    sorMsg.obj = sor;
                                    boundActivityMessenger.send(sorMsg);
                                }
                                break;
                            case MessageTypes.COMMUTE_REQUEST:
                            	Log.d(TAG, "CommuteRequest received");
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
		                            displayNotification("New Message");
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
	/** Message code to clear the conversation cache */
	public static final int CLEAR_CONVERSATION_CACHE = -4;
	/** Message code for errors */
	public static final int ERROR_MESSAGE = -5;

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
					Log.e(TAG, "checkUnreadmessage case");
	                checkUnreadMessages();
	                break;

                /*
                 * Register the messenger for an activitiy. The activity that is registering
                 * puts its own specific code in msg.arg1 so that the service knows which
                 * activity is currenctly bound to it.
                 */
                case CLIENT_REGISTRATION:
                    Log.i(TAG, "Client activity registration. Code: " + msg.arg1);
	                bound = true;
                    boundActivityMessenger = msg.replyTo;
                    boundActivityCode = msg.arg1;
	                /* Saving the name of the recipient during a conversation */
	                if (boundActivityCode == CHAT_ACTIVITY) {
		                conversationRecipient = (String)msg.obj;
	                }

	                getFBElements();

	                if (boundActivityCode == SETTING_ACTIVITY) {
		                break;
	                }
	                registerActivityAndStartListening();
                    break;

                /* Remove the current activity bound */
                case CLIENT_UNREGISTRATION:
                    Log.i(TAG, "Client unregistration");
	                bound = false;
                    boundActivityMessenger = null;
	                conversationRecipient = null;
                    break;

                /* Search a specific user in the directory server */
                case MessageTypes.SEARCH_USER_REQUEST:
                    SearchUserRequest userReq = (SearchUserRequest) msg.obj;
                    Log.v(TAG, "Searching user " + userReq.user);
	                if (socket == null) {
		                Log.e(TAG, "socket is null");
		                break;
	                }
	                try {
		                if (out == null) {
			                Log.w(TAG, "output buffer was null");
			                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		                }
		                String jsonSearchUserRequest = userReq.toJSONString();
		                out.writeBytes(jsonSearchUserRequest);
		                out.flush();
	                } catch (IOException ioe) {
		                Log.e(TAG, "unable to obtain output stream from socket");
		                ioe.printStackTrace();
	                } catch (JSONException e) {
		                Log.e(TAG, "Unable to parse json");
		                e.printStackTrace();
	                }
	                break;

                case MessageTypes.SEARCH_STATION_REQUEST:
                    try {
	                    if (socket == null) {
		                    Log.e(TAG, "SearchStation failed with null socket");
		                    sendErrorMessage("Could not retrieve station list");
		                    break;
	                    }
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
	                    if (socket == null) {
		                    Log.e(TAG, "ModifyRequest failed with null socket");
		                    sendErrorMessage("Could not modify request");
		                    break;
	                    }
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
                	if (socket == null) {
		                try {
			                socket = new Socket(HOST, PORT);
		                } catch (IOException ioe) {
			                Log.e(TAG, "Cannot create socket");
			                ioe.printStackTrace();
			                sendErrorMessage("Unable to connect to server");
			                break;
		                }
	                }
                    try {
                        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        CommuteRequest creq = (CommuteRequest)msg.obj;
	                    creq.email = userEmail;
                        Log.i(TAG,creq.toString());
                        String jsonCreq = creq.toJSONString();
                        Log.i(TAG,jsonCreq);
                        out.writeBytes(jsonCreq);
                        out.flush();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
	                break;
                case HTTP_REQUEST:
                    HttpURLConnection urlConn = null;
                    try {
                        String pathReq = (String) msg.obj;
                        URL url = new URL("https://maps.googleapis.com/maps/api/directions/json?"+pathReq);
                        Log.i(TAG,pathReq);
                        urlConn = (HttpURLConnection) url.openConnection();
	                    if (urlConn == null) {
		                    Log.e(TAG, "Unable to open connection toward Google Map service");
		                    sendErrorMessage("Unable to connect to Google Map Service");
		                    break;
	                    }
                        BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                        StringBuilder builder = new StringBuilder();
                        String response,s;
                        while((s=in.readLine())!=null){
                            builder.append(s+'\n');
                        }
                        response = builder.toString();
                        int responseCode = urlConn.getResponseCode();
                        String resp = urlConn.getResponseMessage();
                        in.close();
                        ArrayList<LatLng> points;
                        points = new ArrayList<>();
                        JSONObject obj = new JSONObject(response);
                        List<List<HashMap<String,String>>> routes;
                        routes = DirectionsJSONParser.parse(obj);
                        for(int i = 0;i<routes.get(0).size();i++) {
                            double lat = Double.parseDouble(routes.get(0).get(i).get("lat"));
                            double lng = Double.parseDouble(routes.get(0).get(i).get("lng"));
                            points.add(new LatLng(lat,lng));
                        }
                        Message pointsResp = Message.obtain(null,HTTP_REQUEST);
                        pointsResp.obj = points;
                        boundActivityMessenger.send(pointsResp);
                        Log.i(TAG,response);
                        Log.i(TAG,resp +" "+responseCode);
//                        JSONObject obj = new JSONObject(tmp.toString());
                    } catch (RemoteException | JSONException | IOException e) {
                        e.printStackTrace();
                    } finally {
	                    if (urlConn != null) {
		                    urlConn.disconnect();
	                    }
                    }
                    break;
                /* Sending a new chat message (and storing it in the local db) */
                case MessageTypes.CHAT_MESSAGE:
	                Log.i(TAG, "received chat message");
	                if (socket == null) {
		                sendErrorMessage("Unable to connect to chat server");
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

	            case CLEAR_CONVERSATION_CACHE:
		            lastMessageTs = 0;
		            getSharedPreferences(SampleService.PREF_FILE_NAME,0).edit().remove(LAST_MESSAGE_TS_KEY).apply();
		            mDbHelper = new ConversationsDbHelper(getApplicationContext());
		            SQLiteDatabase db = mDbHelper.getWritableDatabase();
		            int res = db.delete(ConversationsDbHelper.ChatMessageEntry.TABLE_NAME, "1", null);
		            Log.i(TAG, Integer.toString(res) + " deleted rows");
		            db.close();
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
    private static final String HOST = "192.168.1.136";
    private static final int PORT = 1234;
    Socket socket = null;
    DataOutputStream out = null;
    DataInputStream in = null;

    /* Preference file name used to save the last received message ts */
    private static final String PREF_FILE_NAME = "com.example.andrea.tabsactionbar.saved_ts";
    /* This is the key used for saving the last message ts in the PREF_FILE_NAME file */
    private static final String LAST_MESSAGE_TS_KEY = "savedTs";

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

	/**
	 * This is called when the service is periodically started by the alarm
	 * @param intent
	 */
	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		Log.i(TAG, "onHandleIntent");
		Log.d(TAG, "username: " + userFullName + " useremail: " + userEmail);
		Log.d(TAG, "Looking for new messages");
		if (!bound) {
			getFBElements();
			if (userFullName != null && userEmail != null) {
				checkUnreadMessages();
			}
		}
		AlarmReceiver.completeWakefulIntent(intent);
	}

	private void getFBElements() {
		AccessToken accessToken = AccessToken.getCurrentAccessToken();
		if (accessToken == null || accessToken.isExpired()) {
			Log.d(TAG, "FB AccessToken is either null or expired");
			return;
		}
		GraphRequest request = GraphRequest.newMeRequest(
				accessToken,
				new GraphRequest.GraphJSONObjectCallback() {
					@Override
					public void onCompleted(JSONObject object, GraphResponse response) {
						try {
							userFullName = object.getString("name");
							userEmail = object.getString("email");
						} catch (JSONException e) {
							e.printStackTrace();
						}
						Log.d(TAG, "user");
					}
				});
		Bundle parameters = new Bundle();
		parameters.putString("fields", "id,name,link,email");
		request.setParameters(parameters);
		Log.i(TAG, "Requesting fb credentials");
		request.executeAndWait();
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
	            if (mListener != null) {
		            mListener.join();
		            mListener = null;
	            }
            } catch (IOException e) {
                Log.e(TAG, "Unable to close listening thread's socket");
                e.printStackTrace();
            } catch (InterruptedException e) {
                Log.e(TAG, "Unable to wait for listening thread to stop");
                e.printStackTrace();
            }
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");

        /* saving last message ts */
        SharedPreferences savedTs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = savedTs.edit();
        editor.putLong(LAST_MESSAGE_TS_KEY, lastMessageTs);
        editor.apply();
	    if (socket != null) {
		    try {
			    socket.close();
		    } catch (IOException e) {
			    Log.d(TAG, "Unable to close socket");
			    e.printStackTrace();
		    }
		    socket = null;
	    }
	    super.onDestroy();
    }

	/**
	 * This method saves a chat message in the local sqlite database
	 * @param chatMessage The message to be saved
	 */
	protected void saveChatMessageInDb(ChatMessage chatMessage) {
		/* Initializing connection with local database */
		mDbHelper = new ConversationsDbHelper(getApplicationContext());
		conversationsDb = mDbHelper.getWritableDatabase();

		ContentValues value = new ContentValues();
		String conversation = chatMessage.sender;
		String conversationTitle = chatMessage.senderName;
		if (chatMessage.sender.contentEquals(userEmail)) {
			conversation = chatMessage.recipient;
			conversationTitle = chatMessage.recipientName;
		}
		value.put(ConversationsDbHelper.ChatMessageEntry.CONVERSATION, conversation);
		value.put(ConversationsDbHelper.ChatMessageEntry.CONVERSATION_TITLE, conversationTitle);
		value.put(ConversationsDbHelper.ChatMessageEntry.SENDER, chatMessage.sender);
		value.put(ConversationsDbHelper.ChatMessageEntry.SENDER_NAME, chatMessage.senderName);
		value.put(ConversationsDbHelper.ChatMessageEntry.RECEIVER, chatMessage.recipient);
		value.put(ConversationsDbHelper.ChatMessageEntry.RECEIVER_NAME, chatMessage.recipientName);
		value.put(ConversationsDbHelper.ChatMessageEntry.PAYLOAD, chatMessage.payload);
		value.put(ConversationsDbHelper.ChatMessageEntry.TIMESTAMP, chatMessage.ts);
		if (chatMessage.ts > lastMessageTs) {
			lastMessageTs = chatMessage.ts;
		}
		conversationsDb.insert(ConversationsDbHelper.ChatMessageEntry.TABLE_NAME, null, value);
		conversationsDb.close();
	}

	/**
	 * This method shows a notification with the given text.
	 *
	 * @param content The notification content
	 */
	private void displayNotification(String content) {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_search_white_24px)
						.setContentTitle("EasyOil")
						.setContentText(content)
						.setPriority(NotificationCompat.PRIORITY_HIGH)
						.setAutoCancel(true);
		//Vibration
		mBuilder.setVibrate(new long[] { 0, 200, 200, 200 });

		//LED
		mBuilder.setLights(Color.BLUE, 3000, 3000);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, StartConversationActivity.class);
		resultIntent.putExtra(ConversationActivity.USER_EMAIL_KEY, userEmail);
		resultIntent.putExtra(ConversationActivity.USER_FULL_NAME_KEY, userFullName);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(StartConversationActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
				);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		int mId = 0;
		mNotificationManager.notify(mId, mBuilder.build());
	}

	private void checkUnreadMessages() {
		Log.i(TAG, "Checking unread messages");

		/* Checking fb AccessToken */
		AccessToken.getCurrentAccessToken();
		if (userEmail == null || userFullName == null) {
			Log.w(TAG, "unable to check for new message: either userEmail or userFullName is null");
			return;
		}
		if (socket == null) {
			try {
				socket = new Socket(HOST, PORT);
			} catch (IOException ioe) {
				Log.e(TAG, "Cannot create socket");
				ioe.printStackTrace();
				return;
			}
			try {
				in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			} catch (IOException ioe) {
				Log.e(TAG, "Cannot create socket");
				ioe.printStackTrace();
				return;
			}
		}

		RegistrationRequest regMsg = new RegistrationRequest(userEmail, userFullName, lastMessageTs);
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
				return;
			}

			buffer = new byte[res];
			System.arraycopy(tmp, 0, buffer, 0, res);
			String jsonReply = new String(buffer);
			Log.i(TAG, "read: " + jsonReply);
			//TODO Add a test to check whether the received message type is correct
			RegistrationResponse response = new RegistrationResponse(jsonReply);
			if (response.messages.size() > 0) {
				displayNotification("New Messages");
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
	}

	private void registerActivityAndStartListening() {
		Log.i(TAG, "registering to remote server and starting listening thread");
		if (socket == null) {
			try {
				socket = new Socket(HOST, PORT);
			} catch (IOException ioe) {
				Log.e(TAG, "Cannot create socket");
				sendErrorMessage("Unable to connect to chat server");
				ioe.printStackTrace();
				return;
			}
			try {
                /* this should not be needed because of the listening thread */
				//in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			} catch (IOException ioe) {
				Log.e(TAG, "Cannot create OutputStream");
				ioe.printStackTrace();
				return;
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
			RegistrationRequest registrationRequest = new RegistrationRequest(userEmail, userFullName, lastMessageTs);
			try {
				String json;
				json = registrationRequest.toJSONString();
				out.writeBytes(json);
				out.flush();
			} catch (JSONException je) {
				je.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, "unable to send message");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send a message to the activity. The provided error string is put in the message's obj
	 * attribute.
	 * @param errorString A string describing the error that occurred.
	 */
	private void sendErrorMessage(String errorString) {
		if (!bound || boundActivityMessenger == null) {
			Log.e(TAG, "No activity is currently bound");
			return;
		}
		Message errorMsg = Message.obtain(null, ERROR_MESSAGE);
		errorMsg.obj = errorString;
		try {
			boundActivityMessenger.send(errorMsg);
		} catch (RemoteException re) {
			Log.e(TAG, "Unable to send error message");
			re.printStackTrace();
		}
	}
}
