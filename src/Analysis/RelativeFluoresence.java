package Analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.prefs.Preferences;


public class RelativeFluoresence implements PlugInFilter {
    private ImagePlus imp;
    private RoiManager rm;
    private ArrayList<Roi> cells;

    private int numFrames;

    private static final String PREF_BASELINE = "Baseline";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_ITERATIONS = "Iteration";
    private static final String PREF_METHOD = "Method";
    private String baseline = "";
    private String stimulus = "";
    private int iterations = 2;
    private int method = 0;


    @Override
    public int setup(String s, ImagePlus imagePlus) {
        this.imp = IJ.getImage();
        this.rm = RoiManager.getInstance();



        return DOES_STACKS;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        boolean params = inputParameters();

    }

    private boolean inputParameters() {
        String[] method = new String[]{">3σ", "none"};

        loadParameters();
        GenericDialog gd = new GenericDialog("Relative Fluorescence analysis");
        gd.addMessage("All parameters are optional. Leave blank if excluded");
        gd.addNumericField("Iterations:", 2);
        gd.addMessage("Input timeframe as range from beginning frame to end frame separated with '-' (ex. 1-60)");
        gd.addStringField("Baseline timeframe", "1-60");
        gd.addMessage("Input stimulus as frames points with a duration to search separated by a comma (ex. 60:3,120:3)\nThis will average relative intensity change from frame 60-63 and 120-123");
        gd.addStringField("Stimulus time(s)", "60,120");
        gd.addStringField("Stimulus Names", "OFF,ON");
        gd.addChoice("Response call method", method, method[0]);
        gd.showDialog();


        return true;
    }

    private void loadParameters() {
        Preferences prefs = Preferences.userNodeForPackage(RelativeFluoresence.class);


    }
}
