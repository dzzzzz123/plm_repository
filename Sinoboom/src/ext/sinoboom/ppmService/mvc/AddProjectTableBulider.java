package ext.sinoboom.ppmService.mvc;

import java.util.List;

import com.ptc.mvc.components.AbstractComponentBuilder;
import com.ptc.mvc.components.ColumnConfig;
import com.ptc.mvc.components.ComponentBuilder;
import com.ptc.mvc.components.ComponentBuilderType;
import com.ptc.mvc.components.ComponentConfig;
import com.ptc.mvc.components.ComponentConfigFactory;
import com.ptc.mvc.components.ComponentParams;
import com.ptc.mvc.components.TableConfig;

import ext.sinoboom.ppmService.config.PPMConfig;
import ext.sinoboom.ppmService.entity.PPMProjectEntity;
import ext.sinoboom.ppmService.service.PPMProjectService;
import wt.util.WTException;

@ComponentBuilder(value = { "ppm.add.project.list" }, type = ComponentBuilderType.CONFIG_AND_DATA)
public class AddProjectTableBulider extends AbstractComponentBuilder {

	@Override
	public Object buildComponentData(ComponentConfig config, ComponentParams params) throws Exception {
		PPMProjectService ppmProjectService = new PPMProjectService();
		List<PPMProjectEntity> ppmProjectData = ppmProjectService.getPPMProjectData();

		return ppmProjectData;

	}

	@Override
	public ComponentConfig buildComponentConfig(ComponentParams params) throws WTException {

		String number = PPMConfig.getConfig("projectNumber");
		String name = PPMConfig.getConfig("projectName");
		String status = PPMConfig.getConfig("projectStatus");
		String time = PPMConfig.getConfig("projectTime");
		String url = PPMConfig.getConfig("projectUrl");

		ComponentConfigFactory configFactory = getComponentConfigFactory();
		TableConfig tableConfig = configFactory.newTableConfig();
		tableConfig.setActionModel("add ppm actions");// 设置模型
		tableConfig.setSelectable(true);// 显示表格前面的选择框
		tableConfig.setLabel("设置PPM项目");
		tableConfig.setConfigurable(true);

		ColumnConfig projectName = configFactory.newColumnConfig(name, true);
		projectName.setLabel("项目名称");
		tableConfig.addComponent(projectName);

		ColumnConfig projectNumber = configFactory.newColumnConfig(number, true);
		projectNumber.setLabel("项目编号");
		tableConfig.addComponent(projectNumber);

		ColumnConfig projectTime = configFactory.newColumnConfig(time, true);
		projectTime.setLabel("项目创建时间");
		tableConfig.addComponent(projectTime);

		ColumnConfig projectStatus = configFactory.newColumnConfig(status, true);
		projectStatus.setLabel("项目状态");
		tableConfig.addComponent(projectStatus);

		ColumnConfig projectUrl = configFactory.newColumnConfig(url, true);
		projectUrl.setLabel("项目地址");
		tableConfig.addComponent(projectUrl);

		return tableConfig;
	}
}
