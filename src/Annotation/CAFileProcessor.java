package Annotation;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class CAFileProcessor implements PlugInFilter {
    ImagePlus imp;

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        run(imp.getProcessor());
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {

    }
}
