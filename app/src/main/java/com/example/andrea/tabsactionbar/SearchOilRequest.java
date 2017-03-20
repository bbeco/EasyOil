package com.example.andrea.tabsactionbar;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchOilRequest {
	private final int type = MessageTypes.SEARCH_STATION_REQUEST;
	
	public double latitude, longitude, km;
	
	public SearchOilRequest(double latitude, double longitude, double km) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.km = km;
	}
	
	public SearchOilRequest(String s) throws JSONException {
		JSONObject obj = new JSONObject(s);
		this.latitude = Double.parseDouble(obj.getString("latitude"));
		this.longitude = Double.parseDouble(obj.getString("longitude"));
		this.km = Double.parseDouble(obj.getString("km"));
	}
	
	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(type));
		obj.put("latitude", Double.toString(latitude));
		obj.put("longitude", Double.toString(longitude));
		obj.put("km",Double.toString(km));
		return obj.toString();
	}

}
