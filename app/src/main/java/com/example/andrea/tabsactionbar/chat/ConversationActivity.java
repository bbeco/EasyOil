package com.example.andrea.tabsactionbar.chat;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.example.andrea.tabsactionbar.R;
import com.example.andrea.tabsactionbar.chat.messages.ChatMessage;

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

    /* The email address of the recipient */
    private String recipientEmail;
    /* My own email address */
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ListView l = (ListView)findViewById(R.id.chat_list);
        l.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        l.setStackFromBottom(true);

        /* Retriving emailAddress passed within intent */
        Intent mIntent = getIntent();
        recipientEmail = mIntent.getStringExtra(RECIPIENT_EMAIL_KEY);
        userEmail = mIntent.getStringExtra(USER_EMAIL_KEY);

        /* Retriving all the message for the recipient "recipientEmail" */
        ConversationsDbHelper mDbHelper = new ConversationsDbHelper(this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String query = "SELECT receiver, sender, payload, ts " +
                        "FROM conversation " +
                        "WHERE (receiver=\"" + userEmail + "\" AND sender=\"" + recipientEmail + "\") " +
                        "OR (receiver=\"" + recipientEmail + "\" AND sender=\"" + userEmail + "\") " +
                        "ORDER BY ts ASC";

        Cursor cursor = db.rawQuery(query, null);
        ArrayList<ChatMessage> messageList = new ArrayList<>();
        while (cursor.moveToNext()) {
            int recipientColumnIndex = cursor.getColumnIndex("receiver");
            int payloadColumnIndex = cursor.getColumnIndex("payload");
            int senderColumnIndex = cursor.getColumnIndex("sender");
            int timestampColumnIndex = cursor.getColumnIndex("ts");
            if (recipientColumnIndex == -1 || payloadColumnIndex == -1 || senderColumnIndex == -1 || timestampColumnIndex == -1) {
                Log.e(TAG, "error while reading local conversation db");
                break;
            }
            String recipient = cursor.getString(recipientColumnIndex);
            String payload = cursor.getString(payloadColumnIndex);
            String sender = cursor.getString(senderColumnIndex);
            int ts = cursor.getInt(timestampColumnIndex);
            //Log.i(TAG, recipient + " " + owner + " " + payload);
            messageList.add(new ChatMessage(sender, recipient, payload, ts));
        }
        ChatListAdapter adapter = new ChatListAdapter(getApplicationContext(), messageList, userEmail);
        //Log.d(TAG, Integer.toString(adapter.getCount()));
        ListView listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter);
    }
}
