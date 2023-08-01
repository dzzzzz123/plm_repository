package ext.sinoboom.publishStructure.mvc;

import java.util.ArrayList;

import com.ptc.arbortext.windchill.partlist.PartList;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormProcessorController;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import ext.sinoboom.publishStructure.Util;
import wt.part.WTPart;
import wt.session.SessionHelper;
import wt.util.WTException;

public class RefReNameProcessorContrller implements FormProcessorController {

	@Override
	public FormResult execute(NmCommandBean nmCommandBean) throws WTException {
		try {
			WTPart wtpart = null;
			NmOid pageOid = nmCommandBean.getPageOid();
			if (pageOid != null && pageOid.getRef() instanceof WTPart) {
				wtpart = (WTPart) pageOid.getRef();
			} else {
				NmOid primaryOid = nmCommandBean.getPrimaryOid();
				if (primaryOid != null && primaryOid.getRef() instanceof WTPart) {
					wtpart = (WTPart) primaryOid.getRef();
				}
			}
			if (wtpart == null) {
				ArrayList selected = nmCommandBean.getSelectedOidForPopup();
				if (selected != null && selected.size() == 1) {
					Object object = selected.get(0);
					if (object instanceof NmOid) {
						NmOid primaryOid = (NmOid) object;
						if (primaryOid.getRef() instanceof WTPart) {
							wtpart = (WTPart) primaryOid.getRef();
						}
					}
				}
			}

			if (wtpart == null) {
				FormResult result = new FormResult(FormProcessingStatus.FAILURE);
				result.addFeedbackMessage(new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(), null,
						null, new String[] { "修改请选择一个部件列表载体进行修改！" }));
				return result;
			}

			String partNewName = nmCommandBean.getTextParameter("partNewName");
			String refNewName = nmCommandBean.getTextParameter("refNewName");
			String partListNewName = nmCommandBean.getTextParameter("partListNewName");
			System.out.println("----partNewName------" + partNewName);
			System.out.println("----refNewName------" + refNewName);
			System.out.println("----partListNewName------" + partListNewName);
			if (partNewName.length() > 0) {
				Util.changeWtPartNameBySql(wtpart, partNewName);
			}
			WTPart ref = Util.getLinkedRefByPart(wtpart);
			if (refNewName.length() > 0 && ref != null) {
				Util.changeWtPartNameBySql(ref, refNewName);
			}
			PartList partList = Util.getPartListByPart(wtpart);
			if (partListNewName.length() > 0 && ref != null) {
				int i = Util.changePartListNameBySql(partList, partListNewName);
				System.out.println("修改了" + i + "条数据！");
			}

			FormResult result = new FormResult(FormProcessingStatus.SUCCESS);
			result.addFeedbackMessage(new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(), null, null,
					new String[] { "修改成功!" }));
			result.setSkipPageRefresh(true);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			FormResult result = new FormResult(FormProcessingStatus.FAILURE);
			result.addFeedbackMessage(new FeedbackMessage(FeedbackType.FAILURE, SessionHelper.getLocale(), null, null,
					new String[] { "创建失败：" + e.getMessage() }));
			return result;
		}
	}
}
