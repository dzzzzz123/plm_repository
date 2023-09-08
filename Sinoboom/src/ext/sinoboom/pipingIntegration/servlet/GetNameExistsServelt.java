package ext.sinoboom.pipingIntegration.servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ext.sinoboom.pipingIntegration.entity.ResponseMessage;
import ext.sinoboom.pipingIntegration.util.CommUtil;
import wt.pom.WTConnection;

public class GetNameExistsServelt implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

		response.setCharacterEncoding("utf-8");
		response.setContentType("appliction/json;charset=utf-8");
		PrintWriter out = response.getWriter();

		try {
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
			StringBuilder responseStrBuilder = new StringBuilder();
			String inputStr;
			while ((inputStr = streamReader.readLine()) != null) {
				responseStrBuilder.append(inputStr);
			}

			JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
			String creoFileName = (String) jsonObject.get("CreoFileName");

			if (ifExists(creoFileName)) {
				out.print(ResponseMessage.of().code(200).msg("获取windchill数据库中是否有重名结果成功").success(true));

			} else {
				out.print(ResponseMessage.of().code(200).msg("获取windchill数据库中是否有重名结果成功").success(false));
			}

		} catch (Exception e) {
			out.print(ResponseMessage.of().code(301).msg("获取Windchill数据库中是否有重名结果失败，请联系管理员").success(false));

		} finally {
			out.close();
		}

		return null;
	}

	/**
	 * 根据creoFileName判断CAD文件是存在
	 */
	private boolean ifExists(String creoFileName) throws Exception {
		String SelectQuery = "SELECT IDA2A2 FROM EPMDocumentMaster WHERE CADNAME = ? ";
		WTConnection con = CommUtil.getWTConnection();
		PreparedStatement statement = con.prepareStatement(SelectQuery);
		// 设置参数值
		statement.setString(1, creoFileName);
		ResultSet executeQuery = statement.executeQuery();

		String id = null;
		while (executeQuery.next()) {
			id = executeQuery.getString(1);
		}
		if (id != null) {
			return true;
		}

		return false;
	}

}
