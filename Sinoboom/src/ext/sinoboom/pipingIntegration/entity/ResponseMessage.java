package ext.sinoboom.pipingIntegration.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseMessage {

	private boolean success;
	private String msg;
	private int code;
	private Object data;

	public static ResponseMessage of() {
		return new ResponseMessage();
	}

	public ResponseMessage success(boolean success) {
		this.success = success;
		return this;
	}

	public ResponseMessage code(int code) {
		this.code = code;
		return this;
	}

	public ResponseMessage msg(String msg) {
		this.msg = msg;
		return this;
	}

	public ResponseMessage data(Object resultJson) {
		this.data = resultJson;
		return this;
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("success", success);
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
