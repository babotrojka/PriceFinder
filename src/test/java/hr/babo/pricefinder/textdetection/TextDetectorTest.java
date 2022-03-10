package hr.babo.pricefinder.textdetection;

import hr.babo.pricefinder.DriverCombined;
import hr.babo.pricefinder.python.Python;
import org.junit.jupiter.api.Test;

public class TextDetectorTest {

    private static String imagesPath = "katalozi/";
    private static String kmeansScript = "scripts/kmeans_script.py";
    private static String outPath = "out.txt";
    private static String coordsFile = "coords/";


    @Test
    public void testTextDetector() {
        TextDetector textDetector = new TextDetector("katalozi/");
        DriverCombined.clearFile(outPath);
        textDetector.textFromImages("kaufland-1.jpg");
        Python.pythonCallNoOutput(kmeansScript, coordsFile, outPath);
    }
}
