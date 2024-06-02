package Jolt;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.Prefs;
import ij.Undo;
import ij.WindowManager;
import ij.gui.ColorChooser;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageRoi;
import ij.gui.MessageDialog;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import ij.gui.RoiProperties;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.gui.YesNoCancelDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.io.SaveDialog;
import ij.macro.Interpreter;
import ij.macro.MacroRunner;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Colors;
import ij.plugin.MacroInstaller;
import ij.plugin.OverlayCommands;
import ij.plugin.OverlayLabels;
import ij.plugin.RoiRotator;
import ij.plugin.RoiScaler;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.Filler;
import ij.plugin.frame.PlugInFrame;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.Tools;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CellManager extends PlugInFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener, Iterable<Roi> {
    public static final String LOC_KEY = "manager.loc";
    private static final String MULTI_CROP_DIR = "multi-crop.dir";
    private static final int BUTTONS = 11;
    private static final int DRAW = 0;
    private static final int FILL = 1;
    private static final int LABEL = 2;
    private static final int SHOW_ALL = 0;
    private static final int SHOW_NONE = 1;
    private static final int LABELS = 2;
    private static final int NO_LABELS = 3;
    private static final int MENU = 0;
    private static final int COMMAND = 1;
    private static final int IGNORE_POSITION = -999;
    private static final int CHANNEL = 0;
    private static final int SLICE = 1;
    private static final int FRAME = 2;
    private static final int SHOW_DIALOG = 3;
    private static int rows = 15;
    private static int lastNonShiftClick = -1;
    private static boolean allowMultipleSelections = true;
    private static String moreButtonLabel = "More »";
    private Panel panel;
    private static Frame instance;
    private static int colorIndex = 4;
    private JList list;
    private DefaultListModel listModel;
    private ArrayList rois = new ArrayList();
    private boolean canceled;
    private boolean macro;
    private boolean ignoreInterrupts;
    private PopupMenu pm;
    private Button moreButton;
    private Button colorButton;
    private Checkbox showAllCheckbox = new Checkbox("Show All", false);
    private Checkbox labelsCheckbox = new Checkbox("Labels", false);
    private Overlay overlayTemplate;
    private static boolean measureAll = true;
    private static boolean onePerSlice = true;
    private static boolean restoreCentered;
    private int prevID;
    private boolean noUpdateMode;
    private int defaultLineWidth = 1;
    private Color defaultColor;
    private boolean firstTime = true;
    private boolean appendResults;
    private static ResultsTable mmResults;
    private static ResultsTable mmResults2;
    private int imageID;
    private boolean allowRecording;
    private boolean recordShowAll = true;
    private boolean allowDuplicates;
    private double translateX = 10.0;
    private double translateY = 10.0;
    private static String errorMessage;
    private boolean multiCropShow = true;
    private boolean multiCropSave;
    private int multiCropFormatIndex;
    private static double angle = 45.0;
    private static double xscale = 1.5;
    private static double yscale = 1.5;
    private static boolean scaleCentered = true;
    private static boolean channel = false;
    private static boolean slice = true;
    private static boolean frame = false;


    public CellManager() {
        super("Cell Manager");
        if(instance != null){
            WindowManager.toFront(instance);
        } else {
            instance = this;
            this.list = new JList();
            errorMessage = null;
            this.showWindow();
        }
        showWindow();
    }

    void showWindow() {
        ImageJ ij = IJ.getInstance();

        this.addKeyListener(ij);
        this.addMouseListener(this);
        this.addMouseWheelListener(this);

        WindowManager.addWindow(this);
        this.setLayout(new BorderLayout());

        this.listModel = new DefaultListModel();
        this.list.setModel(this.listModel);
        GUI.scale(list);
        list.setPrototypeCellValue("Cell-0000-0000");

        list.addListSelectionListener(this);
        list.addKeyListener(ij);
        list.addMouseListener(this);
        list.addMouseWheelListener(this);

        JScrollPane scrollPane = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add("Center", scrollPane);
        panel = new Panel();
        int nButtons = 8;
        panel.setLayout(new GridLayout(nButtons, 1, 5, 0));

        addButton("Add [t]");
        addButton("Delete");
        addButton("Rename");
        addButton("Measure Grey");
        addButton("Measure DeltaF");
        addButton("Deselect");
        addButton("Properties...");
        addButton(moreButtonLabel);

        showAllCheckbox.addItemListener(this);
        panel.add(labelsCheckbox);
        labelsCheckbox.addItemListener(this);

        panel.add(labelsCheckbox);
        add("East", panel);
        addPopupMenu();
        GUI.scale(this);
        pack();
        Dimension size = getSize();
        if (size.width>270)
            setSize(size.width-40, size.height);
        list.remove(0);
        Point loc = Prefs.getLocation(LOC_KEY);
        if (loc!=null)
            setLocation(loc);
        else
            GUI.centerOnImageJScreen(this);

        setVisible(true);
    }

    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        b.addKeyListener(IJ.getInstance());
        b.addMouseListener(this);
        if (label.equals(moreButtonLabel)) moreButton = b;
        panel.add(b);
    }

    void addPopupMenu() {
        pm = new PopupMenu();
        GUI.scalePopupMenu(pm);
        addPopupItem("Open...");
        addPopupItem("Save...");
        add(pm);
    }

    void addPopupItem(String s) {
        MenuItem mi=new MenuItem(s);
        mi.addActionListener(this);
        pm.add(mi);
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
    public void valueChanged(ListSelectionEvent e) {

    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    public Iterator<Roi> iterator() {
        return null;
    }
}
