package ae.paleta.colores;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

public class ColorPaletteProblem extends AbstractIntegerProblem {
    private BufferedImage image;
    private int maxPaletteSize;
    private int numComponents = 3;
    

    public ColorPaletteProblem(BufferedImage image, int maxPaletteSize) {
        this.image = image;
        this.maxPaletteSize = maxPaletteSize;
        		
        setNumberOfVariables(maxPaletteSize * numComponents);
        setNumberOfObjectives(2);
        setName("ColorPaletteProblem");

        List<Integer> lowerLimit = new ArrayList<>(getNumberOfVariables());
        List<Integer> upperLimit = new ArrayList<>(getNumberOfVariables());

     	// Establecer límites entre 0 y 255 para variables RGB
        for (int i = 0; i < getNumberOfVariables(); i++) {
            lowerLimit.add(0);
            upperLimit.add(255);
        }

        setVariableBounds(lowerLimit, upperLimit);
    }

    @Override
    public void evaluate(IntegerSolution solution) {
        List<Integer> variables = solution.getVariables();
        List<Color> palette = new ArrayList<>();

        // Extraer la paleta de colores del solution
        for (int i = 0; i < maxPaletteSize; i++) {
            int index = i * numComponents;
            int r = variables.get(index).intValue();
            int g = variables.get(index + 1).intValue();
            int b = variables.get(index + 2).intValue();
            Color color = new Color(clamp(r), clamp(g), clamp(b));
            palette.add(color);
        }

        // Objetivo 1: Minimizar el Delta E total
        double totalDist = 0.0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                double minDist = Double.MAX_VALUE;
                for (Color paletteColor : palette) {
                    double dist = calculateColorDistance(pixelColor, paletteColor);
                    if (dist < minDist) {
                    	minDist = dist;
                    }
                }
                totalDist += minDist;
            }
        }

        // Objetivo 2: Minimizar el número de colores únicos en la paleta
        Set<Color> uniqueColors = new HashSet<>(palette);
        int numUniqueColors = uniqueColors.size();

        // Asignar los valores a los objetivos
        solution.setObjective(0, totalDist);
        solution.setObjective(1, numUniqueColors);
        
        System.out.println(totalDist);
        System.out.println(numUniqueColors);
        System.out.println(palette);
        System.out.println("-------");
    }

    // Función para asegurar que los valores RGB estén entre 0 y 255
    static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private double calculateColorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff); // Distancia Euclidiana
    }
}