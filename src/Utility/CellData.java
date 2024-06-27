package Utility;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class CellData {
    public Roi roi;
    public String name;
    public int centerX;
    public int centerY;
    public double fnot;
    public double[] df;

    public CellData(){
    }

    public CellData(Roi cell){
        this.roi = cell;
        this.name = cell.getName();
        this.centerX = (int) cell.getBounds().getCenterX();
        this.centerY = (int) cell.getBounds().getCenterY();
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public String getName(){
        return name;
    }

    public double[] getDf() {
        return df;
    }

    public void setFnot(double fnot){
        this.fnot = fnot;
    }

    public void setDf(double[] df){
        this.df = df;
    }

    public String printData() {
        return "Cell Data{" +
                "Center X= " + centerX +
                ", Center Y= " + centerY +
                "}";
    }

}
