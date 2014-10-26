/*
 * Created by JFormDesigner on Sat Oct 18 16:54:05 MDT 2014
 */

package scout.clientPnls.IEPnls;

import au.com.bytecode.opencsv.CSVWriter;
import guiUtil.CustomChooser;
import guiUtil.SelectionBorder;
import scout.clientPnls.PnlBadgeConf;
import scout.dbObjects.Advancement;
import scout.dbObjects.Requirement;
import scout.dbObjects.RequirementTypeConst;
import util.LogicAdvancement;
import util.LogicMeritBadge;
import util.LogicRequirement;
import util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author User #2
 */
public class ExportDialog extends JDialog {
    private int exportTypeId;
    private java.util.List<String> srcList;
    private java.util.List<String> exportList;

    public ExportDialog(Frame owner) {
        super(owner, true);
        initComponents();
    }

    public ExportDialog(Frame owner, int exportTypeId) {
        super(owner, true);
        initComponents();

        contentPanel.setBorder(new SelectionBorder());

        this.exportTypeId = exportTypeId;

        switch (exportTypeId) {
            case PnlBadgeConf.ADVANCEMENT:
                setTitle("Advancement Export");
                srcList = LogicAdvancement.getAdvancementList();
                if (srcList == null) {
                    srcList = new ArrayList<String>();
                }
                break;
            case PnlBadgeConf.MERIT_BAGDGE:
                setTitle("Merit Badge Export");
                srcList = LogicMeritBadge.getNameList();
                if (srcList == null) {
                    srcList = new ArrayList<String>();
                }
                break;
            case PnlBadgeConf.OTHER:
                break;
        }

        exportList = new ArrayList<String>();
        rbtnExportAll.setSelected(true);
        showAll(false);
    }

    private void cancelButtonMouseClicked() {
        dispose();
    }

    private void exportButtonMouseClicked() {
        boolean success = false;
        switch (exportTypeId) {
            case PnlBadgeConf.ADVANCEMENT:
                success = handleAdvancementExport();
                break;
            case PnlBadgeConf.MERIT_BAGDGE:
                success = handleMeritBadgeExport();
                break;
            case PnlBadgeConf.OTHER:
                break;
        }

        if (success) {
            dispose();
        }
    }

    private void showAll(boolean show) {
        scrollPane1.setVisible(show);
        scrollPane2.setVisible(show);
        btnAdd.setVisible(show);
        btnRemove.setVisible(show);
        lblAvailable.setVisible(show);
        lblSelected.setVisible(show);
    }

    private void rbtnExportAllMouseClicked() {
        showAll(false);
    }

    private void populateSrcList() {
        listSrc.setListData(srcList.toArray());
        listSrc.revalidate();
    }

    private void resetExportList() {
        exportList.clear();
        listExport.setListData(exportList.toArray());
        listExport.revalidate();
    }

    private void populateExportList() {
        listExport.setListData(exportList.toArray());
        listExport.revalidate();
    }

    private void rbtnExportSelectedMouseClicked() {
        showAll(true);
        populateSrcList();
        resetExportList();
    }

    private void btnAddMouseClicked() {
        if (listSrc.getSelectedValue() == null) {
            return;
        }

        Object[] nameList = listSrc.getSelectedValues();
        srcList.removeAll(Arrays.asList(nameList));

        for (Object name : nameList) {
            exportList.add(name.toString());
        }

        populateSrcList();
        populateExportList();
    }

    private void btnRemoveMouseClicked() {
        if (listExport.getSelectedValue() == null) {
            return;
        }

        Object[] nameList = listExport.getSelectedValues();
        exportList.removeAll(Arrays.asList(nameList));

        for (Object name : nameList) {
            srcList.add(name.toString());
        }

        populateSrcList();
        populateExportList();
    }

    private boolean handleMeritBadgeExport() {
        // todo: add logic here
        return false;
    }

