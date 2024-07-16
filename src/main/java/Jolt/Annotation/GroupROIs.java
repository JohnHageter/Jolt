package Jolt.Annotation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.TextField;
import java.util.ArrayList;
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
    ArrayList<Roi> groupingRois = new ArrayList<>();

    private static final String PREF_NUM_GROUPS = "NumGroups";
    private int nGroups = 1;

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
        boolean params;
        try {
            params = inputParameters();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }

        this.groupingRM = new RoiManager(false);
        if (params && this.gd.wasOKed()) {
            int cellIndex = 0;
            Roi[] cellROIs = this.rm.getRoisAsArray();

            for (Roi cell : cellROIs){
                if(!cell.getName().contains("Cell")){
                    this.rm.select(this.rm.getIndex(cell.getName()));
                    this.rm.runCommand("Rename", "Cell" + cellIndex);
                    cellIndex++;
                }
            }

            for (String group : this.groupNames) {
                if(!group.isEmpty()){
                    applyGroup(group);
                }
            }

            //rename non slected cells to *_NULL
            for (Roi cell : cellROIs) {
                boolean named = false;
                for (Roi group : this.groupingRois) {
                    int cellCenterX = (int) cell.getBounds().getCenterX();
                    int cellCenterY = (int) cell.getBounds().getCenterY();
                    if (group.contains(cellCenterX, cellCenterY)){
                        if(named) {
                            IJ.log("Warning: " + cell.getName() + " was found in multiple groups");
                        }
                        named = true;
                    }
                }

                if(!named) {
                    this.rm.select(this.rm.getIndex(cell.getName()));
                    this.rm.runCommand("Rename", cell.getName() + "_NULL");
                }
            }

        }

        FileInfo iminfo = this.imp.getFileInfo();
        this.rm.runCommand("Save...", iminfo.directory + "/ROIset_annotated.zip");
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
        this.groupingRois.add(groupingROI);

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
        boolean canceled = false;

        while (!okPressed && !canceled) {
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
            if(this.gd.wasCanceled()){
                canceled = true;
                return canceled;
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
