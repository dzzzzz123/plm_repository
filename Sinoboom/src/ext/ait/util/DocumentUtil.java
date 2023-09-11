package ext.ait.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.ptc.windchill.enterprise.part.commands.AssociationLinkObject;
import com.ptc.windchill.enterprise.part.commands.PartDocServiceCommand;

import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentRoleType;
import wt.content.URLData;
import wt.doc.WTDocument;
import wt.doc.WTDocumentDependencyLink;
import wt.doc.WTDocumentMaster;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentType;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.org.WTPrincipal;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.vc.VersionControlHelper;
import wt.vc.config.LatestConfigSpec;

public class DocumentUtil {

	/**
	 * 获取被参考文档
	 * 
	 * @param doc
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static ArrayList<WTDocument> getDepByDoc(WTDocument doc) throws WTException {
		ArrayList<WTDocument> doclist = new ArrayList<WTDocument>();
		try {
			QuerySpec qs = new QuerySpec(WTDocumentDependencyLink.class);
			qs.appendWhere(new SearchCondition(WTDocumentDependencyLink.class, "rolBeObjectRef.key.id",
					SearchCondition.EQUAL, doc.getPersistInfo().getObjectIdentifier().getId()));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			while (qr.hasMoreElements()) {
				WTDocumentDependencyLink link = (WTDocumentDependencyLink) qr.nextElement();
				WTDocument depByDoc = (WTDocument) link.getRoleAObject();
				doclist.add(depByDoc);
			}
		} catch (Exception e) {
			throw new WTException(e);
		}
		return doclist;
	}

	/**
	 * 根据编号获取对应最新版本的文档
	 * 
	 * @param number
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static WTDocument getDocByNumber(String number) {
		try {
			if (StringUtils.isBlank(number)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(WTDocument.class);
			qs.appendWhere(
					new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL, number.trim()));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			qr = new LatestConfigSpec().process(qr);
			if (qr.hasMoreElements()) {
				WTDocument doc = (WTDocument) qr.nextElement();
				return doc;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据编号获取最新版本的EPM文档
	 * 
	 * @param String
	 * @return EPMDocument
	 */
	@SuppressWarnings("deprecation")
	public static EPMDocument getEPMByNumber(String number) {
		try {
			if (StringUtils.isBlank(number)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(EPMDocument.class);
			qs.appendWhere(
					new SearchCondition(EPMDocument.class, EPMDocument.NUMBER, SearchCondition.EQUAL, number.trim()));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			qr = new LatestConfigSpec().process(qr);
			if (qr.hasMoreElements()) {
				EPMDocument doc = (EPMDocument) qr.nextElement();
				return doc;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据3D获取2D drawing
	 * 
	 * @param WTPart
	 * @return List<EPMDocument>
	 * @throws Exception
	 */
	public static List<EPMDocument> get2DDrawingByWTPart(WTPart part) throws Exception {
		EPMDocumentType cadDrawing = EPMDocumentType.toEPMDocumentType("CADDRAWING");
		List<EPMDocument> result = new ArrayList<>();
		try {
			QueryResult qr = PartDocServiceCommand.getAssociatedCADDocuments(part);// CAD文档
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof EPMDocument) {
					EPMDocument doc = (EPMDocument) obj;
					EPMDocumentType docType = doc.getDocType();
					if (cadDrawing.equals(docType)) {
						result.add(doc);
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
			throw e;
		}
		return result;
	}

	/**
	 * 获取部件关联的参考文档
	 * 
	 * @param WTPart
	 * @return List<WTDocument>
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static List<WTDocument> getReferenceDocumentsFromPart(WTPart part) throws WTException {
		QueryResult qr = null;
		List<WTDocument> list = new ArrayList<WTDocument>();
		try {
			// 根据part获取关联的参考文档
			qr = WTPartHelper.service.getReferencesWTDocumentMasters(part);
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof WTDocumentMaster) {
					WTDocumentMaster docmaster = (WTDocumentMaster) obj;
					QueryResult qs = VersionControlHelper.service.allVersionsOf(docmaster);
					WTDocument doc = (WTDocument) qs.nextElement();
					list.add(doc);
				}
			}

		} catch (WTException e) {
			e.printStackTrace();
			throw new WTException("No Reference Document on Part :" + part.getName());
		}
		return list;
	}

	/**
	 * 获取部件关联的说明方文档
	 * 
	 * @param WTPart
	 * @return List<WTDocument>
	 * @throws WTException
	 */
	public static List<WTDocument> getDescribedDocumentsFromPart(WTPart part) throws WTException {
		QueryResult qr = null;
		List<WTDocument> list = new ArrayList<WTDocument>();
		try {
			qr = WTPartHelper.service.getDescribedByDocuments(part);// 普通说明文档
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof WTDocument) {
					WTDocument doc = (WTDocument) obj;
					list.add(doc);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
			throw new WTException("No Described Document on Part :" + part.getName());
		}
		return list;
	}

	/**
	 * 部件与CAD文档， 说明关系 通过部件获取CAD文档，可能包含3D和2D 1）如果是结构部件，找结构3D图纸 CADCOMPONENT & PROE
	 * 2）如果是PCBA，找ECAD图纸 3）如果是软件部件，找autocad图纸
	 * 
	 * @param WTPart
	 * @return List<EPMDocument>
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static List<EPMDocument> getAllCADByPart(WTPart part) throws Exception {
		List<EPMDocument> docs = new ArrayList<EPMDocument>();
		WTPrincipal currentUser = null;
		try {
			currentUser = SessionHelper.manager.getPrincipal();
			SessionHelper.manager.setAdministrator();
			Collection cadDocumentsAndLinks = PartDocServiceCommand.getAssociatedCADDocumentsAndLinks(part);
			for (Object object : cadDocumentsAndLinks) {
				if (object instanceof AssociationLinkObject) {
					AssociationLinkObject alo = (AssociationLinkObject) object;
					EPMDocument doc = alo.getCadObject();
					docs.add(doc);
				}
			}

		} catch (WTException e) {
			e.printStackTrace();
			throw new Exception("No 3D CAD Document on Part :" + part.getName());
		} finally {
			if (currentUser != null) {
				SessionHelper.manager.setPrincipal(currentUser.getName());
			}
		}
		return docs;
	}

	/**
	 * 获取部件关联的.asm或者.drw文档
	 * 
	 * @param WTPart
	 * @param String
	 * @return EPMDocument
	 * @throws Exception
	 */
	public static EPMDocument get2DOr3DByPart(WTPart part, String flag) throws Exception {
		EPMDocument doc = null;
		WTPrincipal currentUser = null;
		try {
			currentUser = SessionHelper.manager.getPrincipal();
			SessionHelper.manager.setAdministrator();
			Collection cadDocumentsAndLinks = PartDocServiceCommand.getAssociatedCADDocumentsAndLinks(part);
			for (Object object : cadDocumentsAndLinks) {
				if (object instanceof AssociationLinkObject) {
					AssociationLinkObject alo = (AssociationLinkObject) object;
					doc = alo.getCadObject();
					if (StringUtils.endsWithIgnoreCase(doc.getCADName(), ".drw") && flag.equals("2D")) {// 2d drw
						return doc;
					} else if (StringUtils.endsWithIgnoreCase(doc.getCADName(), ".asw") && flag.equals("3D")) {
						return doc;
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
			throw new Exception("No ECAD Document on Part :" + part.getName());
		} finally {
			if (currentUser != null) {
				SessionHelper.manager.setPrincipal(currentUser.getName());
			}
		}
		return null;
	}

	/**
	 * 获取文档的主要内容
	 * 
	 * @param ContentHolder
	 * @return ArrayList<ApplicationData>
	 */
	public static ArrayList<ApplicationData> getSecondaryContent(ContentHolder holder) {
		ContentHolder contentHolder = null;
		ArrayList<ApplicationData> dataList = new ArrayList<ApplicationData>();
		try {
			contentHolder = ContentHelper.service.getContents(holder);
			QueryResult qr = ContentHelper.service.getContentsByRole(contentHolder, ContentRoleType.SECONDARY);
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof URLData) {

				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return dataList;
	}
}