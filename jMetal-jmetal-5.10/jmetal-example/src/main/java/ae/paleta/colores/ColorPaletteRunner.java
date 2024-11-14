package ae.paleta.colores;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

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
    	BufferedImage image = ImageIO.read(ColorPaletteRunner.class.getResourceAsStream("test1.jpg"));
    	    	
        ColorPaletteProblem problem = new ColorPaletteProblem(image, 10);

        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(0.9, 20);
        MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(1 / problem.getNumberOfVariables(), 20);
        SelectionOperator<List<IntegerSolution>, IntegerSolution> selection = new BinaryTournamentSelection<>();

        Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation, 100)
                .setSelectionOperator(selection)
                .setMaxEvaluations(5000)
                .build();
        
        algorithm.run();
        
        IntegerSolution bestSolution = algorithm.getResult().get(0);
        List<Integer> variables = bestSolution.getVariables();
        int numComponents = 3; // R, G, B
        int numColors = (int) bestSolution.getObjective(1);

        System.out.println("Paleta de colores obtenida:");
        Color[] palette = new Color[numColors];
        for (int i = 0; i < numColors; i++) {
            int index = i * numComponents;
            int r = variables.get(index).intValue();
            int g = variables.get(index + 1).intValue();
            int b = variables.get(index + 2).intValue();
            Color color = new Color(r, g, b);
            palette[i] = color;
            System.out.println("Color " + (i + 1) + ": R=" + r + " G=" + g + " B=" + b);
        }
        
        int squareSize = 50;
        BufferedImage paletteImage = new BufferedImage(squareSize * numColors, squareSize, BufferedImage.TYPE_INT_RGB);
        Graphics g = paletteImage.getGraphics();
        for (int i = 0; i < numColors; i++) {
            g.setColor(palette[i]);
            g.fillRect(i * squareSize, 0, squareSize, squareSize);
        }
        g.dispose();

        File outputfile = new File("palette.png");
        ImageIO.write(paletteImage, "png", outputfile);

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(outputfile);
        }

        System.out.println("Objetivo 1 (Distancia): " + bestSolution.getObjective(0));
        System.out.println("Objetivo 2 (Tam): " + bestSolution.getObjective(1));
    }
   
}
