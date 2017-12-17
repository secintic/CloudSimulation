package Simulation;

public class Main {
    public static void main(String[] args) {
        int numberOfVm = 4;
        SimulationEngine sim = SimulationEngine.builder().numberOfVm(numberOfVm).energyConsumptionArray(new double[1000]).build();
        sim.run(1000, 30, 100, 0, "noConsolidation.txt");
        sim.run(1000, 30, 100, 1, "consolidate.txt");
        sim.run(1000, 30, 100, 2, "ftm.txt");
    }
}
