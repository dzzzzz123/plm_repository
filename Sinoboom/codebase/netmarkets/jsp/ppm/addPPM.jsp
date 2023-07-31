 <%@ taglib prefix="jca" uri="http://www.ptc.com/windchill/taglib/components"%> 
 <%@ include file="/netmarkets/jsp/util/begin.jspf"%> 
 <%@ taglib prefix="mvc" uri="http://www.ptc.com/windchill/taglib/jcaMvc"%> 
 <%@ taglib prefix="wrap" uri="http://www.ptc.com/windchill/taglib/wrappers"%> 
 <%@ taglib tagdir="/WEB-INF/tags" prefix="wctags"%>
 <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 
 


<mvc:tableContainer compId="ppm.add.project.list" height="500" />


<script>

   // 在1000毫秒（3秒）后执行一次函数
setTimeout(function() {
    console.log("Delayed execution after 3 seconds.");
            // 获取所有的按钮元素
        var buttons = document.querySelectorAll(".x-btn-text");


        // 循环遍历每个按钮
        buttons.forEach(function(button) {
            // 获取按钮的文本内容
            var buttonText = button.textContent;

            // 根据按钮的文本内容来绑定单击事件
            switch (buttonText) {
                case "确定(O)":
                    button.addEventListener("click", function() {
                      var table = PTC.jca.table.Utils.getTable('ppm.add.project.list');
      
                      var allSelection = table.getSelectionModel().getSelections();
                      console.log(allSelection);
  
                      var data = [];

                      for (let i = 0; i < allSelection.length; i++) {
                         var project = allSelection[i].json;
						 console.log(project);
                         var newProject = removeHtmlTagsFromData(project);
                         data.push(newProject)
                    
                      }
                         var rsJson = JSON.stringify(data);
                         console.log(rsJson);
                         sendPPMProject(rsJson);
                    });
                    break;


                default:

                    break;
            }
        });

    
}, 1000);


// 定义移除 HTML 标签的函数
function removeHtmlTagsFromData(item) {
  const { projectName, projectNumber, projectStatus, projectTime, projectUrl } = item;
  const plainProjectUrl = projectUrl.replace(/&amp;/g, '&');
  return {
    projectName: projectName,
    projectNumber: projectNumber,
    projectStatus: projectStatus,
    projectTime: projectTime,
    projectUrl: plainProjectUrl
  };
}


function sendPPMProject(rsJson){

      jQuery.ajax({
        url: "servlet/Navigation/PPMProductAddProjectServlet",
        contentType: 'application/json',
        async: false,
        data: rsJson,
        dataType: "json",
        type: "post",
        success: function () {
          console.log("");
        }
      });

}



</script>

  
 <%@ include file="/netmarkets/jsp/util/end.jspf" %> 