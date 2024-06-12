package Annotation;

import ij.*;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class FileProcessor implements PlugInFilter {
    ImagePlus imp;
    int bitDepth;
    int setChannels = (int) Prefs.get("Deint_ch.int", 2);
    boolean interleaved = Prefs.get("Deint_inter.boolean", true);
    int regChannel;
    ImageStack regStack;
    String inputFilename;
    ImageStack shuffledStack;

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        this.regStack = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        this.bitDepth = this.imp.getBitDepth();
        this.inputFilename = this.imp.getTitle();

        GenericDialog gd = new GenericDialog("File Processor");
        gd.addNumericField("Number Channels", 2);
        gd.addNumericField("Registration Channel", 1);
        gd.addCheckbox("Is Interleaved", true);
        gd.showDialog();

        if (gd.wasOKed()){
            this.setChannels = (int) gd.getNextNumber();
            this.regChannel = (int) gd.getNextNumber();
            this.interleaved = gd.getNextBoolean();
            Prefs.set("Deint_ch.int", this.setChannels);
            Prefs.set("Deint_inter.boolean", this.interleaved);
        } else if (gd.wasCanceled()) {
            return;
        }

        this.shuffledStack = makeShuffled(this.imp.getStack());
        ImagePlus shuffledImage = new ImagePlus(this.inputFilename, this.shuffledStack);

        if (this.interleaved) {
            deinterleaveStack();
            shuffledImage.changes = false;
            shuffledImage.close();
        }

        registerStack();

    }

    private void registerStack() {
        if(this.interleaved){
            //TODO Select window with registration channel and NCCXORR transform
            ImagePlus imp = new ImagePlus("Register Channel", this.regStack);
            imp.show();

        }
    }


    /* Russell Kincaid, rekincai@syr.edu, November 15, 2002.
     * hacked from Slice_Multiplier.java etc.
     * further modified	by tony collins
     * rewritten 2014-09-19, git@tds.xyz */
    private void deinterleaveStack() {
        int stackSize = this.shuffledStack.getSize();
        int nFrames = stackSize/this.setChannels;

        Calibration oc = this.imp.getCalibration().copy();
        for(int channel = 0; channel <= this.setChannels; channel++) {
            int beginSlice = channel * nFrames;
            int endSlice = Math.min(beginSlice + nFrames, stackSize);
            if(beginSlice == endSlice) break;
            String channelName = Integer.toString(channel+1);
            ImagePlus channelImage = new ImagePlus(this.inputFilename + " #" + channelName, makeSubStack(this.shuffledStack, beginSlice, endSlice));
            channelImage.show();
            channelImage.setCalibration(oc);
            ImageWindow win = channelImage.getWindow();

            if(channel == this.regChannel){
                this.regStack.addSlice(channelImage.getProcessor());
            }

            if (null != win)
                win.repaint();
        }
    }

    /* Russell Kincaid, rekincai@syr.edu, November 15, 2002.
     * hacked from Slice_Multiplier.java etc.
     * further modified	by tony collins
     * rewritten 2014-09-19, git@tds.xyz */
    public ImageStack makeSubStack(ImageStack stack, int beginSlice, int endSlice) {
        ImageStack newStack = new ImageStack(stack.getWidth(), stack.getHeight(), stack.getColorModel());
        for(int i = beginSlice; i < endSlice; ++i) {
            newStack.addSlice(stack.getSliceLabel(i+1), stack.getProcessor(i+1));
        }
        return newStack;
    }

    /* Russell Kincaid, rekincai@syr.edu, November 15, 2002.
     * hacked from Slice_Multiplier.java etc.
     * further modified	by tony collins
     * rewritten 2014-09-19, git@tds.xyz */
    public ImageStack makeShuffled(ImageStack stack) {
        ImageStack newStack = new ImageStack(stack.getWidth(), stack.getHeight(), stack.getColorModel());
        for(int channel = 0; channel < this.setChannels; ++channel) {
            for(int i = channel; i < stack.getSize(); i += this.setChannels) {
                newStack.addSlice(stack.getSliceLabel(i+1), stack.getProcessor(i+1));
            }
        }
        return newStack;
    }

}
