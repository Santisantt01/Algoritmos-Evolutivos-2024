package ae.paleta.colores;

import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import smile.clustering.GMeans;
import smile.clustering.KMeans;

public class ColorPaletteNSGAII extends NSGAII<IntegerSolution> {
    enum InitialPopulationAlgorithm {
        DEFAULT,
        KMEANS,
        GMEANS
    }

    private InitialPopulationAlgorithm initialPopulationAlgorithm;

    public ColorPaletteNSGAII(
        ColorPaletteProblem problem,
        int maxEvaluations,
        int populationSize,
        CrossoverOperator<IntegerSolution> crossoverOperator,
        MutationOperator<IntegerSolution> mutationOperator,
        SelectionOperator<List<IntegerSolution>, IntegerSolution> selectionOperator,
        InitialPopulationAlgorithm initialPopulationAlgorithm
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
            new SequentialSolutionListEvaluator<>()
        );
        this.initialPopulationAlgorithm = initialPopulationAlgorithm;
    }
    
    private double[][] extractPixels(BufferedImage image) {
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


    @Override
    protected List<IntegerSolution> createInitialPopulation() {
        if (initialPopulationAlgorithm == InitialPopulationAlgorithm.DEFAULT) {
            return super.createInitialPopulation();
        } else {
            List<IntegerSolution> initialPopulation = new ArrayList<>(getMaxPopulationSize());
            BufferedImage image = ((ColorPaletteProblem) getProblem()).image;
            int maxK = ((ColorPaletteProblem) getProblem()).maxPaletteSize;

            double[][] pixelArray = extractPixels(image);

            double[][] centroids;
            if (initialPopulationAlgorithm == InitialPopulationAlgorithm.GMEANS) {
                centroids = GMeans.fit(pixelArray, maxK).centroids;
            } else {
                centroids = KMeans.fit(pixelArray, maxK).centroids;
            }

            for (double[] centroid : centroids) {
                IntegerSolution solution = getProblem().createSolution();
                solution.setVariable(0, (int) centroid[0]);
                solution.setVariable(1, (int) centroid[1]);
                solution.setVariable(2, (int) centroid[2]);
                solution.setVariable(3, 1); // Active flag
                initialPopulation.add(solution);
            }
           
            return initialPopulation;
        }
    }
    
    @Override 
    protected boolean isStoppingConditionReached() {
        // Check if the algorithm is stuck in a local optimum
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

    public static void main(String[] args) {
        try {
            String imageName = "test1";
            BufferedImage image = ImageIO.read(ColorPaletteNSGAII.class.getResourceAsStream(imageName + ".jpg"));

            int maxPaletteSize = 10;
            ColorPaletteProblem problem = new ColorPaletteProblem(image, maxPaletteSize);

            CrossoverOperator<IntegerSolution> crossoverOperator = new IntegerSBXCrossover(0.8, 20);
            MutationOperator<IntegerSolution> mutationOperator = new IntegerPolynomialMutation(1.0 / problem.getNumberOfVariables(), 20);
            SelectionOperator<List<IntegerSolution>, IntegerSolution> selectionOperator = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

            ColorPaletteNSGAII algorithm = new ColorPaletteNSGAII(
                problem,
                5000,
                50,
                crossoverOperator,
                mutationOperator,
                selectionOperator,
                InitialPopulationAlgorithm.DEFAULT
            );

            long startTime = System.currentTimeMillis();
            algorithm.run();
            long endTime = System.currentTimeMillis();

            List<IntegerSolution> population = algorithm.getResult();
            IntegerSolution bestSolution = population.get(0);

            List<Integer> variables = bestSolution.getVariables();
            int numComponents = 4; // R, G, B, Active flag

            System.out.println("Generated color palette:");
            List<Color> palette = new ArrayList<>();
            for (int i = 0; i < variables.size(); i += numComponents) {
                int r = variables.get(i);
                int g = variables.get(i + 1);
                int b = variables.get(i + 2);
                int active = variables.get(i + 3);

                if (active == 1) {
                    Color color = new Color(r, g, b);
                    palette.add(color);
                    System.out.println("Color: R=" + r + ", G=" + g + ", B=" + b);
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

            System.out.println("Objective 1 (Distance): " + bestSolution.getObjective(0));
            System.out.println("Objective 2 (Palette size): " + palette.size());

            long executionTime = endTime - startTime;
            System.out.println("Total execution time: " + (executionTime / 1000.0) + " seconds");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