    public boolean handleAdvancementExport() {
        try {
            java.util.List<Advancement> advancementExportList;
            if (rbtnExportAll.isSelected()) {
                advancementExportList = LogicAdvancement.getAllAdvancements();
            } else {
                advancementExportList = new ArrayList<Advancement>();
                for (int i = 0; i < listExport.getModel().getSize(); ++i) {
                    advancementExportList.add(LogicAdvancement.findByName(listExport.getModel().getElementAt(i).toString()));
                }
            }

            if (Util.isEmpty(advancementExportList)) {
                JOptionPane.showMessageDialog(this, "There is nothing to export, please select at least one advancement and try again.", "No Items Selected", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // get the file path
            CustomChooser chooser = new CustomChooser("Select export location", JFileChooser.FILES_ONLY);

            chooser.setSelectedFile(new File("AdvancementExport.csv"));
            int returnValue = chooser.showSaveDialog(this);
            chooser.resetLookAndFeel();

            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return false;
            }

            String exportPath = chooser.getSelectedFile().getPath();

            // write to location
            StringWriter writer = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(writer, ',');
            java.util.List<String[]> records = new ArrayList<String[]>();

            records.add(new String[]{"Advancement Name", "Advancement Image Path"});
            records.add(new String[]{"Requirement Name", "Requirement Description"});

            boolean firstPass = true;
            for (Advancement advancement : advancementExportList) {
                if (!firstPass) {
                    records.add(new String[]{""});
                }

                if (Util.isEmpty(advancement.getImgPath())) {
                    records.add(new String[]{advancement.getName()});
                } else {
                    records.add(new String[]{advancement.getName(), advancement.getImgPath()});
                }

                java.util.List<Requirement> requirementList = LogicRequirement.findAllByParentIdTypeId(advancement.getId(), RequirementTypeConst.ADVANCEMENT.getId());
                if (Util.isEmpty(requirementList)) {
                    continue;
                }

                for (Requirement requirement : requirementList) {
                    records.add(new String[]{requirement.getName(), requirement.getDescription()});
                }

                if (firstPass) {
                    firstPass = false;
                }
            }

            csvWriter.writeAll(records);
            csvWriter.close();

            // check directory and let know if we are overwriting something, ask if it is ok
            if (!exportPath.endsWith(".csv")) {
                exportPath += ".csv";
            }

            FileWriter export = new FileWriter(exportPath);
            export.append(writer.toString());
            export.flush();
            export.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        JOptionPane.showMessageDialog(this, "Your selected advancement(s) have been successfully exported.", "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        rbtnExportAll = new JRadioButton();
        label1 = new JLabel();
        rbtnExportSelected = new JRadioButton();
        lblAvailable = new JLabel();
        lblSelected = new JLabel();
        scrollPane1 = new JScrollPane();
        listSrc = new JList();
        scrollPane2 = new JScrollPane();
        listExport = new JList();
        btnAdd = new JButton();
        btnRemove = new JButton();
        buttonBar = new JPanel();
        exportButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setMinimumSize(new Dimension(625, 465));
        setName("this");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setBackground(Color.white);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setMinimumSize(new Dimension(600, 400));
                contentPanel.setBackground(Color.white);
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new GridBagLayout());
                ((GridBagLayout)contentPanel.getLayout()).columnWidths = new int[] {15, 235, 85, 235, 10, 0};
                ((GridBagLayout)contentPanel.getLayout()).rowHeights = new int[] {0, 10, 0, 21, 0, 45, 45, 0, 10, 0};
                ((GridBagLayout)contentPanel.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0, 0.0, 1.0, 1.0E-4};
                ((GridBagLayout)contentPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0E-4};

                //---- rbtnExportAll ----
                rbtnExportAll.setText("Export All");
                rbtnExportAll.setBackground(Color.white);
                rbtnExportAll.setForeground(new Color(51, 102, 153));
                rbtnExportAll.setFont(new Font("Vijaya", Font.PLAIN, 22));
                rbtnExportAll.setFocusPainted(false);
                rbtnExportAll.setName("rbtnExportAll");
                rbtnExportAll.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        rbtnExportAllMouseClicked();
                    }
                });
                contentPanel.add(rbtnExportAll, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(10, 10, 7, 5), 0, 0));

                //---- label1 ----
                label1.setBackground(new Color(51, 102, 153));
                label1.setOpaque(true);
                label1.setMinimumSize(new Dimension(0, 2));
                label1.setPreferredSize(new Dimension(0, 2));
                label1.setMaximumSize(new Dimension(0, 2));
                label1.setName("label1");
                contentPanel.add(label1, new GridBagConstraints(0, 1, 5, 1, 0.0, 0.0,
                    GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 10, 5, 10), 0, 0));

                //---- rbtnExportSelected ----
                rbtnExportSelected.setText("Export Selected");
                rbtnExportSelected.setFont(new Font("Vijaya", Font.PLAIN, 22));
                rbtnExportSelected.setBackground(Color.white);
                rbtnExportSelected.setForeground(new Color(51, 102, 153));
                rbtnExportSelected.setFocusPainted(false);
                rbtnExportSelected.setName("rbtnExportSelected");
                rbtnExportSelected.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        rbtnExportSelectedMouseClicked();
                    }
                });
                contentPanel.add(rbtnExportSelected, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 10, 5, 5), 0, 0));

                //---- lblAvailable ----
                lblAvailable.setText("Available");
                lblAvailable.setForeground(new Color(51, 102, 153));
                lblAvailable.setFont(new Font("Tahoma", Font.PLAIN, 16));
                lblAvailable.setName("lblAvailable");
                contentPanel.add(lblAvailable, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

                //---- lblSelected ----
                lblSelected.setText("Selected");
                lblSelected.setForeground(new Color(51, 102, 153));
                lblSelected.setFont(new Font("Tahoma", Font.PLAIN, 16));
                lblSelected.setName("lblSelected");
                contentPanel.add(lblSelected, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- listSrc ----
                    listSrc.setBorder(new LineBorder(new Color(51, 102, 153), 2));
                    listSrc.setFont(new Font("Tahoma", Font.PLAIN, 14));
                    listSrc.setName("listSrc");
                    scrollPane1.setViewportView(listSrc);
                }
                contentPanel.add(scrollPane1, new GridBagConstraints(1, 4, 1, 4, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

                //======== scrollPane2 ========
                {
                    scrollPane2.setName("scrollPane2");

                    //---- listExport ----
                    listExport.setBorder(new LineBorder(new Color(51, 102, 153), 2));
                    listExport.setFont(new Font("Tahoma", Font.PLAIN, 14));
                    listExport.setName("listExport");
                    scrollPane2.setViewportView(listExport);
                }
                contentPanel.add(scrollPane2, new GridBagConstraints(3, 4, 1, 4, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

                //---- btnAdd ----
                btnAdd.setText("Add");
                btnAdd.setBackground(new Color(51, 156, 229));
                btnAdd.setForeground(Color.white);
                btnAdd.setFont(new Font("Tahoma", Font.PLAIN, 14));
                btnAdd.setFocusPainted(false);
                btnAdd.setName("btnAdd");
                btnAdd.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        btnAddMouseClicked();
                    }
                });
                contentPanel.add(btnAdd, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 5, 5, 10), 0, 0));

                //---- btnRemove ----
                btnRemove.setText("Remove");
                btnRemove.setBackground(new Color(206, 0, 0));
                btnRemove.setForeground(Color.white);
                btnRemove.setFont(new Font("Tahoma", Font.PLAIN, 14));
                btnRemove.setFocusPainted(false);
                btnRemove.setName("btnRemove");
                btnRemove.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        btnRemoveMouseClicked();
                    }
                });
                contentPanel.add(btnRemove, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 5, 5, 10), 0, 0));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setBackground(Color.white);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).rowHeights = new int[] {50};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- exportButton ----
                exportButton.setText("Export");
                exportButton.setForeground(Color.white);
                exportButton.setBackground(new Color(51, 156, 229));
                exportButton.setFont(new Font("Tahoma", Font.PLAIN, 14));
                exportButton.setFocusPainted(false);
                exportButton.setName("exportButton");
                exportButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        exportButtonMouseClicked();
                    }
                });
                buttonBar.add(exportButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setForeground(Color.white);
                cancelButton.setBackground(new Color(206, 0, 0));
                cancelButton.setFont(new Font("Tahoma", Font.PLAIN, 14));
                cancelButton.setFocusPainted(false);
                cancelButton.setName("cancelButton");
                cancelButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        cancelButtonMouseClicked();
                    }
                });
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        ButtonGroup buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(rbtnExportAll);
        buttonGroup1.add(rbtnExportSelected);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JRadioButton rbtnExportAll;
    private JLabel label1;
    private JRadioButton rbtnExportSelected;
    private JLabel lblAvailable;
    private JLabel lblSelected;
    private JScrollPane scrollPane1;
    private JList listSrc;
    private JScrollPane scrollPane2;
    private JList listExport;
    private JButton btnAdd;
    private JButton btnRemove;
    private JPanel buttonBar;
    private JButton exportButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
