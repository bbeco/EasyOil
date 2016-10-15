package com.example.andrea.tabsactionbar;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

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
    /** Currently bound activity's code (it is not meaningful if no activity is bound). This must
     * be set to one of the code described above.
     */
    private int boundActivityCode;

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
                                Log.v(TAG, "registration response received");
                                RegistrationResponse response = new RegistrationResponse(json);
                                //if the current activity is not the expected one, discard the message and continue
                                if (!bound || boundActivityCode != CHAT_ACTIVITY) {
                                    Log.w(TAG, "received message for an unbound activity");
                                    //show a notification
                                    continue;
                                }

                                Message msg = Message.obtain(null, MessageTypes.REGISTRATION_RESPONSE);
                                boundActivityMessenger.send(msg);
                                break;
                            case MessageTypes.SEARCH_STATION_RESPONSE:
                                if(!bound || boundActivityCode != MAPS_ACTIVITY){
                                    Log.w(TAG, "received message for an unbound activity");
                                    continue;
                                }
                                Log.v(TAG,"in the search station response case");
                                SearchOilResponse sor = new SearchOilResponse(json);
                                Message sorMsg = Message.obtain(null,MessageTypes.SEARCH_STATION_RESPONSE);
                                sorMsg.obj = sor;
                                boundActivityMessenger.send(sorMsg);
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

    /**
     * Service handler's codes
     */
    /** This one does not start the thread */
    public static final int CHECK_UNREAD_MESSAGES = -1;
    /** Message code to save the activity's messenger in this service after binding*/
    public static final int CLIENT_REGISTRATION = -2;
    /** Message code to remove the activity's messenger from this service before unbinding */
    public static final int CLIENT_UNREGISTRATION = -3;


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

                    long ts = 0;
                    RegistrationRequest regMsg = new RegistrationRequest("andrea",
                            "Andrea Beconcini", ts);
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
                        for (ChatMessage m : response.messages) {
                            Log.i(TAG, "message: " + m);
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
                        RegistrationRequest req = new RegistrationRequest("andrea", "Andrea Beconcini", 0);
                        try {
                            json = req.toJSONString();
                            out.writeBytes(json);
                            out.flush();
                        } catch (JSONException je) {

                        } catch (IOException e) {
                            Log.e(TAG, "unable to send message");
                            e.printStackTrace();
                            break;
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
                    break;

                /* Remove the current activity bound */
                case CLIENT_UNREGISTRATION:
                    Log.i(TAG, "Client unregistration");
                    boundActivityMessenger = null;
                    break;
                case MessageTypes.SEARCH_STATION_REQUEST:
                    try {
                        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                        SearchOilRequest req = (SearchOilRequest) msg.obj;
                        String jsonReq = req.toJSONString();
                        out.writeBytes(jsonReq);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
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
    private static final String HOST = "10.0.0.67";
    private static final int PORT = 1234;
    Socket socket = null;
    DataOutputStream out = null;
    DataInputStream in = null;

    public SampleService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mMessenger = new Messenger(mServiceHandler);
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
    }
}
