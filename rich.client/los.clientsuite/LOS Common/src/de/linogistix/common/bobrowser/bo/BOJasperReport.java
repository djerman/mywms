/*
 * Copyright (c) 2012-2013 LinogistiX GmbH
 *
 *  www.linogistix.com
 *
 *  Project myWMS-LOS
 */
package de.linogistix.common.bobrowser.bo;

import de.linogistix.common.bobrowser.masternode.BOLOSJasperReportMasterNode;
import de.linogistix.common.res.CommonBundleResolver;
import de.linogistix.common.services.J2EEServiceLocator;
import de.linogistix.common.userlogin.LoginService;
import de.linogistix.common.util.ExceptionAnnotator;
import de.linogistix.los.query.BusinessObjectQueryRemote;
import de.linogistix.los.query.LOSJasperReportQueryRemote;
import de.linogistix.los.crud.BusinessObjectCRUDRemote;
import de.linogistix.los.crud.LOSJasperReportCRUDRemote;
import de.wms2.mywms.product.ItemDataState;
import de.wms2.mywms.report.Report;
import java.util.ArrayList;
import java.util.List;
import org.mywms.globals.Role;
import org.mywms.model.BasicEntity;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.util.Lookup;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;

import org.openide.util.actions.SystemAction;
import de.linogistix.common.bobrowser.action.BOJasperReportCompileAction;
import de.linogistix.common.bobrowser.action.BOJasperReportRunAction;

/**
 * @author krane
 *
 */
public class BOJasperReport extends BO {

    private static String[] allowedRoles = new String[]{Role.ADMIN_STR,Role.FOREMAN_STR,Role.INVENTORY_STR,Role.CLEARING_STR};

    protected String initName() {
        return "JasperReports";
    }

    @Override
    protected String initIconBaseWithExtension() {
        return "de/linogistix/common/res/icon/Document.png";
    }

    protected BusinessObjectQueryRemote initQueryService() {

        BusinessObjectQueryRemote ret = null;

        try {
            J2EEServiceLocator loc = (J2EEServiceLocator) Lookup.getDefault().lookup(J2EEServiceLocator.class);
            ret = loc.getStateless(LOSJasperReportQueryRemote.class);

        } catch (Throwable t) {
            ExceptionAnnotator.annotate(t);
        }
        return ret;
    }

    protected BasicEntity initEntityTemplate() {
        Report c = new Report();

        LoginService login = (LoginService) Lookup.getDefault().lookup(LoginService.class);
        c.setClient( login.getUsersClient() );

        c.setName("Template");

        return c;
    }

    protected BusinessObjectCRUDRemote initCRUDService() {
        BusinessObjectCRUDRemote ret = null;

        try {
            J2EEServiceLocator loc = (J2EEServiceLocator) Lookup.getDefault().lookup(J2EEServiceLocator.class);
            ret = loc.getStateless(LOSJasperReportCRUDRemote.class);

        } catch (Throwable t) {
            ExceptionAnnotator.annotate(t);
        }
        return ret;
    }

    @Override
    protected Class initBundleResolver() {
        return CommonBundleResolver.class;
    }

    @Override
    protected String[] initIdentifiableProperties() {
        return new String[]{"name"};
    }

    @Override
    public String[] getAllowedRoles() {
        return allowedRoles;
    }

    @Override
    protected Property[] initBoMasterNodeProperties() {
        return BOLOSJasperReportMasterNode.boMasterNodeProperties();
    }

    @Override
    protected Class<? extends Node> initBoMasterNodeType() {
        return BOLOSJasperReportMasterNode.class;
    }

    @Override
    public List<Object> getValueList(String fieldName) {
        if( "state".equals(fieldName) ) {
            List<Object> entryList = new ArrayList<Object>();
            entryList.add(ItemDataState.ACTIVE);
            entryList.add(ItemDataState.INACTIVE);

            return entryList;
        }

        return super.getValueList(fieldName);
    }

    @Override
    public String getBundlePrefix() {
        return "JasperReports";
    }
   
    public BOJasperReport() {
            super();

    List<SystemAction> actions = new ArrayList<>();
    actions.add(SystemAction.get(BOJasperReportCompileAction.class));
    actions.add(SystemAction.get(BOJasperReportRunAction.class));

    setMasterActions(actions); // постојећи метод из BO класе
    }

}
