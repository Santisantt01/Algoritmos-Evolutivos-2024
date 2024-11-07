package ae.paleta.colores;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.impl.SBXIntegerCrossover;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class MainRGB {
    public static void main(String[] args) throws Exception {
        // Cargar la imagen
        BufferedImage image = ImageIO.read(new File("path/to/image.jpg"));

        // Crear el problema
        ImagePaletteProblemRGB problem = new ImagePaletteProblemRGB(image, 16); // Paleta con máximo 16 colores

        // Configurar operadores genéticos
        var crossover = new SBXIntegerCrossover(0.9, 20.0); // Crossover basado en variables enteras
        var mutation = new IntegerPolynomialMutation(1.0 / problem.getNumberOfVariables(), 20.0);

        // Configurar el algoritmo
        Algorithm<List<DefaultIntegerSolution>> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation)
                .setSelectionOperator(new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>()))
                .setMaxEvaluations(25000)
                .setPopulationSize(100)
                .build();

        // Ejecutar el algoritmo
        algorithm.run();

        // Obtener resultados
        List<DefaultIntegerSolution> result = algorithm.getResult();
        result.forEach(solution -> {
            System.out.println("Error de reconstrucción: " + solution.getObjective(0));
            System.out.println("Tamaño efectivo de la paleta: " + solution.getObjective(1));
        });
    }
}
