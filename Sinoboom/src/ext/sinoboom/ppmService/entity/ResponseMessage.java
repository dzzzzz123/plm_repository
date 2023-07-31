package ext.sinoboom.ppmService.entity;

import org.apache.poi.ss.formula.functions.T;
import org.json.JSONException;
import org.json.JSONObject;

public class ResponseMessage {

	private int status;
	private String message;
	private T data;

	public static ResponseMessage of() {
		return new ResponseMessage();
	}

	public ResponseMessage status(int status) {
		this.status = status;
		return this;
	}

	public ResponseMessage message(String message) {
		this.message = message;
		return this;
	}

	public ResponseMessage data(T data) {
		this.data = data;
		return this;
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("status", status);
			json.put("message", message);
			json.put("data", data);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return json.toString();
	}

}
