package Simulation;

import lombok.Builder;

import java.util.*;

@Builder
public class SimulationEngine {
    @Builder.Default
    private Map<UUID, Vm> Vms = new HashMap<>();
    private int numberOfVm;
    private List<Task> tasks;

    public void run(int simulationDuration, int threshold) {
        createVms();
        tasks = ReadGoogleData.read();
        int taskCursor = 0;
        for (int simulationTime = 0; simulationTime < simulationDuration; simulationTime++) {
            System.out.println("Simulation time: " + simulationTime);

            //check start time of the tasks --> could be divided into new method
            while (taskCursor < tasks.size()) {
                if (tasks.get(taskCursor).getStartTime() <= simulationTime) {
                    if (findAppropriateVm(tasks.get(taskCursor)) != null)
                        assignToVm(tasks.get(taskCursor), findAppropriateVm(tasks.get(taskCursor)));
                    else {
                        assignToVm(tasks.get(taskCursor), createNewVm());
                    }
                    taskCursor++;
                } else
                    break;
            }


            //check end time of the tasks --> could be divided into new method
            for (int j = 0; j < taskCursor && tasks.get(0).getEndTime() < simulationTime; j++) {
                if (tasks.get(j).getEndTime() < simulationTime) {
                    removeFromVm(j);
                    tasks.remove(j);
                    taskCursor--;
                    j--;
                }
            }
            Set<Object> toBeConsolidated = new HashSet<>();
            for (Object vmId : Vms.keySet()) {
                if (Vms.get(vmId).getUtilization() < threshold && simulationTime > 500 && (tasks.size() / 10) < Vms.size()) {
                    toBeConsolidated.add(vmId);
                }
            }
            for (Object vmId : toBeConsolidated) {
                consolidateVm(vmId);
            }
            toBeConsolidated.clear();
            printVmsTasks();
        }
    }

    private void printVmsTasks() {
        Vms.forEach((key, value) -> System.out.print(value.getTasks().size() + ":" + value.getUtilization() + "     "));
        System.out.println();
    }

    private void createVms() {
        for (int i = 0; i < numberOfVm; i++) {
            UUID vmId = UUID.randomUUID();
            Vms.put(vmId, Vm.builder().VmId(vmId).capacity("CPU", 2.0).capacity("Memory", 2.0).isRunning(true).build());
        }
    }

    //Check other vms before consolidation
    private void consolidateVm(Object vmId) {
        System.out.println("Consolidating Vm#" + vmId);
        Vms.get(vmId).setRunning(false);
        Vms.get(vmId).getTasks().forEach((taskId, task) -> assignToVm(task, findAppropriateVm(task)));
        Vms.get(vmId).getTasks().clear();
        Vms.remove(vmId);
        numberOfVm = Vms.size();
        System.out.println("Consolidated Vm#" + vmId);
    }

    private Object createNewVm() {
        UUID vmId = UUID.randomUUID();
        System.out.println("Creating New Vm" + vmId);
        Vms.put(vmId, Vm.builder().VmId(vmId).capacity("CPU", 2.0).capacity("Memory", 2.0).isRunning(true).build());
        numberOfVm = Vms.size();
        return vmId;
    }

    private Object findAppropriateVm(Task task) {
        for (Object vmId : Vms.keySet()) {
            if (Vms.get(vmId).checkVmSpace(task)) {
                return vmId;
            }
        }
        return null;
    }

    private void assignToVm(Task task, Object vmId) {
        System.out.println("task#" + task.getTaskId() + " assigned to " + vmId);
        Vms.get(vmId).assignTask(task); //RR algorithm
    }

    private void removeFromVm(int taskId) {
        Vms.get(tasks.get(taskId).getVmId()).removeTask(tasks.get(taskId));
    }
}
