import Jolt.CellManager;
import ij.*;
import Annotation.*;
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

public class toplevel_testIJ {
    private static final File cellRoiFile = new File("resources/CellMap_rois.zip");
    private static final File mapFile = new File("resources/CellMap.tif");
    public static void main(String[] args) throws IOException, FormatException {
        new ImageJ();
        //runGroupROIs();
        processFile();
    }

    //Test Functions
    private static void runCellManager() {
        CellManager cm = new CellManager();
    }

    private static void runCaPipe() {
        ca_Pipe cP = new ca_Pipe();
        cP.run(null);
    }

    private static void runGroupROIs(){
        GroupROIs gR = new GroupROIs();
        RoiManager rm = new RoiManager();

        rm.runCommand("Open", cellRoiFile.getAbsolutePath());
        IJ.log(cellRoiFile.getAbsolutePath());


        ImagePlus imp = IJ.openImage(mapFile.getAbsolutePath());
        imp.show();
        gR.setup("",IJ.getImage());
        gR.run(IJ.getProcessor());
    }

    private static void processFile() throws IOException, FormatException {
        CAFileProcessor ca = new CAFileProcessor();
        File run = new File("resources/f03_RSD/Run_00001.tif");

        ImageReader reader = new ImageReader();
        IMetadata metadata = MetadataTools.createOMEXMLMetadata();
        reader.setMetadataStore((MetadataStore) metadata);
        reader.setId(run.getAbsolutePath());

        boolean isInterleaved = reader.isInterleaved();
        IJ.log("Is the image interleaved? " + isInterleaved);

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
