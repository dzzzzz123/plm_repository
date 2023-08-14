package ext.sinoboom.publishStructure;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.jsonldjava.shaded.com.google.common.base.Objects;
import com.ptc.arbortext.windchill.partlist.PartList;
import com.ptc.arbortext.windchill.partlist.PartListMaster;
import com.ptc.arbortext.windchill.partlist.PartListMasterIdentity;
import com.ptc.arbortext.windchill.partlist.PartToPartListLink;
import com.ptc.core.meta.common.impl.TypeIdentifierUtilityHelper;

import ext.ait.util.CommonUtil;
import ext.ait.util.IBAUtil;
import ext.ait.util.WorkflowUtil;
import ext.bht.tool.CommUtil;
import ext.util.PartUtil;
import wt.fc.IdentityHelper;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.lifecycle.LifeCycleState;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.part.WTPartUsageLink;
import wt.pom.WTConnection;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.config.LatestConfigSpec;

public class Util {

	public static String partAuthoringLanguageEng = "windchill.authoring.language.eng";
	public static String partAuthoringLanguageJap = "windchill.authoring.language.jap";

	public static String partListRefInnerName = "windchill.partList.ref";
	public static String contentRefInnerName = "windchill.content.ref";
	public static String psSectionInnerName = "windchill.PsSection.ref";
	public static String psRootInnerName = "windchill.PsRoot.ref";
	public static String textualContentInnerName = "windchill.TextualContent.Ref";

	public static String IBATranSou = "iba.trans.source";
	public static String IBATranEng = "iba.trans.english";
	public static String IBATranJap = "iba.trans.japan";

