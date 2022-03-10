import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PdfToImage {

    private static String pdfPath = "katalozi/konzum.pdf";
    private static String imagesPath = "katalozi/konzum";

    public static void main(String[] args) {
        pdfToImages(pdfPath, imagesPath, 3, 7, 150);
    }

    public static void pdfToImages(String pdfPath, String imagesPath, Integer start,  Integer end, int dpi) {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int s = start == null ? 0 : start;
            int ps = end == null ? document.getNumberOfPages() : end;
            for (int page = s; page < ps; ++page) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(
                        page, dpi, ImageType.RGB);
                ImageIOUtil.writeImage(
                        bim, String.format("%s-%d.%s", imagesPath, page + 1, "jpg"), dpi);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
