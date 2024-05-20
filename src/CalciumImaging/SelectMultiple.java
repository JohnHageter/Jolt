package CalciumImaging;


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

//        Calibration cal = im.getCalibration();
//        String unit = cal.getUnit();

        //points = selected points ,,, cellROIs = cell mask in ROI manager
        IJ.log("Number of cell Rois: " + cellRois.length);
        IJ.log("Number of points selected: " + points.npoints);

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
            // Log the minimum distance and the corresponding cell ROI
            //IJ.log("Minimum distance for point " + (point + 1) + ": " + minDistance);
            if (minCellIndex != -1) {
                //IJ.log("Cell ROI with minimum distance: " + cellRois[minCellIndex].getName());
                //Roi foundCell = (Roi) cellRois[minCellIndex].clone();
                rm.rename(rm.getRoiIndex(cellRois[minCellIndex]),
                        cellRois[minCellIndex].getName() + "_" + appendName);
                rm.select(rm.getRoiIndex(cellRois[minCellIndex]));
                rm.runCommand("Set Color", "magenta");
                rm.runCommand("Set Line Width", "2");
                //IJ.log("Index " + minCellIndex);
                //rm.select(minCellIndex);
                //rm.runCommand("Delete");
                //foundCell.setName(cellRois[minCellIndex].getName() + "_" + appendName);
                //IJ.log("New ROI name: " + foundCell.getName());
                //rm.addRoi(foundCell);
            } else {
                IJ.log("No cell ROI found.");
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
