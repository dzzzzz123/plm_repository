package ext.sinoboom.ppmService.mvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import ext.sinoboom.ppmService.config.PPMConfig;
import ext.sinoboom.ppmService.entity.PPMProjectEntity;
import ext.sinoboom.ppmService.entity.ProjectNumberOrUrlInfo;
import ext.sinoboom.ppmService.servlet.PPMProductAddProjectServlet;
import ext.sinoboom.ppmService.util.IBAUtil;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.iba.definition.URLDefinition;
import wt.iba.value.URLValue;
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

			IBAUtil ibaUtil = new IBAUtil(ref);
			List<PPMProjectEntity> list = PPMProductAddProjectServlet.list;

			if (list == null) {
				throw new Exception("--数据为空");
			}
			;

			ArrayList<String> projectNames = new ArrayList();
			ArrayList<ProjectNumberOrUrlInfo> values = new ArrayList();

			for (PPMProjectEntity item : list) {

				projectNames.add(item.getProjectName());
				ProjectNumberOrUrlInfo info = new ProjectNumberOrUrlInfo();
				System.out.println("url: " + item.getProjectUrl());
				info.setProjectNumber(item.getProjectNumber());
				info.setProjectUrl(item.getProjectUrl());
				values.add(info);

			}

			ibaUtil.newIBAAttributes(ref, name, projectNames);

			newIBAURLAttributes(ref, number, ibaUtil, values);

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

	private void newIBAURLAttributes(PDMLinkProduct ref, String number, IBAUtil ibaUtil,
			ArrayList<ProjectNumberOrUrlInfo> values) throws WTException {
		URLDefinition fd = ibaUtil.findURLDefinition(number);

		/**
		 * 先删除所有的值
		 */
		Vector<URLValue> urlValues = ibaUtil.getURLValues(number);
		for (URLValue Item : urlValues) {
			PersistenceHelper.manager.delete(Item);
		}

		/**
		 * 再增加传入的值，如果没有值，则清空软属性
		 */

		for (ProjectNumberOrUrlInfo value : values) {
			URLValue fv = URLValue.newURLValue(fd, ref, value.getProjectUrl(), value.getProjectNumber());
			PersistenceServerHelper.manager.insert(fv);
		}
	}

}
