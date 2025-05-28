package de.linogistix.common.bobrowser.action;

import de.linogistix.common.bobrowser.masternode.BOLOSJasperReportMasterNode;
import de.linogistix.los.query.dto.LOSJasperReportTO;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import de.linogistix.common.res.CommonBundleResolver;

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import de.linogistix.los.query.dto.LOSJasperReportTO;

import de.linogistix.common.bobrowser.dialog.ReportParameterDialog;
import java.util.Map;


/**
 * Акција за покретање Jasper извештаја – појављује се у десном клику
 */
public class BOJasperReportRunAction extends NodeAction {

    private static final Logger log = Logger.getLogger(BOJasperReportRunAction.class.getName());

    @Override
    protected boolean enable(Node[] activatedNodes) {
        // Активирај само ако је извештај компајлиран
        if (activatedNodes == null || activatedNodes.length != 1) {
            return false;
        }

        Node n = activatedNodes[0];
        if (n instanceof BOLOSJasperReportMasterNode) {
            BOLOSJasperReportMasterNode node = (BOLOSJasperReportMasterNode) n;
            LOSJasperReportTO to = (LOSJasperReportTO) node.getEntity();
            return to.isCompiled();
        }

        return false;
    }

@Override
protected void performAction(Node[] activatedNodes) {
    if (activatedNodes == null || activatedNodes.length != 1) {
        return;
    }

    Node n = activatedNodes[0];
    if (n instanceof BOLOSJasperReportMasterNode) {
        BOLOSJasperReportMasterNode node = (BOLOSJasperReportMasterNode) n;
        LOSJasperReportTO to = (LOSJasperReportTO) node.getEntity();

        if (!to.isCompiled()) {
            NotifyDescriptor nd = new NotifyDescriptor.Message(
                NbBundle.getMessage(CommonBundleResolver.class, "BOJasperReportRunAction.notCompiled"),
                NotifyDescriptor.INFORMATION_MESSAGE
            );
            DialogDisplayer.getDefault().notify(nd);
            return;
        }

        Long reportId = to.getId();
        String name = to.getName();

        Map<String, Object> params = ReportParameterDialog.showDialog();
        if (params != null) {
           System.out.println("Изабрани параметри: " + params);
        }

        log.info("Покрећем извештај: ID=" + reportId + ", Назив=" + name);

        // TODO: Отвори дијалог за параметре и покрени извештај
    }
}


    @Override
    public String getName() {
        return NbBundle.getMessage(CommonBundleResolver.class, "BOJasperReportRunAction.name");
    }

    @Override
    protected String iconResource() {
        return "de/linogistix/common/res/icon/ReportRun.png"; // стави стварну иконицу ако имаш
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
