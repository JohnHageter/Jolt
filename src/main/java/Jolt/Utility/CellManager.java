package Jolt.Utility;

import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import ij.IJ;
import ij.ImageJ;
import ij.WindowManager;
import ij.plugin.frame.PlugInFrame;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

public class CellManager extends PlugInFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener, Iterable<CellData> {
    private static Frame instance;
    private static String errorMessage;
    private JList cellList;
    private int nButtons = 0;
    private GridBagLayout layout = new GridBagLayout();
    private static JPanel panel = new JPanel();
    private static GridBagConstraints gbc = new GridBagConstraints();
    private boolean allowRecording;


    public CellManager() {
        super("Cell Manager");
        if (instance != null) {
            WindowManager.toFront(instance);
            return;
        }

        instance = this;
        cellList = new JList();
        errorMessage = null;
        showCellManager();

    }

    private void showCellManager() {
        ImageJ ij = IJ.getInstance();

        try {
            FlatOneDarkIJTheme.setup();
            UIManager.setLookAndFeel(new FlatOneDarkIJTheme());
        } catch (Exception e) {
            IJ.log("Failed to initialize theme: " + e.getMessage());
        }

        addKeyListener(ij);
        addMouseListener(this);
        addMouseWheelListener(this);
        WindowManager.addWindow(this);;

        panel.setLayout(layout);

        addButton("Register",       2,0,1,1);
        addButton("Apply group(s)", 2,1,1,1);
        addButton("Select multiple",2,2,1,1);
        addButton("Analyze",        2,3,1,1);
        addButton("Area analysis",  2,4,1,1);
        addButton("Load",           2,5,1,1);
        addButton("Save",           2,6,1,1);
        addButton("More...",        2,8,1,1);

        addButton("Cells", 0, nButtons, 1,1); nButtons--;
        addButton("Groups", 1, nButtons, 1,1); nButtons--;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth= 2;
        gbc.gridheight = nButtons;

        JScrollPane roiPane = new JScrollPane(cellList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        layout.setConstraints(roiPane, gbc);
        panel.add(roiPane, gbc);

        this.add(panel);
        this.pack();
        this.setVisible(true);
    }

    void addButton(String name, int gridx, int gridy, int gridwidth, int gridheight) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;

        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;

        gbc.fill = GridBagConstraints.BOTH;
        JButton b = new JButton(name);
        b.addActionListener(this);
        b.addKeyListener(IJ.getInstance());
        b.addMouseListener(this);
        panel.add(b, gbc);
        nButtons++;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label == null) {
            return;
        }
        allowRecording = true;


//        addButton("Register",       2,0,1,1);
//        addButton("Apply group(s)", 2,1,1,1);
//        addButton("Select multiple",2,2,1,1);
//        addButton("Analyze",        2,3,1,1);
//        addButton("Area analysis",  2,4,1,1);
//        addButton("Load",           2,5,1,1);
//        addButton("Save",           2,6,1,1);
//        addButton("More...",        2,8,1,1);
//
//        addButton("Cells", 0, nButtons, 1,1); nButtons--;
//        addButton("Groups", 1, nButtons, 1,1); nButtons--;


        switch (label) {
            case "Register":
                IJ.log("Register");
                break;
            case "Apply group(s)":
                IJ.log("Apply group(s)");
                break;
            case "Select multiple":
                IJ.log("Select multiple");
                break;
            case "Analyze":
                IJ.log("Analyze");
                break;
            case "Area analysis":
                IJ.log("Area analysis");
                break;
            case "Load":
                IJ.log("Load");
                break;
            case "Save":
                IJ.log("Save");
                break;
            case "More...":
                IJ.log("More...");
                break;
            case "Groups":
                IJ.log("Switch to group tab");
                break;
            case "Cells":
                IJ.log("Switch to Cell tab");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + label);
        }

    }

    @Override
    public void itemStateChanged(ItemEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

    }

    @Override
    public Iterator<CellData> iterator() {
        return null;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

    }
}
