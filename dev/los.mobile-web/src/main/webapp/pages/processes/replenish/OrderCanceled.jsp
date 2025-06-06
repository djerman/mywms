<%-- 
  Copyright (c) 2010 LinogistiX GmbH

  www.linogistix.com
  
  Project: myWMS-LOS
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
   		<meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>LOS</title>
        <link rel="stylesheet" href="<%=request.getContextPath()%>/pages/stylesheet.css" type="text/css" />
        <link rel="stylesheet" href="<%=request.getContextPath()%>/pages/responsive.css" type="text/css" />
    </head>
    
    <body class="verticalscroll" topmargin="0" leftmargin="0" marginwidth="0" marginheight="0" onload="load()">
        <f:view locale="#{ReplenishBean.locale}">
            <f:loadBundle var="bundle" basename ="de.linogistix.mobile.processes.replenish.ReplenishBundle" /> 
            
            <h:form id="Form" styleClass="form" >
                <%-- Page Title--%>
                <p id="pHeader"class="pageheader">
                	<h:outputText id="pagetitle" value="#{bundle.TitleCanceled}" styleClass="pagetitle"/>
	                <h:graphicImage id="logo" url="/pics/logo.gif" styleClass="logo"/>
               	</p>
                
                <div class="space">
                    <h:messages id="messages"  styleClass="error"/> 
                    <table  width="100%" border="0" cellspacing="0">
                    	<colgroup>
							<col width="10%"/>
							<col width="90%"/>
						</colgroup>
                    	
                   		<tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="clientLabel" value="#{bundle.LabelClient}:" styleClass="param" rendered="#{ReplenishBean.showClient}"/> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="clientData" value="#{ReplenishBean.clientNumber}" styleClass="label" rendered="#{ReplenishBean.showClient}"/>
                            </td>
                        </tr>
                    	<tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="sourceLabel" value="#{bundle.LabelSourceLocation}:" styleClass="param"/> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="sourceData" value="#{ReplenishBean.sourceName}" styleClass="labelBold"/>
                            </td>
                        </tr>
                    	<tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="ulLabel" value="#{bundle.LabelUnitLoad}:" styleClass="param" /> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="ulData" value="#{ReplenishBean.sourceUnitLoad}" styleClass="labelBold" />
                            </td>
                        </tr>
                        <tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="destinationLabel" value="#{bundle.LabelDestination}:" styleClass="param"/> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="destinationData" value="#{ReplenishBean.destinationName}" styleClass="label"/>
                            </td>
                        </tr>
                    </table>

				</div>
				
                <div class="space" style="padding-top:15px">
                   	<h:outputLabel id="numPosLabel" value="#{bundle.MsgOrderCanceled}" styleClass="labelBold"/> 
                </div>
                

                <%-- Command Buttons --%>
                <div class="buttonbar">  
                    <h:commandButton id="BUTTON_CONTINUE" 
	                				 value="#{bundle.ButtonContinue}" 
	                				 action="#{ReplenishBean.processOrderDone}" 
	                				 styleClass="commandButton"  />
                </div>
            </h:form>
        </f:view> 
        <script type="text/javascript">
            
            function load() {            
                setFocus();    
            }    
            
            function setFocus() {
                document.getElementById('Form:BUTTON_CONTINUE').focus();
            }    
            
        </script>
        
    </body>
</html>
