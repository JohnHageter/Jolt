package DacksLab;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.ZProjector;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.CompositeImage;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Binned_Intensity implements PlugInFilter {
    ImagePlus imp;
    Roi roi;

    int process;
    boolean fixed;
    int bins;
    int step;
    boolean showPlot;

    ResultsTable rt;

    static class BinData {
        public Roi bin;
        public boolean inPoly;
        public int numCol;
        public int numRow;

        public BinData(Roi bin, boolean inPoly, int numCol, int numRow){
            this.bin = bin;
            this.inPoly = inPoly;
            this.numCol = numCol;
            this.numRow = numRow;
        }
    }

    public int setup(String arg, ImagePlus imp){
        if (!this.getParameters()) return 0;
        this.imp = applyProcess(imp);

        if(this.imp == null) {
            IJ.showMessage("Need to have image open");
            return 0;
        }

        return DOES_ALL;
    }

    public void run(ImageProcessor ip){
        IJ.setTool("polygon");
        (new WaitForUserDialog("Binned intensity distribution", "Select ROI to bin")).show();
        this.roi = this.imp.getRoi();

        if(this.roi == null || !this.roi.isArea()){
            IJ.showMessage("Error", "Polygon ROI needed");
            return;
        }

        ArrayList<BinData> bins = null;
        if(!this.fixed){
            bins = gridROI();
        } else {
            bins = fixedROI();
        }

        this.imp.updateAndDraw();

        this.rt = new ResultsTable();
        for (int c=1; c<=this.imp.getNChannels(); c++) {
            this.imp.setC(c);
            double maxI = getMaxIntensity(bins);
            for (BinData bin : bins) {
                this.rt.incrementCounter();
                this.rt.addValue("Bin Column", bin.numCol);
                this.rt.addValue("Bin Row", bin.numRow);
                this.rt.addValue("X.adj", bin.bin.getBounds().getMinX() - this.roi.getBounds().getMinX());
                this.rt.addValue("Y.adj", bin.bin.getBounds().getMinY() - this.roi.getBounds().getMinY());
                double intensity = getIntensityWithinBin(bin.bin);
                this.rt.addValue("Channel", c);
                if(bin.inPoly) {
                    this.rt.addValue("Norm.Intensity", (intensity / maxI));
                } else {
                    this.rt.addValue("Norm.Intensity", "");
                }
            }

        }

        this.rt.show("Normalized Intensity");



    }

    private boolean getParameters(){
        String[] preProcess = new String[]{"Max Projection", "Sum Projection", "Median Projection", "Average Projection", "No Projection (not implemented)"};

        Map<String, Integer> methodMap = new HashMap<>();
        methodMap.put("Max Projection", ZProjector.MAX_METHOD);
        methodMap.put("Sum Projection", ZProjector.SUM_METHOD);
        methodMap.put("Median Projection", ZProjector.MEDIAN_METHOD);
        methodMap.put("Average Projection", ZProjector.AVG_METHOD);
        methodMap.put("No Projection (not implemented)", -1);

        GenericDialog var3 = new GenericDialog("Binned intensity distribution");
        var3.addMessage("Select region for intensity distribution");
        var3.addChoice("Projection Method", preProcess, preProcess[0]);
        var3.addCheckbox("Floating binwidth?", true);
        var3.addNumericField("Binwidth", 10);
        //var3.addNumericField("Step size (don't trust this)", this.step);
        //var3.addCheckbox("show intensity distribution plot?", true);
        var3.showDialog();

        if (var3.wasCanceled()){
            return false;
        } else {
            String selectedMethod = var3.getNextChoice();
            this.process = methodMap.get(selectedMethod);
            this.fixed = var3.getNextBoolean();
            this.bins = (int) var3.getNextNumber();
            //this.showPlot = var3.getNextBoolean();
            IJ.log("process: " + selectedMethod + "  " + "Binwidth: " + this.bins + "  " + "Show Plot: " + this.showPlot);


            return true;
        }

    }

    private ArrayList<BinData> gridROI() {
        int roiX = (int) this.roi.getBounds().getX();
        int roiY = (int) this.roi.getBounds().getY();
        int roiWidth = (int) this.roi.getBounds().getWidth();
        int roiHeight = (int) this.roi.getBounds().getHeight();

        ArrayList<BinData> bins = new ArrayList<>();

        Overlay ol = new Overlay();
        for (int i = 0; i < roiWidth / this.bins; i++) {
            for (int j = 0; j < roiHeight / this.bins; j++) {
                int binX = roiX + i * this.bins;
                int binY = roiY + j * this.bins;

                Roi bin = new Roi(binX, binY, this.bins, this.bins);
                BinData binData = new BinData(bin, false, i, j);

                if (this.roi.contains((int) bin.getBounds().getCenterX(), (int) bin.getBounds().getCenterY())) {
                    binData.inPoly = true;
                    bin.setStrokeColor(Color.GREEN);
                    ol.add(bin);
                    this.imp.setOverlay(ol);
                } else {
                    bin.setStrokeColor(Color.RED);
                    ol.add(bin);
                    this.imp.setOverlay(ol);
                }

                bins.add(binData);
            }
        }
        this.imp.setOverlay(ol);

        return bins;
    }

    private ArrayList<BinData> fixedROI() {
        int roiX = (int) this.roi.getBounds().getX();
        int roiY = (int) this.roi.getBounds().getY();
        int roiWidth = (int) this.roi.getBounds().getWidth();
        int roiHeight = (int) this.roi.getBounds().getHeight();

        int binWidth = (int) Math.floor((double) roiWidth / this.bins);
        int binHeight = (int) Math.floor((double) roiHeight / this.bins);

        int numCols = roiWidth / binWidth;
        int numRows = roiHeight / binHeight;

        IJ.log("Width: " + numCols + " Height: " + numRows);

        ArrayList<BinData> bins = new ArrayList<>();
        Overlay ol = new Overlay();

        for (int i = 0; i < numCols; i++) {
            for (int j = 0; j < numRows; j++) {
                int x = roiX + i * binWidth; //topleft ROI plus the number bin in column were on
                int y = roiY + j * binHeight; //topleft ROI plus the number bin in row were on

                Roi subROI = new Roi(x, y, binWidth, binHeight);
                BinData binData = new BinData(subROI, false, i+1, Math.abs(j -this.bins));

                if (this.roi.contains((int) subROI.getBounds().getCenterX(), (int) subROI.getBounds().getCenterY())) {
                    binData.inPoly = true;
                    subROI.setStrokeColor(Color.GREEN);
                } else {
                    subROI.setStrokeColor(Color.RED);
                }
                ol.add(subROI);
                this.imp.setOverlay(ol);

                bins.add(binData);
            }
        }

        this.imp.setOverlay(ol);
        return bins;
    }

    private double getIntensityWithinBin(Roi bin) {
        this.imp.setRoi(bin);
        ImageProcessor ip = this.imp.getProcessor();
        return ip.getStatistics().mean;
    }

    private double getMaxIntensity(ArrayList<BinData> bins) {
        double maxI = Double.MIN_VALUE;

        for (BinData bin : bins) {
            // Get the intensity within the current bin
            double intensity = getIntensityWithinBin(bin.bin);

            // Update max intensity and corresponding bin if necessary
            if (intensity > maxI) {
                maxI = intensity;
            }
        }

        return maxI;
    }


    private ImagePlus applyProcess(ImagePlus imp) {
        int channels = imp.getNChannels();

        if (channels == 1) {
            ZProjector projector = new ZProjector(imp);
            projector.setMethod(this.process);
            projector.doProjection();
            CompositeImage projImage = new CompositeImage(projector.getProjection(), CompositeImage.COLOR);

            projImage.show();
            return projImage;
        } else if (channels > 1) {
            ZProjector projector = new ZProjector(imp);
            projector.setMethod(this.process);
            projector.setStartSlice(1);
            projector.setStopSlice(imp.getStackSize()/channels);

            projector.doHyperStackProjection(true);

            ImagePlus projImage = projector.getProjection();

            projImage.show();
            return projImage;
        }
        return null;
    }
}