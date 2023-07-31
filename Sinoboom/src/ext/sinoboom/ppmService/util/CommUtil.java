package ext.sinoboom.ppmService.util;

import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.ptc.core.htmlcomp.util.TypeHelper;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.meta.type.mgmt.common.TypeDefinitionDefaultView;
import com.ptc.core.meta.type.mgmt.server.impl.WTTypeDefinition;

import ext.util.ChangeSession;
import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.epm.util.EPMSoftTypeServerUtilities;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.method.MethodContext;
import wt.org.WTGroup;
import wt.part.PartUsesOccurrence;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartUsageLink;
import wt.pds.StatementSpec;
import wt.pom.WTConnection;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.type.TypeDefinitionReference;
import wt.util.WTException;
import wt.vc.VersionControlHelper;
import wt.vc.config.LatestConfigSpec;
import wt.vc.views.View;
import wt.vc.views.ViewHelper;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

/**
 * 工具类
 */

public class CommUtil {

	public static String Design = "Design";
	private static long viewId_Design = 0L;
	public static View view_Design = null;

	public static String Manufacturing = "Manufacturing";
	private static long viewId_Manufacturing = 0L;
	public static View view_Manufacturing = null;
	static {
		try {
			view_Design = ViewHelper.service.getView(Design);
			viewId_Design = view_Design.getPersistInfo().getObjectIdentifier().getId();
			view_Manufacturing = ViewHelper.service.getView(Manufacturing);
			viewId_Manufacturing = view_Manufacturing.getPersistInfo().getObjectIdentifier().getId();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isCheckOut(Workable workable) {
		try {
			return WorkInProgressHelper.isCheckedOut(workable);
		} catch (WTException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 获取Design视图part
	 * 
	 * @return WTPart
	 * @param
	 */
	public static WTPart getDesignWTPart(String number) {
		WTPart part = null;
		try {
			QuerySpec qs = new QuerySpec(WTPart.class);
			SearchCondition scnumber = new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, number);
			qs.appendWhere(scnumber);
			qs.appendAnd();
			SearchCondition sc = new SearchCondition(WTPart.class, "view.key.id", SearchCondition.EQUAL, viewId_Design);
			qs.appendWhere(sc);
			QueryResult qr = PersistenceHelper.manager.find(qs);
			LatestConfigSpec cfg = new LatestConfigSpec();
			qr = cfg.process(qr);
			if (qr != null && qr.hasMoreElements()) {
				part = (WTPart) qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return part;
	}

	/**
	 * 获取EPM文档
	 * 
	 * @return EPMDocument
	 * @param
	 */
	public static EPMDocument getEPM_DOC(String number) {
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
	 * 获取最新版本的文档
	 */
	public static WTDocument getDoc(String number) {
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
	 * 获取doc or part类型显示名称
	 * 
	 * @return String
	 * @param
	 */
	public static String getDisplayName(WTObject obj) {
		String typeDisplayName = null;
		TypeDefinitionReference ref = null;
		WTTypeDefinition definition = null;
		if (obj instanceof WTDocument) {
			ref = ((WTDocument) obj).getTypeDefinitionReference();
		} else if (obj instanceof WTPart) {
			ref = ((WTPart) obj).getTypeDefinitionReference();
		}
		try {
			@SuppressWarnings("deprecation")
			TypeDefinitionDefaultView view = EPMSoftTypeServerUtilities.getTypeDefinition(ref);
			definition = (WTTypeDefinition) PersistenceHelper.manager.refresh(view.getObjectID());
			TypeIdentifier typeIdentifier = TypeIdentifierHelper.getTypeIdentifier(definition.getName());
			typeDisplayName = TypeHelper.getTypeIdentifierDisplayName(typeIdentifier);
		} catch (WTException e) {
			e.printStackTrace();
		}

		return typeDisplayName;
	}

	/**
	 * 根据编号查询part
	 * 
	 * @param number
	 * @return part
	 */
	public static WTPart getWTPartByNumber(String number) {
		WTPart result = null;
		QueryResult qr = null;
		try {
			QuerySpec qs = new QuerySpec(WTPart.class);
			SearchCondition scnumber = new SearchCondition(WTPart.class, wt.part.WTPart.NUMBER, SearchCondition.EQUAL,
					number.toUpperCase());
			qs.appendSearchCondition(scnumber);
			qs.appendAnd();
			SearchCondition sclatest = VersionControlHelper.getSearchCondition(wt.part.WTPart.class, true);
			qs.appendSearchCondition(sclatest);
			qr = PersistenceHelper.manager.find(qs);
			LatestConfigSpec cfg = new LatestConfigSpec();
			qr = cfg.process(qr);
			if (qr != null && qr.hasMoreElements()) {
				result = (WTPart) qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 根据part得到其子部件
	 */
	public static List<WTPart> getBomByPart(WTPart ProductPart) {
		WTPart sPart = null;
		QueryResult qr2 = null;
		List<WTPart> list = new ArrayList<WTPart>();
		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(ProductPart);
			while (qr.hasMoreElements()) {
				WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
				qr2 = VersionControlHelper.service.allVersionsOf(usageLink.getUses());
				if (qr2.hasMoreElements()) {
					sPart = (WTPart) qr2.nextElement();
					list.add(sPart);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 根据part得到其子部件link
	 */
	public static List<WTPartUsageLink> getBomLinkByPart(WTPart ProductPart) {
		List<WTPartUsageLink> list = new ArrayList<WTPartUsageLink>();
		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(ProductPart);
			while (qr.hasMoreElements()) {
				WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
				list.add(usageLink);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	public static Set<WTPart> getAllBomByPart(WTPart productPart, Set<WTPart> list) {
		WTPart sPart = null;
		QueryResult qr2 = null;
		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(productPart);
			while (qr.hasMoreElements()) {
				WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
				qr2 = VersionControlHelper.service.allVersionsOf(usageLink.getUses());
				if (qr2.hasMoreElements()) {
					sPart = (WTPart) qr2.nextElement();
					list.add(sPart);
					getAllBomByPart(sPart, list);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 获取原样位号
	 * 
	 * @param WTPartUsageLink
	 * @return String
	 */
	public static String getPartUsesOccurrence(WTPartUsageLink useagelink) throws WTException {
		List listWTPart2 = new ArrayList();
		long linkid = PersistenceHelper.getObjectIdentifier(useagelink).getId();
		QuerySpec qs = new QuerySpec(PartUsesOccurrence.class);
		qs.appendWhere(
				new SearchCondition(PartUsesOccurrence.class, "linkReference.key.id", SearchCondition.EQUAL, linkid));
		QueryResult qr = PersistenceHelper.manager.find(qs);
		String occurrenceStr = "";
		while (qr.hasMoreElements()) {
			PartUsesOccurrence occurrence = (PartUsesOccurrence) qr.nextElement();
			listWTPart2.add(occurrence.getName());
		}
		for (int i = 0; i < listWTPart2.size(); i++) {
			if (listWTPart2.get(i) != null) {
				String ocName = listWTPart2.get(i).toString();
				if (i == 0) {
					occurrenceStr = ocName;
				} else {
					occurrenceStr = occurrenceStr + "," + ocName;
				}
			}
		}
		String numberStr = sortPlaceNumber(occurrenceStr);// 对位号排序
		return numberStr;
	}

	// 对位号排序
	public static String sortPlaceNumber(String numberStr) {
		TreeMap tm = new TreeMap();
		String[] s = numberStr.split(",");
		for (int i = 0; i < s.length; i++) {
			tm.put(s[i], s[i]);
		}
		Iterator it = tm.keySet().iterator();
		String number = "";
		int a = 0;
		while (it.hasNext()) {
			if (a == 0) {
				number = (String) it.next();
			} else {
				number = number + "," + (String) it.next();
			}
			a++;
		}
		return number;
	}

	/**
	 * 获取windchill DB链接
	 * 
	 * @return WTConnection
	 * @param
	 */
	public static WTConnection getWTConnection() throws Exception {
		MethodContext methodcontext = MethodContext.getContext();
		WTConnection wtconnection = (WTConnection) methodcontext.getConnection();
		return wtconnection;
	}

	public static boolean executeUpdateSQL(String sql) {
		Statement stm = null;
		boolean flag = false;
		try {
			WTConnection wtconnection = getWTConnection();
			stm = wtconnection.prepareStatement(sql);
			stm.executeUpdate(sql);
//			System.out.println("sql===="+sql);
			flag = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stm != null) {
				try {
					stm.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return flag;
	}

	/**
	 * 转换日期格式
	 * 
	 * @param value     riqi
	 * @param formatStr "yyyy-MM-dd HH:mm:ss"
	 * @return String
	 */
	public static String formatDate(Date date, String formatStr) {
		String currentTime = "";
		try {
			TimeZone zone = TimeZone.getTimeZone("GMT+8:00");
			SimpleDateFormat sdf = new SimpleDateFormat(formatStr, Locale.ENGLISH);
			sdf.setTimeZone(zone);
			currentTime = sdf.format(date);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return currentTime;
	}

	public static WTGroup queryGroupByName(String groupName) {
		try {
			ChangeSession.administratorSession();
			if (StringUtils.isBlank(groupName)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(WTGroup.class);
			SearchCondition sc = new SearchCondition(WTGroup.class, WTGroup.NAME, SearchCondition.EQUAL,
					groupName.trim());
			int[] index = { 0 };
			qs.appendWhere(sc, index);
			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			while (qr.hasMoreElements()) {
				WTGroup group = (WTGroup) qr.nextElement();
				return group;
			}
		} catch (WTException e) {
			e.printStackTrace();
		} finally {
			ChangeSession.goPreviousSession();
		}
		return null;
	}
}
