package CalciumImaging;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.util.prefs.Preferences;

public class GroupROIs implements PlugInFilter, DialogListener {
    ImagePlus imp;
    RoiManager rm;
    GenericDialog gd;

    private static final String PREF_GROUP_PARAMETER = "Groups";
    private static final int DEFAULT_GROUPS = 5;
    private int groups;

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        this.rm = new RoiManager();

        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor ip) {
        this.imp.setRoi(new Roi(20,20,20,20));
        this.imp.updateAndDraw();
    }

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


    public static void main(String[] args) {
        ImageJ.main(null);
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        return false;
    }

    public boolean inputParameters() {
        gd = new GenericDialog("Group ROIs by selection");
        loadParameters();

//        if(gd.wasCanceled()){
//            IJ.showMessage("Input parameters required");
//        } else {
//            gd.addButton("add group", new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    currentGroups++;
//                    updateDialog();
//                }
//            });
//            gd.addButton("remove group", new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    if (currentGroups > 1) {
//                        currentGroups--;
//                        updateDialog();
//                    }
//                }
//            });
//        }

        return false;
    }

    private void loadParameters() {
        Preferences pref = Preferences.userNodeForPackage(GroupROIs.class);
        pref.putInt(PREF_GROUP_PARAMETER, groups);
    }
}