	/**
	 * 英文发布结构收集发布结构的总方法
	 * 
	 * @param part
	 * @throws WTException
	 */
	public static void collectContentCarriers(WTPart part) throws WTException {
		String partAuthoringLanguageEngValue = PropertiesHelper.getStrFromProperties(partAuthoringLanguageEng);
		System.out.println("-------------开始收集内容载体-------------------------");
		// 判断部件的创作语言与部件的子类型
		String typeInnerName = PropertiesHelper.getStrFromProperties(partListRefInnerName);
		if (!Objects.equal(part.getAuthoringLanguage(), partAuthoringLanguageEngValue)) {
			throw new WTException("只能收集英文发布结构的内容载体");
		}
		try {
			ArrayList<WTPart> wtParts = getWtPartsFromWTPart(part);
			for (WTPart wtPart : wtParts) {
				String type = getPerType(wtPart);
				if (Objects.equal(type, typeInnerName)) {
					// 删除所有已存在的部件列表，然后插入源语言（中文）已挂载的部件列表
					removeLinkByPart(wtPart);
					insertLinkByPart(wtPart);
				}
			}
		} catch (Exception e) {
			System.out.println("收集内容载体出现了问题，请联系管理员！" + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 删除部件列表载体已挂载的所有部件列表
	 * 
	 * @param wtpart
	 * @throws Exception
	 */
	public static void removeLinkByPart(WTPart wtpart) throws Exception {
		Long partNumber = PersistenceHelper.getObjectIdentifier(wtpart).getId();
		ArrayList<PartToPartListLink> sourcePartToPartListLinks = getPartListLinkListByNumber(partNumber);
		for (PartToPartListLink partToPartListLink : sourcePartToPartListLinks) {
			String toDeleteId = String.valueOf(PersistenceHelper.getObjectIdentifier(partToPartListLink).getId());
			// 使用winchill api无法完成此处的删除操作，执行sql直接删除
			int i = deletePartToPartListLink(toDeleteId);
//			System.out.println("删除了一行id为" + toDeleteId + "的PartToPartListLink");
//			PersistenceHelper.manager.delete(partToPartListLink);
		}
	}

	/**
	 * 添加部件列表载体与部件列表之间链接
	 * 
	 * @param wtpart
	 * @throws WTException
	 */
	public static void insertLinkByPart(WTPart wtpart) {
		String memoQIBATranSourceValue = PropertiesHelper.getStrFromProperties(IBATranSou);
		// 找到部件对应源语言的url，从url中生成WTPART,然后找到这个part对应的所有parttopartlistlink
		try {
			String source = new IBAUtil(wtpart).getIBAValue(memoQIBATranSourceValue);
			Long id = PersistenceHelper.getObjectIdentifier(getPartFromURL(source)).getId();
			ArrayList<PartToPartListLink> partToPartListLinks = getPartListLinkListByNumber(id);
			PartList partList = null;
			// 对parttopartlistlink的数量进行判断，如果只有一个可以直接插入，如果存在多个进行判断插入最新的link
			if (partToPartListLinks.size() == 1) {
				partList = (PartList) partToPartListLinks.get(0).getRoleBObject();
			} else {
				PartToPartListLink link = getLatestPartToPartListLink(partToPartListLinks);
				partList = (PartList) link.getRoleBObject();
			}
			PartToPartListLink partListLink = PartToPartListLink.newPartToPartListLink(wtpart, partList);
			PersistenceServerHelper.manager.insert(partListLink);
			partListLink = (PartToPartListLink) PersistenceHelper.manager.refresh(partListLink);
		} catch (Exception e) {
			System.out.println("根据源语言查询到的url无法生成part");
		}
	}

	/**
	 * 通过部件列表载体维持相关联的部件列表载体的url
	 * 
	 * @param ref
	 */
	public static String maintainRefUrl(WTPart ref) {
		String IBATranSourceValue = PropertiesHelper.getStrFromProperties(IBATranSou);
		String IBATranEngValue = PropertiesHelper.getStrFromProperties(IBATranEng);
		String IBATranJapValue = PropertiesHelper.getStrFromProperties(IBATranJap);
		String partAuthoringLanguageEngValue = PropertiesHelper.getStrFromProperties(partAuthoringLanguageEng);
		String partAuthoringLanguageJapValue = PropertiesHelper.getStrFromProperties(partAuthoringLanguageJap);
		try {
			String name = ref.getName();
			IBAUtil partIba = new IBAUtil(ref);
			String source = partIba.getIBAValue(IBATranSourceValue);
			if (source.length() > 0) {
				String authorLang = ref.getAuthoringLanguage();
				WTPart wtPartToChange = getPartFromURL(source);
				if (wtPartToChange == null)
					return ref.getName() + "Url链接存在问题，请更新其他语言的载体";
				wtPartToChange = (WTPart) PartUtil.getLatestPersistableByNumber(wtPartToChange.getNumber(),
						WTPart.class);
				WTPart wtPartToSet = (WTPart) PartUtil.getLatestPersistableByNumber(ref.getNumber(), WTPart.class);
				String url = WorkflowUtil.getPersUrl(wtPartToSet);
				if (authorLang.equals(partAuthoringLanguageEngValue)) {
					IBAUtil.newIBAURLAttribute(wtPartToChange, IBATranEngValue, url, name);
				} else if (authorLang.equals(partAuthoringLanguageJapValue)) {
					IBAUtil.newIBAURLAttribute(wtPartToChange, IBATranJapValue, url, name);
				} else {
					System.out.println("------------authoringLanguage not found---------------");
				}
			} else {
				String urlToWTPartEng = partIba.getIBAValue(IBATranEngValue);
				String urlToWTPartJap = partIba.getIBAValue(IBATranJapValue);
				WTPart wtPartToChange = null;
				if (urlToWTPartEng.length() > 0) {
					wtPartToChange = getPartFromURL(urlToWTPartEng);
				} else if (urlToWTPartJap.length() > 0) {
					wtPartToChange = getPartFromURL(urlToWTPartJap);
				}
				if (wtPartToChange == null)
					return ref.getName() + "Url链接存在问题，请更新其他语言的载体";
				wtPartToChange = (WTPart) PartUtil.getLatestPersistableByNumber(wtPartToChange.getNumber(),
						WTPart.class);
				WTPart wtPartToSet = (WTPart) PartUtil.getLatestPersistableByNumber(ref.getNumber(), WTPart.class);
				String url = WorkflowUtil.getPersUrl(wtPartToSet);
				IBAUtil.newIBAURLAttribute(wtPartToChange, IBATranSourceValue, url, name);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 修改
	 * 
	 * @param partList
	 * @param currentState
	 */
	public static void alterRefByPartList(PartList partList, LifeCycleState currentState) {
		System.out.println("-----------alterRefByPartList-------------");
		Long id = PersistenceHelper.getObjectIdentifier(partList).getId();
		try {
			ArrayList<PartToPartListLink> partToPartListLinks = getPartListLinkListByPartListNumber(id);
			for (PartToPartListLink partToPartListLink : partToPartListLinks) {
				WTPart part = (WTPart) partToPartListLink.getRoleAObject();
				Long ida2a2 = PersistenceHelper.getObjectIdentifier(part).getId();
				String updateQuery = "UPDATE WTPART SET STATESTATE = ? WHERE IDA2A2 = ?";
				WTConnection connection = CommUtil.getWTConnection();
				PreparedStatement statement = connection.prepareStatement(updateQuery);
				statement.setString(1, currentState.toString());
				statement.setString(2, String.valueOf(ida2a2));
				int i = statement.executeUpdate();
				System.out.println("修改的条数为：" + i);
			}
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据部件列表载体的url修改相对应的其他语言的部件列表载体的状态
	 * 
	 * @param ref
	 */
	public static void alterRefByRefUrl(WTPart ref, LifeCycleState currentState) {
		System.out.println("-----------alterRefByRefUrl-------------");
		try {
			WTPart wtPartToChange = getLinkedRefByPart(ref);
			if (wtPartToChange == null)
				return;
			Long ida2a2 = PersistenceHelper.getObjectIdentifier(wtPartToChange).getId();
			String updateQuery = "UPDATE PARTLIST SET STATESTATE = ? WHERE IDA2A2 = ?";
			WTConnection connection = CommUtil.getWTConnection();
			PreparedStatement statement = connection.prepareStatement(updateQuery);
			statement.setString(1, currentState.toString());
			statement.setString(2, String.valueOf(ida2a2));
			int i = statement.executeUpdate();
			System.out.println("修改的条数为：" + i);
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据部件查询出部件列表载体已挂载的所有部件列表的关系
	 * 
	 * @param sourcePartOid
	 * @return
	 * @throws WTException
	 */
	public static ArrayList<PartToPartListLink> getPartListLinkListByNumber(Long sourcePartOid) throws WTException {
		ArrayList<PartToPartListLink> partToPartListLinks = new ArrayList<>();
		QuerySpec querySpec = new QuerySpec(PartToPartListLink.class);
		querySpec.appendSearchCondition(
				new SearchCondition(PartToPartListLink.class, "roleAObjectRef.key.id", "=", sourcePartOid));
		QueryResult queryResult = PersistenceHelper.manager.find(querySpec);
		while (queryResult.hasMoreElements()) {
			partToPartListLinks.add((PartToPartListLink) queryResult.nextElement());
		}
		return partToPartListLinks;
	}

	/**
	 * 根据部件列表查询出已挂载的所有部件列表载体的关系
	 * 
	 * @param sourcePartOid
	 * @return
	 * @throws WTException
	 */
	public static ArrayList<PartToPartListLink> getPartListLinkListByPartListNumber(Long sourcePartOid)
			throws WTException {
		ArrayList<PartToPartListLink> partToPartListLinks = new ArrayList<>();
		QuerySpec querySpec = new QuerySpec(PartToPartListLink.class);
		querySpec.appendSearchCondition(
				new SearchCondition(PartToPartListLink.class, "roleBObjectRef.key.id", "=", sourcePartOid));
		QueryResult queryResult = PersistenceHelper.manager.find(querySpec);
		while (queryResult.hasMoreElements()) {
			partToPartListLinks.add((PartToPartListLink) queryResult.nextElement());
		}
		return partToPartListLinks;
	}

	/**
	 * 遍历递归得到所有的部件,并以list的形式返回
	 * 
	 * @param wtPart
	 * @return
	 * @throws WTException
	 */
	public static ArrayList<WTPart> getWtPartsFromWTPart(WTPart wtPart) throws WTException {
		ArrayList<WTPart> wtParts = new ArrayList<>();
		wtParts.add(wtPart);
		QueryResult queryResult = WTPartHelper.service.getUsesWTPartMasters(wtPart);
		while (queryResult.hasMoreElements()) {
			WTPartUsageLink link = (WTPartUsageLink) queryResult.nextElement();
			WTPartMaster master = (WTPartMaster) link.getUses();
			WTPart wtPart2 = getWtPartByNumber(master.getNumber());
			if (WTPartHelper.service.getUsesWTPartMasters(wtPart2).hasMoreElements()) {
				wtParts.addAll(getWtPartsFromWTPart(wtPart2));
			} else {
				wtParts.add(wtPart2);
			}
		}
		return wtParts;
	}

	/**
	 * 根据wtpartmaster的name获取wtpart
	 * 
	 * @param partNumber 部件的编号
	 * @return
	 * @throws WTException
	 */
	public static WTPart getWtPartByNumber(String partNumber) throws WTException {
		QuerySpec querySpec = new QuerySpec(WTPart.class);
		querySpec.appendSearchCondition(new SearchCondition(WTPart.class, WTPart.NUMBER, "=", partNumber));
		QueryResult queryResult = PersistenceHelper.manager.find(querySpec);
		LatestConfigSpec cfg = new LatestConfigSpec();
		QueryResult queryResult2 = cfg.process(queryResult);
		if (queryResult2.hasMoreElements()) {
			return (WTPart) queryResult2.nextElement();
		}
		return null;
	}

	/**
	 * 获取子类型 内部名称
	 * 
	 * @param per 持久化对象
	 * @return
	 * @throws Exception
	 */
	public static String getPerType(Persistable per) throws Exception {
		String type = TypeIdentifierUtilityHelper.service.getTypeIdentifier(per).toString();
		String[] typeArray = type.split("\\|");
		return typeArray[typeArray.length - 1];
	}

	/**
	 * 根据url获取对应的wtpart对象
	 * http://plmtest.sinoboom.com.cn/Windchill/app/#ptc1/tcomp/infoPage?u8=1&oid=OR%3Awt.part.WTPart%3A804374507
	 * http://plmtest.sinoboom.com.cn/Windchill/app/#ptc1/tcomp/infoPage?oid=OR:wt.part.WTPart:1099247509
	 * 
	 * @param url
	 * @return
	 */
	private static WTPart getPartFromURL(String url) {
		try {
			String oid = extractIdFromURL(url);
			WTPart part = (WTPart) CommonUtil.getObjByOR(WTPart.class, oid);
			return part;
		} catch (Exception e) {
			System.out.println("系统未找到url指定的部件");
			return null;
		}
	}

	/**
	 * 使用正则表达式提取oid
	 * 
	 * @param url 部件对应的url
	 * @return oid
	 */
	private static String extractIdFromURL(String url) {
		Pattern pattern = Pattern.compile("(?<=WTPart)(%3A|:)([0-9]+)");
		Matcher matcher = pattern.matcher(url);

		if (matcher.find()) {
			return matcher.group(2);
		}

		return null;
	}

	/**
	 * 获取PartToPartListLink最新的那一个
	 * 
	 * @param links
	 * @return
	 */
	private static PartToPartListLink getLatestPartToPartListLink(ArrayList<PartToPartListLink> links) {
		Long id = 0L;
		if (links.size() == 0) {
			return null;
		}
		PartToPartListLink link = null;
		for (PartToPartListLink partToPartListLink : links) {
			long tempId = PersistenceHelper.getObjectIdentifier(partToPartListLink).getId();
			if (tempId > id) {
				id = tempId;
				link = partToPartListLink;
			}
		}
		return link;
	}

	public static int deletePartToPartListLink(String id) throws Exception {
		String sql = "DELETE FROM PARTTOPARTLISTLINK WHERE IDA2A2 = ?";
		WTConnection con = CommUtil.getWTConnection();
		PreparedStatement statement = con.prepareStatement(sql);
		statement.setString(1, id);
		int result = statement.executeUpdate();
		return result;
	}

	/**
	 * 根据部件列表载体获取部件列表和相关联的部件列表载体
	 * 
	 * @param ref
	 * @return
	 */
	public static PartList getPartListByPart(WTPart ref) {
		PartList partList = null;
		try {
			// 获取部件列表载体相关联的部件列表
			Long partNumber = PersistenceHelper.getObjectIdentifier(ref).getId();
			ArrayList<PartToPartListLink> sourcePartToPartListLinks = getPartListLinkListByNumber(partNumber);
			PartToPartListLink link = getLatestPartToPartListLink(sourcePartToPartListLinks);
			if (link != null) {
				partList = (PartList) link.getRoleBObject();
			}
		} catch (WTException e) {
			System.out.println("根据部件列表载体获取部件列表的时候出现了问题！");
			e.printStackTrace();
		}
		return partList;
	}

	/**
	 * 根据部件列表载体的iba属性url获取相关联的部件列表载体
	 * 
	 * @param ref
	 * @return
	 */
	public static WTPart getLinkedRefByPart(WTPart ref) {
		String IBATranSourceValue = PropertiesHelper.getStrFromProperties(IBATranSou);
		String IBATranEngValue = PropertiesHelper.getStrFromProperties(IBATranEng);
		String IBATranJapValue = PropertiesHelper.getStrFromProperties(IBATranJap);
		WTPart wtPart = null;
		IBAUtil partIba;
		try {
			partIba = new IBAUtil(ref);
			ArrayList<String> urlList = new ArrayList<>();
			urlList.add(partIba.getIBAValue(IBATranSourceValue));
			urlList.add(partIba.getIBAValue(IBATranEngValue));
			urlList.add(partIba.getIBAValue(IBATranJapValue));
			for (String source : urlList) {
				if (source.length() > 0) {
					System.out.println("-------source------" + source);
					wtPart = getPartFromURL(source);
				}
			}
		} catch (WTException e) {
			System.out.println("根据部件列表载体获取相关联的部件列表载体的时候出现了问题！");
			e.printStackTrace();
		}
		return wtPart;
	}

	public static void changeWtPartName(WTPart wtpart, String newName) {
		try {
			WTPartMaster partMaster = (WTPartMaster) wtpart.getMaster();
			WTPartMasterIdentity partMasteridentity = (WTPartMasterIdentity) partMaster.getIdentificationObject();
			partMasteridentity.setName(newName);
			partMaster = (WTPartMaster) IdentityHelper.service.changeIdentity(partMaster, partMasteridentity);
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
	}

	public static int changeWtPartNameBySql(WTPart wtpart, String newName) throws Exception {
		WTPartMaster partMaster = (WTPartMaster) wtpart.getMaster();
		Long ida2a2 = PersistenceHelper.getObjectIdentifier(partMaster).getId();
		String updateQuery = "UPDATE WTPARTMASTER SET NAME = ? WHERE IDA2A2 = ?";
		WTConnection connection = CommUtil.getWTConnection();
		PreparedStatement statement = connection.prepareStatement(updateQuery);
		statement.setString(1, newName);
		statement.setString(2, String.valueOf(ida2a2));
		return statement.executeUpdate();
	}

	public static void changePartListName(PartList partList, String newName) {
		try {
			PartListMaster partListMaster = (PartListMaster) partList.getMaster();
			PartListMasterIdentity partListMasteridentity = (PartListMasterIdentity) partListMaster
					.getIdentificationObject();
			partListMasteridentity.setName(newName);
			partListMaster = (PartListMaster) IdentityHelper.service.changeIdentity(partListMaster,
					partListMasteridentity);
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
	}

	public static int changePartListNameBySql(PartList partList, String newName) throws Exception {
		PartListMaster partListMaster = (PartListMaster) partList.getMaster();
		Long ida2a2 = PersistenceHelper.getObjectIdentifier(partListMaster).getId();
		String updateQuery = "UPDATE PARTLISTMASTER SET NAME = ? WHERE IDA2A2 = ?";
		WTConnection connection = CommUtil.getWTConnection();
		PreparedStatement statement = connection.prepareStatement(updateQuery);
		statement.setString(1, newName);
		statement.setString(2, String.valueOf(ida2a2));
		return statement.executeUpdate();
	}

	public static HashSet<String> getInnerNameList() {
		HashSet<String> innerNameList = new HashSet<>();
		innerNameList.add(PropertiesHelper.getStrFromProperties(Util.partListRefInnerName));
		innerNameList.add(PropertiesHelper.getStrFromProperties(Util.contentRefInnerName));
		innerNameList.add(PropertiesHelper.getStrFromProperties(Util.psSectionInnerName));
		innerNameList.add(PropertiesHelper.getStrFromProperties(Util.psRootInnerName));
		innerNameList.add(PropertiesHelper.getStrFromProperties(Util.textualContentInnerName));
		return innerNameList;
	}
}