package Jolt.Processing;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.MaximumFinder;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.nio.FloatBuffer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacv.Java2DFrameUtils;

public class cvMatch_Template implements PlugIn {
    private static String title1 = "";
    private static String title2 = "";
    private static String[] methods = new String[]{"Square difference", "Normalized square difference", "Cross correlation", "Normalized cross correlation", "Correlation coefficient", "Normalized correlation coefficient"};
    private static String[] titles;
    private static int method = -1;
    private static boolean showR = true;
    private static boolean multiM = false;
    private static boolean showRT = true;
    private static boolean log = false;
    private static double mmt = 0.1;
    private static double mmth = 0.0;
    int[] dxdy = new int[2];
    private static ImagePlus img1;
    private static ImagePlus img2;
    private BufferedImage bi;
    private BufferedImage bi2;
    private int[] wList;

    public cvMatch_Template() {
    }

    public void run(String var1) {
        if (var1.equals("about")) {
            this.showAbout();
        } else {
            this.wList = WindowManager.getIDList();
            if (this.wList == null) {
                IJ.noImage();
            } else {
                int var2 = WindowManager.getImageCount();
                if (var2 < 2) {
                    IJ.error("we need two images to do template matching");
                } else if (this.getParams()) {
                    if ((method == 0 || method == 1) && multiM) {
                        IJ.error("Multiple match is not compatible with the square difference method");
                    } else if (img1.getBitDepth() != img2.getBitDepth()) {
                        IJ.error("Images need to have the same type (bit depth)");
                    } else {
                        long var3 = System.currentTimeMillis();
                        FloatProcessor var5 = doMatch(img1, img2, method, showR);
                        long var6 = System.currentTimeMillis() - var3;
                        if (multiM) {
                            MaximumFinder var8 = new MaximumFinder();
                            var8.findMaxima(var5, mmt, mmth, 4, false, false);
                            ResultsTable var10 = ResultsTable.getResultsTable();
                            int var11 = img2.getWidth();
                            int var12 = img2.getHeight();
                            int var13 = var10.getCounter();
                            int[] var14 = new int[var13];
                            int[] var15 = new int[var13];
                            Overlay var16 = new Overlay();

                            for(int var17 = 0; var17 < var13; ++var17) {
                                var14[var17] = (int)var10.getValue("X", var17);
                                var15[var17] = (int)var10.getValue("Y", var17);
                                Roi var18 = new Roi(var14[var17], var15[var17], var11, var12);
                                var18.setStrokeColor(Color.green);
                                var16.add(var18);
                            }

                            img1.setOverlay(var16);
                            if (log) {
                                IJ.log("Best match found at= " + var14[0] + "," + var15[0]);
                            }
                        } else {
                            if (method != 0 && method != 1) {
                                this.dxdy = findMax(var5, 0);
                            } else {
                                this.dxdy = findMin(var5, 0);
                            }

                            if (showRT) {
                                ResultsTable var19 = new ResultsTable();
                                ResultsTable.getResultsTable();
                                var19.reset();
                                var19.incrementCounter();
                                var19.addValue("X", (double)this.dxdy[0]);
                                var19.addValue("Y", (double)this.dxdy[1]);
                                var19.updateResults();
                                var19.show("Results");
                            }

                            if (log) {
                                IJ.log("match found at= " + this.dxdy[0] + "," + this.dxdy[1]);
                            }

                            img1.setRoi(this.dxdy[0], this.dxdy[1], img2.getWidth(), img2.getHeight());
                        }

                        if (showR) {
                            (new ImagePlus("Match result", var5)).show();
                        }

                        IJ.showStatus("Matching done in " + var6 + "msec");
                    }
                }
            }
        }
    }

    private boolean getParams() {
        titles = new String[this.wList.length];

        for(int var1 = 0; var1 < this.wList.length; ++var1) {
            ImagePlus var2 = WindowManager.getImage(this.wList[var1]);
            if (var2 != null) {
                titles[var1] = var2.getTitle();
            } else {
                titles[var1] = "";
            }
        }

        GenericDialog var5 = new GenericDialog("MatchTemplate", IJ.getInstance());
        String var6;
        if (title1.equals("")) {
            var6 = titles[0];
        } else {
            var6 = title1;
        }

        var5.addChoice("Image:", titles, var6);
        if (method == -1) {
            var6 = methods[3];
        } else {
            var6 = methods[method];
        }

        var5.addChoice("Method:", methods, var6);
        if (title2.equals("")) {
            var6 = titles[1];
        } else {
            var6 = title2;
        }

        var5.addChoice("Template:", titles, var6);
        var5.addCheckbox("Output in resultTable ?", showRT);
        var5.addCheckbox("log result?", log);
        var5.addCheckbox("Show correlation map image?", showR);
        var5.addCheckbox("Multiple match?", multiM);
        var5.addNumericField("tolerence for Multi match ", mmt, 2);
        var5.addNumericField("threshold for Multi match ", mmth, 2);
        var5.showDialog();
        if (var5.wasCanceled()) {
            return false;
        } else {
            int var3 = var5.getNextChoiceIndex();
            title1 = titles[var3];
            method = var5.getNextChoiceIndex();
            int var4 = var5.getNextChoiceIndex();
            title2 = titles[var4];
            img1 = WindowManager.getImage(this.wList[var3]);
            img2 = WindowManager.getImage(this.wList[var4]);
            showRT = var5.getNextBoolean();
            log = var5.getNextBoolean();
            showR = var5.getNextBoolean();
            multiM = var5.getNextBoolean();
            mmt = var5.getNextNumber();
            mmth = var5.getNextNumber();
            return true;
        }
    }

