package ext.ait.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.ptc.core.lwc.server.LWCLocalizablePropertyValue;
import com.ptc.core.lwc.server.LWCPropertyDefinition;
import com.ptc.core.lwc.server.LWCStructEnumAttTemplate;
import com.ptc.core.meta.type.mgmt.common.TypeDefinitionDefaultView;
import com.ptc.core.meta.type.mgmt.server.impl.WTTypeDefinition;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.windchill.enterprise.part.commands.AssociationLinkObject;
import com.ptc.windchill.enterprise.part.commands.PartDocServiceCommand;
import com.ptc.windchill.enterprise.workflow.WorkflowCommands;

import wt.configuration.TraceCode;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.epm.EPMDocument;
import wt.epm.build.EPMBuildRule;
import wt.epm.util.EPMSoftTypeServerUtilities;
import wt.fc.Identified;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.folder.CabinetBased;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleState;
import wt.lifecycle.State;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.org.WTPrincipal;
import wt.part.PartType;
import wt.part.PartUsesOccurrence;
import wt.part.QuantityUnit;
import wt.part.Source;
import wt.part.WTPart;
import wt.part.WTPartAlternateLink;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.part.WTPartSubstituteLink;
import wt.part.WTPartUsageLink;
import wt.pds.StatementSpec;
import wt.query.CompositeWhereExpression;
import wt.query.ConstantExpression;
import wt.query.LogicalOperator;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.query.TableColumn;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.type.TypeDefinitionReference;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.baseline.ManagedBaseline;
import wt.vc.config.LatestConfigSpec;
import wt.vc.struct.StructHelper;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;
import wt.workflow.engine.WfProcess;
import wt.workflow.engine.WfState;

public class PartUtil {

