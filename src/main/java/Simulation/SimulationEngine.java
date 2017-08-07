package Simulation;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public class SimulationEngine {
    @Builder.Default
    List<Vm> Vms = new ArrayList<>();
    int numberOfVm;

    public void run() {
        createVms();
        List<Task> tasks = ReadGoogleData.read();
        System.out.println(tasks);
    }

    private void createVms() {
        for (int i = 0; i < numberOfVm; i++) {
            Vms.add(Vm.builder().VmId(i).capacity("CPU", 2.0).capacity("Memory", 2.0).build());
        }
    }
}
