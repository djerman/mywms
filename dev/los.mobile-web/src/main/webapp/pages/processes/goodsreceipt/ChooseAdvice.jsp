<%-- 
  Copyright (c) 2006 - 2010 LinogistiX GmbH

  www.linogistix.com
  
  Project: myWMS-LOS
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
The taglib directive below imports the JSTL library. If you uncomment it,
you must also add the JSTL library to the project. The Add Library... action
on Libraries node in Projects view can be used to add the JSTL 1.1 library.
--%>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
   		<meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>LOS</title>
        <link rel="stylesheet" href="<%=request.getContextPath()%>/pages/stylesheet.css" type="text/css" />
        <link rel="stylesheet" href="<%=request.getContextPath()%>/pages/responsive.css" type="text/css" />
        <!-- У <head> секцији -->
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/pages/custom-select.css" />
        <script src="${pageContext.request.contextPath}/pages/custom-select.js"></script>
    </head>
    
    <body class="verticalscroll" topmargin="0" leftmargin="0" marginwidth="0" marginheight="0" onload="load()">
        <f:view locale="#{GoodsReceiptBean.locale}">	
        
            <f:loadBundle var="bundle" basename ="de.linogistix.mobile.res.Bundle" /> 
            
            <h:form id="Form" styleClass="form" onsubmit="#{GoodsReceiptBean.chooseAdviceSubmited}">
                
                <p id="pHeader"class="pageheader">
	                <h:outputText id="pagetitle" value="#{bundle.GoodsReceipt_choose_advice}" styleClass="pagetitle"/>
	                <h:graphicImage id="logo" url="/pics/logo.gif" styleClass="logo"/>
                </p>
                   
                <div class="space">
                    <%--                <h:outputFormat id="infoMsg"  value="" styleClass="info"/> --%>
                    <h:messages id="messages" styleClass="error"/> 
                    <table width="100%" border="0">
                        <tr>
                            <td>
                                <h:outputText value="#{bundle.GoodsReceipt_advice_positions}"></h:outputText>
                            </td>
                        </tr>
                        
                        <tr>
                            <td>
                                <!-- <h:selectOneMenu id="adviceComboBox"
                                				 onchange="submit();"
                                				 valueChangeListener="#{GoodsReceiptBean.adviceComboBoxValueChanged}"
                                                 value="#{GoodsReceiptBean.selectedAdvice}" style="width:100%;" >
                                    <f:selectItems value="#{GoodsReceiptBean.assignedAdviceList}" />
                                </h:selectOneMenu> -->
                                
							<div class="custom-select-wrapper">
								<div class="custom-select" tabindex="0">
									<h:outputText value="#{''}" />
								</div>
								<div class="custom-options">
									<c:forEach var="item"
										items="${GoodsReceiptBean.assignedAdviceList}">
										<div class="custom-option" data-value="${item.value}">${item.label}</div>
									</c:forEach>
								</div>
								<h:inputHidden id="hiddenSelect"
									value="#{GoodsReceiptBean.selectedAdvice}" />
							</div>

						</td>
                            
                        </tr>
                        
                        <tr>
                            <td style="margin-left:5px">
                            	<h:outputLabel id="lotLabel" value="#{bundle.Lot}" styleClass="label" />
                            </td> 
                        </tr>
                        
                        <tr>
                            <td colspan="4" style="margin-left:5px;">
                                <table width="100%">
                                    <tr topmargin="0">
                                        <th scope="col">
                                        	<h:inputText id="lotTextField" 
                                        				 value="#{GoodsReceiptBean.lotName_Input}" 
                                        				 styleClass="input" />
                                        </th>
                                    </tr>
                            	</table>
                            </td>
                        </tr>
                        

                        <tr>
                            <td style="margin-left:5px">
                            	<h:outputLabel id="validToLabel" value="#{bundle.GoodsReceipt_valid_to}" styleClass="label" />
                            </td> 
                        </tr>
                        <tr>
                            <td colspan="4" style="margin-left:5px;">
                                <table width="100%">
                                    <tr topmargin="0">
                                        <th scope="col">
                                        	<h:inputText id="validToTextField" 
                                        				 value="#{GoodsReceiptBean.validTo_Input}" 
                                        				 styleClass="input" />
                                       	</th>
                                    </tr>
                            	</table>
                            </td>
                        </tr>
                    </table>
				</div>
                                       
				<%-- Command Buttons --%>
				<div class="buttonbar">  
                    <h:commandButton id="forwardButton" 
                    				 value="#{bundle.Forward}" 
                    				 action="#{GoodsReceiptBean.processAdviceChoosen}"
                    				 styleClass="commandButton"  />

                    <h:commandButton id="backButton" 
                    				 value="#{bundle.Cancel}" 
                    				 action="#{GoodsReceiptBean.backToSelection}" 
                    				 styleClass="commandButton"  />
                </div>
            </h:form>
        </f:view> 
        <script type="text/javascript">
            
            function load() {            
                setFocus();    
            }    
            
            function setFocus() {
                var s = document.getElementById('Form:adviceComboBox');
                //var s = document.querySelector('.custom-select');
                if(s){
                  s.focus();
                }
            }    
            
        </script>
    </body>
</html>
