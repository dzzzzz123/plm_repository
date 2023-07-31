package ext.sinoboom.ppmService.entity;

import wt.fc.WTObject;

public class PPMProjectEntity extends WTObject {

	private String projectName;
	private String projectNumber;
	private String projectTime;
	private String projectStatus;
	private String projectUrl;

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectNumber() {
		return projectNumber;
	}

	public void setProjectNumber(String projectNumber) {
		this.projectNumber = projectNumber;
	}

	public String getProjectTime() {
		return projectTime;
	}

	public void setProjectTime(String projectTime) {
		this.projectTime = projectTime;
	}

	public String getProjectStatus() {
		return projectStatus;
	}

	public void setProjectStatus(String projectStatus) {
		this.projectStatus = projectStatus;
	}

	public String getProjectUrl() {
		return projectUrl;
	}

	public void setProjectUrl(String projectUrl) {
		this.projectUrl = projectUrl;
	}

	@Override
	public String toString() {
		return "PPMProjectEntity [projectName=" + projectName + ", projectNumber=" + projectNumber + ", projectTime="
				+ projectTime + ", projectStatus=" + projectStatus + ", projectUrl=" + projectUrl + "]";
	}

}
