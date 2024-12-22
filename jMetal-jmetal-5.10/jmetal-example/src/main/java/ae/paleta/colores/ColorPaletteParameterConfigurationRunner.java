package ae.paleta.colores;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.util.front.Front;
import org.uma.jmetal.util.front.impl.ArrayFront;

public class ColorPaletteParameterConfigurationRunner {

    private static final String RESULTS_FILE = "parameters_configurations_results_metrics_final.csv";
    private static final String[] TEST_IMAGES = {"test3.jpg", "test4.jpg", "test5.jpg"};
    private static final int MAX_WIDTH = 750;
    private static final int MAX_HEIGHT = 750;
    private static final int MAX_PALETTE_SIZE = 10;
    private static final double[] CROSSOVER_PROBABILITIES = {0.7};
    private static final double[] MUTATION_PROBABILITIES = {0.08};
    private static final int[] POPULATION_SIZES = {150};
    private static final int[] ITERATIONS = {10000};
    private static final int SEEDS = 30;

    public static void main(String[] args) throws IOException {
        List<ParameterCombination> parameterCombinations = ParameterCombination.generateParameterCombinations();

        for (String imageName : TEST_IMAGES) {
            BufferedImage image = ColorPaletteUtils.loadAndResizeImage(imageName, MAX_WIDTH, MAX_HEIGHT);

            for (ParameterCombination params : parameterCombinations) {
                List<List<IntegerSolution>> allSolutions = new ArrayList<>();
                
                System.out.println(imageName + ", Crossover: " + params.crossoverProbability + ", Mutation: " + params.mutationProbability + ", Population Size: " + params.populationSize + ", Iterations: " + params.iterations);

                for (int seed = 1; seed <= SEEDS; seed++) {
                    ColorPaletteProblem problem = new ColorPaletteProblem(image, MAX_PALETTE_SIZE, false);

                    CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(params.getCrossoverProbability(), 20);
                    MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(params.getMutationProbability(), 20);
                    SelectionOperator<List<IntegerSolution>, IntegerSolution> selection = new BinaryTournamentSelection<>(
                            new RankingAndCrowdingDistanceComparator<>());

                    ColorPaletteNSGAII algorithm = new ColorPaletteNSGAII(
                            problem,
                            params.getIterations(),
                            params.getPopulationSize(),
                            crossover,
                            mutation,
                            selection,
                            ColorPaletteNSGAII.InitialPopulationAlgorithm.KMEANS
                    );

                    algorithm.run();
                    allSolutions.add(ColorPaletteUtils.removeDuplicates(algorithm.getPopulation(), MAX_PALETTE_SIZE));

                    System.out.println(imageName + ", N: " + seed);
                }

                List<IntegerSolution> combinedSolutions = SolutionListUtils.getNonDominatedSolutions(ColorPaletteUtils.removeDuplicates(allSolutions.stream().flatMap(List::stream).collect(Collectors.toList()), MAX_PALETTE_SIZE));

                Front referenceFront = new ArrayFront(combinedSolutions);
                Spread<IntegerSolution> spreadIndicator = new Spread<>(referenceFront);
                GenerationalDistance<IntegerSolution> gdIndicator = new GenerationalDistance<>(referenceFront);
                PISAHypervolume<IntegerSolution> hypervolume = new PISAHypervolume<>(getReferencePoint(combinedSolutions));

                double realHV = hypervolume.evaluate(combinedSolutions);

                List<Double> spreads = new ArrayList<>();
                List<Double> generationalDistances = new ArrayList<>();
                List<Double> diffs = new ArrayList<>();

                KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
                NormalDistribution normalDist = new NormalDistribution();

                for (List<IntegerSolution> sols : allSolutions) {
                    spreads.add(spreadIndicator.evaluate(sols));
                    generationalDistances.add(gdIndicator.evaluate(sols));
                    double hv = hypervolume.evaluate(sols);
                    diffs.add(realHV - hv);
                }

                double meanDiffHV= diffs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double stdDevDiffHV = Math.sqrt(diffs.stream().mapToDouble(d -> Math.pow(d - meanDiffHV, 2)).average().orElse(0.0));

                double ksStatistic = ksTest.kolmogorovSmirnovStatistic(normalDist, diffs.stream().mapToDouble(Double::doubleValue).toArray());
                double ksPValue = ksTest.kolmogorovSmirnovTest(normalDist, diffs.stream().mapToDouble(Double::doubleValue).toArray());

                double meanSpread = spreads.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double stdDevSpread = Math.sqrt(spreads.stream().mapToDouble(s -> Math.pow(s - meanSpread, 2)).average().orElse(0.0));

                double meanGD = generationalDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double stdDevGD = Math.sqrt(generationalDistances.stream().mapToDouble(gd -> Math.pow(gd - meanGD, 2)).average().orElse(0.0));

                System.out.println(meanDiffHV + ", " + stdDevDiffHV + ", " + ksPValue + ", " + meanSpread
                        + ", " + stdDevSpread + ", " + meanGD + ", " + stdDevGD + ", " + combinedSolutions.size());

                saveResults(imageName, params, meanDiffHV, stdDevDiffHV, ksStatistic, ksPValue, meanSpread, stdDevSpread, meanGD, stdDevGD);
                saveParetoFront(imageName, params, combinedSolutions);
            }
        }
    }

