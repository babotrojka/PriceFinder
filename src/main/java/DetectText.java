import hr.babo.pricefinder.textdetection.Center;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DetectText {

    private static String imageName = "konzum-7.jpg";
    private static String sourceImage = "katalozi/" + imageName;

    private static String logs = "log/";

    private static List<String> falses = new ArrayList<>(Arrays.asList("cijen", "niž", "ponud", "nov", "prilik"));
    private static double hLimit = 0.03;
    private static double distanceThreshold = 0.18;


    public static void main(String[] args) {
        textFromImages(sourceImage);
    }

    public static void textFromImages(String sourceImage) {
        Region region = Region.US_EAST_1;
        TextractClient texClient = TextractClient.builder()
                .region(region)
                .build();

        detectText(texClient, sourceImage);

        texClient.close();
    }

    public static void detectText(TextractClient client, String sourceImage) {
        SdkBytes sourceBytes = null;
        try {
            sourceBytes = SdkBytes.fromInputStream(new FileInputStream(sourceImage));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Document image = Document.builder().bytes(sourceBytes).build();

        DetectDocumentTextRequest request = DetectDocumentTextRequest.builder().document(image).build();

        DetectDocumentTextResponse response = client.detectDocumentText(request);
        List<Block> detections = response.blocks();

        List<Block> prices = findPrices(detections);
        List<Block> probables = findProbableLines(detections, prices);

        writeEverything(detections);
        writeDistances(detections, prices);

        writeToFile("coords.txt", prices, probables);

    }

    private static void writeToFile(String fileName, List<Block> prices, List<Block> probables) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))){
            String separator = "&";

            writer.write(prices.size() + "\n");

            for(Block p : prices) {
                writer.write(stringInFile(p, parsePrice(p.text()), separator));
            }

            for(Block p : probables) {
                writer.write(stringInFile(p, null, separator));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String stringInFile(Block t, String text, String separator) {
        Center c = getCenter(t.geometry().boundingBox());
        return new StringBuilder()
                .append(text != null ? text : t.text())
                .append(separator)
                .append(c.x)
                .append(separator)
                .append(c.y)
                .append("\n")
                .toString();
    }

    private static List<Block> findProbableLines(List<Block> detections, List<Block> prices) {
        List<Block> probables = new ArrayList<>();

        for (Block td : detections) {
            if(td.blockType() != BlockType.LINE) continue;
            if(prices.contains(td)) continue;

            boolean important = true;
            for(String f : falses)
                if(td.text().toLowerCase().contains(f))
                    important = false;

            if(!important) continue;

            double closestDistance = 1;
            for(Block center : prices)
                closestDistance = Math.min(closestDistance, distance(getCenter(td.geometry().boundingBox()), getCenter(center.geometry().boundingBox())));

            if (closestDistance > distanceThreshold) continue;

            probables.add(td);
        }

        return probables;
    }

    private static void writeDistances(List<Block> detections, List<Block> prices) {
        try {
            BufferedWriter distanceWriter = new BufferedWriter(new FileWriter(String.format("%sdistances-%s.txt", logs, imageName)));

            List<Block> closes = new ArrayList<>();
            for(Block td : detections) {
                if(prices.contains(td)) continue;
                double closestDistance = 1;
                for(Block center : prices)
                    closestDistance = Math.min(closestDistance, distance(getCenter(td.geometry().boundingBox()), getCenter(center.geometry().boundingBox())));

                distanceWriter.write(td.text() + " = " + closestDistance + "\n");

            }
            distanceWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Block> findPrices(List<Block> detections) {
        List<Block> prices = new ArrayList<>();

        for (int i = 0; i < detections.size(); i++) {
            Block b = detections.get(i);
            if(b.blockType() != BlockType.LINE) continue;

            if(b.geometry().boundingBox().height() > hLimit
                    && isPrice(b.text())) {
                prices.add(b);
            }

            if(i > 0 && b.text().trim().length() == 2) { //looking for bad text detection when price is separated into 2 blocks
                String text = b.text().trim();
                try {
                    Integer.parseInt(text);
                    Integer.parseInt(detections.get(i - 1).text().trim());
                    prices.add(detections.get(i - 1));
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return prices;
    }

    private static boolean isPrice(String price) {
        try {
            Integer.parseInt(parsePrice(price));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String parsePrice(String price) {
        return price.replace(",", "").replace(" ", "").replace("\"", "99");
    }

    private static double distance(Center c1, Center c2) {
        return Math.hypot(c1.x - c2.x, c1.y - c2.y);
    }

    private static Center getCenter(BoundingBox box) {
        return new Center(box.left() + box.width() / 2, box.top() + box.height() / 2);
    }

    private static void writeEverything(List<Block> detections) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%soutput_all-%s.txt", logs, imageName)));
            for(Block td : detections) {

                writer.write("\nDetected: " + td.text());
                writer.write("\nConfidence: " + td.confidence());
                writer.write("\nId : " + td.id());
                writer.write("\nType: " + td.blockType());
                BoundingBox box = td.geometry().boundingBox();
                writer.write("\nLeft + width: " + box.left() + " " + box.width());
                writer.write("\nTop + height: " + box.top() + " " + box.height());
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
