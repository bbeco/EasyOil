package com.example.andrea.tabsactionbar.chat.messages;

import com.example.andrea.tabsactionbar.MessageTypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public class RegistrationResponse {
	private final int type = MessageTypes.REGISTRATION_RESPONSE;
	
	public ArrayList<ChatMessage> messages;
	
	public RegistrationResponse() {
		messages = new ArrayList<>();
	}
	
	public RegistrationResponse(String s) throws JSONException {
		JSONObject obj = new JSONObject(s);
		
		messages = new ArrayList<>();
		JSONArray array = obj.getJSONArray("messages");
		for (int i = 0; i < array.length(); i++) {
			JSONObject jsonMessage = array.getJSONObject(i);
			messages.add(new ChatMessage(jsonMessage.getString("sender"),
					jsonMessage.getString("recipient"),
					jsonMessage.getString("payload"),
					Long.parseLong(jsonMessage.getString("ts"))));
		}
		
	}
	
	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(type));
		
		Collection<JSONObject> jsonCollection = new ArrayList<>();
		
		for (ChatMessage m : messages) {
			JSONObject jo = new JSONObject();
			jo.put("sender", m.sender);
			jo.put("recipient", m.recipient);
			jo.put("payload", m.payload);
			jo.put("ts", Long.toString(m.ts));
			jsonCollection.add(jo);
		}
		
		obj.put("messages", jsonCollection);
		return obj.toString();
	}
}
