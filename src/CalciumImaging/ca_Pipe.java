
package CalciumImaging;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import javax.swing.JFileChooser;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ca_Pipe implements PlugIn {

    int runCounter = 1;
    @Override
    public void run(String arg) {
        GenericDialog gd = new GenericDialog("Select Parent Directory");
        gd.addMessage("Please select the parent directory:");

        JFileChooser fileChooser = new JFileChooser("D:/");
        fileChooser.setDialogTitle("Select Parent Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int userSelection = fileChooser.showDialog(null, "Select");

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            IJ.log("Selected Directory: " + selectedDirectory.getAbsolutePath());
			File[] subDirectories = selectedDirectory.listFiles(File::isDirectory);

            assert subDirectories != null;
            for (File subDirectory : subDirectories){
            			processSubfolders(subDirectory);
				}
        }
    }
    private void processSubfolders(File directory) {
        File[] subfolders = directory.listFiles(File::isDirectory);

        if (subfolders != null) {
            for (File subfolder : subfolders) {
                IJ.log("Processing subfolder: " + subfolder.getAbsolutePath());
				deleteSpecificFiles(subfolder);
                // Check for TIFF files in the current subfolder
                String tifDirectory = findTifDirectory(subfolder);
                if (tifDirectory != null) {
                    IJ.log("Directory with TIFF files: " + tifDirectory);

					deleteSpecificFiles(new File(tifDirectory));
                    // Check for and process keyword files in the TIFF directory
                    checkForAndProcessKeywordFiles(new File(tifDirectory), "map");
                    checkForAndProcessKeywordFiles(new File(tifDirectory), "run");
                    // Process the current subfolder
                    processRun(new File(tifDirectory));

                    // Process the next subfolder
                    processSubfolders(new File(tifDirectory));
                } else {
                    IJ.log("No TIFF files found in the subfolder.");
                }
            }
        }
    }
    private void checkForAndProcessKeywordFiles(File directory, String keyword) {
        File[] files = directory.listFiles();
		
        if (files != null) {
            List<File> runFiles = new ArrayList<>();
            int runCounter = 1;

            for (File file : files) {
            	
                if (file.isFile() && file.getName().toLowerCase().endsWith(".tif") &&
                        file.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    IJ.log("Found " + keyword + " file: " + file.getAbsolutePath());

                    if (keyword.equals("map")) {
                        processMapFile(directory, file);
                    } else if (keyword.equals("run")) {
                        processRunFile(directory, file, runCounter);
                        runCounter ++;
                    }
                }
            }
        }
    }
    private void processMapFile(File parentFolder, File mapFile) {
	       // Get the path to the "map" file
	    String mapFilePath = mapFile.getAbsolutePath();
	
	    // Open the "map" image
	    ImagePlus mapImage = IJ.openImage(mapFilePath);
	    
	    // Rename to "map.tif"
    	mapImage.setTitle("map.tif");

        // Convert to 8-bit
        IJ.run(mapImage, "8-bit", "");
        IJ.run(mapImage, "Enhance Contrast", "saturated=0.35");

        // Deinterleave
        IJ.run(mapImage, "Deinterleave", "how=2");
		IJ.selectWindow(mapImage.getTitle() + " #1");
		IJ.run("Rename...", "title=Ch1");
		IJ.run("Z Project...", "projection=[Sum Slices]");
		
		IJ.selectWindow("SUM_Ch1");
        IJ.run("Enhance Contrast", "saturated=0.35");
        IJ.run("8-bit");
        IJ.run("Duplicate...", "title=RegMap");
        String Ch1Title = IJ.getImage().getTitle();
		IJ.save(parentFolder + "\\" + Ch1Title + ".tif");
        
		IJ.selectWindow(mapImage.getTitle() + " #2");
		IJ.run("Rename...", "title=Ch2");
		IJ.run("Z Project...", "projection=[Sum Slices]");
		
		IJ.selectWindow("SUM_Ch2");
        IJ.run("Enhance Contrast", "saturated=0.35");
        IJ.run("8-bit");

        IJ.run("Concatenate...", "  title=MergeMap image1=[SUM_Ch2] image2=[SUM_Ch1] image3=[-- None --]");
        IJ.selectWindow("MergeMap");
        IJ.run("8-bit");

		IJ.selectWindow("Ch2");
		IJ.run("Close");
		IJ.selectWindow("Ch1");
		IJ.run("Close");
		IJ.selectWindow("MergeMap");
		String activeWindowTitle = IJ.getImage().getTitle();
		IJ.save(parentFolder + "\\" + activeWindowTitle + ".tif");
		IJ.run("Close");
        mapImage.close();
    }
    private void processRunFile(File parentFolder, File runFile, int runCounter) {
	    	// Get the path to the "run" file
	    String runFilePath = runFile.getAbsolutePath();
	    IJ.log("Opening: " + runFilePath);

	    // Open the "run" image
	    ImagePlus runImage = IJ.openImage(runFilePath);
	    runImage.show();
	
	    // Rename to "Run1", "Run2", etc. in ascending order
	    runImage.setTitle("Run" + runCounter);
	
	    // Continue with the rest of your processing logic for "run" files...
	    if (runImage != null) {
	        IJ.log("Opened: " + runImage.getTitle());
	    } else {
	        IJ.log("Error opening the run image: " + runFilePath);
	    }
    }
    private void processRun(File parentFolder) {
    	String lightFilePath = parentFolder.getAbsolutePath() + File.separator + "Light.tif";
    	IJ.log(lightFilePath);
    	String toRegisterFilePath = parentFolder.getAbsolutePath() + File.separator + "Registered.tif";
    	String cellMapPath = parentFolder.getAbsolutePath() + File.separator + "CellMap.tif";
    	
        IJ.run("Concatenate...", "  title=Moving image1=[Run1] image2=[Run2] image3=[-- None --]");
        IJ.selectWindow("Moving");
    	IJ.run("Deinterleave", "how=2");
    	IJ.selectWindow("Moving #1");
    	IJ.run("Rename...", "title=toRegister");
    	IJ.run("Enhance Contrast", "saturated=0.35");
    	IJ.run("8-bit");
    	
    	IJ.selectWindow("RegMap");
    	IJ.run("Copy", "");
    	IJ.selectWindow("toRegister");
    	IJ.run("Paste");
    	
    	IJ.selectWindow("RegMap");
    	IJ.run("Close");
    	
    	IJ.selectWindow("Moving #2");
    	IJ.saveAs("Tiff", lightFilePath);
    	IJ.run("Close");
    	
    	IJ.selectWindow("toRegister");
    	IJ.run("Align slices in stack...", 
    	"method=5 windowsizex=341 windowsizey=324 x0=85 y0=102 swindow=0 subpixel=true itpmethod=0 ref.slice=1 show=true");
    	IJ.saveAs("Tiff", toRegisterFilePath);
    	IJ.run("Z Project...", "projection=[Sum Slices]");
    	IJ.run("Enhance Contrast", "saturated=0.35");
        IJ.run("8-bit");
        IJ.saveAs("Tiff", cellMapPath);
        IJ.run("Close");
        IJ.selectWindow("Registered.tif");
        IJ.run("Close");
    }
    private String findTifDirectory(File directory) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".tif")) {
                    return directory.getAbsolutePath();
                }
            }

            for (File subDir : files) {
            	deleteSpecificFiles(subDir);
                if (subDir.isDirectory()) {
                	deleteSpecificFiles(subDir);
                    String result = findTifDirectory(subDir);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }
    private void deleteSpecificFiles(File directory) {
        List<String> filesToDelete = Arrays.asList(
                "CellMap.tif", "Light.tif", "MergeMap.tif", "Registered.tif", "RegMap.tif"
        );

        for (String fileName : filesToDelete) {
            File fileToDelete = new File(directory, fileName);
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    IJ.log("Deleted file: " + fileToDelete.getAbsolutePath());
                } else {
                    IJ.log("Failed to delete file: " + fileToDelete.getAbsolutePath());
                }
            }
        }
    }
}
