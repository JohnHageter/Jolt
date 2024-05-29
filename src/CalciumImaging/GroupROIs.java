package CalciumImaging;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import java.awt.Color;
import java.util.Random;

public class GroupROIs implements PlugInFilter, DialogListener {
    ImagePlus imp;
    GenericDialog gd;
    RoiManager rm;
    RoiManager groupingRM;
    RoiManager compositeROIs = new RoiManager(false);

    private static final String PREF_GROUP_NAME = "GroupName";
    private static final String DEFAULT_NAME = "";
    ArrayList<String> groupNames = new ArrayList<>();

    private static final String PREF_NUM_GROUPS = "NumGroups";
    private int nGroups = 1;

    static class CellData {
        public Roi cell;
        public boolean inPoly;
        public int centerX;
        public int centerY;

        public CellData(Roi cell, boolean inPoly, int centerX, int centerY) {
            this.cell = cell;
            this.inPoly = inPoly;
            this.centerY = centerY;
            this.centerX = centerX;
        }
    }

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;

        if (RoiManager.getInstance() == null) {
            this.rm = new RoiManager();
        } else {
            this.rm = RoiManager.getInstance();
        }

        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor ip) {
        boolean params = false;
        try {
            params = inputParameters();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }

        this.groupingRM = new RoiManager(false);
        if (params && this.gd.wasOKed()) {
            for (String group : this.groupNames) {
                if(group.isEmpty()){
                } else{
                    applyGroup(group);
                }
            }
            Roi[] cellROIs = this.rm.getRoisAsArray();

            for (Roi cell : cellROIs) {
                boolean nameFound = false;
                for (String name : this.groupNames) {
                    if (cell.getName().contains(name)) {
                        nameFound = true;
                        break;
                    }
                }
                if (!nameFound) {
                    String newName = cell.getName() + "_NULL";
                    int index = this.rm.getIndex(cell.getName());
                    if (index != -1) {
                        this.rm.select(index);
                        this.rm.runCommand("Rename", newName);
                        System.out.println("Renamed ROI: " + cell.getName() + " to " + newName);
                    } else {
                        System.out.println("ROI not found in manager: " + cell.getName());
                    }
                }
            }

        }
    }

    private void applyGroup(String group) {
        Roi[] cellROIs = this.rm.getRoisAsArray();
        IJ.setTool("polygon");
        this.compositeROIs.runCommand("Show All");

        for (Roi cell : cellROIs) {
            this.compositeROIs.addRoi(cell);
        }

        (new WaitForUserDialog("Group ROIs", "Select " + group + " region")).show();
        Roi groupingROI = this.imp.getRoi();
        if (groupingROI == null || !groupingROI.isArea()) {
            IJ.showMessage("Error", "Polygonal ROI needed");
            return;
        }

        groupingROI.setStrokeColor(colorRandomizer());
        groupingROI.setStrokeWidth(2);
        this.groupingRM.addRoi(groupingROI);
        this.compositeROIs.addRoi(groupingROI);
        this.imp.updateAndDraw();

        for (Roi cell : cellROIs) {
            if (groupingROI.contains((int) cell.getBounds().getCenterX(), (int) cell.getBounds().getCenterY())) {
                this.rm.select(this.rm.getIndex(cell.getName()));
                this.rm.runCommand("Rename", cell.getName() + "_" + group);
            }
        }
    }

    public boolean inputParameters() throws BackingStoreException {
        loadParameters();
        return createDialog();
    }

    private boolean createDialog() throws BackingStoreException {
        boolean okPressed = false;

        while (!okPressed) {
            this.gd = new GenericDialog("Group ROIs by selection");
            this.gd.addButton("Add group", e -> {
                this.nGroups++;
                try {
                    updateDialog();
                } catch (BackingStoreException ex) {
                    throw new RuntimeException(ex);
                }
            });
            this.gd.addButton("Remove group", e -> {
                if (this.nGroups > 1) {
                    this.nGroups--;
                    try {
                        updateDialog();
                    } catch (BackingStoreException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            for (int i = 0; i < this.nGroups; i++) {
                String groupName = (i < this.groupNames.size()) ? this.groupNames.get(i) : DEFAULT_NAME;
                this.gd.addStringField("Group #" + (i + 1) + ": ", groupName);
            }

            this.gd.addDialogListener(this);
            this.gd.showDialog();

            if (this.gd.wasOKed()) {
                updateGroupNamesFromDialog();
                saveParameters();
                okPressed = true;
            }
        }

        return okPressed;
    }

    private void updateDialog() throws BackingStoreException {
        updateGroupNamesFromDialog();
        saveParameters();
        this.gd.dispose();
    }

    private void loadParameters() {
        Preferences prefs = Preferences.userNodeForPackage(GroupROIs.class);
        this.nGroups = prefs.getInt(PREF_NUM_GROUPS, this.nGroups);
        this.groupNames.clear();
        for (int i = 0; i < this.nGroups; i++) {
            this.groupNames.add(prefs.get(PREF_GROUP_NAME + i, DEFAULT_NAME));
        }
    }

    private void saveParameters() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(GroupROIs.class);
        prefs.clear();
        prefs.putInt(PREF_NUM_GROUPS, this.nGroups);

        for (int i = 0; i < this.groupNames.size(); i++) {
            if (!Objects.equals(this.groupNames.get(i), "")) {
                prefs.put(PREF_GROUP_NAME + i, this.groupNames.get(i));
            }
        }
    }

    private void updateGroupNamesFromDialog() {
        this.groupNames.clear();
        Vector<?> stringFieldsVector = this.gd.getStringFields();

        for (Object field : stringFieldsVector) {
            if (field instanceof TextField) {
                TextField textField = (TextField) field;
                String text = textField.getText().trim();
                this.groupNames.add(text.isEmpty() ? DEFAULT_NAME : text);
            }
        }

        while (this.groupNames.size() < this.nGroups) {
            this.groupNames.add(DEFAULT_NAME);
        }
    }

    private Color colorRandomizer() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);

        return new Color(r, g, b);
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        return true;
    }
}
