package com.example.andrea.tabsactionbar.chat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.example.andrea.tabsactionbar.chat.messages.ChatMessage;


/**
 * Created by andrea on 2/20/17.
 */

public class ConversationsDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "ConversationDbHelper";

    public static class ChatMessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message";
	    public static final String CONVERSATION = "conversation";
	    public static final String CONVERSATION_TITLE = "conversationTitle";
        public static final String SENDER = "sender";
	    public static final String SENDER_NAME = "senderName";
        public static final String RECEIVER = "receiver";
	    public static final String RECEIVER_NAME = "receiverName";
        public static final String PAYLOAD = "payload";
        public static final String TIMESTAMP = "ts";
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ChatMessageEntry.TABLE_NAME + " (" +
                    ChatMessageEntry._ID + " INTEGER PRIMARY KEY, " +
		            ChatMessageEntry.CONVERSATION + " TEXT, " +
		            ChatMessageEntry.CONVERSATION_TITLE + " TEXT, " +
                    ChatMessageEntry.SENDER + " TEXT, " +
		            ChatMessageEntry.SENDER_NAME + " TEXT, " +
                    ChatMessageEntry.RECEIVER + " TEXT, " +
		            ChatMessageEntry.RECEIVER_NAME + " TEXT, " +
                    ChatMessageEntry.PAYLOAD + " TEXT, " +
                    ChatMessageEntry.TIMESTAMP + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ChatMessageEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "ChatMessages.db";

    public ConversationsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.i(TAG, "onUpgrade");
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }
}
