package hr.babo.pricefinder;

import hr.babo.pricefinder.pdftoimage.PDFToImage;
import hr.babo.pricefinder.python.Python;
import hr.babo.pricefinder.textdetection.TextDetector;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DriverCombined {

    private static String pdfPath = "katalozi/kaufland.pdf";
    private static String imagesPath = "katalozi/";
    private static String kmeansScript = "scripts/kmeans_script.py";
    private static String outPath = "out.txt";
    private static String coordsFile = "coords/";

    public static void main(String[] args) {
        String storeName = pdfPath.substring(pdfPath.lastIndexOf("/") + 1, pdfPath.lastIndexOf(".pdf"));

        //int dLength = PDFToImage.pdfToImages(pdfPath, imagesPath + storeName, 0, 3, 150);

        clearFile(outPath);

        int dLength = 2;
        TextDetector textDetector = new TextDetector(imagesPath);
        for(int i = 0; i < dLength; i++) {
            textDetector.textFromImages(String.format("%s-%d.%s", storeName, i + 1, "jpg"));
            int exitCode = Python.pythonCallNoOutput(kmeansScript, String.format("%scoords-%s-%d.txt", coordsFile, storeName, i + 1), outPath);
            if (exitCode != 0) {
                System.err.printf("Error executing kmeansScript with page %d\n", i + 1);
                break;
            }

            System.out.printf("Finished with page %d\n", i + 1);
        }

    }

    public static void clearFile(String file) {
        try {
            new PrintWriter(file).close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
