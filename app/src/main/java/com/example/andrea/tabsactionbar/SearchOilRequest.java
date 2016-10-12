package com.example.andrea.tabsactionbar;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchOilRequest {
	private final int type = MessageTypes.SEARCH_STATION_REQUEST;
	
	public double latitude, longitude;
	
	public SearchOilRequest(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public SearchOilRequest(String s) throws JSONException {
		JSONObject obj = new JSONObject(s);
		this.latitude = Double.parseDouble(obj.getString("latitude"));
		this.longitude = Double.parseDouble(obj.getString("longitude"));
	}
	
	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(type));
		obj.put("latitude", Double.toString(latitude));
		obj.put("longitude", Double.toString(longitude));
		return obj.toString();
	}

}
