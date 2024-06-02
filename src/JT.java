import ij.*;
import CalciumImaging.*;
import ij.plugin.frame.RoiManager;

import java.io.File;

public class JT {

    public static void main(String[] args){
        String cellRois = "resources/CellMap_rois.zip";
        String map = "resources/CellMap.tif";
        File cellRoiFile = new File(cellRois);
        File mapFile = new File(map);

        new ImageJ();
<<<<<<< HEAD
        //ImagePlus imp = IJ.openImage(mapFile.getAbsolutePath());

//        if (imp != null) {
//            imp.show();
//        }

        //RoiManager rm = RoiManager.getInstance();

        //if (rm == null) {
        //    rm = new RoiManager();
        //}

        CellManager cM = new CellManager();

        cM.run("");
=======
        CellManager cm = new CellManager();
>>>>>>> 7074810e5b594ac7286e3b714e4ccdf2b2339009


    }

}
