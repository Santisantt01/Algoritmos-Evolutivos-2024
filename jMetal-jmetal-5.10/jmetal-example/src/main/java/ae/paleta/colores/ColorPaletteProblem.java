package ae.paleta.colores;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;

public class ColorPaletteProblem extends AbstractIntegerProblem {
    BufferedImage image;
    int maxPaletteSize;
    int iter = 0;
    boolean withProgress = false;

    public ColorPaletteProblem(BufferedImage image, int maxPaletteSize, boolean withProgress) {
        this.image = image;
        this.maxPaletteSize = maxPaletteSize;
        this.withProgress = withProgress;

        setNumberOfVariables(maxPaletteSize * 4);
        setNumberOfObjectives(2);
        setNumberOfConstraints(1);
        setName("ImagePaletteProblem");

        List<Integer> lowerLimit = new ArrayList<>(getNumberOfVariables());
        List<Integer> upperLimit = new ArrayList<>(getNumberOfVariables());

        for (int i = 0; i < getNumberOfVariables(); i++) {
            if ((i + 1) % 4 == 0) {
                lowerLimit.add(0);
                upperLimit.add(1);
            } else {
                lowerLimit.add(0);
                upperLimit.add(255);
            }
        }

        setVariableBounds(lowerLimit, upperLimit);
    }

    @Override
    public void evaluate(IntegerSolution solution) {
        List<Color> palette = ColorPaletteUtils.extractPalette(solution, maxPaletteSize);

        double distance = ColorPaletteUtils.calculatePaletteDistance(image, palette);
        double paletteSize = palette.size();

        double maxPossibleDistance = ColorPaletteUtils.calculateColorDistance(new Color(255,255,255), new Color(0,0,0));

        double normalizedDistance = distance / maxPossibleDistance;
        double normalizedPaletteSize = paletteSize / maxPaletteSize;

        solution.setObjective(0, normalizedDistance);
        solution.setObjective(1, normalizedPaletteSize);
        
        evaluateConstraints(solution);
        
        if (withProgress) {
        	iter++;
        	System.out.println(iter + ". Distance: " + normalizedDistance + ", Palette Size: " + (int) paletteSize);
        }
    }
    
    public void evaluateConstraints(IntegerSolution solution) {
        List<Color> palette = ColorPaletteUtils.extractPalette(solution, maxPaletteSize);
        
        solution.setConstraint(0, palette.isEmpty() ? -1 : 1);
    }
    
    @Override
    public IntegerSolution createSolution() {
      return new DefaultIntegerSolution(bounds, getNumberOfObjectives(), getNumberOfConstraints());
    }
}