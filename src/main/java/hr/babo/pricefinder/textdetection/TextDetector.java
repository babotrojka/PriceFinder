package hr.babo.pricefinder.textdetection;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.*;
import java.util.*;

public class TextDetector {
    private String imageName;
    private String imageFolder;

    private String logs = "log/";
    private String coordsFile = "coords/";

    private String separator = "&";

    private List<String> falses = new ArrayList<>(Arrays.asList("cijen", "niž", "ponud", "nov", "prilik"));

    /**
        Parameter for heigth limit for a price. Nothing below this is considered a price
     */
    private double hLimit = 0.025;

    /**
     * Parameter for distance a line can be from a price. If something is further than this threshold from a price, it isn't considered important
     */
    private double distanceThreshold = 0.18;


    public TextDetector(String imageFolder) {
        this.imageFolder = imageFolder;
    }

    public void textFromImages(String imageName) {
        this.imageName = imageName;

        Region region = Region.US_EAST_1;
        TextractClient texClient = TextractClient.builder()
                .region(region)
                .build();

        detectText(texClient, imageFolder + imageName);

        texClient.close();
    }

    private void detectText(TextractClient client, String sourceImage) {
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

        Map<Block, String> prices = findPrices(detections);
        List<Block> probables = findProbableLines(detections, prices);

        writeEverything(detections);
        //writeDistances(detections, prices);

        writeToFile(coordsFile, prices, probables);

    }

    private void writeToFile(String fileName, Map<Block, String> prices, List<Block> probables) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%scoords-%s.txt", coordsFile, imageName.replace(".jpg", ""))))){
            writer.write(prices.size() + "\n");

            for(Map.Entry<Block, String> me : prices.entrySet()) {
                writer.write(stringInFile(me.getKey(), me.getValue(), separator));
            }

            for(Block p : probables) {
                writer.write(stringInFile(p, null, separator));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String stringInFile(Block t, String text, String separator) {
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

    private List<Block> findProbableLines(List<Block> detections, Map<Block, String> prices) {
        List<Block> probables = new ArrayList<>();

        for (Block td : detections) {
            if(td.blockType() != BlockType.LINE) continue;
            if(prices.containsKey(td)) continue;

            boolean important = true;
            for(String f : falses)
                if(td.text().toLowerCase().contains(f))
                    important = false;

            if(!important) continue;

            double closestDistance = 1;
            for(Block center : prices.keySet())
                closestDistance = Math.min(closestDistance, distance(getCenter(td.geometry().boundingBox()), getCenter(center.geometry().boundingBox())));

            if (closestDistance > distanceThreshold) continue;

            probables.add(td);
        }

        return probables;
    }

    private void writeDistances(List<Block> detections, Map<Block, String> prices) {
        try {
            BufferedWriter distanceWriter = new BufferedWriter(new FileWriter(String.format("%sdistances-%s.txt", logs, imageName.replace(".jpg", ""))));

            for(Block td : detections) {
                if(prices.containsKey(td)) continue;
                double closestDistance = 1;
                for(Block center : prices.keySet())
                    closestDistance = Math.min(closestDistance, distance(getCenter(td.geometry().boundingBox()), getCenter(center.geometry().boundingBox())));

                distanceWriter.write(td.text() + " = " + closestDistance + "\n");
            }
            distanceWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a map of pairs representing prices in catalogue
     * @param detections
     * @return
     */
    private Map<Block, String> findPrices(List<Block> detections) {
        Map<Block, String> prices = new HashMap<>();

        for (int i = 0; i < detections.size(); i++) {
            Block b = detections.get(i);
            if(b.blockType() != BlockType.LINE) continue;

            if(b.geometry().boundingBox().height() > hLimit
                    && isPrice(b.text())) {
                prices.put(b, makePrice(b.text().trim()));
            }

            if(i > 0 && !prices.containsKey(detections.get(i  -1)) && b.text().trim().length() == 2) { //looking for bad text detection when price is separated into 2 blocks
                String text = b.text().trim();
                try {
                    Integer.parseInt(text);
                    Integer.parseInt(detections.get(i - 1).text().trim());
                    prices.put(detections.get(i - 1), makePrice(detections.get(i - 1).text().trim() + text));
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return prices;
    }

    private String makePrice(String price) {
        price = parsePrice(price);
        if(price.length() < 3) price += "99";

        return price.substring(0, price.length() - 2) + "," + price.substring(price.length() - 2);

    }

    private boolean isPrice(String price) {
        try {
            Integer.parseInt(parsePrice(price));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String parsePrice(String price) {
        return price //errors in ocr
                .replace(".", "")
                .replace(",", "")
                .replace(" ", "")
                .replace("\"", "99")
                .replace("°", "9");
    }

    private double distance(Center c1, Center c2) {
        return Math.hypot(c1.x - c2.x, c1.y - c2.y);
    }

    private Center getCenter(BoundingBox box) {
        return new Center(box.left() + box.width() / 2, box.top() + box.height() / 2);
    }

    private void writeEverything(List<Block> detections) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%soutput_all-%s.txt", logs, imageName.replace(".jpg", ""))));
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
