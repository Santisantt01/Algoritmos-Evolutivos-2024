package ae.paleta.colores;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

public class ColorPaletteProblem extends AbstractIntegerProblem {
    private BufferedImage image;
    private int iter = 0;
    private int maxPaletteSize;
    
    public ColorPaletteProblem(BufferedImage image, int maxPaletteSize) {
        this.image = image;
        this.maxPaletteSize = maxPaletteSize;
        
        setNumberOfVariables(maxPaletteSize * 3);
        setNumberOfObjectives(2);
        setName("ImagePaletteProblem");

        List<Integer> lowerLimit = new ArrayList<>(getNumberOfVariables());
        List<Integer> upperLimit = new ArrayList<>(getNumberOfVariables());

        for (int i = 0; i < getNumberOfVariables(); i++) {
            lowerLimit.add(0);
            upperLimit.add(255);
        }

        setVariableBounds(lowerLimit, upperLimit);
    }

    @Override
    public void evaluate(IntegerSolution solution) {
    	List<Color> palette = extractPalette(solution, maxPaletteSize);
    	
        List<Color> filteredPalette = filterPalette(palette);

        double distance = calculatePaletteDistance(filteredPalette);
        int paletteSize = filteredPalette.size();

        paletteSize = Math.max(paletteSize, 1);

        solution.setObjective(0, distance);
        solution.setObjective(1, paletteSize);
        
        iter++;
        System.out.println(iter + ". " + paletteSize + " " + distance);
    }
    
    private List<Color> filterPalette(List<Color> palette) {
        List<Color> uniquePalette = new ArrayList<>();

        for (Color color : palette) {
            boolean isUnique = true;
            for (Color uniqueColor : uniquePalette) {
                if (calculateColorDistance(uniqueColor, color) < 25) {
                    isUnique = false;
                    break;
                }
            }
            if (isUnique) {
                uniquePalette.add(color);
            }
        }

        return uniquePalette;
    }
    
    private List<Color> extractPalette(IntegerSolution solution, int paletteSize) {
        List<Color> palette = new ArrayList<>();
        for (int i = 0; i < paletteSize * 3; i += 3) {
            int r = solution.getVariable(i);
            int g = solution.getVariable(i + 1);
            int b = solution.getVariable(i + 2);
            palette.add(new Color(r, g, b));
        }
        return palette;
    }
   
     private double calculatePaletteDistance(List<Color> palette) {
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

}