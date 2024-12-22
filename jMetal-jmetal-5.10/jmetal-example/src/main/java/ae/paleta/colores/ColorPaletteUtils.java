package ae.paleta.colores;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.uma.jmetal.solution.integersolution.IntegerSolution;

public class ColorPaletteUtils {
	
    static List<Color> extractPalette(IntegerSolution solution, int maxPaletteSize) {
        List<Color> palette = new ArrayList<>();
        for (int i = 0; i < maxPaletteSize * 4; i += 4) {
            int r = solution.getVariable(i);
            int g = solution.getVariable(i + 1);
            int b = solution.getVariable(i + 2);
            int active = solution.getVariable(i + 3);

            if (active == 1) {
                palette.add(new Color(r, g, b));
            }
        }
        return palette;
    }
    
    static double calculatePaletteDistance(BufferedImage image, List<Color> palette) {
        if (palette.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double distTotal = 0.0;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                double minDist = Double.MAX_VALUE;

                for (Color paletteColor : palette) {
                    double dist = calculateColorDistance(paletteColor, pixelColor);
                    minDist = Math.min(minDist, dist);
                }
                distTotal += minDist;
            }
        }

        return distTotal / (image.getWidth() * image.getHeight());
    }

    static double calculateColorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
    
    static double[][] extractPixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] pixels = new double[width * height][3];

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                pixels[index][0] = color.getRed();
                pixels[index][1] = color.getGreen();
                pixels[index][2] = color.getBlue();
                index++;
            }
        }
        return pixels;
    }
    
    static BufferedImage loadAndResizeImage(String imageName, int maxWidth, int maxHeight) throws IOException {
        BufferedImage image = ImageIO.read(ColorPaletteNSGAII.class.getResourceAsStream(imageName));
        if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
            double scale = Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());
            int newWidth = (int) (image.getWidth() * scale);
            int newHeight = (int) (image.getHeight() * scale);
            BufferedImage resized = new BufferedImage(newWidth, newHeight, image.getType());
            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            image = resized;
        }
        return image;
    }

    static List<IntegerSolution> removeDuplicates(List<IntegerSolution> population, int maxPaletteSize) {
        List<IntegerSolution> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (IntegerSolution solution : population) {
            List<Color> palette = ColorPaletteUtils.extractPalette(solution, maxPaletteSize);
            String key = palette.stream()
                                .map(c -> c.getRed() + "-" + c.getGreen() + "-" + c.getBlue())
                                .collect(Collectors.joining(","));
            if (seen.add(key)) {
                unique.add(solution);
            }
        }
        return unique;
    }

    static void createOutputDirectory(String imageName) {
        File dir = new File(imageName);
        if (dir.exists()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                file.delete();
            }
            dir.delete();
        }
        dir.mkdirs();
    }

    static void createPalettesImages(List<IntegerSolution> population, String outputDir, int maxPaletteSize, boolean printPalettes, boolean openImages) throws IOException {
        int squareSize = 50;
        for(int i = 0; i < population.size(); i++) {
            IntegerSolution solution = population.get(i);    
            List<Color> palette = extractPalette(solution, maxPaletteSize);
            
            if (printPalettes) {
            	System.out.printf("%d. Distance: %.4f, Palette size: %d%n", i + 1, solution.getObjective(0), palette.size());
            	palette.forEach(c -> System.out.printf("Color: R=%d, G=%d, B=%d%n", c.getRed(), c.getGreen(), c.getBlue()));
            }

            BufferedImage paletteImage = new BufferedImage(squareSize * palette.size(), squareSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = paletteImage.createGraphics();
            for (int j = 0; j < palette.size(); j++) {
                g.setColor(palette.get(j));
                g.fillRect(j * squareSize, 0, squareSize, squareSize);
            }
            g.dispose();

            File image = new File(outputDir, "palette" + (i + 1) + ".png");
            ImageIO.write(paletteImage, "png", image);
            
            if (openImages && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(image);
            }
        }
    }

    static void writeCSV(List<IntegerSolution> population, String outputDir, int maxPaletteSize, boolean openFile) throws IOException {
        File csvFile = new File(outputDir, "results.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            writer.write("N,Distance,Palette Size,Palette\n");
            for(int i = 0; i < population.size(); i++) {
                IntegerSolution solution = population.get(i);
                List<Color> palette = extractPalette(solution, maxPaletteSize);
                String paletteStr = palette.stream()
                                           .map(c -> String.format("R:%d G:%d B:%d", c.getRed(), c.getGreen(), c.getBlue()))
                                           .collect(Collectors.joining("; "));
                writer.write(String.format("%d,%.5f,%d,\"%s\"\n", i + 1, solution.getObjective(0), palette.size(), paletteStr));
            }
        }
        
        if (openFile && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(csvFile);
        }

    }
    
}
