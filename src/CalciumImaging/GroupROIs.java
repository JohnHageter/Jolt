package CalciumImaging;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class GroupROIs implements PlugInFilter, DialogListener {
    ImagePlus imp;
    RoiManager rm;
    GenericDialog gd;


    private static final String PREF_GROUP_PARAMETER = "Groups";
    private static final String PREF_GROUP_NAME = "GroupName";
    private static final int DEFAULT_GROUPS = 5;
    private int groups;

    static class CellData {
        public Roi cell;
        public boolean inPoly;
        public int centerX;
        public int centerY;

        public CellData(Roi cell, boolean inPoly, int centerX, int centerY){
            this.cell = cell;
            this.inPoly = inPoly;
            this.centerY = centerY;
            this.centerX = centerX;
        }
    }

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;

        if(RoiManager.getInstance() == null) {
            this.rm = new RoiManager();
        }

        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor ip) {
        boolean params = inputParameters();
    }


    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        return false;
    }

    public boolean inputParameters() {
        gd = new GenericDialog("Group ROIs by selection");
        loadParameters();

        if(gd.wasCanceled()){
            IJ.showMessage("Input parameters required");
        } else {
            gd.addButton("add group", e -> {
                groups++;
                updateDialog();
            });
            gd.addButton("remove group", e -> {
                if (groups > 1) {
                    groups--;
                    updateDialog();
                }
            });

            for (int i = 0; i < groups; i++) {
                gd.addStringField("Group #" + (i + 1) + ": ", "");
            }

            gd.addDialogListener(this);
            gd.showDialog();

            if (!gd.wasCanceled()) {
                saveParameters();
                return true;
            }
        }

        return false;
    }

    private void updateDialog() {
        gd.dispose();
        gd = new GenericDialog("Group ROIs by polygon selection");
        gd.addButton("add group", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                groups++;
                updateDialog();
            }
        });

        gd.addButton("remove group", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (groups > 1) {
                    groups--;
                    updateDialog();
                }
            }
        });

        for (int i = 0; i < groups; i++) {
            gd.addStringField("Group #" + (i + 1) + ": ", "");
        }

        gd.addDialogListener(new DialogListener() {
            @Override
            public boolean dialogItemChanged(GenericDialog genericDialog, AWTEvent awtEvent) {
                return false;
            }
        });
        gd.showDialog();
    }

    private void loadParameters() {
        Preferences pref = Preferences.userNodeForPackage(GroupROIs.class);
        pref.putInt(PREF_GROUP_PARAMETER, groups);
    }

    private void saveParameters() {
        Preferences pref = Preferences.userNodeForPackage(GroupROIs.class);
        pref.putInt(PREF_GROUP_PARAMETER, DEFAULT_GROUPS);

        for (int i = 0; i < DEFAULT_GROUPS; i++) {
            String value = gd.getNextString();
            pref.put(PREF_GROUP_NAME + i, value);
        }
    }

    private String loadGroupName(int index) {
        Preferences pref = Preferences.userNodeForPackage(GroupROIs.class);
        return pref.get(PREF_GROUP_NAME + index, "");
    }

    private ArrayList<String> getNonEmptyStringFields() {
        ArrayList<String> nonEmptyFields = new ArrayList<>();
        for (int i = 0; i < DEFAULT_GROUPS; i++) {
            String field = gd.getNextString();
            if (field != null && !field.trim().isEmpty()) {
                nonEmptyFields.add(field);
            }
        }
        return nonEmptyFields;
    }
}
