package Jolt;

import Processing.FileProcessor;
import ij.plugin.frame.PlugInFrame;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.util.Iterator;

import ij.*;
import ij.gui.*;

import Annotation.*;

public class CellManager extends PlugInFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener, Iterable<Roi> {
    private static final int BUTTONS = 7;

    private static Frame instance;
    private Panel panel;
    private JPanel buttonPanel;
    private PopupMenu popupMenu;
    private JTabbedPane cellTabs;
    private Button moreButton;
    private JPanel cellGroups;
    private JPanel cellsInd;

    private JList cellList;
    private DefaultListModel cellListModel;
    private GeneralPath savePath = new GeneralPath();


    public String errorMsg;

    public CellManager() {
        super("Cell Manager");
        if(instance != null) {
            WindowManager.toFront(instance);
            return;
        }

        instance = this;
        cellList = new JList();
        errorMsg = null;
        showWindow();

    }

    private void showWindow() {
        ImageJ ij = IJ.getInstance();
        addKeyListener(ij);
        addMouseListener(this);
        addMouseWheelListener(this);
        WindowManager.addWindow(this);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        { // Initialize list of cell entities
            cellListModel = new DefaultListModel<>();
            cellList.setModel(cellListModel);
            GUI.scale(cellList);
            cellList.setPrototypeCellValue("0000-0000-0000");
            cellList.addListSelectionListener(this);
            cellList.addKeyListener(ij);
            cellList.addMouseWheelListener(this);
            cellList.addMouseListener(this);
        }

        panel = new Panel();
        panel.setLayout(new BorderLayout());

        { // add group tabs to panel
            cellTabs = new JTabbedPane();
            cellGroups = new JPanel();
            cellGroups.setLayout(new BorderLayout());
            cellGroups.add(new JScrollPane(), BorderLayout.WEST);

            cellsInd = new JPanel();
            cellsInd.setLayout(new BorderLayout());
            cellsInd.add(new JScrollPane(), BorderLayout.WEST);

            cellTabs.add("Groups", cellGroups);
            cellTabs.add("Cells", cellsInd);

            cellTabs.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int tabIndex = cellTabs.getSelectedIndex();
                        if (tabIndex != -1) {
                            String newTabName = JOptionPane.showInputDialog(panel, "Enter new name for tab:", cellTabs.getTitleAt(tabIndex));
                            if (newTabName != null && !newTabName.trim().isEmpty()) {
                                cellTabs.setTitleAt(tabIndex, newTabName.trim());
                            }
                        }
                    }
                }
            });

            panel.add(cellTabs, BorderLayout.CENTER);
        }

        { // add buttons to panel
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
            addButton("Add cell [t]");
            addButton("Remove cell [x]");
            addButton("Add group");
            addButton("Remove group");
            addButton("Open...");
            addButton("Save...");
            addButton("Rename");
            addButton("More...");
            addMoreMenu();
            GUI.scale(this);
        }

        { // add panels to frame
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.90;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            add(panel, gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 0.10;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            add(buttonPanel, gbc);
        }

        setSize(400, 400);
        setVisible(true);
    }

    void addMoreMenu(){
        popupMenu = new PopupMenu();
        GUI.scalePopupMenu(popupMenu);
        addMoreMenuItem("Process stack series");
        addMoreMenuItem("Multi-select annotate");
        addMoreMenuItem("Group-select annotate");
        addMoreMenuItem("Measure multiple");
        add(popupMenu);
    }

    void addMoreMenuItem(String s){
        MenuItem mItem = new MenuItem(s);
        mItem.addActionListener(this);
        popupMenu.add(mItem);
    }

    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        b.addKeyListener(IJ.getInstance());
        if (label.equals("More...")) moreButton = b;
        b.addMouseListener(this);
        buttonPanel.add(b);
    }

    private void addNewGroupTab() {
        int groupCount = cellTabs.getTabCount() - 1;
        JPanel newGroupPanel = new JPanel(new BorderLayout());
        newGroupPanel.add(new JScrollPane(), BorderLayout.WEST);
        cellTabs.add("Group " + (groupCount + 1), newGroupPanel);
    }

    private void removeGroupTab() {
        int groupIndex = cellTabs.getSelectedIndex();
        cellTabs.remove(groupIndex);
    }

//    addButton("Add cell [t]");
//    addButton("Remove cell [x]");
//    addButton("Add group");
//    addButton("Remove group");
//    addButton("Open...");
//    addButton("Save...");
//    addButton("Rename");
//    addButton("More...");
//    addMoreMenuItem("Process stack series");
//    addMoreMenuItem("Multi-select annotate");
//    addMoreMenuItem("Group-select annotate");
//    addMoreMenuItem("Measure multiple");

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add group":
                addNewGroupTab();
                break;
            case "Remove group":
                removeGroupTab();
                break;
            case "Add cell [t]":
                IJ.log("Cant do that yet");
                break;
            case "Remove cell [x]":
                IJ.log("Cant do that yet");
                break;
            case "Open...":
                IJ.open();
                break;
            case "Save...":
                if(cellList.getModel().getSize() == 0){
                    error("Cell list is empty");
                    break;
                } else if (cellList.getModel().getSize() > 0){
                    IJ.save(null);
                }
                break;
            case "Rename":
                IJ.log("not implemented");
                cellList.getSelectedIndex();
                break;
            case "More...":
                Point ploc = buttonPanel.getLocation();
                Point bloc = moreButton.getLocation();
                popupMenu.show(this, ploc.x, bloc.y);
                break;
            case "Process stack series":
                //TODO Figure out why align slices halts at waitforuserdialog
                if(IJ.getImage() != null) {
                    FileProcessor fP = new FileProcessor();
                    fP.setup("", IJ.getImage());
                    fP.run(IJ.getImage().getProcessor());
                } else {
                    IJ.noImage();
                }
                break;
            case "Multi-select annotate":
                SelectMultiple sP = new SelectMultiple();
                sP.run("");
                break;
            case "Group-select annotate":
                runGroupAnnotate(IJ.getImage());
                break;
            case "Measure multiple":
                break;
            default:
                break;
        }
    }

    private void runGroupAnnotate(ImagePlus image) {
        if(image != null){
            GroupROIs gR = new GroupROIs();
            gR.setup("", image);
            gR.run(image.getProcessor());
        } else {
            IJ.noImage();
        }
    }

    boolean error(String msg) {
        new MessageDialog(this, "Cell Manager", msg);
        Macro.abort();
        return false;
    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mouseWheelMoved(MouseWheelEvent e) {

    }


    public void itemStateChanged(ItemEvent e) {

    }


    public Iterator<Roi> iterator() {
        return null;
    }


    public void valueChanged(ListSelectionEvent e) {

    }
}
