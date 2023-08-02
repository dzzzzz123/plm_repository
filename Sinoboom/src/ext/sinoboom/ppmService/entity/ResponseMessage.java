package ext.sinoboom.ppmService.entity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ResponseMessage {

	private String msg;
	private int code;
	private JSONArray data;

	public static ResponseMessage of() {
		return new ResponseMessage();
	}

	public ResponseMessage code(int code) {
		this.code = code;
		return this;
	}

	public ResponseMessage msg(String msg) {
		this.msg = msg;
		return this;
	}

	public ResponseMessage data(JSONArray resultJson) {
		this.data = resultJson;
		return this;
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("code", code);
			json.put("msg", msg);
			json.put("data", data);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return json.toString();
	}

}
