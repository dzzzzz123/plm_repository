package ext.ait.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.server.LWCLocalizablePropertyValue;
import com.ptc.core.lwc.server.LWCTypeDefinition;
import com.ptc.core.meta.common.IdentifierFactory;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.type.mgmt.server.impl.WTTypeDefinition;
import com.ptc.core.meta.type.mgmt.server.impl.WTTypeDefinitionMaster;
import com.ptc.windchill.enterprise.part.commands.PartDocServiceCommand;

import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentRoleType;
import wt.content.URLData;
import wt.doc.WTDocument;
import wt.doc.WTDocumentDependencyLink;
import wt.doc.WTDocumentMaster;
import wt.doc.WTDocumentMasterIdentity;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentMaster;
import wt.epm.EPMDocumentMasterIdentity;
import wt.epm.EPMDocumentType;
import wt.fc.IdentityHelper;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.folder.Folder;
import wt.folder.FolderHelper;
import wt.folder.FolderingInfo;
import wt.folder.SubFolder;
import wt.folder.SubFolderReference;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.log4j.LogR;
import wt.org.WTPrincipal;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.pds.StatementSpec;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.services.ServiceProviderHelper;
import wt.session.SessionHelper;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import wt.vc.Iterated;
import wt.vc.Mastered;
import wt.vc.VersionControlHelper;
import wt.vc.VersionIdentifier;
import wt.vc.Versioned;
import wt.vc.config.ConfigHelper;
import wt.vc.config.ConfigSpec;
import wt.vc.config.LatestConfigSpec;

public class CommonUtil {

	private static ReferenceFactory factory = new ReferenceFactory();

	private static Logger LOGGER = LogR.getLogger(CommonUtil.class.getName());

	private static EPMDocumentType CADDRAWING = EPMDocumentType.toEPMDocumentType("CADDRAWING"); //

	/**
	 * 转换中文格式，避免中文乱码
	 * 
	 * @param value
	 * @return
	 * @throws WTException
	 */
	public static String formatString(String value) throws WTException {
		try {
			if (value != null && value.trim().length() > 0) {
				byte[] tembyte = value.getBytes("gb2312");
				return new String(tembyte);
			} else {
				return value;
			}
		} catch (Exception e) {
			throw new WTException(e);
		}
	}

	/**
	 * 获取对象的文件夹路径
	 * 
	 * @param obj
	 * @return
	 */
	public static String getPath(RevisionControlled obj) {
		StringBuffer path = new StringBuffer();
		SubFolderReference ref = obj.getParentFolder();
		if (ref != null && ref.getObject() instanceof SubFolder) {
			SubFolder subFolder = (SubFolder) ref.getObject();
			getPath(path, subFolder);
		} else {
			path = new StringBuffer("/Default");
		}
		return path.toString();
	}

	/**
	 * 获取对象存储位置
	 * 
	 * @param fInfo
	 * @return
	 */
	public static String getFolderStr(FolderingInfo fInfo) {
		StringBuffer path = new StringBuffer();
		SubFolderReference ref = fInfo.getParentFolder();
		if (ref != null && ref.getObject() instanceof SubFolder) {
			SubFolder subFolder = (SubFolder) ref.getObject();
			getPath(path, subFolder);
		} else {
			path = new StringBuffer("/Default");
		}
		return path.toString();
	}

	/**
	 * 用来递归获取文件夹完整路径的方法
	 * 
	 * @param path
	 * @param subFolder
	 */
	private static void getPath(StringBuffer path, SubFolder subFolder) {
		path.insert(0, subFolder.getName()).insert(0, "/");
		SubFolderReference ref = subFolder.getParentFolder();
		if (ref != null && ref.getObject() instanceof SubFolder) {
			SubFolder sub = (SubFolder) ref.getObject();
			getPath(path, sub);
		} else {
			path.insert(0, "/Default");
		}
	}

