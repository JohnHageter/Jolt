package Jolt.Utility;

import ij.gui.Roi;

public class CellData {
    public Roi roi;
    private String name;
    private int centerX;
    private int centerY;
    private double fnot;
    private double[] df;

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

    public double getDf(int index){
        return df[index];
    }

    public void setFnot(double fnot){
        this.fnot = fnot;
    }

    public double getFnot(){return this.fnot;}

    public void setDf(double[] df){
        this.df = df;
    }

    public void setDf(int index, double df) {
        this.df[index] = df;
    }

    public String[] breakName(String delimiter) {
        return this.getName().split(delimiter);
    }

    public String printData() {
        return "Cell Data{" +
                "Name= " + this.name +
                "Center X= " + this.centerX +
                ", Center Y= " + this.centerY +
                "}";
    }

}
