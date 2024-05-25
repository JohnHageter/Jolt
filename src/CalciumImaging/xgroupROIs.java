package CalciumImaging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.awt.AWTEvent;
import java.util.ArrayList;

public class xgroupROIs implements DialogListener, PlugIn {
    private GenericDialog gd;
    private int currentGroups = 5;
    @Override
    public void run(String arg) {
        boolean params = setup();
        if (params) {
            String[] nonEmptyFields = getNonEmptyStringFields();
            int groupId = 1;
            Roi[] groups = selectGroups(nonEmptyFields);
            for (String field : nonEmptyFields) {
                IJ.log("Group #" + groupId + ": " + field);
                groupId++;
            }
            findROIsWithinGroups(groups);

//            assert groupRois != null;
//            Roi[] tmpGroups = groupRois.getRoisAsArray();
//            for (String field : nonEmptyFields) {
//                for(Roi tmpGroup : tmpGroups) {
//                    if (tmpGroup.getName().equals(field + "_" + field)){
//                        groupRois.select(groupRois.getRoiIndex(tmpGroup));
//                        groupRois.runCommand("Delete");
//                    }
//                }
//            }

        }
    }

    public boolean setup() {
        gd = new GenericDialog("Group ROIs by polygon selection");
        if (gd.wasCanceled()) {
            IJ.log("Input parameters required.");
            return false;
        } else {
            gd.addButton("add group", new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    currentGroups++;
                    updateDialog();
                }
            });
            gd.addButton("remove group", new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (currentGroups > 1) {
                        currentGroups--;
                        updateDialog();
                    }
                }
            });

            for (int i = 0; i < currentGroups; i++) {
                gd.addStringField("Group #" + (i + 1) + ": ", "");
            }

            gd.addDialogListener(this);
            gd.showDialog();

            return true;
        }
    }

    private void updateDialog() {
        gd.dispose();
        gd = new GenericDialog("Group ROIs by polygon selection");
        gd.addButton("add group", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentGroups++;
                updateDialog();
            }
        });

        gd.addButton("remove group", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentGroups > 1) {
                    currentGroups--;
                    updateDialog();
                }
            }
        });

        for (int i = 0; i < currentGroups; i++) {
            gd.addStringField("Group #" + (i + 1) + ": ", "");
        }

        gd.addDialogListener(this);
        gd.showDialog();
    }

    public String[] getNonEmptyStringFields() {
        ArrayList<String> nonEmptyFields = new ArrayList<>();
        for (int i = 0; i < currentGroups; i++) {
            String field = gd.getNextString();
            if (!field.isEmpty()) {
                nonEmptyFields.add(field);
            }
        }
        return nonEmptyFields.toArray(new String[0]);
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        return true;
    }

    Roi[] selectGroups(String[] groups) {

        ImagePlus image = IJ.getImage();

        if (image == null) {
            throw new RuntimeException("No images open.");
        }
        image.show();

        IJ.setTool("polygon");
        Roi[] groupingROIs;
        groupingROIs = new Roi[groups.length];
        int index = 0;

        RoiManager groupROIs = RoiManager.getInstance();
        groupROIs.runCommand("Show All");

        for(String group : groups) {
            (new WaitForUserDialog("Group ROIs", "Select " + group + "\nThen click OK.")).show();

            groupingROIs[index] = image.getRoi();
            groupingROIs[index].setName(group);
            groupROIs.addRoi(groupingROIs[index]);

            IJ.log(group + " Center x: " + groupingROIs[index].getBounds().getCenterX() + ", Center y: " + groupingROIs[index].getBounds().getCenterY());
            index++;
        }

        String savePath;
        savePath = image.getOriginalFileInfo().directory;
        IJ.log("Saving group ROIs to: " + savePath);
        if(savePath != null){
            groupROIs.save(savePath + "Grouping_ROIs.zip");
        } else {
            groupROIs.runCommand("Save");
        }
        return groupingROIs;
    }

    public void findROIsWithinGroups(Roi[] groups) {
        ImagePlus im = IJ.getImage();
        String path = im.getOriginalFileInfo().directory;
        RoiManager cellROIs = RoiManager.getInstance();

        if (cellROIs == null) {
            IJ.log("ROI manager not open.");
        }else{
            cellROIs.open(path + "CellMap_rois.zip");
        }

        assert cellROIs != null;
        Roi[] cells = cellROIs.getRoisAsArray();
        if (cells == null || cells.length == 0) {
            IJ.showMessage("Error", "No ROIs in the ROI Manager");
            return;
        }


        for (Roi group : groups) {
            for (Roi cell : cells) {
                if(cell.getName().equals(group.getName())){
                    continue;
                } else if (group.contains((int) cell.getBounds().getCenterX(), (int) cell.getBounds().getCenterY())) {
                    Roi foundRoi = (Roi) cell.clone();
                    foundRoi.setName(cell.getName() + "_" + group.getName());
                    cellROIs.addRoi(foundRoi);
                    cellROIs.select(cellROIs.getRoiIndex(cell));
                    cellROIs.runCommand("Delete");
                    IJ.log(cell.getName() + " was found within: " + group.getName());
                }
            }
        }
    }
}
