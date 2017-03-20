package com.example.andrea.tabsactionbar.chat;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
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
import android.widget.Toast;

import com.example.andrea.tabsactionbar.MessageTypes;
import com.example.andrea.tabsactionbar.R;
import com.example.andrea.tabsactionbar.SampleService;
import com.example.andrea.tabsactionbar.chat.messages.SearchUserRequest;
import com.example.andrea.tabsactionbar.chat.messages.SearchUserResponse;

import java.util.ArrayList;

/**
 * Created by andrea on 3/9/17.
 */

public class ChatSearchActivity extends AppCompatActivity {

	static final String TAG = "ChatSearch";

	/* Messengers used to communicate with the service */
	private Messenger mMessenger;
	private Messenger mService;

	private boolean bound;

	/* User informations */
	private String userEmail;
	private String userFullName;

	/* This is the onClickListener used when clicking on a conversation */
	public class OnConversationClickListener implements View.OnClickListener {

		private final String recipientEmail;
		private final String recipientFullName;

		public OnConversationClickListener(String recipientEmail, String recipientFullName) {
			this.recipientEmail = recipientEmail;
			this.recipientFullName = recipientFullName;
		}
		@Override
		public void onClick(View view) {
			Log.v(TAG, "Starting conversation with " + recipientEmail);
			Intent i = new Intent(getApplicationContext(), ConversationActivity.class);
			i.putExtra(ConversationActivity.RECIPIENT_EMAIL_KEY, recipientEmail);
			i.putExtra(ConversationActivity.RECIPIENT_FULL_NAME, recipientFullName);
			i.putExtra(ConversationActivity.USER_EMAIL_KEY, userEmail);
			i.putExtra(ConversationActivity.USER_FULL_NAME_KEY, userFullName);
			startActivity(i);
		}
	}

	/**
	 * This adapter is used to display user search results information
	 */
	protected class SearchResultAdapter extends ArrayAdapter<SearchUserResponse.User> {

		public class ViewHolder {
			public TextView name;
		}

		public SearchResultAdapter(Context context, ArrayList<SearchUserResponse.User> result) {
			super(context, 0, result);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			SearchUserResponse.User searchResultItem = getItem(position);
			ViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				LayoutInflater inflater = LayoutInflater.from(getContext());
				convertView = inflater.inflate(R.layout.user_search_result_item, parent, false);
				viewHolder.name = (TextView) convertView.findViewById(R.id.name);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			viewHolder.name.setText(searchResultItem.name);
			convertView.setOnClickListener(new OnConversationClickListener(searchResultItem.userId, searchResultItem.name));
			return convertView;
		}
	}

	/* */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			mService = new Messenger(iBinder);
			bound = true;
			Log.i(TAG, "Bound to SampleService");
			registerChatSearchActivity();

			/* This starts the serarch */
			handleIntent();
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

				case MessageTypes.SEARCH_USER_RESPONSE:
					updateSearchResults((SearchUserResponse)msg.obj);
					break;

				case SampleService.ERROR_MESSAGE:
					Log.e(TAG, (String)msg.obj);
					Toast toast = Toast.makeText(getApplicationContext(),"Server unavailable. Retry later",Toast.LENGTH_SHORT);
					toast.show();
					break;

				default:
					Log.w(TAG, "Received a message this activity can not handle");
			}
			super.handleMessage(msg);
		}
	}

	/**
	 * This method updates the list view with the results that have come from the server
	 * @param resp The response from the server
	 */
	private void updateSearchResults(SearchUserResponse resp) {
		ListView listView = (ListView)findViewById(R.id.chat_search_list);
		ArrayList<SearchUserResponse.User> result = resp.names;
		SearchResultAdapter adapter = new SearchResultAdapter(this, result);
		listView.setAdapter(adapter);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_search);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ActionBar ab = getSupportActionBar();
		ab.setTitle("Search Results");

		/* Creating this app's messenger so that the service can communicate with it */
		mMessenger = new Messenger(new IncomingHandler());

		Intent intent = getIntent();
		userEmail = intent.getStringExtra(ConversationActivity.USER_EMAIL_KEY);
		userFullName = intent.getStringExtra(ConversationActivity.USER_FULL_NAME_KEY);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
        /* Binding to the message service.
         * This is completed in an asynchronous fashion, the connection changes listener is
         * mConnection.
         */
		Intent connectionIntent = new Intent(this, SampleService.class);
		bindService(connectionIntent, mConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onPause() {
		Log.i(TAG, "onPause");
		if (bound) {
			unregisterChatSearchActivity();
			unbindService(mConnection);
		}
		mService = null;
		bound = false;
		super.onPause();
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	private void handleIntent() {
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			//use the query to search your data somehow
			if (userFullName == null || userEmail == null) {
				Log.e(TAG, "either user email or name is null");
			} else {
				sendUserRequest(query);
			}
		}
	}

	/**
	 * This method sends a UserRequestMessage to the server (through the service)
	 * @param query name of the user we're looking for
	 */
	private void sendUserRequest(String query) {
		SearchUserRequest msg = new SearchUserRequest(userEmail, query, (long)0);
		Message message = Message.obtain(null, MessageTypes.SEARCH_USER_REQUEST);
		message.obj = msg;
		try {
			mService.send(message);
		} catch (RemoteException re) {
			Log.e(TAG, "Unable to send message to service");
			re.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		// Associate searchable configuration with the SearchView
		SearchManager searchManager =
				(SearchManager) getSystemService(Context.SEARCH_SERVICE);
		MenuItem mSearchMenuItem = menu.findItem(R.id.search);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

		return true;
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
	/*
	@Override
	public boolean onQueryTextSubmit(String query) {

		sendUserRequest(query);
		return true;
	}*/

	/**
	 * Called when the query text is changed by the user.
	 *
	 * @param newText the new content of the query text field.
	 * @return false if the SearchView should perform the default action of showing any
	 * suggestions if available, true if the action was handled by the listener.
	 */
	/*
	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}
	*/
	/**
	 * This method register this activity to the service.
	 * After the registration has been performed, it asks the service to register the user in the
	 * directory server.
	 */
	private void registerChatSearchActivity() {
        /* Activity registration */
		Message registration = Message.obtain(null, SampleService.CLIENT_REGISTRATION);
		registration.arg1 = SampleService.CHAT_SEARCH_ACTIVITY;
		registration.replyTo = mMessenger;

		try {
			mService.send(registration);
		} catch (RemoteException re) {
            /* Service has crashed, display an error */
			Log.e(TAG, "Unable to send client registration to service");
		}
	}

	private void unregisterChatSearchActivity() {
		Message unregistration = Message.obtain(null, SampleService.CLIENT_UNREGISTRATION);
		unregistration.arg1 = SampleService.CHAT_SEARCH_ACTIVITY;
		unregistration.replyTo = mMessenger;

		try {
			mService.send(unregistration);
		} catch (RemoteException re) {
			Log.e(TAG, "unable to send unregistration");
            /* Service crashed. Nothing to do */
		}
	}
}
