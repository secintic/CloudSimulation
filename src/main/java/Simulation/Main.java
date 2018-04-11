package Simulation;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {
        int numberOfVm = 4;
        int simulationDuration = 1000;
        int threshold = 50;
        String[] filenames = new String[]{"noConsolidation.csv", "consolidate.csv", "ftm.csv"};
        for (int i = 0; i < 3; i++) {
            SimulationEngine sim = SimulationEngine.builder().numberOfVm(numberOfVm).VmLimit(10).energyConsumptionArray(new double[simulationDuration]).
                    faultTimesAccordingToWeibullDist(new ArrayList<Integer>() {
                        {
                            add(49);
                            add(72);
                            add(88);
                            add(137);
                            add(157);
                            add(337);
                            add(364);
                            add(475);
                            add(733);
                            add(891);
                        }
                    }).build();
            sim.run(simulationDuration, threshold, i, filenames[i]);
        }

    }
}
