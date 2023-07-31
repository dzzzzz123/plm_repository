package ext.sinoboom.ppmService.servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ext.sinoboom.ppmService.config.PPMConfig;
import ext.sinoboom.ppmService.entity.PPMProjectEntity;

public class PPMProductAddProjectServlet implements Controller {

	public static List<PPMProjectEntity> list = new ArrayList();

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setCharacterEncoding("utf-8");
		response.setContentType("appliction/json;charset=utf-8");
		PrintWriter out = response.getWriter();
		try {
			list.clear();

			String number = PPMConfig.getConfig("projectNumber");
			String name = PPMConfig.getConfig("projectName");
			String status = PPMConfig.getConfig("projectStatus");
			String time = PPMConfig.getConfig("projectTime");
			String url = PPMConfig.getConfig("projectUrl");

			BufferedReader streamReader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
			StringBuilder responseStrBuilder = new StringBuilder();
			String inputStr;
			while ((inputStr = streamReader.readLine()) != null) {
				responseStrBuilder.append(inputStr);
			}
			System.out.println("responseStrBuilder.toString()>>>" + responseStrBuilder.toString());

			JSONArray jsonArray = new JSONArray(responseStrBuilder.toString());
			JSONArray resultJson = new JSONArray();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String projectNumber = (String) jsonObject.get(number);
				String projectName = (String) jsonObject.get(name);
				String projectStatus = (String) jsonObject.get(status);
				String projectTime = (String) jsonObject.get(time);
				String projectUrl = (String) jsonObject.get(url);
				PPMProjectEntity ppmProjectEntity = new PPMProjectEntity();
				ppmProjectEntity.setProjectName(projectName);
				ppmProjectEntity.setProjectNumber(projectNumber);
				ppmProjectEntity.setProjectStatus(projectStatus);
				ppmProjectEntity.setProjectTime(projectTime);
				ppmProjectEntity.setProjectUrl(projectUrl);
				list.add(ppmProjectEntity);
			}

			out.print(resultJson.toString());
		} catch (Exception e) {
			out.print(e.getMessage());
			e.printStackTrace();
		}

		finally {
			out.close();

		}

		return null;
	}

}