    public static FloatProcessor doMatch(ImagePlus var0, ImagePlus var1, int var2, boolean var3) {
        return doMatch(var0.getProcessor(), var1.getProcessor(), var2, var3);
    }

    public static FloatProcessor doMatch(ImageProcessor var0, ImageProcessor var1, int var2, boolean var3) {
        BufferedImage var4 = null;
        BufferedImage var5 = null;
        FloatProcessor var6 = null;
        int var7 = var0.getWidth();
        int var8 = var0.getHeight();
        int var9 = var1.getWidth();
        int var10 = var1.getHeight();
        Loader.load(opencv_core.class);
        opencv_core.IplImage var14 = null;
        opencv_core.IplImage var15 = null;
        switch (var0.getBitDepth()) {
            case 8:
                var4 = var0.getBufferedImage();
                var14 = Java2DFrameUtils.toIplImage(var4);
                var5 = var1.getBufferedImage();
                var15 = Java2DFrameUtils.toIplImage(var5);
                break;
            case 16:
                var14 = opencv_core.cvCreateImage(opencv_core.cvSize(var7, var8), 32, 1);
                var4 = ((ShortProcessor)var0).get16BitBufferedImage();
                opencv_core.IplImage var11 = Java2DFrameUtils.toIplImage(var4);
                opencv_core.cvConvertScale(var11, var14, 1.5259021896696422E-5, 0.0);
                var15 = opencv_core.cvCreateImage(opencv_core.cvSize(var9, var10), 32, 1);
                var5 = ((ShortProcessor)var1).get16BitBufferedImage();
                opencv_core.IplImage var12 = Java2DFrameUtils.toIplImage(var5);
                opencv_core.cvConvertScale(var12, var15, 1.5259021896696422E-5, 0.0);
                var11.release();
                var12.release();
                break;
            case 32:
                opencv_core.CvMat var16 = CvMat.create(var8, var7, opencv_core.CV_32FC1);
                double[] var17 = float2DtoDouble1DArray(var0.getFloatArray(), var7, var8);
                var16.put(0, var17, 0, var17.length);
                var14 = var16.asIplImage();
                opencv_core.CvMat var18 = CvMat.create(var10, var9, opencv_core.CV_32FC1);
                double[] var19 = float2DtoDouble1DArray(var1.getFloatArray(), var9, var10);
                var18.put(0, var19, 0, var19.length);
                var15 = var18.asIplImage();
                break;
            default:
                IJ.error("Unsupported image type");
        }

        opencv_core.IplImage var13 = opencv_core.cvCreateImage(opencv_core.cvSize(var7 - var9 + 1, var8 - var10 + 1), 32, 1);
        opencv_imgproc.cvMatchTemplate(var14, var15, var13, var2);
        FloatBuffer var20 = var13.getFloatBuffer();
        float[] var21 = new float[var13.width() * var13.height()];
        var20.get(var21, 0, var21.length);
        var6 = new FloatProcessor(var13.width(), var13.height(), var21, (ColorModel)null);
        opencv_core.cvReleaseImage(var13);
        switch (var0.getBitDepth()) {
            case 8:
                var14.release();
                var15.release();
                break;
            case 16:
                opencv_core.cvReleaseImage(var14);
                opencv_core.cvReleaseImage(var15);
                break;
            case 32:
                var14.release();
                var15.release();
        }

        return var6;
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

    public static int[] findMin(ImageProcessor var0, int var1) {
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
                if ((float)var0.getPixel(var7, var6) < var3) {
                    var3 = (float)var0.getPixel(var7, var6);
                    var2[0] = var7;
                    var2[1] = var6;
                }
            }
        }

        return var2;
    }

    private static double[] float2DtoDouble1DArray(float[][] var0, int var1, int var2) {
        double[] var3 = new double[var1 * var2];

        for(int var4 = 0; var4 < var2; ++var4) {
            for(int var5 = 0; var5 < var1; ++var5) {
                var3[var4 * var1 + var5] = (double)var0[var5][var4];
            }
        }

        return var3;
    }

    public void showAbout() {
        IJ.showMessage("cvMatch Template", "This plugin implements the tempalte matching function from\nthe OpenCV library. It will try to find an object (template)\nwithin a given image (image).Six different matching algorithms\n(methods)could be used. The matching result could be printed\nin the log window or the result table. \nYou can also decide to display the correlation map. The coordinates\nof the maximum (or minimum for the square difference methods)\ncorrespond to the best match.\nBy checking the multimatch option, not only the best match will\nbe shown, but also all the similar pattern above the defined\nthreshold will be shown (Find maximum function on the correlation map)\nMore details on \nhttps://sites.google.com/site/qingzongtseng/template-matching-ij-plugin");
    }
}

