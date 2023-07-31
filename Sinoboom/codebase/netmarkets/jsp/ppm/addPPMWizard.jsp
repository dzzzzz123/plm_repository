 <%@ taglib prefix="jca" uri="http://www.ptc.com/windchill/taglib/components"%>
  <%@ page language="java" contentType="text/html; charset=UTF-8"   pageEncoding="UTF-8"%>
 <%@ include file="/netmarkets/jsp/components/beginWizard.jspf" %> 
 <%@ include file="/netmarkets/jsp/components/includeWizBean.jspf" %> 


 <jca:wizard title="设置PPM项目"> 
           <jca:wizardStep action="addPPM" type="ppmWizard"/> 
 </jca:wizard>
 
  
 <%@include file="/netmarkets/jsp/util/end.jspf"%> 
