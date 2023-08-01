package ext.sinoboom.publishStructure;

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

public class CollectContentRefProcessor extends DefaultObjectFormProcessor {

	@Override
	public FormResult doOperation(NmCommandBean nmCommandBean, List<ObjectBean> paramList) throws WTException {
		Transaction t = new Transaction();
		try {
			t.start();
			NmOid primaryOid2 = nmCommandBean.getPrimaryOid();
			Object content = primaryOid2.getRef();
			if (content instanceof WTPart) {
				WTPart part = (WTPart) content;
				Util.collectContentCarriers(part);
			} else {
				throw new WTException("只能在发布结构下收集载体");
			}
		} catch (Exception e) {
			t.rollback();
			e.printStackTrace();
			FormResult result = new FormResult(FormProcessingStatus.FAILURE);
			result.addFeedbackMessage(new FeedbackMessage(FeedbackType.FAILURE, SessionHelper.getLocale(), null, null,
					new String[] { "收集载体失败" + "，" + e.getMessage() }));
			return result;
		} finally {
			t.commit();
		}
		FormResult result = new FormResult(FormProcessingStatus.SUCCESS);
		result.addFeedbackMessage(new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(), null, null,
				new String[] { "收集载体成功！" }));
		return result;
	}
}
