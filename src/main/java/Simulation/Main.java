package Simulation;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int numberOfVm = 4;
        int simulationDuration = 1000;
        int threshold = 50;
        int errorFreq = 50;

        // ReadGoogleData.generatePoisson(simulationDuration);
      //  for (threshold = 10; threshold < 100; threshold = threshold + 10) {
            String[] filenames = new String[]{"noConsolidation.csv", "consolidate.csv", "ftm.csv"};
            for (int i = 0; i < 3; i++) {
                SimulationEngine sim = SimulationEngine.builder().numberOfVm(numberOfVm).energyConsumptionArray(new double[simulationDuration]).build();
                sim.run(simulationDuration, threshold, errorFreq, i, filenames[i]);
            }
     //   }
    }
}
