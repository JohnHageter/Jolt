package Analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import Utility.CellData;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class RelativeFluorescence implements PlugInFilter {
    private ImagePlus imp;
    private RoiManager rm;
    private int numFrames;

    private static final String PREF_BASELINE = "Baseline";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_STIMULUS_NAMES = "Stimulus Names";
    private static final String PREF_ITERATIONS = "Iteration";
    private static final String PREF_METHOD = "Method";
    private static final String PREF_SHOW_PLOT = "Show Plot";

    private String baselineInput = "1-60";
    private String stimulusInput = "60-63,120-123,180-183";
    private String stimNamesInput = "Off,On,Off";
    private int iterations = 2;
    private int method = 0;
    private Boolean plot = true;
    private Plot groupPlot;
    private ImagePlus plotImage;

    private ArrayList<String> stimPoints;
    private ArrayList<String> stimPointNames;
    private int beginBaseline;
    private int endBaseline;

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        this.imp = imagePlus;
        this.rm = RoiManager.getInstance();
        return DOES_STACKS;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        try {
            if (inputParameters()) {
                IJ.log("We made it here so far");
                getAnalysisParameters();
                measureMultipleCells();
            }
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private void measureMultipleCells() {
        Roi[] rois = this.rm.getRoisAsArray();
        ArrayList<CellData> cellRois = new ArrayList<>();
        this.groupPlot = new Plot("Relative Fluorescence", "Slice", "Delta F/F");

        for (Roi roi : rois) {
            cellRois.add(new CellData(roi));
        }

        for (CellData cell : cellRois) {
            double sum = 0;
            int count = 0;

            for (int slice = this.beginBaseline; slice <= this.endBaseline; slice++) {
                this.imp.setSlice(slice);
                ImageProcessor ip = this.imp.getProcessor();
                ip.setRoi(cell.roi);
                ImageStatistics stat = ip.getStatistics();

                sum += stat.mean * stat.pixelCount;
                count += stat.pixelCount;
            }

            double fnot = sum / count;
            cell.setFnot(fnot);
        }


        double maxDf = 0;
        double minDf = 0;

        for (CellData cell : cellRois) {
            double[] df = new double[this.imp.getStackSize()];

            for (int slice = 1; slice <= this.imp.getStackSize(); slice++) {
                this.imp.setSlice(slice);
                ImageProcessor ip = this.imp.getProcessor();
                ip.setRoi(cell.roi);
                ImageStatistics stats = ip.getStatistics();

                df[slice - 1] = (stats.mean - cell.fnot) / cell.fnot;

                if (df[slice - 1] > maxDf) {
                    maxDf = df[slice - 1];
                }
                if (df[slice - 1] < minDf) {
                    minDf = df[slice - 1];
                }
            }

            if (plot){
                double[] slices = seq(1, this.imp.getNSlices(), 1);
                cell.setDf(df);
                this.groupPlot.addPoints(slices, cell.getDf(), Plot.LINE);
                this.groupPlot.setLineWidth(2);
                this.groupPlot.setColor(randomColor());

            }
        }

        if(plot){
            this.groupPlot.setLimits(1, this.imp.getNSlices(), minDf - (minDf * 0.05), maxDf + (maxDf * 0.2));

            this.plotImage = this.groupPlot.getImagePlus();
            this.plotImage.show();
        }
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
        this.stimPointNames = parseInput(this.stimNamesInput, ",");

        int diff = this.stimPoints.size() - this.stimPointNames.size();
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                this.stimPointNames.add("N/A");
            }
        } else if (diff < 0) {
            for (int i = 1; i <= Math.abs(diff); i++) {
                this.stimPointNames.remove(this.stimPointNames.size() - 1);
            }
            IJ.log("Removed extra names");
        }

        IJ.log("Baseline Begin: " + this.beginBaseline + ", Baseline End: " + this.endBaseline + "\n");

        for (String point : this.stimPoints) {
            IJ.log("Stimulus point: " + point);
        }

        for (String name : this.stimPointNames) {
            IJ.log("Stimulus Name: " + name);
        }
    }

    private boolean inputParameters() throws BackingStoreException {
        String[] method = new String[]{">3σ", "none"};

        loadParameters();
        GenericDialog gd = new GenericDialog("Relative Fluorescence analysis");
        gd.addMessage("All parameters are optional. Leave blank if excluded");
        gd.addNumericField("Iterations:", this.iterations);
        gd.addMessage("Input timeframe as range from beginning frame to end frame separated with '-' (ex. 1-60)");
        gd.addStringField("Baseline timeframe", this.baselineInput);
        gd.addMessage("Input stimulus as frames points with a duration to search separated by a comma (ex. 60-63,120-123)\nThis will average relative intensity change from frame 60-63 and 120-123");
        gd.addStringField("Stimulus time(s)", this.stimulusInput);
        gd.addStringField("Stimulus Names", this.stimNamesInput);
        gd.addChoice("Response call method", method, method[this.method]);
        gd.addCheckbox("Show group plot", this.plot);
        gd.showDialog();

        if (gd.wasOKed()) {
            this.iterations = (int) gd.getNextNumber();
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
        this.iterations = prefs.getInt(PREF_ITERATIONS, this.iterations);
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
        prefs.putInt(PREF_ITERATIONS, this.iterations);
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
}
