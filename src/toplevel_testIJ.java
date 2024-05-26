import ij.*;
import CalciumImaging.*;
import DacksLab.*;
import ij.plugin.frame.RoiManager;

public class toplevel_testIJ {

    public static void main(String[] args){
        String cellRois = "D:/Jolt/resources/f08_LSD/CellMap_rois.zip";

        new ImageJ();
        ImagePlus imp = IJ.openImage("D:/Jolt/resources/f08_LSD/CellMap.tif");

        if (imp != null) {
            imp.show();
        }

        RoiManager rm = RoiManager.getInstance();

        if (rm == null) {
            rm = new RoiManager();
        }


        IJ.run("ROI Manager...");
        rm.runCommand("Open", cellRois);


        GroupROIs gR = new GroupROIs();

        gR.setup(null,imp);
        gR.run(null);


    }

}
