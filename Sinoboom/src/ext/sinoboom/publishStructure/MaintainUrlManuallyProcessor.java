package ext.sinoboom.publishStructure;

import java.util.HashSet;
import java.util.List;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.part.WTPart;
import wt.pom.Transaction;
import wt.session.SessionHelper;
import wt.util.WTException;

public class MaintainUrlManuallyProcessor extends DefaultObjectFormProcessor {

	@Override
	public FormResult doOperation(NmCommandBean nmCommandBean, List<ObjectBean> paramList) throws WTException {
		Transaction t = new Transaction();
		String ErrorMsg = "";
		try {
			t.start();
			NmOid primaryOid2 = nmCommandBean.getPrimaryOid();
			Object content = primaryOid2.getRef();
			HashSet<String> innerNameList = Util.getInnerNameList();
			if (content instanceof WTPart) {
				WTPart part = (WTPart) content;
				String typeName = Util.getPerType(part);
				if (innerNameList.contains(typeName)) {
					ErrorMsg = Util.maintainRefUrl(part);
					if (ErrorMsg.length() > 0) {
						throw new Exception(ErrorMsg);
					}
				} else {
					ErrorMsg = "只能更新部件列表载体，内容载体，发布部分，发布结构，文字内容载体这些部件类型的Url链接";
					throw new Exception(ErrorMsg);
				}
			} else {
				ErrorMsg = "只能更新部件的Url链接";
				throw new Exception(ErrorMsg);
			}
		} catch (Exception e) {
			e.printStackTrace();
			FormResult result = new FormResult(FormProcessingStatus.FAILURE);
			result.addFeedbackMessage(new FeedbackMessage(FeedbackType.FAILURE, SessionHelper.getLocale(), null, null,
					"更新Url链接失败，" + e.getMessage()));
			t.rollback();
			return result;
		} finally {
			t.commit();
		}
		FormResult result = new FormResult(FormProcessingStatus.SUCCESS);
		result.addFeedbackMessage(
				new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(), null, null, "更新Url链接成功！"));
		return result;
	}
}