	/**
	 * 更改部件/文档/图纸的名称
	 * 
	 * @param mast
	 * @param name
	 */
	public static void changeName(Mastered mast, String name) {
		WTPrincipal user = null;
		try {
			user = SessionHelper.getPrincipal();
			SessionHelper.manager.setAdministrator();
			if (mast instanceof WTDocumentMaster) {
				WTDocumentMaster master = (WTDocumentMaster) mast;
				master = (WTDocumentMaster) PersistenceHelper.manager.refresh(master);
				WTDocumentMasterIdentity identity = (WTDocumentMasterIdentity) master.getIdentificationObject();
				identity.setName(name);
				master = (WTDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof WTPartMaster) {
				WTPartMaster master = (WTPartMaster) mast;
				master = (WTPartMaster) PersistenceHelper.manager.refresh(master);
				WTPartMasterIdentity identity = (WTPartMasterIdentity) master.getIdentificationObject();
				identity.setName(name);
				master = (WTPartMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof EPMDocumentMaster) {
				EPMDocumentMaster master = (EPMDocumentMaster) mast;
				master = (EPMDocumentMaster) PersistenceHelper.manager.refresh(master);
				EPMDocumentMasterIdentity identity = (EPMDocumentMasterIdentity) master.getIdentificationObject();
				identity.setName(name);
				master = (EPMDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			}
			SessionHelper.manager.setPrincipal(user.getName());
			user = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (user != null) {
				try {
					SessionHelper.manager.setPrincipal(user.getName());
				} catch (WTException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 修改文档、物料、图纸的编号
	 * 
	 * @param mast
	 * @param number
	 */
	public static void changeNumber(Mastered mast, String number) {
		WTPrincipal user = null;
		try {
			user = SessionHelper.getPrincipal();
			SessionHelper.manager.setAdministrator();
			if (mast instanceof WTDocumentMaster) {
				WTDocumentMaster master = (WTDocumentMaster) mast;
				master = (WTDocumentMaster) PersistenceHelper.manager.refresh(master);
				WTDocumentMasterIdentity identity = (WTDocumentMasterIdentity) master.getIdentificationObject();
				identity.setNumber(number);
				master = (WTDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof WTPartMaster) {
				WTPartMaster master = (WTPartMaster) mast;
				master = (WTPartMaster) PersistenceHelper.manager.refresh(master);
				WTPartMasterIdentity identity = (WTPartMasterIdentity) master.getIdentificationObject();
				identity.setNumber(number);
				master = (WTPartMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof EPMDocumentMaster) {
				EPMDocumentMaster master = (EPMDocumentMaster) mast;
				master = (EPMDocumentMaster) PersistenceHelper.manager.refresh(master);
				EPMDocumentMasterIdentity identity = (EPMDocumentMasterIdentity) master.getIdentificationObject();
				identity.setNumber(number);
				master = (EPMDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			}
			SessionHelper.manager.setPrincipal(user.getName());
			user = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (user != null) {
				try {
					SessionHelper.manager.setPrincipal(user.getName());
				} catch (WTException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 获取文档的主要内容
	 * 
	 * @param holder
	 * @return
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

	/**
	 * 获取对象的版本，如A.1
	 * 
	 * @param revisionControlled
	 * @return
	 */
	public static String getVersion(RevisionControlled revisionControlled) {
		return revisionControlled.getVersionInfo().getIdentifier().getValue() + "."
				+ revisionControlled.getIterationInfo().getIdentifier().getValue();
	}

	/**
	 * 获取对象的oid
	 * 
	 * @param obj
	 * @return
	 */
	public static String object2Oid(WTObject obj) {
		return "OR:" + obj.getClass().getName() + ":" + obj.getPersistInfo().getObjectIdentifier().getId();
	}

	/**
	 * 获取oid所对应的对象
	 * 
	 * @param oid
	 * @return
	 * @throws WTException
	 */
	public static WTObject oid2Object(String oid) throws WTException {
		return (WTObject) factory.getReference(oid).getObject();
	}

	/**
	 * 校验对象是否为最新版
	 * 
	 * @param interated
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static boolean isLatestIterated(Iterated interated) throws WTException {

		Iterated localIterated = null;
		boolean bool = false;
		LatestConfigSpec localLatestConfigSpec = new LatestConfigSpec();

		QueryResult localQueryResult = ConfigHelper.service.filteredIterationsOf(interated.getMaster(),
				localLatestConfigSpec);
		if ((localQueryResult != null) && (localQueryResult.size() <= 0)) {
			ConfigSpec localConfigSpec = ConfigHelper.service.getDefaultConfigSpecFor(WTPartMaster.class);
			localQueryResult = ConfigHelper.service.filteredIterationsOf(interated.getMaster(), localConfigSpec);
		}

		while ((localQueryResult.hasMoreElements()) && (!bool)) {
			localIterated = (Iterated) localQueryResult.nextElement();
			bool = localIterated.isLatestIteration();
		}
		LOGGER.debug("    the latest iteration=" + localIterated.getIdentity());
		if (localIterated.equals(interated)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取最新版本的对象
	 * 
	 * @param master
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static Iterated getLatestInterated(Mastered master) throws WTException {

		Iterated localIterated = null;
		LatestConfigSpec localLatestConfigSpec = new LatestConfigSpec();

		QueryResult localQueryResult = ConfigHelper.service.filteredIterationsOf(master, localLatestConfigSpec);
		if ((localQueryResult != null) && (localQueryResult.size() <= 0)) {
			ConfigSpec localConfigSpec = ConfigHelper.service.getDefaultConfigSpecFor(WTPartMaster.class);
			localQueryResult = ConfigHelper.service.filteredIterationsOf(master, localConfigSpec);
		}

		while ((localQueryResult.hasMoreElements())) {
			Iterated localIterated1 = (Iterated) localQueryResult.nextElement();
			if (localIterated1.isLatestIteration()) {
				localIterated = localIterated1;
			}
		}
		return localIterated;
	}

	/**
	 * 获取上一个大版本的最新小版本，如果没有上一个大版本，则返回当前版本的最新小版本
	 * 
	 * @param revisionControlled
	 * @return
	 */
	public static RevisionControlled getLastBigOne(RevisionControlled revisionControlled) {
		RevisionControlled last = null;
		try {
			last = (RevisionControlled) VersionControlHelper.service.predecessorOf(revisionControlled);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return last;
	}

	/**
	 * 获取上一个大版本的最新小版本，如果没有上一个大版本，则返回当前版本的最新小版本
	 * 
	 * @param revisionControlled
	 * @return
	 */
	public static RevisionControlled getLasterBigOne(RevisionControlled revisionControlled) {
		RevisionControlled last = null;
		try {
			if ("A".equals(revisionControlled.getVersionInfo().getIdentifier().getValue())) {
				return (RevisionControlled) getLatestInterated(revisionControlled.getMaster());
			}
			last = (RevisionControlled) VersionControlHelper.service.predecessorOf(revisionControlled);
			if (!last.getVersionInfo().getIdentifier().getValue()
					.equals(revisionControlled.getVersionInfo().getIdentifier().getValue())) {
				return last;
			} else {
				last = getLasterBigOne(last);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return last;
	}

	/**
	 * 根据用户id获取WTUser对象
	 * 
	 * @param id
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static WTUser getUserById(String id) throws WTException {
		WTUser user = null;
		try {
			if (id != null && id.trim().length() > 0) {
				QuerySpec qs = new QuerySpec(WTUser.class);
				SearchCondition sc1 = new SearchCondition(WTUser.class, WTUser.NAME, SearchCondition.EQUAL, id);
				SearchCondition sc2 = new SearchCondition(WTUser.class, WTUser.FULL_NAME, SearchCondition.EQUAL, id);
				qs.appendWhere(sc1);
				qs.appendOr();
				qs.appendWhere(sc2);
				LOGGER.debug("searchUsers sql where --->" + qs.getWhere());
				QueryResult qr = new QueryResult();
				qr = PersistenceHelper.manager.find(qs);
				while (qr.hasMoreElements()) {
					user = (WTUser) qr.nextElement();
				}
			}
			return user;
		} catch (Exception e) {
			throw new WTException(e);
		}
	}

	/**
	 * 通过高级查询获取文档类型的ID
	 * 
	 * @param name
	 * @return
	 * @throws WTException
	 */
	public static long getTypeDefinitionIdByName(String name) throws WTException {
		long id = 0;
		WTTypeDefinition typeDef = getTypeDefinitionByName(name);
		if (typeDef.isLatestIteration()) {
			id = typeDef.getPersistInfo().getObjectIdentifier().getId();
		}
		LOGGER.debug("###[" + id + "] isInheritedDomain --->" + typeDef.isInheritedDomain() + " ;;;isUserAttributeable "
				+ typeDef.isUserAttributeable() + ";;;; isLatestIteration " + typeDef.isLatestIteration());
		return id;
	}

	/**
	 * 通过高级查询获取文档类型的对象
	 * 
	 * @param name
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static WTTypeDefinition getTypeDefinitionByName(String name) throws WTException {
		WTTypeDefinition type = null;
		QuerySpec qs = new QuerySpec();
		int typeDefine = qs.appendClassList(WTTypeDefinition.class, true);
		int typeDefineMaster = qs.appendClassList(WTTypeDefinitionMaster.class, false);
		qs.setAdvancedQueryEnabled(true);
		SearchCondition typebyMaster = new SearchCondition(WTTypeDefinition.class, "masterReference.key.id",
				WTTypeDefinitionMaster.class, "thePersistInfo.theObjectIdentifier.id");
		qs.appendWhere(typebyMaster, new int[] { typeDefine, typeDefineMaster });
		qs.appendAnd();
		SearchCondition typeMasterName = new SearchCondition(WTTypeDefinitionMaster.class, "displayNameKey", "=", name);
		qs.appendWhere(typeMasterName, typeDefineMaster);
		QueryResult qr = PersistenceHelper.manager.find(qs);
		while (qr.hasMoreElements()) {
			Object[] objs = (Object[]) qr.nextElement();
			if (objs[0] instanceof WTTypeDefinition) {
				WTTypeDefinition typeDef = (WTTypeDefinition) objs[0];
				if (typeDef.isLatestIteration()) {
					type = typeDef;
				}

			}
		}
		return type;
	}

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
	 * 通过类型的Key获取国际化的名称
	 * 
	 * @param key
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static String getTypeDisplayName(String key) throws WTException {
		String typeDisplayName = "";
		try {
			LWCTypeDefinition lwcType = null;
			QuerySpec lwcSpec = new QuerySpec(LWCTypeDefinition.class);
			lwcSpec.appendSearchCondition(
					new SearchCondition(LWCTypeDefinition.class, LWCTypeDefinition.NAME, SearchCondition.EQUAL, key));
			QueryResult qr = PersistenceHelper.manager.find(lwcSpec);
			while (qr.hasMoreElements()) {
				lwcType = (LWCTypeDefinition) qr.nextElement();
			}
			if (lwcType != null) {
				/**
				 * LWCLocalizablePropertyValue记录所有的国际化字段
				 */
				QuerySpec valueSpec = new QuerySpec(LWCLocalizablePropertyValue.class);
				valueSpec.appendSearchCondition(
						new SearchCondition(LWCLocalizablePropertyValue.class, "contextReference.key.id",
								SearchCondition.EQUAL, lwcType.getPersistInfo().getObjectIdentifier().getId()));
				valueSpec.appendAnd();
				valueSpec.appendSearchCondition(
						new SearchCondition(LWCLocalizablePropertyValue.class, "holderReference.key.id",
								SearchCondition.EQUAL, lwcType.getPersistInfo().getObjectIdentifier().getId()));
				QueryResult vqr = PersistenceHelper.manager.find(valueSpec);
				while (vqr.hasMoreElements()) {
					LWCLocalizablePropertyValue value = (LWCLocalizablePropertyValue) vqr.nextElement();
					typeDisplayName = value.getValue(Locale.CHINA);
					if (typeDisplayName == null || typeDisplayName.trim().length() == 0) {
						typeDisplayName = value.getValue();
					}
				}
			}
			return typeDisplayName;
		} catch (Exception e) {
			throw new WTException(e);
		}
	}

	/**
	 * 得到指定文件夹的对象，如果没有则创建该文件夹（尚不明晰，看上去并不那么好用）
	 * 
	 * @param strFolder
	 * @param wtContainer
	 * @return
	 * @throws WTException
	 */
	public static Folder getFolder(String strFolder, WTContainer wtContainer) throws WTException {
		WTPrincipal curUser = SessionHelper.manager.getPrincipal();
		SessionHelper.manager.setAdministrator();
		Folder folder = null;
		String subPath = "Default/" + strFolder;
		WTContainerRef ref = WTContainerRef.newWTContainerRef(wtContainer);
		try {
			folder = FolderHelper.service.getFolder(subPath, ref);
		} catch (WTException e) {
			folder = FolderHelper.service.createSubFolder(subPath, ref);
		} finally {
			SessionHelper.manager.setPrincipal(curUser.getName());
		}
		return folder;
	}

	/**
	 * 根据对象类型和类型获取对象（暂不清楚）
	 * 
	 * @param queryClass
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public static QueryResult findObjectByType(Class queryClass, String type) throws Exception {
		IdentifierFactory identifier_factory = (IdentifierFactory) ServiceProviderHelper
				.getService(IdentifierFactory.class, "logical");
		TypeIdentifier tid = (TypeIdentifier) identifier_factory.get(type);
		QuerySpec qs = new QuerySpec(queryClass);
		int idx = qs.addClassList(queryClass, true);
		SearchCondition sc = TypedUtilityServiceHelper.service.getSearchCondition(tid, true);
		qs.appendWhere(sc, new int[] { idx });
		System.out.println(queryClass);
		QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
		return qr;
	}

	/**
	 * 根据容器的名称获取容器对象
	 * 
	 * @param containerName
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static WTContainer getContainer(String containerName) throws Exception {
		QuerySpec qs = new QuerySpec(WTContainer.class);
		SearchCondition sc = new SearchCondition(WTContainer.class, WTContainer.NAME, "=", containerName);
		qs.appendWhere(sc);
		QueryResult qr = PersistenceHelper.manager.find(qs);
		while (qr.hasMoreElements()) {
			WTContainer container = (WTContainer) qr.nextElement();
			return container;
		}
		return null;
	}

	/**
	 * 根据编号获取对应最新版本的文档
	 * 
	 * @param number
	 * @return
	 */
	@SuppressWarnings("deprecation")
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
	 * 根据VR获取对应版本的对象
	 * 
	 * @param queryClass
	 * @param vr
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Object getObjByVR(Class queryClass, String vr) {
		try {
			if (StringUtils.isBlank(vr)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(queryClass);
			qs.appendWhere(
					new SearchCondition(queryClass, "iterationInfo.branchId", SearchCondition.EQUAL, Long.valueOf(vr)));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			qr = new LatestConfigSpec().process(qr);
			if (qr.hasMoreElements()) {
				return qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * 根据编号获取对象
	 * 
	 * @param number
	 * @param queryClass
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Object getObjByNumber(String number, Class queryClass) {
		try {
			if (StringUtils.isBlank(number)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(queryClass);
			qs.appendWhere(new SearchCondition(queryClass, "master>number", SearchCondition.EQUAL, number.trim()));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			qr = new LatestConfigSpec().process(qr);
			if (qr.hasMoreElements()) {
				return qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据组织名称获取组织对象
	 * 
	 * @param orgName
	 * @return
	 */
	public static OrgContainer getOrgContainer(String orgName) {
		try {
			QuerySpec queryspec = new QuerySpec(OrgContainer.class);
			QueryResult qr = PersistenceHelper.manager.find(queryspec);
			while (qr.hasMoreElements()) {
				OrgContainer org = (OrgContainer) qr.nextElement();
				if (StringUtils.equalsIgnoreCase(orgName, org.getName())) {
					return org;
				}
			}
		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据对象的number找到最新版本的对象
	 *
	 * @author gongke
	 * @param number    要查询的对象的编号
	 * @param thisClass class对象
	 * @return 由number标识的最新版本对象
	 */
	public static Persistable getLatestPersistableByNumber(String number, Class thisClass) {
		Persistable persistable = null;
		try {
			int[] index = { 0 };
			QuerySpec qs = new QuerySpec(thisClass);
			String attribute = (String) thisClass.getField("NUMBER").get(thisClass);
			qs.appendWhere(new SearchCondition(thisClass, attribute, SearchCondition.EQUAL, number), index);
			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			LatestConfigSpec configSpec = new LatestConfigSpec();
			qr = configSpec.process(qr);
			if (qr != null && qr.hasMoreElements()) {
				persistable = (Persistable) qr.nextElement();
			}
		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return persistable;
	}

	/**
	 * 根据对象的number verision找到最新版本的对象
	 *
	 * @author gongke
	 * @param number    要查询的对象的编号
	 * @param thisClass class对象
	 * @return 由number标识的最新版本对象
	 */
	public static Persistable getLatestPersistableByNumberAndVersion(String number, String version, Class thisClass) {
		Persistable persistable = null;
		try {
			int[] index = { 0 };
			QuerySpec qs = new QuerySpec(thisClass);
			String attribute = (String) thisClass.getField("NUMBER").get(thisClass);
			qs.appendWhere(new SearchCondition(thisClass, attribute, SearchCondition.EQUAL, number), index);
			qs.appendAnd();
			qs.appendWhere(new SearchCondition(thisClass,
					Versioned.VERSION_IDENTIFIER + "." + VersionIdentifier.VERSIONID, SearchCondition.EQUAL, version),
					index);
			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			LatestConfigSpec configSpec = new LatestConfigSpec();
			qr = configSpec.process(qr);
			if (qr != null && qr.hasMoreElements()) {
				persistable = (Persistable) qr.nextElement();
			}
		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return persistable;
	}

	/**
	 * 根据3D获取2D drawing
	 *
	 * @param cad
	 * @throws Exception
	 */
	public static List<Persistable> get2DDrawingByWTPart(WTPart part) throws Exception {
		List<Persistable> result = new ArrayList<Persistable>();
		try {
			QueryResult qr = PartDocServiceCommand.getAssociatedCADDocuments(part);// CAD文档
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof EPMDocument) {
					EPMDocument doc = (EPMDocument) obj;
					EPMDocumentType docType = doc.getDocType();
					if (CADDRAWING.equals(docType)) {
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
}
