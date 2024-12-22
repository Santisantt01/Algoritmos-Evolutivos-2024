package ae.paleta.colores;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

public class ColorPaletteRunner {
    public static void main(String[] args) throws Exception {
    	String imageName = "hola";
        BufferedImage image = ImageIO.read(ColorPaletteRunner.class.getResourceAsStream(imageName + ".jpg"));

        int maxPaletteSize = 10;
        ColorPaletteProblem problem = new ColorPaletteProblem(image, maxPaletteSize, true);

        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(0.8, 20);
        MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(0.03, 20);
        SelectionOperator<List<IntegerSolution>, IntegerSolution> selection = new BinaryTournamentSelection<>();

        Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation, 50)
                .setSelectionOperator(selection)
                .setMaxEvaluations(5000)
                .build();

        algorithm.run();

        List<IntegerSolution> population = algorithm.getResult();
        IntegerSolution bestSolution = population.get(0);

        List<Integer> variables = bestSolution.getVariables();
        int numComponents = 4; // R, G, B, Active flag

        System.out.println("Paleta de colores obtenida:");
        List<Color> palette = new ArrayList<>();
        for (int i = 0; i < variables.size(); i += numComponents) {
            int r = variables.get(i);
            int g = variables.get(i + 1);
            int b = variables.get(i + 2);
            int active = variables.get(i + 3);

            if (active == 1) {
                Color color = new Color(r, g, b);
                palette.add(color);
                System.out.println("Color: R=" + r + " G=" + g + " B=" + b);
            }
        }

        int squareSize = 50;
        BufferedImage paletteImage = new BufferedImage(squareSize * palette.size(), squareSize, BufferedImage.TYPE_INT_RGB);
        Graphics g = paletteImage.getGraphics();
        for (int i = 0; i < palette.size(); i++) {
            g.setColor(palette.get(i));
            g.fillRect(i * squareSize, 0, squareSize, squareSize);
        }
        g.dispose();

        File outputfile = new File(imageName + "_palette.png");
        ImageIO.write(paletteImage, "png", outputfile);

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(outputfile);
        }

        System.out.println("Objetivo 1 (Distancia): " + bestSolution.getObjective(0));
        System.out.println("Objetivo 2 (Tamaño de paleta): " + palette.size());
    }
}