	/**
	 * @description 得到对象的自定义/内部名称
	 * @param WTObject
	 * @return String
	 * @throws WTException
	 */
	public static String getTypeName(WTObject obj) {
		String typeDisplayName = null;
		TypeDefinitionReference ref = null;
		WTTypeDefinition definition = null;
		if (obj instanceof WTPart) {
			ref = ((WTPart) obj).getTypeDefinitionReference();
			try {
				@SuppressWarnings("deprecation")
				TypeDefinitionDefaultView view = EPMSoftTypeServerUtilities.getTypeDefinition(ref);
				definition = (WTTypeDefinition) PersistenceHelper.manager.refresh(view.getObjectID());
				typeDisplayName = definition.getDisplayNameKey(); // 类型的key
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return typeDisplayName;
	}

	/**
	 * 取得所有子部件的关联
	 * 
	 * @param WTPart
	 * @return Vector<WTPartUsageLink>
	 */
	@SuppressWarnings("unchecked")
	public static Vector<WTPartUsageLink> getSubPartUsagelinks(WTPart parentPart) throws WTException {
		Vector<WTPartUsageLink> partlist = new Vector<>();
		QueryResult subParts = WTPartHelper.service.getUsesWTPartMasters(parentPart);
		while (subParts.hasMoreElements()) {
			WTPartUsageLink usagelink = (WTPartUsageLink) subParts.nextElement();
			partlist.addElement(usagelink);
		}
		return partlist;
	}

	/**
	 * 根据编号查询part
	 * 
	 * @param String
	 * @return WTPart
	 */
	@SuppressWarnings("deprecation")
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
			QueryResult qr1 = cfg.process(qr);
			if (qr1 != null && qr1.hasMoreElements()) {
				result = (WTPart) qr1.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 不递归获取部件所有的子部件
	 * 
	 * @param WTPart
	 * @return List<WTPart>
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
	 * 获取分类的全名称/路径（暂不清楚）
	 * 
	 * @param LWCStructEnumAttTemplate
	 * @return String
	 */
	public static String getClassificationFullPath(LWCStructEnumAttTemplate structureEum) {
		String part = "";
		try {
			QuerySpec queryspec = new QuerySpec();
			int a = queryspec.appendClassList(LWCLocalizablePropertyValue.class, true);
			int b = queryspec.appendClassList(LWCPropertyDefinition.class, false);
			queryspec.setAdvancedQueryEnabled(true);
			String[] aliases = new String[2];
			aliases[0] = queryspec.getFromClause().getAliasAt(a);
			aliases[1] = queryspec.getFromClause().getAliasAt(b);
			TableColumn tc1 = new TableColumn(aliases[0], "IDA3C4");
			TableColumn tc3 = new TableColumn(aliases[0], "CLASSNAMEKEYC4");
			TableColumn tc11 = new TableColumn(aliases[0], "IDA3B4");
			TableColumn tc33 = new TableColumn(aliases[0], "CLASSNAMEKEYB4");
			TableColumn tc2 = new TableColumn(aliases[0], "IDA3A4");
			TableColumn tc4 = new TableColumn(aliases[1], "IDA2A2");
			TableColumn tc5 = new TableColumn(aliases[1], "NAME");
			TableColumn tc6 = new TableColumn(aliases[1], "CLASSNAME");
			CompositeWhereExpression andExpression = new CompositeWhereExpression(LogicalOperator.AND);
			andExpression.append(new SearchCondition(tc1, "=",
					new ConstantExpression(structureEum.getPersistInfo().getObjectIdentifier().getId())));
			andExpression.append(new SearchCondition(tc3, "=",
					new ConstantExpression("com.ptc.core.lwc.server.LWCStructEnumAttTemplate")));
			andExpression.append(new SearchCondition(tc11, "=",
					new ConstantExpression(structureEum.getPersistInfo().getObjectIdentifier().getId())));
			andExpression.append(new SearchCondition(tc33, "=",
					new ConstantExpression("com.ptc.core.lwc.server.LWCStructEnumAttTemplate")));
			andExpression.append(new SearchCondition(tc2, "=", tc4));
			andExpression.append(new SearchCondition(tc5, "=", new ConstantExpression("displayName")));
			andExpression.append(new SearchCondition(tc6, "=",
					new ConstantExpression("com.ptc.core.lwc.server.LWCAbstractAttributeTemplate")));
			queryspec.appendWhere(andExpression, null);

			QueryResult qr = PersistenceHelper.manager.find(queryspec);
			if (qr.hasMoreElements()) {
				Object[] nextElement = (Object[]) qr.nextElement();
				LWCLocalizablePropertyValue value = (LWCLocalizablePropertyValue) nextElement[0];
				String zh = value.getValue(Locale.CHINA);
				if (StringUtils.isBlank(zh)) {
					return value.getValue();
				} else {
					return zh;
				}
			}
		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		}
		return part;

	}

	/**
	 * 获取分类的父节点（暂不清楚）
	 * 
	 * @param LWCStructEnumAttTemplate
	 * @return LWCStructEnumAttTemplate
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static LWCStructEnumAttTemplate getParentStructEnum(LWCStructEnumAttTemplate structureEum)
			throws WTException {
		boolean setAccessEnforced = SessionServerHelper.manager.setAccessEnforced(false);
		LWCStructEnumAttTemplate result = null;
		try {
			if (structureEum.getParent() == null) {
				return result;
			}
			QuerySpec queryspec = new QuerySpec(LWCStructEnumAttTemplate.class);
			queryspec.appendWhere(new SearchCondition(LWCStructEnumAttTemplate.class,
					"thePersistInfo.theObjectIdentifier", "=", structureEum.getParentReference().getObjectId()),
					new int[] {});
			QueryResult qr = PersistenceServerHelper.manager.query(queryspec);
			if (qr.hasMoreElements()) {
				result = (LWCStructEnumAttTemplate) qr.nextElement();
			}
		} catch (WTException e) {
			throw e;
		} finally {
			SessionServerHelper.manager.setAccessEnforced(setAccessEnforced);
		}
		return result;
	}

	/**
	 * 根据名称获取分类节点
	 * 
	 * @param String
	 * @return LWCStructEnumAttTemplate
	 * @throws WTException
	 */
	public static LWCStructEnumAttTemplate getStructureEum(String name) throws WTException {
		LWCStructEnumAttTemplate result = null;
		QuerySpec queryspec = new QuerySpec(LWCStructEnumAttTemplate.class);
		queryspec.appendWhere(
				new SearchCondition(LWCStructEnumAttTemplate.class, LWCStructEnumAttTemplate.NAME, "=", name),
				new int[] {});
//		queryspec.appendAnd();
//		queryspec.appendWhere(new SearchCondition(LWCStructEnumAttTemplate.class,
//				LWCStructEnumAttTemplate.DELETED_ID, SearchCondition.IS_NULL), new int[] {});
		QueryResult qr = PersistenceServerHelper.manager.query(queryspec);
		while (qr.hasMoreElements()) {
			result = (LWCStructEnumAttTemplate) qr.nextElement();
			if (StringUtils.isBlank(result.getDeletedId())) {
				return result;
			}
		}
		return result;
	}

//	这里缺少ClassificationNodeDefaultView类，在12.x后windchill不再让直接调用这个包
//	public static String getClassificationFullPath(ClassificationNodeDefaultView clsNodeDftView) {
//		if (clsNodeDftView == null)
//			return "";
//		String clsPath = clsNodeDftView.getName();
//		ClassificationNodeDefaultView tmpNodeView = null;
//		try {
//			for (tmpNodeView = ClassificationHelper.service.getParentNodeDefaultView(
//					clsNodeDftView); tmpNodeView != null; tmpNodeView = ClassificationHelper.service
//							.getParentNodeDefaultView(tmpNodeView))
//				clsPath = tmpNodeView.getName() + "/" + clsPath;
//		} catch (RemoteException rme2) {
//			rme2.printStackTrace();
//		} catch (CSMClassificationNavigationException csmclassificationnavigationexception2) {
//			csmclassificationnavigationexception2.printStackTrace();
//		} catch (WTException wte2) {
//			wte2.printStackTrace();
//		}
//		return clsPath;
//	}
//
//	public static ClassificationNodeDefaultView getClassificationNodeDefaultView(ClassificationNode clsNode) {
//		ClassificationNodeDefaultView cndv = null;
//		if (clsNode != null) {
//			try {
//				cndv = ClassificationObjectsFactory.newClassificationNodeDefaultView(clsNode);
//			} catch (CSMClassificationNavigationException cne) {
//				return null;
//			}
//		}
//		return cndv;
//	}

	/**
	 * 修改部件编号
	 * 
	 * @param WTPart
	 * @param String
	 * @return boolean
	 * @throws Exception
	 */
	public static boolean changePartNumber(WTPart part, String newPartNumber) throws Exception {
		try {
			part = (WTPart) PersistenceHelper.manager.refresh(part);
			Identified identified = (Identified) part.getMaster();
			WTPartMasterIdentity partIdentity = (WTPartMasterIdentity) identified.getIdentificationObject();
			partIdentity.setNumber(newPartNumber);
			identified = wt.fc.IdentityHelper.service.changeIdentity(identified, partIdentity);
			part = (WTPart) PersistenceHelper.manager.refresh(part);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 修改部件名称
	 * 
	 * @param WTPart
	 * @param String
	 * @return WTPart
	 * @throws WTException
	 */
	public static WTPart changePartName(WTPart part, String newName) throws WTException {
		try {
			part = (WTPart) PersistenceHelper.manager.refresh(part);
			Identified identified = (Identified) part.getMaster();
			WTPartMasterIdentity partIdentity = (WTPartMasterIdentity) identified.getIdentificationObject();
			partIdentity.setName(newName);
			identified = wt.fc.IdentityHelper.service.changeIdentity(identified, partIdentity);
			part = (WTPart) PersistenceHelper.manager.refresh(part);
			return part;
		} catch (Exception e) {
			e.printStackTrace();
			throw new WTException(e.getMessage());
		}
	}

	/**
	 * 获取原样位号
	 * 
	 * @param WTPartUsageLink
	 * @return String
	 * @throws WTException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
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
		return sortPlaceNumber(occurrenceStr);
	}

	// 对位号排序
	public static String sortPlaceNumber(String numberStr) {
		TreeMap<String, String> tm = new TreeMap<>();
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
	 * 获取替代料的关联关系
	 * 
	 * @param WTPartUsageLink
	 * @return List<WTPartSubstituteLink>
	 * @throws WTException
	 */
	public static List<WTPartSubstituteLink> getSubstitutePartsLink(WTPartUsageLink useagelink) throws WTException {
		if (useagelink == null) {
			return null;
		}
		List<WTPartSubstituteLink> list = new ArrayList<WTPartSubstituteLink>();
		long linkid = PersistenceHelper.getObjectIdentifier(useagelink).getId();
		int[] index = { 0 };
		QuerySpec qs = new QuerySpec(WTPartSubstituteLink.class);
		qs.appendWhere(
				new SearchCondition(WTPartSubstituteLink.class, "roleAObjectRef.key.id", SearchCondition.EQUAL, linkid),
				index);
		QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
		while (qr.hasMoreElements()) {
			WTPartSubstituteLink sLink = (WTPartSubstituteLink) qr.nextElement();
			list.add(sLink);
		}
		return list;
	}

	/**
	 * 获取替代料
	 * 
	 * @param WTPartUsageLink
	 * @return List<WTPart>
	 * @throws WTException
	 */
	public static List<WTPart> getSubstituteParts(WTPartUsageLink useagelink) throws WTException {
		List<WTPart> list = new ArrayList<WTPart>();
		List<WTPartSubstituteLink> wtPartSubstituteLinks = getSubstitutePartsLink(useagelink);
		for (WTPartSubstituteLink wtPartSubstituteLink : wtPartSubstituteLinks) {
			WTPartMaster partmast = wtPartSubstituteLink.getSubstitutes();
			WTPart part = getWTPartByNumber(partmast.getNumber());
			list.add(part);
		}
		return list;
	}

	/**
	 * 获取替代料的编号，用 | 隔开
	 * 
	 * @param WTPartUsageLink
	 * @return String
	 * @throws WTException
	 */
	public static String getSubstitutePart(WTPartUsageLink useagelink) throws WTException {
		String nums = "";
		List<WTPart> wtParts = getSubstituteParts(useagelink);
		for (WTPart wtPart : wtParts) {
			String s = wtPart.getNumber();
			if (nums == "") {
				nums = s;
			} else {
				nums = nums + "|" + s;
			}
		}
		return nums;
	}

	/**
	 * 根据部件获取替代料的关联关系
	 * 
	 * @param WTPart
	 * @return List<WTPartAlternateLink>
	 * @throws WTException
	 */
	public static List<WTPartAlternateLink> getWTPartAlternateLinks(WTPart wtpart) throws WTException {
		if (wtpart == null) {
			return null;
		}
		List<WTPartAlternateLink> list = new ArrayList<WTPartAlternateLink>();
		long masterId = PersistenceHelper.getObjectIdentifier(wtpart.getMaster()).getId();
		int[] index = { 0 };
		QuerySpec qs = new QuerySpec(WTPartAlternateLink.class);
		qs.appendWhere(new SearchCondition(WTPartAlternateLink.class, "roleAObjectRef.key.id", SearchCondition.EQUAL,
				masterId), index);
		QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
		while (qr.hasMoreElements()) {
			WTPartAlternateLink sLink = (WTPartAlternateLink) qr.nextElement();
			list.add(sLink);
		}
		return list;
	}

	/**
	 * 修改部件生命周期状态
	 * 
	 * @param WTPart
	 * @param String
	 * @return String
	 * @throws WTPropertyVetoException
	 * @throws WTException
	 */
	public static String changePartState(WTPart part, String state) throws WTPropertyVetoException, WTException {
		String message = "";
		WTPrincipal currentUser = null;
		try {
			currentUser = SessionHelper.manager.getPrincipal();
			SessionHelper.manager.setAdministrator();

			part = (WTPart) wt.lifecycle.LifeCycleHelper.service.setLifeCycleState((wt.lifecycle.LifeCycleManaged) part,
					State.toState(state));

			PersistenceHelper.manager.refresh(part);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WTException(e);
		} finally {
			if (currentUser != null) {
				SessionHelper.manager.setPrincipal(currentUser.getName());
			}
		}
		return message;
	}

	/**
	 * 修改部件状态（直接使用State进行修改）
	 * 
	 * @param WTPart
	 * @param State
	 * @return String
	 * @throws WTPropertyVetoException
	 * @throws WTException
	 */
	public static String changePartState(WTPart part, State state) throws WTPropertyVetoException, WTException {
		String message = "";
		WTPrincipal currentUser = null;
		try {
			currentUser = SessionHelper.manager.getPrincipal();
			SessionHelper.manager.setAdministrator();

			part = (WTPart) wt.lifecycle.LifeCycleHelper.service.setLifeCycleState((wt.lifecycle.LifeCycleManaged) part,
					state);

			PersistenceHelper.manager.refresh(part);
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (currentUser != null) {
				SessionHelper.manager.setPrincipal(currentUser.getName());
			}
		}
		return message;
	}

	/**
	 * 修改几乎所有存在于Windchill中对象的状态
	 * 
	 * @param Object
	 * @param Object
	 * @return String
	 * @throws WTPropertyVetoException
	 * @throws WTException
	 */
	public static String changeObjectState(Object obj, Object objState) throws WTPropertyVetoException, WTException {
		String message = "";
		State newState = null;
		WTPrincipal currentUser = null;
		try {
			currentUser = SessionHelper.manager.getPrincipal();
			SessionHelper.manager.setAdministrator();
			if (objState instanceof State) {
				newState = (State) objState;
			} else if (objState instanceof String) {
				newState = State.toState(objState + "");
			}

			if (obj instanceof WTPart) {
				WTPart part = (WTPart) obj;
				part = (WTPart) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) part, newState);
				PersistenceHelper.manager.refresh(part);

			} else if (obj instanceof EPMDocument) {
				EPMDocument epmDocument = (EPMDocument) obj;
				epmDocument = (EPMDocument) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) epmDocument, newState);
				PersistenceHelper.manager.refresh(epmDocument);

			} else if (obj instanceof WTDocument) {
				WTDocument wtDocument = (WTDocument) obj;
				wtDocument = (WTDocument) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) wtDocument, newState);
				PersistenceHelper.manager.refresh(wtDocument);

			} else if (obj instanceof ManagedBaseline) {
				ManagedBaseline managedBaseline = (ManagedBaseline) obj;
				managedBaseline = (ManagedBaseline) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) managedBaseline, newState);
				PersistenceHelper.manager.refresh(managedBaseline);

			} else if (obj instanceof LifeCycleManaged) {
				LifeCycleManaged lifeCycleManaged = (LifeCycleManaged) obj;
				lifeCycleManaged = (LifeCycleManaged) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) lifeCycleManaged, newState);
				PersistenceHelper.manager.refresh(lifeCycleManaged);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (currentUser != null) {
				SessionHelper.manager.setPrincipal(currentUser.getName());
			}
		}
		return message;
	}

	/**
	 * 通过说明文档找到部件
	 * 
	 * @param WTDocument
	 * @return List<WTPart>
	 */
	public static List<WTPart> getPartByDescribesDoc(WTDocument doc) {
		QueryResult qr = null;
		WTPart part = null;
		List<WTPart> list = new ArrayList<WTPart>();
		try {
			qr = WTPartHelper.service.getDescribesWTParts(doc);// 说明文档
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof WTPart) {
					part = (WTPart) obj;
					list.add(part);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 将CAD文档关联到部件上
	 * 
	 * @param WTPart
	 * @param EPMDocument
	 * @param int
	 */
	public static void addCAD2WTPart(WTPart part, EPMDocument cad, int linkType) {
		try {
			EPMBuildRule newEPMBuildRule = EPMBuildRule.newEPMBuildRule(cad, part, linkType);
			PersistenceServerHelper.manager.insert(newEPMBuildRule);
		} catch (WTException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取部件关联的结构图文档（以.prt.asm结尾）
	 * 
	 * @param WTPart
	 * @return EPMDocument
	 * @throws Exception
	 */
	public static EPMDocument getProECADByPart(WTPart part) throws Exception {
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
					return doc;
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
		return null;
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
	 * 获取参考文档关联的部件
	 * 
	 * @param WTDocument
	 * @return List<WTPart>
	 */
	public static List<WTPart> getPartByRefrenceDoc(WTDocument doc) {
		QueryResult qr = null;
		WTPart part = null;
		List<WTPart> list = new ArrayList<WTPart>();
		try {
			qr = StructHelper.service.navigateReferencedBy(doc.getMaster());// 参考关系
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof WTPart) {
					part = (WTPart) obj;
					list.add(part);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 获取部件间的关联关系
	 * 
	 * @param WTPart
	 * @param WTPart
	 * @return WTPartUsageLink
	 */
	public static WTPartUsageLink getLinkByPart(WTPart fatherPart, WTPart sonPart) {
		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(fatherPart);
			while (qr.hasMoreElements()) {
				WTPartUsageLink link = (WTPartUsageLink) qr.nextElement();
				WTPart part = getBomPartBylink(link);
				if (part.getNumber().equals(sonPart.getNumber())) {
					return link;
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据部件间的关联关系获取子部件
	 * 
	 * @param WTPartUsageLink
	 * @return WTPart
	 */
	public static WTPart getBomPartBylink(WTPartUsageLink link) {
		try {
			QueryResult qr = VersionControlHelper.service.allVersionsOf(link.getUses());
			if (qr.hasMoreElements()) {
				WTPart part = (WTPart) qr.nextElement();
				return part;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取部件与其所有子部件间的关联关系
	 * 
	 * @param WTPart
	 * @return List<WTPartUsageLink>
	 */
	public static List<WTPartUsageLink> getLinksByPart(WTPart fatherPart) {
		List<WTPartUsageLink> list = new ArrayList<WTPartUsageLink>();
		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(fatherPart);
			while (qr.hasMoreElements()) {
				WTPartUsageLink link = (WTPartUsageLink) qr.nextElement();
				list.add(link);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 获取部件的子部件和数量关系
	 * 
	 * @param WTPart
	 * @param List<Object[]>
	 * @return List<Object[]>
	 */
	public static List<Object[]> getAllBomByParentPart(WTPart parentPart, List<Object[]> list) {
		WTPart sPart = null;
		QueryResult qr2 = null;
		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(parentPart);
			while (qr.hasMoreElements()) {
				WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
				qr2 = VersionControlHelper.service.allVersionsOf(usageLink.getUses());
				if (qr2.hasMoreElements()) {
					sPart = (WTPart) qr2.nextElement();
					boolean found = false;
					for (Object[] old : list) {
						WTPart object = (WTPart) old[0];
						String number = object.getNumber();
						if (sPart.getNumber().equalsIgnoreCase(number)) {
							old[1] = (Double) old[1] + usageLink.getQuantity().getAmount();
							found = true;
							break;
						}
					}
					if (!found) {
						list.add(new Object[] { sPart, usageLink.getQuantity().getAmount() });
					}
					getAllBomByParentPart(sPart, list);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 递归获取部件的所有子部件
	 *
	 * @param WTPart
	 * @return List<WTPart>
	 */
	public static List<WTPart> getAllBomByPart(WTPart part) {
		List<WTPart> localList = new ArrayList<>(); // 局部列表用于递归操作的累积结果

		WTPart sPart = null;
		QueryResult qr2 = null;
		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(part);
			while (qr.hasMoreElements()) {
				WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
				qr2 = VersionControlHelper.service.allVersionsOf(usageLink.getUses());
				if (qr2.hasMoreElements()) {
					sPart = (WTPart) qr2.nextElement();
					localList.add(sPart);

					// 递归调用，将结果合并到局部列表
					localList.addAll(getAllBomByPart(sPart));
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return localList; // 返回局部列表
	}

	/**
	 * 判断对象是否处于检出状态
	 * 
	 * @param Workable
	 * @return boolean
	 */
	public static boolean isCheckOut(Workable workable) {
		try {
			return WorkInProgressHelper.isCheckedOut(workable);
		} catch (WTException e) {

		}
		return false;
	}

	/**
	 * 检出部件
	 * 
	 * @param Workable
	 * @return Workable
	 * @throws WTException
	 */
	public static Workable checkoutPart(Workable workable) throws WTException {
		if (workable == null) {
			return null;
		}
		if (WorkInProgressHelper.isWorkingCopy(workable)) {
			return workable;
		}
		Folder folder = WorkInProgressHelper.service.getCheckoutFolder();
		try {
			CheckoutLink checkoutLink = WorkInProgressHelper.service.checkout(workable, folder, "AutoCheckOut");
			workable = checkoutLink.getWorkingCopy();
		} catch (WTPropertyVetoException ex) {
			ex.printStackTrace();
		}
		if (!WorkInProgressHelper.isWorkingCopy(workable)) {
			workable = WorkInProgressHelper.service.workingCopyOf(workable);
		}
		return workable;
	}

	/**
	 * 检入部件
	 * 
	 * @param WTPart
	 * @param String
	 * @return WTPart
	 * @throws WTException
	 */
	public static WTPart checkinPart(WTPart partParentPrevious, String comment) throws WTException {
		WTPart part = null;
		if (partParentPrevious != null) {
			try {
				wt.org.WTPrincipal wtprincipal = SessionHelper.manager.getPrincipal();
				if (WorkInProgressHelper.isCheckedOut((Workable) partParentPrevious, wtprincipal)) {
					if (!WorkInProgressHelper.isWorkingCopy((Workable) partParentPrevious)) {
						partParentPrevious = (WTPart) WorkInProgressHelper.service
								.workingCopyOf((Workable) partParentPrevious);
					}
					part = (WTPart) WorkInProgressHelper.service.checkin((Workable) partParentPrevious, comment);
				}
			} catch (WTPropertyVetoException ex) {
				ex.printStackTrace();
			}
		}
		return part;
	}

	/**
	 * 根据替代料获取原部件
	 * 
	 * @param WTPart
	 * @return List<WTPart>
	 * @throws WTException
	 */
	public static List<WTPart> getPart(WTPart part) throws WTException {
		List<WTPart> list = new ArrayList<WTPart>();
		if (part == null) {
			return list;
		}
		long linkid = PersistenceHelper.getObjectIdentifier(part.getMaster()).getId();

		int[] index = { 0 };
		QuerySpec qs = new QuerySpec(WTPartSubstituteLink.class);
		qs.appendWhere(
				new SearchCondition(WTPartSubstituteLink.class, "roleBObjectRef.key.id", SearchCondition.EQUAL, linkid),
				index);
		QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
		while (qr.hasMoreElements()) {
			WTPartSubstituteLink sLink = (WTPartSubstituteLink) qr.nextElement();
			WTPartUsageLink ulink = sLink.getSubstituteFor();
			QueryResult qr2 = VersionControlHelper.service.allVersionsOf(ulink.getUses());
			if (qr2.hasMoreElements()) {
				WTPart part2 = (WTPart) qr2.nextElement();
				list.add(part2);
			}
		}
		return list;
	}

	/**
	 * 根据子部件获取所有父部件之间的关联
	 * 
	 * @param WTPart
	 * @return List<WTPartUsageLink>
	 */
	@SuppressWarnings("deprecation")
	public static List<WTPartUsageLink> getBomLinkByChildPart(WTPart childPart) {
		List<WTPartUsageLink> list = new ArrayList<WTPartUsageLink>();
		try {
			if (childPart != null) {
				QuerySpec queryspec = new QuerySpec(WTPartUsageLink.class);
				queryspec.appendWhere(new SearchCondition(WTPartUsageLink.class, "roleBObjectRef.key", "=",
						PersistenceHelper.getObjectIdentifier((WTPartMaster) childPart.getMaster())));
				QueryResult qr = PersistenceHelper.manager.find((StatementSpec) queryspec);
				while (qr.hasMoreElements()) {
					WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
					list.add(usageLink);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 复制一个新部件（名称与编号不同）
	 * 
	 * @param WTPart
	 * @param String
	 * @return WTPart
	 */
	@SuppressWarnings("deprecation")
	public static WTPart copyPart(WTPart part, String partName) {
		WTPart newPart = null;
		try {
			String number = part.getNumber();
			LifeCycleState state = part.getState();
			Source source = part.getSource();
			PartType partType = part.getPartType(); // 装配模式
			boolean endItem = part.isEndItem(); // 成品
			QuantityUnit defaultUnit = part.getDefaultUnit();// 单位
			boolean hidePartInStructure = part.getHidePartInStructure();// 收集部件
			TraceCode defaultTraceCode = part.getDefaultTraceCode(); // 默认追踪代码
			String location = FolderHelper.getLocation((CabinetBased) part);
			WTContainer container2 = WTContainerHelper.getContainer((WTContained) part);
			WTContainerRef containerRef = WTContainerRef.newWTContainerRef(container2);

			String newNumber = number.substring(0, number.length() - 1);
			String suffix = number.substring(number.length() - 1);

			if ("0".equals(suffix)) {
				newNumber += "1";
			} else if ("1".equals(suffix)) {
				newNumber += "0";
			}

			newPart = WTPart.newWTPart();
			newPart.setName(partName);
			newPart.setNumber(newNumber);
			newPart.setState(state);
			newPart.setSource(source);
			newPart.setPartType(partType);
			newPart.setEndItem(endItem);
			newPart.setHidePartInStructure(hidePartInStructure);
			newPart.setDefaultUnit(defaultUnit);
			newPart.setDefaultTraceCode(defaultTraceCode);
			newPart.setView(part.getView());
			FolderHelper.assignLocation((FolderEntry) newPart, location, containerRef);
			wt.fc.PersistenceHelper.manager.save(newPart);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newPart;
	}

	/**
	 * 获取部件的完整版本号
	 * 
	 * @param WTPart
	 * @return String
	 */
	public static String getVersion(WTPart part) {
		if (part == null) {
			return "";
		}
		String version = part.getVersionInfo().getIdentifier().getValue();
		String value = part.getIterationInfo().getIdentifier().getValue();
		return new StringBuffer().append(version).append(".").append(value).toString();
	}

	/**
	 * 校验部件是否正在其他工作流中运行（直接抛出异常？太奇怪了）
	 * 
	 * @param List<WTPart>
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static void checkOtherWorkflow(List<WTPart> list) throws WTException {
		for (WTPart wtPart : list) {
			WTCollection localList = new WTHashSet();
			localList.add(wtPart);
			WTCollection promotionNotices = MaturityHelper.getService().getPromotionNotices(localList);
			for (Object object : promotionNotices) {
				if (object instanceof ObjectReference) {
					ObjectReference pn = (ObjectReference) object;
					Persistable object2 = pn.getObject();
					if (object2 instanceof PromotionNotice) {
						PromotionNotice promotion = (PromotionNotice) object2;
						QueryResult localQueryResult = WorkflowCommands.getRoutingHistory(new NmOid(promotion));
						while (localQueryResult.hasMoreElements()) {
							Object nextElement = localQueryResult.nextElement();
							if (nextElement instanceof WfProcess) {
								WfProcess process = (WfProcess) nextElement;
								WfState state = process.getState();
								if (WfState.OPEN_RUNNING.equals(state)) {// 正在运行
									String number = promotion.getNumber();
									String name = process.getName();
									String partNumber = wtPart.getNumber();
									String partName = wtPart.getName();
									throw new WTException("物料编码为" + partNumber + "的物料【" + partName + "】正在运行另一个升级请求："
											+ number + "," + name + "。请勿重复提交升级请求流程。");
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 获取部件的单位
	 * 
	 * @param WTPart
	 * @return String
	 * @throws WTException
	 */
	public static String getUnit(WTPart part) throws WTException {

		if (part == null) {
			return "";
		}

		String defaultUnit = part.getDefaultUnit().toString().toUpperCase();// 默认单位
		IBAUtil ibautil = new IBAUtil(part);
		String jldw = ibautil.getIBAValue("net.haige.jldw");// 计量单位
		String P_UNIT = ibautil.getIBAValue("net.haige.P_UNIT");// 结构件单位
		String typeName = PartUtil.getTypeName(part);
		if (StringUtils.equalsIgnoreCase(typeName, "com.ptc.ElectricalPart")) {// 电子元器件
			if (StringUtils.isNotBlank(jldw)) {
				return jldw.toUpperCase();
			} else {
				return defaultUnit;
			}
		} else if (StringUtils.equalsIgnoreCase(typeName, "Part")
				|| StringUtils.equalsIgnoreCase(typeName, "wt.part.WTPart")) {
			if (StringUtils.isNotBlank(P_UNIT)) {
				return P_UNIT.toUpperCase();
			} else {
				return defaultUnit;
			}
		}
		if (StringUtils.isNotBlank(jldw)) {
			return jldw.toUpperCase();
		} else if (StringUtils.isNotBlank(P_UNIT)) {
			return P_UNIT.toUpperCase();
		} else {
			return defaultUnit;
		}
	}

	/**
	 * 修改部件的单位
	 * 
	 * @param WTPart
	 * @param String
	 * @throws WTException
	 */
	public static void replaceUnit(WTPart part, String unit) throws WTException {

		if (part == null || StringUtils.isBlank(unit)) {
			return;
		}
		QuantityUnit defaultUnit = part.getDefaultUnit();
		QuantityUnit quantityUnit = QuantityUnit.toQuantityUnit(unit);

		if (defaultUnit.equals(quantityUnit)) {
			return;
		}

		try {
			Identified identified = (Identified) part.getMaster();
			WTPartMaster master = (WTPartMaster) part.getMaster();
			master.setDefaultUnit(quantityUnit);
			WTPartMasterIdentity partIdentity = (WTPartMasterIdentity) identified.getIdentificationObject();
			identified = wt.fc.IdentityHelper.service.changeIdentity(master, partIdentity);
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
	}
}
