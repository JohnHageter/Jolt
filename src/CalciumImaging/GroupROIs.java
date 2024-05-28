package CalciumImaging;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.prefs.Preferences;

public class GroupROIs implements PlugInFilter, DialogListener {
    ImagePlus imp;
    RoiManager rm;
    GenericDialog gd;

    private static final String PREF_GROUP_NAME = "GroupName";
    private static final String DEFAULT_NAME = "";
    ArrayList<String> groupNames = new ArrayList<>();

    private static final String PREF_NUM_GROUPS = "NumGroups";
    public static final int DEFAULT_GROUPS = 5;
    private int nGroups;

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
        }

        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor ip) {
        boolean params = inputParameters();

        if(params && this.gd.wasOKed()){
            for (String group : groupNames){
                IJ.log("Group: " + group);
            }
        }
    }

    public boolean inputParameters() {
        loadParameters();
        createDialog();

        if (this.gd.wasOKed()) {
            updateGroupNamesFromDialog();
            saveParameters();
            return true;
        }

        return false;
    }

    private void createDialog() {
        gd = new GenericDialog("Group ROIs by selection");

        gd.addButton("add group", e -> {
            nGroups++;
            updateDialog();
        });

        gd.addButton("remove group", e -> {
            if (nGroups > 1) {
                nGroups--;
                updateDialog();
            }
        });

        for (int i = 0; i < nGroups; i++) {
            String groupName = (i < groupNames.size()) ? groupNames.get(i) : DEFAULT_NAME;
            gd.addStringField("Group #" + (i + 1) + ": ", groupName);
        }

        gd.addDialogListener(this);
        gd.showDialog();
    }

    private void updateDialog() {
        updateGroupNamesFromDialog();
        gd.dispose();
        createDialog();
    }

    private void loadParameters() {
        Preferences prefs = Preferences.userNodeForPackage(GroupROIs.class);
        nGroups = prefs.getInt(PREF_NUM_GROUPS, DEFAULT_GROUPS);
        groupNames.clear();

        for (int i = 0; i < nGroups; i++) {
            groupNames.add(prefs.get(PREF_GROUP_NAME + i, DEFAULT_NAME));
        }
    }

    private void saveParameters() {
        Preferences prefs = Preferences.userNodeForPackage(GroupROIs.class);
        prefs.putInt(PREF_NUM_GROUPS, nGroups);

        for (int i = 0; i < groupNames.size(); i++) {
            prefs.put(PREF_GROUP_NAME + i, groupNames.get(i));
        }
    }

    private void updateGroupNamesFromDialog() {
        groupNames.clear();
        List<?> stringFields = gd.getStringFields();

        for (Object field : stringFields) {
            TextField textField = (TextField) field;
            String text = textField.getText().trim();

            if (!text.isEmpty()) {
                groupNames.add(text);
            }
        }
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        return true;
    }
}