    private static void saveResults(String imageName, ParameterCombination params, double meanDiff, double stdDevDiff,
            double ksStatistic, double ksPValue, double meanSpread, double stdSpread, double meanGD, double stdGD) {
        try (FileWriter writer = new FileWriter(RESULTS_FILE, true)) {
            File file = new File(RESULTS_FILE);
            if (file.length() == 0) {
                writer.write("Image,Crossover,Mutation,Population,Iterations,HVMean,HVStdDev,KS_Statistic,KS_PValue,SpreadMean,SpreadStdDev,GenerationalDistanceMean,GenerationalDistanceStdDev\n");
            }
            writer.write(String.format("%s,%.2f,%.2f,%d,%d,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f"
            		+ "\n",
                    imageName,
                    params.getCrossoverProbability(),
                    params.getMutationProbability(),
                    params.getPopulationSize(),
                    params.getIterations(),
                    meanDiff,
                    stdDevDiff,
                    ksStatistic,
                    ksPValue,
                    meanSpread,
                    stdSpread,
                    meanGD,
                    stdGD));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveParetoFront(String imageName, ParameterCombination params, List<IntegerSolution> paretoFront) throws IOException {
        String filename = String.format("pareto_front_%s_cossover_%.2f_mutation_%.2f_population_%d_iterations_%d.csv",
                imageName.replaceAll("\\.jpg$", ""),
                params.getCrossoverProbability(),
                params.getMutationProbability(),
                params.getPopulationSize(),
                params.getIterations());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("N,Distance,Palette Size,Palette\n");
            for(int i = 0; i < paretoFront.size(); i++) {
                IntegerSolution solution = paretoFront.get(i);
                List<Color> palette = ColorPaletteUtils.extractPalette(solution, MAX_PALETTE_SIZE);
                String paletteStr = palette.stream()
                                           .map(c -> String.format("R:%d G:%d B:%d", c.getRed(), c.getGreen(), c.getBlue()))
                                           .collect(Collectors.joining("; "));
                writer.write(String.format("%d,%.5f,%d,\"%s\"\n", i + 1, solution.getObjective(0), palette.size(), paletteStr));
            }
        }
    }

    private static double[] getReferencePoint(List<IntegerSolution> solutions) {
        if (solutions.isEmpty()) {
            return new double[]{0.0};
        }
        int numObjectives = solutions.get(0).getNumberOfObjectives();
        double[] reference = new double[numObjectives];
        for (IntegerSolution sol : solutions) {
            for (int i = 0; i < numObjectives; i++) {
                reference[i] = Math.max(reference[i], sol.getObjective(i));
            }
        }
        for (int i = 0; i < reference.length; i++) {
            reference[i] += 1.0;
        }
        return reference;
    }

    static class ParameterCombination {
        private final double crossoverProbability;
        private final double mutationProbability;
        private final int populationSize;
        private final int iterations;

        public ParameterCombination(double crossoverProbability, double mutationProbability, int populationSize, int iterations) {
            this.crossoverProbability = crossoverProbability;
            this.mutationProbability = mutationProbability;
            this.populationSize = populationSize;
            this.iterations = iterations;
        }

        public double getCrossoverProbability() {
            return crossoverProbability;
        }

        public double getMutationProbability() {
            return mutationProbability;
        }

        public int getPopulationSize() {
            return populationSize;
        }

        public int getIterations() {
            return iterations;
        }

        public static List<ParameterCombination> generateParameterCombinations() {
            List<ParameterCombination> combinations = new ArrayList<>();
            for (double pc : CROSSOVER_PROBABILITIES) {
                for (double pm : MUTATION_PROBABILITIES) {
                    for (int ps : POPULATION_SIZES) {
                        for (int it : ITERATIONS) {
                            combinations.add(new ParameterCombination(pc, pm, ps, it));
                        }
                    }
                }
            }
            return combinations;
        }
    }
}
