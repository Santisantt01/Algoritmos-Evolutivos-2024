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

import java.awt.image.BufferedImage;
import java.util.*;

import smile.clustering.KMeans;

public class ColorPaletteNSGAII extends NSGAII<IntegerSolution> {
    enum InitialPopulationAlgorithm {
        DEFAULT,
        KMEANS
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
    
    @Override
    protected List<IntegerSolution> createInitialPopulation() {
        if (initialPopulationAlgorithm == InitialPopulationAlgorithm.DEFAULT) {
            return super.createInitialPopulation();
        } else {
            List<IntegerSolution> initialPopulation = new ArrayList<>(getMaxPopulationSize());
            ColorPaletteProblem problem = (ColorPaletteProblem) getProblem();
            BufferedImage image = problem.image;
            int maxK = problem.maxPaletteSize;

            double[][] pixels = ColorPaletteUtils.extractPixels(image);
            double[][] centroids = KMeans.fit(pixels, maxK).centroids;

            for (double[] centroid : centroids) {
                IntegerSolution solution = problem.createSolution();
                solution.setVariable(0, (int) centroid[0]);
                solution.setVariable(1, (int) centroid[1]);
                solution.setVariable(2, (int) centroid[2]);
                solution.setVariable(3, 1);
                initialPopulation.add(solution);
            }
           
            return initialPopulation;
        }
    }

    public static void main(String[] args) {
        String imageName = "test9.jpg";
        int maxWidth = 750;
        int maxHeight = 750;
        int maxPaletteSize = 10;

        try {
            BufferedImage image = ColorPaletteUtils.loadAndResizeImage(imageName, maxWidth, maxHeight);
            ColorPaletteProblem problem = new ColorPaletteProblem(image, maxPaletteSize, true);
            
            CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(0.7, 20);
            MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(0.08, 20);
            SelectionOperator<List<IntegerSolution>, IntegerSolution> selection = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

            ColorPaletteNSGAII algorithm = new ColorPaletteNSGAII(
                problem,
                10000,
                150,
                crossover,
                mutation,
                selection,
                InitialPopulationAlgorithm.KMEANS
            );

            long startTime = System.currentTimeMillis();
            algorithm.run();
            long endTime = System.currentTimeMillis();
            List<IntegerSolution> population = algorithm.getResult();

            List<IntegerSolution> uniquePopulation = ColorPaletteUtils.removeDuplicates(population, maxPaletteSize);
            
            imageName = imageName.substring(0, imageName.lastIndexOf("."));

            ColorPaletteUtils.createOutputDirectory(imageName);
            ColorPaletteUtils.createPalettesImages(uniquePopulation, imageName, maxPaletteSize, true, true);
            ColorPaletteUtils.writeCSV(uniquePopulation, imageName, maxPaletteSize, true);

            System.out.printf("\nExecution time: %.2f seconds \n", (endTime - startTime) / 1000.0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
