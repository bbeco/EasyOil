package com.example.andrea.tabsactionbar.chat.messages;

import com.example.andrea.tabsactionbar.MessageTypes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

public class ChatMessage {
	
	private final int type = MessageTypes.CHAT_MESSAGE;
	public String sender, senderName, recipient, recipientName, payload;
	public long ts = 0;

	/**
	 * This constructor automatically sets the right timestamp
	 *
	 * @param sender
	 * @param senderName
	 * @param recipient
	 * @param recipientName
	 * @param payload
	 */
	public ChatMessage(String sender, String senderName, String recipient, String recipientName, String payload) {

		this.sender = sender;
		this.senderName = senderName;
		this.recipient = recipient;
		this.recipientName = recipientName;
		this.payload = payload;
		// get current time in second since the Epoch
		ts = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().getTime()/1000;
	}

	public ChatMessage(String sender, String senderName, String recipient, String recipientName, String payload, long ts) {

		this.sender = sender;
		this.senderName = senderName;
		this.recipient = recipient;
		this.recipientName = recipientName;
		this.payload = payload;
		this.ts = ts;
	}
	public ChatMessage (String s) throws JSONException{
		JSONObject obj = new JSONObject(s);
		this.sender = obj.getString("sender");
		this.senderName = obj.getString("senderName");
		this.recipient = obj.getString("recipient");
		this.recipientName = obj.getString("recipientName");
		this.payload = obj.getString("payload");
		this.ts = Long.parseLong(obj.getString("ts"),10);
	}
	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(this.type));
		obj.put("sender", this.sender);
		obj.put("senderName", this.senderName);
		obj.put("recipient", this.recipient);
		obj.put("recipientName", this.recipientName);
		obj.put("payload", this.payload);
		obj.put("ts", Long.toString(this.ts));
		return obj.toString();
	}

	public String toString() {
		return "{ sender: \""  + sender + "\", senderName: \"" + senderName + "\", recipient: \"" + recipient + "\", recipientName: \"" + recipientName + "\", payload: \"" + payload + "\" }";
	}

}
