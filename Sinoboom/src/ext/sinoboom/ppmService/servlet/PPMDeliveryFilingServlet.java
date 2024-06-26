package ext.sinoboom.ppmService.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ext.sinoboom.ppmService.config.PPMConfig;
import ext.sinoboom.ppmService.entity.ResponseMessage;
import ext.sinoboom.ppmService.util.CommUtil;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.content.URLData;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerRef;
import wt.pdmlink.PDMLinkProduct;
import wt.pom.Transaction;
import wt.pom.WTConnection;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;

public class PPMDeliveryFilingServlet implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setCharacterEncoding("utf-8");
		response.setContentType("appliction/json;charset=utf-8");
		PrintWriter out;
		out = response.getWriter();
		String number = PPMConfig.getConfig("deliveryProjectNumber");
		String name = PPMConfig.getConfig("deliveryProjectName");
		String url = PPMConfig.getConfig("deliverableURL");
		String folderPath = PPMConfig.getConfig("folderPath");

		Transaction t = new Transaction();
		try {
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
			StringBuilder responseStrBuilder = new StringBuilder();
			String inputStr;
			while ((inputStr = streamReader.readLine()) != null) {
				responseStrBuilder.append(inputStr);
			}
			System.out.println("responseStrBuilder.toString()>>>" + responseStrBuilder.toString());

			JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());

			String projectNumber = (String) jsonObject.get(number);
			String projectName = (String) jsonObject.get(name);
			String projectUrl = (String) jsonObject.get(url);

			t.start();
			List<String> productIds = getProductId(projectNumber);
			if (productIds.isEmpty() || productIds == null) {
				throw new Exception("没有此项目编号的产品");
			}
			for (String item : productIds) {
				PDMLinkProduct product = (PDMLinkProduct) getObjByOR(PDMLinkProduct.class, item);
				WTContainerRef containerRef = WTContainerRef.newWTContainerRef(product);

				Folder folder = FolderHelper.service.getFolder(folderPath, containerRef);
				String documentId = null;
				Long oid = getFolderOid(folder);
				if ("/Default".equals(folderPath)) {
					documentId = getDocumentId("0", projectName, "AND WTDocument.IDA3A2FOLDERINGINFO ='" + oid + "'");
				} else {
					documentId = getDocumentId(oid.toString(), projectName);
				}

				if (documentId != null) {
					WTDocument wtDocument = (WTDocument) getObjByOR(WTDocument.class, documentId);
					PersistenceHelper.manager.delete(wtDocument);
				}

				WTDocument doc = WTDocument.newWTDocument();
				doc.setName(projectName);
				FolderHelper.assignLocation((FolderEntry) doc, folder);

				doc = (WTDocument) wt.fc.PersistenceHelper.manager.store(doc);

				URLData data = URLData.newURLData(doc);
				data.setUrlLocation(projectUrl);
				data.setDisplayName(projectName);
				data.setDescription(projectName);
				data.setStale(true);

				data.setRole(ContentRoleType.PRIMARY);
				ContentServerHelper.service.updateContent(doc, data);

			}
			t.commit();
			out.print(ResponseMessage.of().msg("该项目交付件已成功归档").code(0));
		} catch (Exception e) {
			out.print(ResponseMessage.of().code(-1).msg("交付件归档失败，请联系管理员-- " + e.getMessage()));
			e.printStackTrace();
			t.rollback();
		}

		finally {
			out.close();
		}

		return null;
	}

	/**
	 * 根据项目编号得到产品id
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
	private Object getObjByOR(Class queryClass, String or) {
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

	private Long getFolderOid(Folder folder) throws WTException {
		Long id = PersistenceHelper.getObjectIdentifier(folder).getId();
		return id;
	}

	/**
	 * 根据Folder id 与name获取匹配document
	 * 
	 * @throws Exception
	 */

	private String getDocumentId(String folderId, String name) throws Exception {
		String SelectQuery = "SELECT WTDocument.IDA2A2 FROM WTDOCUMENT\r\n"
				+ "INNER JOIN WTDOCUMENTMASTER ON WTDOCUMENT.IDA3MASTERREFERENCE = WTDOCUMENTMASTER.IDA2A2\r\n"
				+ "WHERE WTDocument.IDA3B2FOLDERINGINFO= ? AND WTDOCUMENTMASTER.NAME = ?  ORDER BY WTDocument.CREATESTAMPA2 DESC FETCH FIRST 1 ROW ONLY";

		WTConnection con = CommUtil.getWTConnection();
		PreparedStatement statement = con.prepareStatement(SelectQuery);
		// 设置参数值
		statement.setString(1, folderId);
		statement.setString(2, name);

		ResultSet executeQuery = statement.executeQuery();
		String id = null;

		while (executeQuery.next()) {
			id = executeQuery.getString(1);
			System.out.println("DocmentIda2a2:" + id);
		}

		return id;
	}

	private String getDocumentId(String folderId, String name, String str) throws Exception {
		String SelectQuery = "SELECT WTDocument.IDA2A2 FROM WTDOCUMENT\r\n"
				+ "INNER JOIN WTDOCUMENTMASTER ON WTDOCUMENT.IDA3MASTERREFERENCE = WTDOCUMENTMASTER.IDA2A2\r\n"
				+ "WHERE WTDocument.IDA3B2FOLDERINGINFO= ? AND WTDOCUMENTMASTER.NAME = ? " + str
				+ " ORDER BY WTDocument.CREATESTAMPA2 DESC FETCH FIRST 1 ROW ONLY";

		WTConnection con = CommUtil.getWTConnection();
		PreparedStatement statement = con.prepareStatement(SelectQuery);
		// 设置参数值
		statement.setString(1, folderId);
		statement.setString(2, name);
		ResultSet executeQuery = statement.executeQuery();
		String id = null;

		while (executeQuery.next()) {
			id = executeQuery.getString(1);
			System.out.println("DocmentIda2a2:" + id);
		}

		return id;
	}

}
