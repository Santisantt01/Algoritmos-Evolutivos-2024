package ae.paleta.colores;

import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class ColorPaletteNSGAII extends NSGAII<IntegerSolution> {

    public ColorPaletteNSGAII(
		ColorPaletteProblem problem,
		int maxEvaluations,
		CrossoverOperator<IntegerSolution> crossoverOperator,
	    MutationOperator<IntegerSolution> mutationOperator,
	    SelectionOperator<List<IntegerSolution>, IntegerSolution> selectionOperator,
		int populationSize
    ) {	
        super(
        	problem,
        	maxEvaluations, 
        	populationSize, 
        	populationSize, 
        	populationSize, 
        	crossoverOperator,
        	mutationOperator, 
        	selectionOperator, 
        	new SequentialSolutionListEvaluator<IntegerSolution>()
        );
    }

    @Override
    protected List<IntegerSolution> createInitialPopulation() {
        return super.createInitialPopulation(); // Default behavior
    }
    
    // @Override 
    // protected boolean isStoppingConditionReached() {
    //     return super.isStoppingConditionReached(); // Default behavior
    // }

    @Override 
    protected boolean isStoppingConditionReached() {
        int stagnationThreshold = 50; // Number of generations to check for stagnation
        double epsilon = 1e-4; // Small value to determine if the fitness values are considered equal
        if (evaluations >= stagnationThreshold) {
            List<Double> bestFitnessValues = new ArrayList<>();
            for (int i = 0; i < getPopulation().size(); i++) {
                bestFitnessValues.add(getPopulation().get(i).getObjective(0));
            }

            double averageFitness = bestFitnessValues.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            boolean isStuck = true;
            for (double fitness : bestFitnessValues) {
                if (Math.abs(fitness - averageFitness) > epsilon) {
                    isStuck = false;
                    break;
                }
            }

            if (isStuck) {
                return true;
            }
        }

        return super.isStoppingConditionReached();
    }

    public static void main(String[] args) throws Exception {
    	String imageName = "test1";
        BufferedImage image = ImageIO.read(ColorPaletteRunner.class.getResourceAsStream(imageName + ".jpg"));

        int maxPaletteSize = 10;
        ColorPaletteProblem problem = new ColorPaletteProblem(image, maxPaletteSize);
        
        // Create custom initial population
        // // Perform K-Means clustering to generate initial population
        // int k = 50; // Number of clusters
        // KMeansClustering kMeans = new KMeansClustering(image, k);
        // List<IntegerSolution> initialPopulation = kMeans.generateInitialPopulation(problem);

        // // Set the initial population in the algorithm
        // algorithm.setInitialPopulation(initialPopulation);

        CrossoverOperator<IntegerSolution> crossoverOperator = new IntegerSBXCrossover(0.8, 20);
        MutationOperator<IntegerSolution> mutationOperator = new IntegerPolynomialMutation(1.0 / problem.getNumberOfVariables(), 20);
        SelectionOperator<List<IntegerSolution>, IntegerSolution> selectionOperator = new BinaryTournamentSelection<IntegerSolution>(new RankingAndCrowdingDistanceComparator<IntegerSolution>());

        ColorPaletteNSGAII algorithm = new ColorPaletteNSGAII(
            problem,
            5000,
            crossoverOperator,
            mutationOperator,
            selectionOperator,
            50
        );

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
