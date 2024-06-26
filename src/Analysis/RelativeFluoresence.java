package Analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class RelativeFluoresence implements PlugInFilter {
    private ImagePlus imp;
    private RoiManager rm;
    private ArrayList<Roi> cells;

    private int numFrames;

    private static final String PREF_BASELINE = "Baseline";
    private static final String PREF_STIMULUS = "Stimulus";
    private static final String PREF_STIMULUS_NAMES = "Stimulus Names";
    private static final String PREF_ITERATIONS = "Iteration";
    private static final String PREF_METHOD = "Method";
    private String baseline = "1-60";
    private String stimulus = "60:3,120:3,180:3";
    private String stimNames = "Off,On";
    private int iterations = 2;
    private int method = 0;

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        this.imp = imagePlus;
        this.rm = RoiManager.getInstance();

        return DOES_STACKS;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        boolean params = false;
        try {
            params = inputParameters();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }

        if(params){
            IJ.log("We made it here so far");
        }
    }

    private boolean inputParameters() throws BackingStoreException {
        String[] method = new String[]{">3σ", "none"};

        loadParameters();
        GenericDialog gd = new GenericDialog("Relative Fluorescence analysis");
        gd.addMessage("All parameters are optional. Leave blank if excluded");
        gd.addNumericField("Iterations:", this.iterations);
        gd.addMessage("Input timeframe as range from beginning frame to end frame separated with '-' (ex. 1-60)");
        gd.addStringField("Baseline timeframe", this.baseline);
        gd.addMessage("Input stimulus as frames points with a duration to search separated by a comma (ex. 60:3,120:3)\nThis will average relative intensity change from frame 60-63 and 120-123");
        gd.addStringField("Stimulus time(s)", this.stimulus);
        gd.addStringField("Stimulus Names", this.stimNames);
        gd.addChoice("Response call method", method, method[this.method]);
        gd.showDialog();

        if(gd.wasOKed()){
            this.iterations = (int) gd.getNextNumber();
            this.baseline = gd.getNextString();
            this.stimulus = gd.getNextString();
            this.stimNames = gd.getNextString();
            this.method = gd.getNextChoiceIndex();
            saveParameters();
            return true;
        }

        return false;
    }

    private void loadParameters() {
        Preferences prefs = Preferences.userNodeForPackage(RelativeFluoresence.class);
        this.iterations = prefs.getInt(PREF_ITERATIONS, this.iterations);
        this.baseline = prefs.get(PREF_BASELINE, this.baseline);
        this.stimulus = prefs.get(PREF_STIMULUS, this.stimulus);
        this.stimNames = prefs.get(PREF_STIMULUS_NAMES, this.stimNames);
        this.method = prefs.getInt(PREF_METHOD, this.method);
    }

    private void saveParameters() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(RelativeFluoresence.class);
        prefs.clear();
        prefs.put(PREF_BASELINE, this.baseline);
        prefs.put(PREF_STIMULUS, this.stimulus);
        prefs.put(PREF_STIMULUS_NAMES, this.stimNames);
        prefs.putInt(PREF_ITERATIONS, this.iterations);
        prefs.putInt(PREF_METHOD, this.method);
    }
}
