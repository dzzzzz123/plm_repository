package ext.sinoboom.pipingIntegration.servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ext.sinoboom.pipingIntegration.entity.ResponseMessage;
import ext.sinoboom.pipingIntegration.service.GetComponentMaterialCodeService;

public class GetComponentMaterialCodeServlet implements Controller {

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
			String SFeatureNO = jsonObject.getString("SFeatureNO");
			String SPartNO = jsonObject.getString("SPartNO");

			String CADNumber = GetComponentMaterialCodeService.generateNumber("1" + SFeatureNO + SPartNO);
			JSONObject resultJson = new JSONObject();
			resultJson.put("CADNumber", CADNumber);
			out.print(ResponseMessage.of().code(200).msg("获取物料编码成功").success(true).data(resultJson));

		} catch (Exception e) {
			out.print(ResponseMessage.of().code(301).msg("获取物料编码失败，错误信息：" + e.getMessage()).success(false));

		} finally {
			out.close();
		}

		return null;
	}

}
