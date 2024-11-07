package ae.paleta.colores;

import org.uma.jmetal.problem.impl.AbstractProblem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImagePaletteProblemRGB extends AbstractProblem<DefaultIntegerSolution> {
    private final BufferedImage image; // Imagen original
    private final int maxPaletteSize;  // Tamaño máximo permitido para la paleta

    public ImagePaletteProblemRGB(BufferedImage image, int maxPaletteSize) {
        this.image = image;
        this.maxPaletteSize = maxPaletteSize;

        setNumberOfVariables(maxPaletteSize * 3); // Cada color tiene 3 componentes (R, G, B)
        setNumberOfObjectives(2); // Dos objetivos: error de reconstrucción y tamaño efectivo de la paleta
        setNumberOfConstraints(0);
    }

    @Override
    public void evaluate(DefaultIntegerSolution solution) {
        List<Color> palette = extractPalette(solution);

        // Calcula el error de reconstrucción
        double reconstructionError = calculateReconstructionError(image, palette);

        // Calcula el tamaño efectivo de la paleta
        long uniqueColors = palette.stream().distinct().count();

        // Asigna los valores a los objetivos
        solution.setObjective(0, reconstructionError);
        solution.setObjective(1, uniqueColors);
    }

    private List<Color> extractPalette(DefaultIntegerSolution solution) {
        List<Color> palette = new ArrayList<>();
        for (int i = 0; i < solution.getNumberOfVariables(); i += 3) {
            int r = solution.getVariable(i);
            int g = solution.getVariable(i + 1);
            int b = solution.getVariable(i + 2);
            palette.add(new Color(r, g, b));
        }
        return palette;
    }

    private double calculateReconstructionError(BufferedImage image, List<Color> palette) {
        double error = 0.0;
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color originalColor = new Color(image.getRGB(x, y));
                Color closestColor = findClosestColor(originalColor, palette);
                error += calculateColorDistance(originalColor, closestColor);
            }
        }

        return error / (width * height); // Error promedio por píxel
    }

    private Color findClosestColor(Color color, List<Color> palette) {
        Color closestColor = palette.get(0);
        double minDistance = calculateColorDistance(color, closestColor);

        for (Color candidate : palette) {
            double distance = calculateColorDistance(color, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                closestColor = candidate;
            }
        }

        return closestColor;
    }

    private double calculateColorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff); // Distancia Euclidiana
    }

    @Override
    public DefaultIntegerSolution createSolution() {
        DefaultIntegerSolution solution = new DefaultIntegerSolution(this);
        for (int i = 0; i < getNumberOfVariables(); i++) {
            solution.setVariable(i, (int) (Math.random() * 256)); // Genera valores iniciales aleatorios entre 0 y 255
        }
        return solution;
    }
}
