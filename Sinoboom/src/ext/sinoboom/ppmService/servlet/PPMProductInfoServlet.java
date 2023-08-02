package ext.sinoboom.ppmService.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ext.sinoboom.ppmService.config.PPMConfig;
import ext.sinoboom.ppmService.entity.ResponseMessage;
import ext.sinoboom.ppmService.util.CommUtil;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.httpgw.URLFactory;
import wt.pdmlink.PDMLinkProduct;
import wt.pom.WTConnection;
import wt.query.QuerySpec;
import wt.query.SearchCondition;

public class PPMProductInfoServlet implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String projectNumber = PPMConfig.getConfig("projectNumber");
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
			System.out.println("responseStrBuilder.toString()>>>" + responseStrBuilder.toString());

			JSONArray jsonArray = new JSONArray(responseStrBuilder.toString());
			JSONArray resultJson = new JSONArray();
			ReferenceFactory ref = new ReferenceFactory();
			URLFactory factory = new URLFactory();
			Set<String> processedProductIds = new HashSet<>();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String object = (String) jsonObject.get(projectNumber);
				List<String> productIdList = getProductId(object);

				for (String productId : productIdList) {

					if (processedProductIds.contains(productId)) {
						continue;
					}

					processedProductIds.add(productId); // 将当前productId添加到Set中，标记为已处理

					PDMLinkProduct pdmLinkProduct = (PDMLinkProduct) getObjByOR(PDMLinkProduct.class, productId);

					String url = factory.getHREF("app/#ptc1/comp/wt.pdmlink.PDMLinkProduct.infoPage") + "?oid="
							+ ref.getReferenceString(pdmLinkProduct);

					JSONObject item = new JSONObject();
					item.put("Name", pdmLinkProduct.getName().toString());
					item.put("Url", url);

					resultJson.put(item);
				}
			}

			out.print(ResponseMessage.of().msg("获取产品库地址成功").code(0).data(resultJson));
		} catch (Exception e) {
			out.print(ResponseMessage.of().msg("获取产品库地址失败").code(-1));
			e.printStackTrace();
		}

		finally {
			out.close();
		}
		return null;
	}

	/**
	 * 根据项目编号得到产品id
	 * 
	 */
	private List<String> getProductId(String vulue) throws Exception {
		String projectNumber = PPMConfig.getConfig("projectNumber");
		String SelectQuery = "SELECT IDA3A4 FROM URLVALUE WHERE IDA3A6 =(SELECT IDA2A2 FROM URLDefinition WHERE NAME ='"
				+ projectNumber + "') AND URLVALUE.DESCRIPTION = ? ";

		WTConnection con = CommUtil.getWTConnection();
		PreparedStatement statement = con.prepareStatement(SelectQuery);
		// 设置参数值
		statement.setString(1, vulue);
		ResultSet executeQuery = statement.executeQuery();

		List<String> resultList = new ArrayList<>();

		while (executeQuery.next()) {
			String id = executeQuery.getString(1);
			System.out.println("ida2a2:" + id);
			resultList.add(id);
		}

		return resultList;
	}

	/**
	 * 根据id获取对象
	 */
	private static Object getObjByOR(Class queryClass, String or) {
		try {
			if (StringUtils.isBlank(or)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(queryClass);
			qs.appendWhere(new SearchCondition(queryClass, "thePersistInfo.theObjectIdentifier.id",
					SearchCondition.EQUAL, Long.valueOf(or)));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			if (qr.hasMoreElements()) {
				return qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

}
