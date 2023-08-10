package ext.sinoboom.workFlowVerify;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import wt.change2.WTChangeActivity2;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.part.Source;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.util.WTException;

public class PartSourceSet {

	private static final Set<String> VALID_PART_NUMBERS = Set.of("1", "2");
	private static final String EXTERNAL_SOURCE = "buy";

	/**
	 * 工作流中需要调用的方法
	 * 
	 * @param pbo
	 * @throws WTException
	 */
	public static void SetPartSource(WTObject pbo) throws WTException {
		WTPart part = null;
		if (pbo instanceof PromotionNotice) {
			PromotionNotice pn = (PromotionNotice) pbo;
			// 获取流程的多个对象
			QueryResult qr = MaturityHelper.service.getPromotionTargets(pn);
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof WTPart) {
					part = (WTPart) obj;
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
					}
				}
			}
		}

		if (part != null && setWtPartSource(part)) {
			part.setSource(Source.BUY);
		}
	}

	/**
	 * 判断部件上级部件（三层），设置部件源的属性
	 * 
	 * @param part
	 */
	public static boolean setWtPartSource(WTPart part) {
		if (!VALID_PART_NUMBERS.contains(part.getNumber())) {
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
}
