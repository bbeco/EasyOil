package com.example.andrea.tabsactionbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public class SearchOilResponse {
	private final int type = MessageTypes.SEARCH_STATION_RESPONSE;
	
	class Oils {
		double longitude, latitude, oil, diesel, gpl;
		int id;
		
		public Oils(int id, double latitude, double longitude, double oil, double diesel, double gpl) {
			this.id = id;
			this.latitude = latitude;
			this.longitude = longitude;
			this.oil = oil;
			this.diesel = diesel;
			this.gpl = gpl;
		}
	}
	
	public ArrayList<Oils> oils;
	
	public SearchOilResponse() {
		oils = new ArrayList<>();
	}
	
	public SearchOilResponse(String s) throws JSONException {
		JSONObject obj = new JSONObject(s);
		
		oils = new ArrayList<>();
		JSONArray array = obj.getJSONArray("oils");
		for (int i = 0; i < array.length(); i++) {
			JSONObject station = array.getJSONObject(i);
			oils.add(new Oils(Integer.parseInt(station.getString("id")),
					Double.parseDouble(station.getString("latitude")),
					Double.parseDouble(station.getString("longitude")),
					Double.parseDouble(station.getString("oil")),
					Double.parseDouble(station.getString("diesel")),
					Double.parseDouble(station.getString("gpl"))));
		}
	}
	
	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(type));
		Collection<JSONObject> stations = new ArrayList<JSONObject>(oils.size());
		for (Oils o : oils) {
			JSONObject jo = new JSONObject();
			jo.put("id", Integer.toString(o.id));
			jo.put("latitude", Double.toString(o.latitude));
			jo.put("longitude", Double.toString(o.longitude));
			jo.put("oil", Double.toString(o.oil));
			jo.put("diesel", Double.toString(o.diesel));
			jo.put("gpl", Double.toString(o.gpl));
			stations.add(jo);
		}
		obj.put("oils", stations);
		return obj.toString();
	}

}
