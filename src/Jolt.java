import Jolt.Analysis.*;
import Jolt.Utility.CellManager;
import Jolt.Processing.FileProcessor;
import ij.*;
import Jolt.Annotation.*;
import ij.plugin.frame.RoiManager;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.MetadataStore;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import ome.xml.meta.IMetadata;


import java.io.File;
import java.io.IOException;

public class Jolt {
    private static final File cellRoiFile = new File("resources/f08_LSD/RoiSet_anno.zip");
    private static final File mapFile = new File("resources/f08_LSD/CellMap.tif");
    private static final File runFile = new File("resources/f08_LSD/Substack (60-100).tif");
    public static void main(String[] args) throws IOException, FormatException {
        new ImageJ();
        RoiManager rm = new RoiManager();
        rm.runCommand("Open", cellRoiFile.getAbsolutePath());
        //ImagePlus imp = IJ.openImage(mapFile.getAbsolutePath());
        ImagePlus imp = IJ.openImage(runFile.getAbsolutePath());
        imp.show();

        runAnalysis();
    }

    //Test Functions
    private static void runCellManager() {
        CellManager cm = new CellManager();
    }

    private static void runAnalysis(){
        RelativeFluorescence rf = new RelativeFluorescence();
        rf.setup("", IJ.getImage());
        rf.run(IJ.getProcessor());
    }

    private static void runCaPipe() {
        ca_Pipe cP = new ca_Pipe();
        cP.run(null);
    }

    private static void runGroupROIs(){
        GroupROIs gR = new GroupROIs();
        //IJ.log(cellRoiFile.getAbsolutePath());
        gR.setup("",IJ.getImage());
        gR.run(IJ.getProcessor());
    }

    private static void processFile() throws IOException, FormatException {
        FileProcessor ca = new FileProcessor();
        File run = new File("resources/f08_LSD/Run_00001.tif");

        ImageReader reader = new ImageReader();
        IMetadata metadata = MetadataTools.createOMEXMLMetadata();
        reader.setMetadataStore((MetadataStore) metadata);
        reader.setId(run.getAbsolutePath());

        boolean isInterleaved = reader.isInterleaved();

        ImporterOptions opts = new ImporterOptions();
        opts.setId(run.getAbsolutePath());
        opts.setSplitChannels(false);
        opts.setOpenAllSeries(true);

        ImagePlus[] imps = BF.openImagePlus(opts);

        for (ImagePlus imp : imps) {
            imp.show();
        }

        ca.setup("", IJ.getImage());
        ca.run(null);
    }

}
