package ext.sinoboom.ppmService.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ext.sinoboom.ppmService.config.PPMConfig;
import ext.sinoboom.ppmService.entity.PPMProjectEntity;

public class PPMProjectService {

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat sdf = new SimpleDateFormat(pattern);

	public List<PPMProjectEntity> getPPMProjectData() {

		List<PPMProjectEntity> projects = getProjects();

		return projects;

	}

	private List<PPMProjectEntity> getProjects() {

		GetMethod method = null;

		try {

			String token = getTokenInfo();
			String tenantOid = getTenantInfoOid(token);
			String projectInfoUrl = PPMConfig.getConfig("projectInfoUrl");
			String projectUrlItem = PPMConfig.getConfig("projectUrlItem");

			projectInfoUrl += Integer.MAX_VALUE;

			method = new GetMethod(projectInfoUrl);

			HttpClient client = new HttpClient();
			method.setRequestHeader("accept", "*/*");
			method.setRequestHeader("connection", "Keep-Alive");
			method.setRequestHeader("Content-Type", "application/json; charset=UTF-8");
			method.setRequestHeader("appName", "ppm");
			method.setRequestHeader("accesstoken", token);
			method.setRequestHeader("tenantOid", tenantOid);
			// 设置为默认的重试策略
			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
			// 设置请求参数(请求内容)

			int rspCode = client.executeMethod(method);
			System.out.println(">>>getProjects rspCode>>>" + rspCode);
			StringBuffer stringBuffer = new StringBuffer();
			InputStream is = method.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = "";
			while ((line = br.readLine()) != null) {
				stringBuffer.append(line);
			}
			String ret = stringBuffer.toString();
			// System.out.println(">>>getProjects back ret>>>" + ret.toString());

			// 使用JsonParser解析JSON数据
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = jsonParser.parse(ret).getAsJsonObject();

			// 从result中获取rows字段的JsonArray
			JsonArray rowsArray = jsonObject.getAsJsonObject("result").getAsJsonArray("rows");

			List<PPMProjectEntity> list = new ArrayList();

			// 遍历每个项目对象
			for (JsonElement element : rowsArray) {
				JsonObject project = element.getAsJsonObject();

				// 获取项目的字段值
				String masterOid = getAsStringOrNull(project, ("masterOid"));
				String oid = getAsStringOrNull(project, ("oid"));
				String masterType = getAsStringOrNull(project, ("masterType"));
				String modelDefinition = getAsStringOrNull(project, ("modelDefinition"));
				String number = getAsStringOrNull(project, ("number"));
				String name = getAsStringOrNull(project, ("name"));
				String projectSetupStatus = getAsStringOrNull(project, ("projectStartStatus"));
				String createDate = getAsStringOrNull(project, ("createDate"));
				long creatDateL = Long.parseLong(createDate);
				Date date = new Date(creatDateL);
				createDate = sdf.format(date);

				PPMProjectEntity ppmProjectEntity = new PPMProjectEntity();
				ppmProjectEntity.setProjectNumber(number);
				ppmProjectEntity.setProjectName(name);
				ppmProjectEntity.setProjectStatus(projectSetupStatus);
				ppmProjectEntity.setProjectTime(createDate);
				String url = projectUrlItem + "masterOid=" + masterOid + "&oid=" + oid + "&masterType=" + masterType
						+ "&modelDefinition=" + modelDefinition;
				ppmProjectEntity.setProjectUrl(url);
				list.add(ppmProjectEntity);

			}

			return list;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}

	}

	private String getTokenInfo() {
		String TokenUrl = PPMConfig.getConfig("getTokenUrl");
		PostMethod method = null;
		try {

			method = new PostMethod(TokenUrl);
			System.out.println("TOKENURL:" + TokenUrl);
			HttpClient client = new HttpClient();
			method.setRequestHeader("accept", "*/*");
			method.setRequestHeader("connection", "Keep-Alive");
			method.setRequestHeader("Content-Type", "application/json; charset=UTF-8");
			method.setRequestHeader("appName", "ppm");
			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

			String name = PPMConfig.getConfig("userName");
			String passWord = PPMConfig.getConfig("passWord");

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("account", name);
			jsonObject.put("password", passWord);

			method.setRequestBody(jsonObject.toJSONString());

			int rspCode = client.executeMethod(method);
			System.out.println(">>>getTokenInfo rspCode>>>" + rspCode);

			String ret = method.getResponseBodyAsString();
			// System.out.println(">>>getTokenInfo>>>" + ret.toString());

			Gson gson = new Gson();
			JsonObject object = gson.fromJson(ret, JsonObject.class);
			JsonObject resultObject = object.getAsJsonObject("result");
			String accessToken = resultObject.get("accesstoken").getAsString();
			System.out.println("AccessToken: " + accessToken);
			return accessToken;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}

	}

	private String getTenantInfoOid(String token) {
		GetMethod method = null;
		String tenantInfoUrl = PPMConfig.getConfig("getTenantInfoUrl");

		try {
			method = new GetMethod(tenantInfoUrl);
			HttpClient client = new HttpClient();
			method.setRequestHeader("accept", "*/*");
			method.setRequestHeader("connection", "Keep-Alive");
			method.setRequestHeader("Content-Type", "application/json; charset=UTF-8");
			method.setRequestHeader("accessToken", token);
			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

			int rspCode = client.executeMethod(method);
			System.out.println(">>>getTenantInfo rspCode>>>" + rspCode);

			StringBuffer stringBuffer = new StringBuffer();

			InputStream is = method.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = "";
			while ((line = br.readLine()) != null) {
				stringBuffer.append(line);

			}
			String ret = stringBuffer.toString();
			// System.out.println(">>>getTenantInfo>>>" + ret);
			Gson gson = new Gson();

			// 将JSON字符串解析为JsonObject
			JsonObject jsonObject = gson.fromJson(ret, JsonObject.class);

			// 获取result对象
			JsonObject resultObject = jsonObject.getAsJsonObject("result");

			// 获取tenantList数组
			JsonArray tenantListArray = resultObject.getAsJsonArray("tenantList");

			// 遍历tenantList数组查找匹配的targetName
			String targetName = "研发项目管理组织"; // 要查找的targetName
			String targetOid = null; // 保存匹配的oid值
			for (JsonElement element : tenantListArray) {
				JsonObject tenantObject = element.getAsJsonObject();
				String name = tenantObject.get("name").getAsString();
				if (name.equals(targetName)) {
					targetOid = tenantObject.get("oid").getAsString();
					break;
				}
			}

			// 输出匹配的oid值
			if (targetOid != null) {
				System.out.println("Target OID: " + targetOid);
			} else {
				System.out.println("Target name not found.");
			}
			return targetOid;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}

	}

	// 自定义方法，用于获取字段值，如果为null，则返回null字符串，否则返回实际值
	private static String getAsStringOrNull(JsonObject jsonObject, String key) {
		JsonElement element = jsonObject.get(key);
		if (element != null && !element.isJsonNull()) {
			return element.getAsString();
		}
		return "null";
	}

}
