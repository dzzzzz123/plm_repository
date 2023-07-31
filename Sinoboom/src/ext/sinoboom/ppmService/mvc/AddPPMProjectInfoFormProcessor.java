package ext.sinoboom.ppmService.mvc;

import java.util.ArrayList;
import java.util.List;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import ext.sinoboom.ppmService.config.PPMConfig;
import ext.sinoboom.ppmService.entity.PPMProjectEntity;
import ext.sinoboom.ppmService.servlet.PPMProductAddProjectServlet;
import ext.sinoboom.ppmService.util.IBAUtil;
import wt.pdmlink.PDMLinkProduct;
import wt.session.SessionHelper;
import wt.util.WTException;

public class AddPPMProjectInfoFormProcessor extends DefaultObjectFormProcessor {

	@Override
	public FormResult doOperation(NmCommandBean arg0, List<ObjectBean> arg1) throws WTException {
		PDMLinkProduct ref = (PDMLinkProduct) arg0.getPrimaryOid().getRef();
		FormResult formresult = null;

		try {
			String number = PPMConfig.getConfig("projectNumber");
			String name = PPMConfig.getConfig("projectName");
			String url = PPMConfig.getConfig("projectUrl");

			IBAUtil ibaUtil = new IBAUtil(ref);
			List<PPMProjectEntity> list = PPMProductAddProjectServlet.list;

			if (list == null) {
				new Exception();
			}
			;
			ArrayList<String> projectNumbers = new ArrayList();
			ArrayList<String> projectNames = new ArrayList();
			ArrayList<String> projectUrls = new ArrayList();

			for (PPMProjectEntity item : list) {
				projectNumbers.add(item.getProjectNumber());
				projectNames.add(item.getProjectName());
				projectUrls.add(item.getProjectUrl());
			}
			ibaUtil.newIBAAttributes(ref, number, projectNumbers);
			ibaUtil.newIBAAttributes(ref, name, projectNames);
			ibaUtil.newIBAAttributes(ref, url, projectUrls);

		} catch (Exception e) {
			formresult = new FormResult(FormProcessingStatus.FAILURE);
			formresult.addFeedbackMessage(new FeedbackMessage(FeedbackType.FAILURE, SessionHelper.getLocale(), null,
					null, new String[] { "设置失败！", e.getMessage() }));
			return formresult;
		}

		formresult = new FormResult(FormProcessingStatus.SUCCESS);
		formresult.addFeedbackMessage(new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(), null, null,
				new String[] { "设置成功！" }));
		return formresult;
	}

}
