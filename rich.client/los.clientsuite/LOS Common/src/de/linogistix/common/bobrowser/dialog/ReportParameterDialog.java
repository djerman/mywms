package de.linogistix.common.bobrowser.dialog;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle;
import de.linogistix.common.res.CommonBundleResolver;

public class ReportParameterDialog {

    public static Map<String, Object> showDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));

        JTextField paramName = new JTextField();
        JCheckBox activeOnly = new JCheckBox();

        panel.add(new JLabel(NbBundle.getMessage(CommonBundleResolver.class, "ReportParameterDialog.param.name")));
        panel.add(paramName);

        panel.add(new JLabel(NbBundle.getMessage(CommonBundleResolver.class, "ReportParameterDialog.param.active")));
        panel.add(activeOnly);

        DialogDescriptor dd = new DialogDescriptor(
            panel,
            NbBundle.getMessage(CommonBundleResolver.class, "ReportParameterDialog.title"),
            true,
            DialogDescriptor.OK_CANCEL_OPTION,
            DialogDescriptor.OK_OPTION,
            null
        );

        Object result = DialogDisplayer.getDefault().notify(dd);

        if (result == DialogDescriptor.OK_OPTION) {
            Map<String, Object> params = new HashMap<>();
            params.put("name", paramName.getText());
            params.put("activeOnly", activeOnly.isSelected());
            return params;
        }

        return null;
    }
}
