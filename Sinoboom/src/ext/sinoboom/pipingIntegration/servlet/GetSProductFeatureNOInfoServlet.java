package ext.sinoboom.pipingIntegration.servlet;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.ptc.core.lwc.common.view.EnumerationDefinitionReadView;
import com.ptc.core.lwc.common.view.EnumerationEntryReadView;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;

import ext.sinoboom.pipingIntegration.entity.ResponseMessage;

public class GetSProductFeatureNOInfoServlet implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

		response.setCharacterEncoding("utf-8");
		response.setContentType("appliction/json;charset=utf-8");
		PrintWriter out = response.getWriter();

		try {

			EnumerationDefinitionReadView edr = TypeDefinitionServiceHelper.service.getEnumDefView("SProductFeatureNO");
			JSONArray resultJson = new JSONArray();
			if (edr != null) {
				Map<String, EnumerationEntryReadView> views = edr.getAllEnumerationEntries();
				Set<String> keysOfView = views.keySet();
				for (String key : keysOfView) {
					EnumerationEntryReadView view = views.get(key);
					String enumKey = view.getName();
					String enumName = view.getPropertyValueByName("displayName").getValue().toString();
					if (view.getPropertyValueByName("selectable").getValue().equals(true)) {// 此方法判断枚举值是否在可用列表
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("FeatureNumber", enumKey);
						jsonObject.put("FeatureName", enumName);
						resultJson.put(jsonObject);
					}

				}
			}
			out.print(ResponseMessage.of().success(true).code(200).data(resultJson).msg("产品特征码获取成功"));

		} catch (Exception e) {
			out.print(ResponseMessage.of().success(false).code(301).msg("产品特征码获取失败,请联系管理员"));
		} finally {
			out.close();
		}

		return null;
	}

}
