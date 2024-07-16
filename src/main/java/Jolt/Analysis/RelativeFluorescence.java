package Jolt.Analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import Jolt.Utility.CellData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class RelativeFluorescence implements PlugIn {
    private ImagePlus imp;
    private RoiManager rm;
    private ArrayList<CellData> cellRois;
    private ArrayList<ImagePlus> iteration;

    private static final String PREF_BASELINE = "Baseline";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_STIMULUS_NAMES = "Stimulus Names";
    private static final String PREF_ITERATIONS = "Iteration";
    private static final String PREF_METHOD = "Method";
    private static final String PREF_SHOW_PLOT = "Show Plot";

    private String baselineInput = "1-60";
    private String stimulusInput = "60-63,120-123,180-183";
    private String stimNamesInput = "Off,On,Off";
    private int nIterations = 2;
    private int method = 0;
    private Boolean plot = true;
    private ImagePlus plotImage;

    private ArrayList<String> stimPoints;
    private int beginBaseline;
    private int endBaseline;

    @Override
    public void run(String s) {
        this.imp = IJ.getImage();
        this.rm = RoiManager.getInstance();

        boolean run;
        try {
            run = inputParameters();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }

        if (run) {
            getAnalysisParameters();
            splitRuns();
            ArrayList<ArrayList<CellData>> returnData = new ArrayList<>();
            for (int i = 0; i < this.nIterations; i++) {
                ImagePlus imp = this.iteration.get(i);
                ArrayList<CellData> output = measureMultipleCells(imp);
                returnData.add(output);
            }
            this.cellRois = averageRunData(returnData);
            outputData();
        }
    }

    private ArrayList<CellData> averageRunData(ArrayList<ArrayList<CellData>> dat) {
        int stackSize = this.imp.getStackSize() / this.nIterations;
        ArrayList<CellData> retDat = new ArrayList<>();

        for (int i = 0; i < dat.get(0).size(); i++) {
            CellData cellData = new CellData(dat.get(0).get(i).roi);
            cellData.setDf(new double[stackSize]);
            retDat.add(cellData);
        }

        for (int cellIndex = 0; cellIndex < retDat.size(); cellIndex++) {
            double[] sumDf = new double[stackSize];

            for (ArrayList<CellData> iterationData : dat) {
                double[] dfValues = iterationData.get(cellIndex).getDf();
                for (int i = 0; i < stackSize; i++) {
                    sumDf[i] += dfValues[i];
                }
            }

            double[] avgDf = new double[stackSize];
            for (int i = 0; i < stackSize; i++) {
                avgDf[i] = sumDf[i] / this.nIterations;
            }

            retDat.get(cellIndex).setDf(avgDf);
        }

        return retDat;
    }

    private void splitRuns() {
        int nSlice = this.imp.getNSlices();
        ArrayList<ImageStack> stacks = new ArrayList<>();
        int subStackNSlices = nSlice / this.nIterations;

        if (nSlice % this.nIterations != 0) {
            IJ.showMessage("Error", "Iteration number not a divisor of the stack size.");
            return;
        }

        for (int i = 0; i < this.nIterations; i++) {
            ImageStack subStack = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
            for (int j = 0; j < subStackNSlices; j++) {
                int slice = (i * subStackNSlices) + j + 1;
                this.imp.setSlice(slice);
                ImageProcessor ip = this.imp.getProcessor().duplicate(); // Ensure we duplicate the processor
                subStack.addSlice(ip);
            }
            stacks.add(subStack);
        }

        this.iteration = new ArrayList<>(this.nIterations);
        for (int i = 0; i < stacks.size(); i++) {
            ImagePlus subImp = new ImagePlus(this.imp.getTitle() + "_" + (i + 1), stacks.get(i));
            this.iteration.add(subImp);
            //subImp.show();
        }
    }

    private void outputData() {
        ResultsTable rt = new ResultsTable();
        FileInfo iminfo = this.imp.getOriginalFileInfo();
        String dir = iminfo.directory;
        dir = getDir(dir);

        int index = 0;
        for (CellData cell : this.cellRois){
            String[] subParts = cell.breakName("_");
            rt.incrementCounter();

            rt.addValue("Directory", iminfo.directory);
            rt.addValue("Name", dir);
            for (String part : subParts) {
                rt.addValue("var_" + index, part);
                index++;
            }

            int nameIdx=0;
            String name = "";
            String sigName = "None";
            for (String stimPoint : this.stimPoints) {
                String[] point = stimPoint.split("-");
                int beginStim = strToInt(point[0]);
                int endStim = strToInt(point[1]);


                if(calcSignificance(cell, beginStim, endStim)) {
                    sigName = name + this.stimNamesInput.split(",")[nameIdx];
                }

                nameIdx++;
            }

            rt.addValue("x", cell.getCenterX());
            rt.addValue("y", cell.getCenterY());
            rt.addValue("Response", sigName);

            for(int i = 0; i < this.imp.getStackSize() / this.nIterations; i++){
                rt.addValue("Slice_" + (i+1), cell.getDf(i));
            }
            index=0;
        }
        rt.show("Results");
        rt.save(iminfo.directory + "/Results.csv");
    }

    private boolean calcSignificance(CellData cell, int beginStim, int endStim) {
        int duration = 0;

        double df = 0;
        for (int i = beginStim; i <= endStim; i++) {
            df += cell.getDf()[i];
            duration++;
        }
        df = df/duration;

        return df > (3 * sd(cell.getDf(), this.beginBaseline, this.endBaseline));

    }

    private double sd(double[] df, int beginBaseline, int endBaseline) {
        double sum = 0.0;
        double mean;
        double variance = 0.0;
        double count = 0.0;

        for (int i = beginBaseline; i<=endBaseline; i++){
            sum += df[i];
            count++;
        }
        mean = sum / count;

        for (int i = beginBaseline; i <= endBaseline; i++){
            variance += Math.pow(df[i] - mean, 2);
        }
        variance = variance / count;

        return Math.sqrt(variance);
    }

    private ArrayList<CellData> measureMultipleCells(ImagePlus imp) {
        Roi[] rois = this.rm.getRoisAsArray();
        ArrayList<CellData> cells = new ArrayList<>();
        Plot iterPlot = new Plot("Relative Fluorescence " + imp.getTitle(), "Slice", "Delta F/F");

        for (Roi roi : rois) {
            cells.add(new CellData(roi));
        }

        for (CellData cell : cells) {
            double sum = 0;
            double count = 0;

            for (int slice = this.beginBaseline; slice <= this.endBaseline; slice++) {
                imp.setSlice(slice);
                ImageProcessor ip = imp.getProcessor();
                ip.setRoi(cell.roi);
                ImageStatistics stat = ip.getStatistics();
                sum += stat.mean;
                count++;
            }
            double fnot = sum / count;
            //IJ.log("" + fnot);
            cell.setFnot(fnot);
        }

        double maxDf = 0;
        double minDf = 0;

        for (CellData cell : cells) {
            double[] df = new double[imp.getStackSize()];

            for (int slice = 1; slice <= imp.getStackSize(); slice++) {
                imp.setSlice(slice);
                ImageProcessor ip = imp.getProcessor();
                ip.setRoi(cell.roi);
                ImageStatistics stats = ip.getStatistics();

                df[slice - 1] = (stats.mean - cell.getFnot()) / cell.getFnot();

                if (df[slice - 1] > maxDf) {
                    maxDf = df[slice - 1];
                }
                if (df[slice - 1] < minDf) {
                    minDf = df[slice - 1];
                }
            }

            if (plot){
                double[] slices = seq(1, imp.getNSlices(), 1);
                cell.setDf(df);
                iterPlot.addPoints(slices, cell.getDf(), Plot.LINE);
                iterPlot.setLineWidth(2);
                iterPlot.setColor(randomColor());
                iterPlot.setLimits(1, imp.getNSlices(), minDf - (minDf * 0.05), maxDf + (maxDf * 0.2));
                plotImage = iterPlot.getImagePlus();
            } else {
                cell.setDf(df);
            }
        }

        if(plotImage != null){
            plotImage.show();
        }

        return cells;
    }

    private void getAnalysisParameters() {
        if (parseInput(this.baselineInput, "-").size() == 2) {
            this.beginBaseline = Integer.parseInt(parseInput(this.baselineInput, "-").get(0));
            this.endBaseline = Integer.parseInt(parseInput(this.baselineInput, "-").get(1));
        } else {
            IJ.log("Invalid baseline input. Only single range allowed");
            return;
        }

        this.stimPoints = parseInput(this.stimulusInput, ",");
        ArrayList<String> stimPointNames = parseInput(this.stimNamesInput, ",");

        int diff = this.stimPoints.size() - stimPointNames.size();
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                stimPointNames.add("N/A");
            }
        } else if (diff < 0) {
            for (int i = 1; i <= Math.abs(diff); i++) {
                stimPointNames.remove(stimPointNames.size() - 1);
            }
            IJ.log("Removed extra names");
        }
    }

    private boolean inputParameters() throws BackingStoreException {
        String[] method = new String[]{">3σ", "none"};

        loadParameters();
        GenericDialog gd = new GenericDialog("Relative Fluorescence analysis");
        gd.addMessage("All parameters are optional. Leave blank if excluded");
        gd.addNumericField("Iterations:", this.nIterations);
        gd.addMessage("Input timeframe as range from beginning frame to end frame separated with '-' (ex. 1-60)");
        gd.addStringField("Baseline timeframe", this.baselineInput);
        gd.addMessage("Input stimulus as frames points with a duration to search separated by a comma (ex. 60-63,120-123)\nThis will average relative intensity change from frame 60-63 and 120-123");
        gd.addStringField("Stimulus time(s)", this.stimulusInput);
        gd.addStringField("Stimulus Names", this.stimNamesInput);
        gd.addChoice("Response call method", method, method[this.method]);
        gd.addCheckbox("Show group plot", this.plot);
        gd.showDialog();

        if (gd.wasOKed()) {
            this.nIterations = (int) gd.getNextNumber();
            this.baselineInput = gd.getNextString();
            this.stimulusInput = gd.getNextString();
            this.stimNamesInput = gd.getNextString();
            this.method = gd.getNextChoiceIndex();
            this.plot = gd.getNextBoolean();
            saveParameters();
            return true;
        }

        return false;
    }

    private ArrayList<String> parseInput(String input, String delimiter) {
        return new ArrayList<>(Arrays.asList(input.split(delimiter)));
    }

    private void loadParameters() {
        Preferences prefs = Preferences.userNodeForPackage(RelativeFluorescence.class);
        this.nIterations = prefs.getInt(PREF_ITERATIONS, this.nIterations);
        this.baselineInput = prefs.get(PREF_BASELINE, this.baselineInput);
        this.stimulusInput = prefs.get(PREF_STIMULUS, this.stimulusInput);
        this.stimNamesInput = prefs.get(PREF_STIMULUS_NAMES, this.stimNamesInput);
        this.method = prefs.getInt(PREF_METHOD, this.method);
        this.plot = prefs.getBoolean(PREF_SHOW_PLOT, this.plot);
    }

    private void saveParameters() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(RelativeFluorescence.class);
        prefs.clear();
        prefs.put(PREF_BASELINE, this.baselineInput);
        prefs.put(PREF_STIMULUS, this.stimulusInput);
        prefs.put(PREF_STIMULUS_NAMES, this.stimNamesInput);
        prefs.putInt(PREF_ITERATIONS, this.nIterations);
        prefs.putInt(PREF_METHOD, this.method);
        prefs.putBoolean(PREF_SHOW_PLOT, this.plot);
    }

    private double[] seq(int begin, int end, int by) {
        int size = (end - begin) / by + 1;
        double[] sequence = new double[size];
        int index = 0;
        for (int i = begin; i <= end; i += by) {
            sequence[index++] = i;
        }
        return sequence;
    }

    private Color randomColor() {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        return new Color(red, green, blue);
    }

    private String getDir(String fullPath){
        IJ.log(fullPath);
        if(fullPath == null || fullPath.isEmpty()){
            IJ.log("Directory empty.");
            return "";
        }

        if(fullPath.endsWith("/") || fullPath.endsWith("\\")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }

        int lastSlash = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));

        if(lastSlash != -1){
            return fullPath.substring(lastSlash + 1);
        }else {
            return fullPath;
        }
    }

    private Integer strToInt(String str){
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            IJ.showMessage("Error", "Input must be numeric. Error in: " + str);
            return null;
        }
    }
}
