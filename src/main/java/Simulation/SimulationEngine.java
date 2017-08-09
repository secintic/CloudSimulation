package Simulation;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public class SimulationEngine {
    @Builder.Default
    List<Vm> Vms = new ArrayList<>();
    int numberOfVm;
    List<Task> tasks;

    public void run(int simulationDuration) {
        createVms();
        tasks = ReadGoogleData.read();
        int taskCursor = 0;
        for (int simulationTime = 0; simulationTime < simulationDuration; simulationTime++) {
            System.out.println("Simulation time: " + simulationTime);
            while (taskCursor < tasks.size()) {
                if (tasks.get(taskCursor).startTime <= simulationTime) {
                    assignToVm(taskCursor);
                    taskCursor++;
                } else
                    break;
            }
            for (int j = 0; j < taskCursor && tasks.get(0).endTime < simulationTime; j++) {
                if (tasks.get(j).endTime < simulationTime) {
                    removeFromVm(j);
                    tasks.remove(j);
                    taskCursor--;
                    j--;
                }
            }
            printVmsTasks();
        }
    }

    private void printVmsTasks() {
        for (Vm v : Vms)
            System.out.print(v.getTasks().size() + "     ");
        System.out.println();
    }

    private void createVms() {
        for (int i = 0; i < numberOfVm; i++) {
            Vms.add(Vm.builder().VmId(i).capacity("CPU", 2.0).capacity("Memory", 2.0).build());
        }
    }

    private void assignToVm(int taskId) {
        Vms.get(taskId % 4).assignTask(tasks.get(taskId)); //RR algorithm
    }

    private void removeFromVm(int taskId) {
        Vms.get(tasks.get(taskId).vmId).removeTask(tasks.get(taskId));
    }
}
