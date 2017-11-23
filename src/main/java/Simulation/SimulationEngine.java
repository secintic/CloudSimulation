package Simulation;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Builder
@Slf4j
class SimulationEngine {
    @Builder.Default
    private HashMap<UUID, Vm> Vms = new HashMap<>();
    private int numberOfVm;
    private List<Task> tasks;

    void run(int simulationDuration, int threshold) {
        createVms();
        tasks = ReadGoogleData.read();
        int taskCursor = 0;
        for (int simulationTime = 0; simulationTime < simulationDuration; simulationTime++) {
            log.info("Simulation time: " + simulationTime);

            //check start time of the tasks --> could be divided into new method
            while (taskCursor < tasks.size()) {
                if (tasks.get(taskCursor).getStartTime() <= simulationTime) {
                    Vm foundVm = findAppropriateVm(tasks.get(taskCursor));
                    if (foundVm != null)
                        foundVm.assignTask(tasks.get(taskCursor), false);
                    else {
                        createNewVm().assignTask(tasks.get(taskCursor), false);
                    }
                    taskCursor++;
                } else
                    break;
            }


            //check end time of the tasks --> could be divided into new method
            for (int j = 0; j < taskCursor && tasks.get(0).getEndTime() < simulationTime; j++) {
                if (tasks.get(j).getEndTime() < simulationTime) {
                    removeFromVm(tasks.get(j));
                    tasks.remove(j);
                    taskCursor--;
                    j--;
                }
            }
            Set<Vm> toBeConsolidated = new HashSet<>();
            for (Map.Entry<UUID, Vm> entry : Vms.entrySet()) {
                if (entry.getValue().getUtilization() < threshold && simulationTime > 500) {
                    toBeConsolidated.add(entry.getValue());
                }
            }
            for (Vm vm : toBeConsolidated) {
                consolidateVm(vm);
            }
            toBeConsolidated.clear();
            //printVmsTasks();
            printEnergyConsumption();
        }
    }

    private void printEnergyConsumption() {
        double energyConsumption = 0;
        for (Vm v : Vms.values()) {
            energyConsumption += 100 + v.getUtilization();
        }
        log.info("energy consumption: " + energyConsumption);
    }

    private void printVmsTasks() {
        Vms.forEach((key, value) -> log.info("VMID: " + key + " numberOfTask: " + value.getTasks().size() + "     utilization: " + value.getUtilization()));
    }

    private void createVms() {
        for (int i = 0; i < numberOfVm; i++) {
            UUID vmId = UUID.randomUUID();
            Vms.put(vmId, Vm.builder().VmId(vmId).capacity("CPU", 2.0).capacity("Memory", 2.0).build());
        }
    }

    //Check other vms before consolidation
    private void consolidateVm(Vm vm) {
        UUID vmId = vm.getVmId();
        log.info("Consolidating Vm#" + vmId);
        if (checkOtherVmsForMigration(vm)) {
            vm.getTasks().clear();
            Vms.remove(vmId);
            numberOfVm--;
            log.info("Consolidated Vm#" + vmId);
        } else
            log.info("Vm#" + vmId + "cannot be consolidated");
    }

    private boolean checkOtherVmsForMigration(Vm vm) {
        Map<UUID, Vm> assignableVmList = new HashMap<>();
        Map<UUID, String> assignedTasks = new HashMap<>();
        for (Task task : vm.getTasks().values()) {
            Vm foundVm = findMigrateVm(task, assignableVmList);
            if (foundVm == null) {
                return false;
            } else if (!foundVm.equals(vm)) {
                assignableVmList.put(foundVm.getVmId(), Vm.builder().capacities(foundVm.getCapacities()).usedResources(foundVm.getUsedResources()).tasks(foundVm.getTasks()).VmId(foundVm.getVmId()).build());
                assignableVmList.get(foundVm.getVmId()).assignTask(task, true);
                assignedTasks.put(foundVm.getVmId(), task.getTaskId());
            }
        }
        assignableVmList.forEach((key, value) -> value.getTasks().get(assignedTasks.get(key)).setVmId(key));
        assignableVmList.forEach((key, value) -> Vms.replace(key, value));
        return true;
    }

    private Vm createNewVm() {
        UUID vmId = UUID.randomUUID();
        numberOfVm++;
        Vms.put(vmId, Vm.builder().VmId(vmId)
                .capacity("CPU", 2.0).capacity("Memory", 2.0).build());
        return Vms.get(vmId);
    }

    private Vm findAppropriateVm(Task task) {
        for (Vm vm : Vms.values()) {
            if (vm.checkVmSpace(task)) {
                return vm;
            }
        }
        return null;
    }

    private Vm findMigrateVm(Task task, Map<UUID, Vm> pseudoList) {
        for (Vm vm : Vms.values()) {
            if (!vm.getVmId().equals(task.getVmId())) {
                if (pseudoList.containsKey(pseudoList.get(vm.getVmId()))) {
                    if (pseudoList.get(vm.getVmId()).checkVmSpace(task))
                        return pseudoList.get(vm.getVmId());
                } else {
                    if (vm.checkVmSpace(task)) {
                        return vm;
                    }
                }
            }
        }
        return null;
    }

    private void removeFromVm(Task task) {
        Vms.get(task.getVmId()).removeTask(task);
    }
}
