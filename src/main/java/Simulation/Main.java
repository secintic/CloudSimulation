package Simulation;

public class Main {
    public static void main(String[] args) {
        int numberOfVm = 4;
        SimulationEngine sim = SimulationEngine.builder().numberOfVm(numberOfVm).build();
        sim.run(1000,30,100,2);
    }
}
