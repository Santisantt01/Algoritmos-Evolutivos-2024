package ae.paleta.colores;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;

public class ColorPaletteProblem extends AbstractIntegerProblem {
    BufferedImage image;
    int maxPaletteSize;
    int iter = 0;

    public ColorPaletteProblem(BufferedImage image, int maxPaletteSize) {
        this.image = image;
        this.maxPaletteSize = maxPaletteSize;

        // Each color has R, G, B, and an active flag
        setNumberOfVariables(maxPaletteSize * 4);
        setNumberOfObjectives(2);
        setNumberOfConstraints(1);
        setName("ImagePaletteProblem");

        List<Integer> lowerLimit = new ArrayList<>(getNumberOfVariables());
        List<Integer> upperLimit = new ArrayList<>(getNumberOfVariables());

        for (int i = 0; i < getNumberOfVariables(); i++) {
            if ((i + 1) % 4 == 0) {
                // Active flag: 0 or 1
                lowerLimit.add(0);
                upperLimit.add(1);
            } else {
                // Color components: 0 to 255
                lowerLimit.add(0);
                upperLimit.add(255);
            }
        }

        setVariableBounds(lowerLimit, upperLimit);
    }

    @Override
    public void evaluate(IntegerSolution solution) {
        List<Color> palette = extractPalette(solution);

        double distance = calculatePaletteDistance(palette);
        int paletteSize = palette.size();

        double maxPossibleDistance = calculateColorDistance(new Color(255,255,255), new Color(0,0,0));

        double normalizedDistance = distance / maxPossibleDistance;
        double normalizedPaletteSize = paletteSize / maxPaletteSize;

        solution.setObjective(0, normalizedDistance);
        solution.setObjective(1, normalizedPaletteSize);
        
        evaluateConstraints(solution);
        
        iter++;
        System.out.println(iter + ". Distance: " + distance + ", Palette Size: " + palette.size());
    }

    private List<Color> extractPalette(IntegerSolution solution) {
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

    private double calculatePaletteDistance(List<Color> palette) {
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

    private double calculateColorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
    
    public void evaluateConstraints(IntegerSolution solution) {
        List<Color> palette = extractPalette(solution);
        
        solution.setConstraint(0, palette.isEmpty() ? -1 : 1);
    }
    
    @Override
    public IntegerSolution createSolution() {
      return new DefaultIntegerSolution(bounds, getNumberOfObjectives(), getNumberOfConstraints());
    }
}