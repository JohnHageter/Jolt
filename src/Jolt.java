import Jolt.Utility.CellManager;
import ij.*;
import ij.plugin.PlugIn;


public class Jolt implements PlugIn {
//    public static void main(String[] args) {
//        new ImageJ();
//        new CellManager();
//    }

    @Override
    public void run(String arg) {
        new CellManager();
    }
}
