package Processing;


import TemplateMatching.cvMatch_Template;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.Recorder;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

public class Align_slices implements PlugInFilter {
    ImagePlus imp;
    ImageProcessor ref;
    ImageProcessor tar;
    ImageStack stack;
    Rectangle rect;
    Roi r;
    int method;
    int refSlice;
    int sArea = 0;
    int width;
    int height;
    double disX;
    double disY;
    double rMax;
    double rMin;
    FloatProcessor result;
    ResultsTable rt;
    String arg;
    int windowSizeX;
    int windowSizeY;
    int iniX;
    int iniY;
    boolean subPixel = false;
    boolean showRT = false;
    int itpMethod = 0;

    public Align_slices() {
    }

    public int setup(String var1, ImagePlus var2) {
        this.imp = var2;
        return 2053;
    }

    public void run(ImageProcessor var1) {
        this.stack = this.imp.getStack();
        int var2 = this.imp.getType();
        int var3 = this.stack.getSize();
        String var4 = Macro.getOptions();
        this.width = this.imp.getWidth();
        this.height = this.imp.getHeight();
        if (var4 != null) {
            this.getMacroParameters(var4);
            this.imp.setSlice(this.refSlice);
            this.imp.setRoi(this.iniX, this.iniY, this.windowSizeX, this.windowSizeY);
            this.r = this.imp.getRoi();
            this.rect = this.r.getBounds();
        } else {
            if (!this.getUserParameters()) {
            }

            IJ.setTool("rect");
            //(new WaitForUserDialog("Align slices", "Select a rectangle region as the landmark\non a reference slice")).show();
            this.refSlice = this.imp.getCurrentSlice();
            this.r = new Roi(this.imp.getWidth()/8, this.imp.getHeight()/8, this.imp.getWidth()/2, this.imp.getHeight()/2);
            if (this.r == null || !this.r.isArea()) {
                IJ.showMessage("Error", "rectangular ROI needed");
                return;
            }

            this.rect = this.r.getBounds();
            if (Recorder.record) {
                Recorder.setCommand("Align slices in stack...");
                Recorder.recordOption("method", "" + this.method);
                Recorder.recordOption("windowsizex", "" + this.rect.width);
                Recorder.recordOption("windowsizey", "" + this.rect.height);
                Recorder.recordOption("x0", "" + this.rect.x);
                Recorder.recordOption("y0", "" + this.rect.y);
                Recorder.recordOption("sWindow", "" + this.sArea);
                Recorder.recordOption("subPixel", "" + this.subPixel);
                Recorder.recordOption("itpMethod", "" + this.itpMethod);
                Recorder.recordOption("ref.slice", "" + this.refSlice);
                Recorder.recordOption("show", "" + this.showRT);
            }
        }

        this.ref = this.imp.getProcessor().crop();
        if (this.showRT) {
            this.rt = new ResultsTable();
            ResultsTable.getResultsTable();
        }

        int var5;
        for(var5 = this.refSlice - 1; var5 > 0; --var5) {
            this.alignSlices(var5);
            IJ.log("Slice:" + var5 + " X displacement:" + this.disX + " Y displacement:" + this.disY);
            if (this.showRT) {
                this.rt.incrementCounter();
                this.rt.addValue("Slice", (double)var5);
                this.rt.addValue("dX", this.disX);
                this.rt.addValue("dY", this.disY);
                this.rt.updateResults();
            }
        }

        for(var5 = this.refSlice + 1; var5 < var3 + 1; ++var5) {
            this.alignSlices(var5);
            IJ.log("Slice:" + var5 + " X displacement:" + this.disX + " Y displacement:" + this.disY);
            if (this.showRT) {
                this.rt.incrementCounter();
                this.rt.addValue("Slice", (double)var5);
                this.rt.addValue("dX", this.disX);
                this.rt.addValue("dY", this.disY);
                this.rt.updateResults();
            }
        }

        this.imp.updateAndDraw();
        this.rt.show("Results");
    }

    private void alignSlices(int var1) {
        int[] var2 = new int[2];
        int[] var3 = new int[]{0, 0};
        boolean var4 = false;
        this.tar = this.stack.getProcessor(var1);
        if (this.sArea != 0) {
            int var5 = this.rect.x - this.sArea;
            int var6 = this.rect.y - this.sArea;
            int var7 = this.rect.width + 2 * this.sArea;
            int var8 = this.rect.height + 2 * this.sArea;
            if (var5 < 0) {
                var3[0] = var5;
                var5 = 0;
            }

            if (var6 < 0) {
                var3[1] = var6;
                var6 = 0;
            }

            if (var5 + var7 > this.width) {
                var7 = this.width - var5;
            }

            if (var6 + var8 > this.height) {
                var8 = this.height - var6;
            }

            this.tar.setRoi(var5, var6, var7, var8);
        } else {
            this.tar.resetRoi();
        }

        this.result = cvMatch_Template.doMatch(this.tar.crop(), this.ref, this.method, var4);
        var2 = findMax(this.result, 0);
        if (this.subPixel) {
            double[] var9 = new double[2];
            var9 = this.gaussianPeakFit(this.result, var2[0], var2[1]);
            if (this.sArea == 0) {
                this.disX = (double)this.rect.x - var9[0];
                this.disY = (double)this.rect.y - var9[1];
            } else {
                this.disX = (double)(this.sArea + var3[0]) - var9[0];
                this.disY = (double)(this.sArea + var3[1]) - var9[1];
            }

            this.tar.setInterpolationMethod(this.itpMethod);
        } else if (this.sArea == 0) {
            this.disX = (double)(this.rect.x - var2[0]);
            this.disY = (double)(this.rect.y - var2[1]);
        } else {
            this.disX = (double)(this.sArea + var3[0] - var2[0]);
            this.disY = (double)(this.sArea + var3[1] - var2[1]);
        }

        this.tar.resetRoi();
        this.tar.translate(this.disX, this.disY);
    }

