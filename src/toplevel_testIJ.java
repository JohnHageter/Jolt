import ij.*;
import CalciumImaging.*;
import DacksLab.*;
import ij.plugin.frame.RoiManager;

import java.io.File;

public class toplevel_testIJ {

    public static void main(String[] args){
        String cellRois = "resources/CellMap_rois.zip";
        String map = "resources/CellMap.tif";
        File cellRoiFile = new File(cellRois);
        File mapFile = new File(map);

        new ImageJ();
        ImagePlus imp = IJ.openImage(mapFile.getAbsolutePath());

        if (imp != null) {
            imp.show();
        }

        RoiManager rm = RoiManager.getInstance();

        if (rm == null) {
            rm = new RoiManager();
        }


        IJ.run("ROI Manager...");
        rm.runCommand("Open", cellRoiFile.getAbsolutePath());


        GroupROIs gR = new GroupROIs();

        gR.setup(null,imp);
        gR.run(null);


    }

}
