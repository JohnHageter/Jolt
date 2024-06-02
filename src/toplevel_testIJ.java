import Jolt.CellManager;
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
        CellManager cm = new CellManager();


    }

}