    public static int[] findMax(ImageProcessor var0, int var1) {
        int[] var2 = new int[2];
        float var3 = (float)var0.getPixel(0, 0);
        int var4;
        int var5;
        if (var1 == 0) {
            var4 = var0.getHeight();
            var5 = var0.getWidth();
        } else {
            var4 = var1;
            var5 = var1;
        }

        for(int var6 = (var0.getHeight() - var4) / 2; var6 < (var0.getHeight() + var4) / 2; ++var6) {
            for(int var7 = (var0.getWidth() - var5) / 2; var7 < (var0.getWidth() + var5) / 2; ++var7) {
                if ((float)var0.getPixel(var7, var6) > var3) {
                    var3 = (float)var0.getPixel(var7, var6);
                    var2[0] = var7;
                    var2[1] = var6;
                }
            }
        }

        return var2;
    }

    private double[] gaussianPeakFit(ImageProcessor var1, int var2, int var3) {
        double[] var4 = new double[2];
        if (var2 != 0 && var2 != var1.getWidth() - 1 && var3 != 0 && var3 != var1.getHeight() - 1) {
            var4[0] = (double)var2 + (Math.log((double)var1.getPixel(var2 - 1, var3)) - Math.log((double)var1.getPixel(var2 + 1, var3))) / (2.0 * Math.log((double)var1.getPixel(var2 - 1, var3)) - 4.0 * Math.log((double)var1.getPixel(var2, var3)) + 2.0 * Math.log((double)var1.getPixel(var2 + 1, var3)));
            var4[1] = (double)var3 + (Math.log((double)var1.getPixel(var2, var3 - 1)) - Math.log((double)var1.getPixel(var2, var3 + 1))) / (2.0 * Math.log((double)var1.getPixel(var2, var3 - 1)) - 4.0 * Math.log((double)var1.getPixel(var2, var3)) + 2.0 * Math.log((double)var1.getPixel(var2, var3 + 1)));
        } else {
            var4[0] = (double)var2;
            var4[1] = (double)var3;
        }

        return var4;
    }

    private void getMacroParameters(String var1) {
        this.method = Integer.parseInt(Macro.getValue(var1, "method", "5"));
        this.windowSizeX = Integer.parseInt(Macro.getValue(var1, "windowsizex", "512"));
        this.windowSizeY = Integer.parseInt(Macro.getValue(var1, "windowsizey", "512"));
        this.iniX = Integer.parseInt(Macro.getValue(var1, "x0", "0"));
        this.iniY = Integer.parseInt(Macro.getValue(var1, "y0", "0"));
        this.sArea = Integer.parseInt(Macro.getValue(var1, "sWindow", "0"));
        this.subPixel = Boolean.parseBoolean(Macro.getValue(var1, "subPixel", "TRUE"));
        this.itpMethod = Integer.parseInt(Macro.getValue(var1, "itpMethod", "0"));
        this.refSlice = Integer.parseInt(Macro.getValue(var1, "ref.slice", "1"));
        this.showRT = Boolean.parseBoolean(Macro.getValue(var1, "show", "TRUE"));
    }

    private boolean getUserParameters() {
        String[] var1 = new String[]{"Square difference", "Normalized square difference", "Cross correlation", "Normalized cross correlation", "Correlation coefficient", "Normalized correlation coefficient"};
        String[] var2 = new String[]{"Bilinear", "Bicubic"};
        GenericDialog var3 = new GenericDialog("Align slices by cvMatchTemplate");
        var3.addMessage("Select a rectangle region as the landmark for alignment.");
        var3.addChoice("Matching method", var1, var1[5]);
        var3.addNumericField("Search area(pixels around ROI) ", 0.0, 0);
        var3.addMessage("(Template will be searched on the whole image if search area =0)");
        var3.addCheckbox("Subpixel registration?", this.subPixel);
        var3.addChoice("Interpolation method for subpixel translation", var2, var2[this.itpMethod]);
        var3.addCheckbox("show align coordinates in results table?", true);
        var3.showDialog();
        if (var3.wasCanceled()) {
            return false;
        } else {
            this.method = var3.getNextChoiceIndex();
            this.sArea = (int)var3.getNextNumber();
            this.subPixel = var3.getNextBoolean();
            this.itpMethod = var3.getNextChoiceIndex();
            this.showRT = var3.getNextBoolean();
            return true;
        }
    }
}

