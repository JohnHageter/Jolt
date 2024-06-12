package Annotation;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

public class SelectMultiple implements PlugIn {


    @Override
    public void run(String arg){
        String appendName;

        SelectMultiple ins = new SelectMultiple();
        appendName = SelectMultiple.inputParameters();
        SelectMultiple.selectROIs(appendName);
    }

    private static String inputParameters(){
        GenericDialog gd = new GenericDialog("Select and append");
        gd.addMessage("Name group to append to ROIs");
        gd.addStringField("Label: ", "195pos");
        gd.showDialog();

        String appendName;
        appendName = gd.getNextString();

        return appendName;
    }

    private static void selectROIs(String appendName) {
        ImagePlus im = IJ.getImage();
        RoiManager rm = RoiManager.getInstance();
        Roi[] cellRois;

        if (im == null) {
            IJ.error("No images open.");
            return;
        }
        if (rm == null) {
            IJ.error("ROI manager not found");
            return;
        } else {
            cellRois = rm.getRoisAsArray();
            if (cellRois.length == 0) {
                IJ.error("ROI manager is empty.");
                return;
            }
        }

        // Have Image and cellROIs as array. Multi-Select ROIs
        IJ.setTool("Multi-Point");
        (new WaitForUserDialog("Group ROIs", "Select " + appendName + "\nThen click OK.")).show();

        FloatPolygon points = ((PolygonRoi) im.getRoi()).getFloatPolygon();
        if (points == null) {
            IJ.error("No points selected.");
            return;
        }

        double[][] distances = new double[cellRois.length][points.npoints];
        for (int cell=0; cell<cellRois.length; cell++){
            for (int point=0; point<points.npoints; point++){
                distances[cell][point] = calcDistance(points.xpoints[point], points.ypoints[point],
                        cellRois[cell].getBounds().getCenterX(), cellRois[cell].getBounds().getCenterY());
            }
        }

        for (int point = 0; point < points.npoints; point++) {
            double minDistance = Double.MAX_VALUE;
            int minCellIndex = -1;
            for (int cell = 0; cell < cellRois.length; cell++) {
                if (distances[cell][point] < minDistance) {
                    minDistance = distances[cell][point];
                    minCellIndex = cell;
                }
            }

            if (minCellIndex != -1) {
                rm.rename(rm.getRoiIndex(cellRois[minCellIndex]),
                        cellRois[minCellIndex].getName() + "_" + appendName);
                rm.select(rm.getRoiIndex(cellRois[minCellIndex]));
                rm.runCommand("Set Color", "magenta");
                rm.runCommand("Set Line Width", "2");
            } else {
                IJ.log("No cell ROI found.");
            }
        }

        for (Roi cell : cellRois){
            if (!cell.getName().contains(appendName)) {
                rm.rename(rm.getIndex(cell.getName()), cell.getName() + "_NULL");
            }
        }

    }


    public static double calcDistance(double x1, double y1, double x2, double y2){
            double dx = x2 - x1;
            double dy = y2 - y1;
            double squaredDistance = dx * dx + dy * dy;
            return Math.sqrt(squaredDistance);
    }

}
