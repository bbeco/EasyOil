package com.example.andrea.tabsactionbar;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {
	
	private final int type = MessageTypes.CHAT_MESSAGE;
	public String sender, recipient, payload;
	public long ts = 0;

	public ChatMessage(String sender, String recipient, String payload, long ts) {
		
		this.sender = sender;
		this.recipient = recipient;
		this.payload = payload;
		this.ts = ts;
		// TODO Auto-generated constructor stub
	}
	public ChatMessage (String s) throws JSONException{
		JSONObject obj = new JSONObject(s);
		this.sender = obj.getString("sender");
		this.recipient = obj.getString("recipient");
		this.payload = obj.getString("payload");
		this.ts = Long.parseLong(obj.getString("ts"),10);
	}
	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(this.type));
		obj.put("sender", this.sender);
		obj.put("recipient", this.recipient);
		obj.put("payload", this.payload);
		obj.put("ts", Long.toString(this.ts));
		return obj.toString();
	}

	public String toString() {
		return "{ sender: \""  + sender + "\", recipient: \"" + recipient + "\", payload: \"" + payload + "\" }";
	}


}
