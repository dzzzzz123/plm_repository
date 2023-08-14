package ext.sinoboom.workFlowVerify;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ext.ait.util.IBAUtil;
import ext.bht.tool.CommUtil;
import wt.change2.WTChangeActivity2;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.pom.WTConnection;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

public class PartSourceSet {

	private static final Set<String> VALID_PART_NUMBERS = Set.of("1", "2");
	private static final String EXTERNAL_SOURCE = "buy";

	/**
	 * 工作流中需要调用的方法
	 * ext.sinoboom.workFlowVerify.PartSourceSet.SetPartSource(primaryBusinessObject);
	 * 
	 * @param pbo
	 * @throws Exception
	 */
	public static void SetPartSource(WTObject pbo) throws Exception {
		WTPart part = null;
		if (pbo instanceof PromotionNotice) {
			PromotionNotice pn = (PromotionNotice) pbo;
			// 获取流程的多个对象
			QueryResult qr = MaturityHelper.service.getPromotionTargets(pn);
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof WTPart) {
					part = (WTPart) obj;
					System.out.println("<----PromotionNotice.WTPart--->" + part);
					changeSourceChildPart(part, setWtPartSource(part));
				}
			}
		} else if (pbo instanceof WTChangeActivity2) {
			WTChangeActivity2 eca = (WTChangeActivity2) pbo;
			wt.fc.QueryResult changeables = wt.change2.ChangeHelper2.service.getChangeablesAfter(eca);
			if (changeables != null) {
				while (changeables.hasMoreElements()) {
					Object localObject = changeables.nextElement();
					if (localObject instanceof WTPart) {
						part = (WTPart) localObject;
						System.out.println("<----WTChangeActivity2.WTPart--->" + part);
						changeSourceChildPart(part, setWtPartSource(part));
					}
				}
			}
		}
	}

	/**
	 * 判断部件上级部件（三层），设置部件源的属性
	 * 
	 * @param part
	 */
	public static boolean setWtPartSource(WTPart part) {
		String startNumber = part.getNumber().substring(0, 1);
		if (!VALID_PART_NUMBERS.contains(startNumber)) {
			return false;
		}
		return checkSuperPartsSource(part, 3);
	}

	/**
	 * 使用并行流将下面两个获取父部件和校验部件源的方法使用递归拼接起来
	 * 
	 * @param wtParts
	 * @return
	 */
	private static boolean checkSuperPartsSource(WTPart part, int maxDepth) {
		if (maxDepth <= 0) {
			return false;
		}

		List<WTPart> superParts = getSuperWTPartsFromPart(part);

		if (superParts.isEmpty()) {
			return false;
		}

		return superParts.parallelStream().anyMatch(PartSourceSet::checkWtPartsSource)
				|| superParts.parallelStream().anyMatch(superPart -> checkSuperPartsSource(superPart, maxDepth - 1));
	}

	/**
	 * 从部件中获取所有的父部件
	 * 
	 * @param part
	 * @return
	 */
	private static List<WTPart> getSuperWTPartsFromPart(WTPart part) {
		List<WTPart> superParts = new ArrayList<>();
		WTPartMaster master = part.getMaster();
		try {
			QueryResult queryResult = WTPartHelper.service.getUsedByWTParts(master);
			while (queryResult.hasMoreElements()) {
				Object obj = queryResult.nextElement();
				if (obj instanceof WTPart) {
					superParts.add((WTPart) obj);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return superParts;
	}

	/**
	 * 校验部件的源是否为外购
	 * 
	 * @param part
	 * @return
	 */
	private static boolean checkWtPartsSource(WTPart part) {
		String source = part.getSource().toString();
		return source.equals(EXTERNAL_SOURCE);
	}

	private static void changeSourceChildPart(WTPart part, boolean flag) {
		try {
			IBAUtil ibaPart = new IBAUtil(part);
			if (flag) {
				ibaPart.setIBAValue("SourceChildPart", "是");
			} else {
				ibaPart.setIBAValue("SourceChildPart", "否");
			}
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
	}

	public static int changeSourceBySql(WTPart wtpart) throws Exception {
		Long ida2a2 = PersistenceHelper.getObjectIdentifier(wtpart).getId();
		String updateQuery = "UPDATE WTPART SET SOURCE = 'buy' WHERE IDA2A2 = ?";
		WTConnection connection = CommUtil.getWTConnection();
		PreparedStatement statement = connection.prepareStatement(updateQuery);
		statement.setString(1, String.valueOf(ida2a2));
		return statement.executeUpdate();
	}
}
