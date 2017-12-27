package Simulation;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int numberOfVm = 4;
        SimulationEngine sim = SimulationEngine.builder().numberOfVm(numberOfVm).energyConsumptionArray(new double[1000]).build();
        sim.run(1000, 30, 100, 0, "noConsolidation.csv");
        sim.run(1000, 30, 100, 1, "consolidate.csv");
        sim.run(1000, 30, 100, 2, "ftm.csv");
    }
}
