package Simulation;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

@Builder
@Slf4j
class SimulationEngine {
    @Builder.Default
    private HashMap<UUID, Vm> Vms = new HashMap<>();
    private int numberOfVm;
    private List<Task> tasks;
    private double[] energyConsumptionArray;

    void run(int simulationDuration, int threshold, int errorFreq, int experiment, String fileName) {
        createVms();
        tasks = ReadGoogleData.read();
        int taskCursor = 0;
        for (int simulationTime = 0; simulationTime < simulationDuration; simulationTime++) {
            log.info("Simulation time: " + simulationTime);
            taskCursor = assignNewTask(taskCursor, simulationTime);
            taskCursor = removeDoneTask(taskCursor, simulationTime);
            checkVmFault(simulationTime, errorFreq);
            switch (experiment) {
                case 0:
                    break;
                case 1:
                    consolidateLowUtilVms(threshold, simulationTime, false);
                    break;
                case 2:
                    consolidateLowUtilVms(threshold, simulationTime, true);
                    break;
            }
            energyConsumptionArray[simulationTime] += printEnergyConsumption();
        }
        writeEnergyConsumptionToFile(fileName);
        Vms.clear();
        tasks.clear();
        numberOfVm = 0;
        for (int i=0 ; i < simulationDuration ; i++){
            energyConsumptionArray[i] = 0;
        }
    }

    private void writeEnergyConsumptionToFile(String fileName) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileName), "utf-8"))) {
            writer.append(Arrays.toString(energyConsumptionArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void consolidateLowUtilVms(int threshold, int simulationTime, boolean ftm) {
        Set<Vm> toBeConsolidated = new HashSet<>();
        for (Map.Entry<UUID, Vm> entry : Vms.entrySet()) {
            if (entry.getValue().getUtilization() < threshold && simulationTime > 500) {
                toBeConsolidated.add(entry.getValue());
            }
        }
        for (Vm vm : toBeConsolidated) {
            consolidateVm(vm, ftm);
        }
        toBeConsolidated.clear();
    }

    private void checkVmFault(int simulationTime, int errorFreq) {
        if (simulationTime % errorFreq == 50 & numberOfVm > 4) {
            Random r = new Random();
            Object[] keyArray = Vms.keySet().toArray();
            Vm vm = Vms.get(keyArray[r.nextInt(numberOfVm)]);
            log.info("An Error Occurred On VM: " + vm.getVmId());
            if (!checkOtherVmsForMigration(vm)) {
                Vm v = createNewVm(simulationTime);
                vm.getTasks().values().forEach(task -> v.assignTask(task, false));
                deleteVm(vm);
            } else {
                consolidateVm(vm, false);
            }
        }
    }

    private int removeDoneTask(int taskCursor, int simulationTime) {
        for (int j = 0; j < taskCursor && tasks.get(0).getEndTime() < simulationTime; j++) {
            if (tasks.get(j).getEndTime() < simulationTime) {
                removeFromVm(tasks.get(j));
                tasks.remove(j);
                taskCursor--;
                j--;
            }
        }
        return taskCursor;
    }

    private int assignNewTask(int taskCursor, int simulationTime) {
        while (taskCursor < tasks.size()) {
            if (tasks.get(taskCursor).getStartTime() <= simulationTime) {
                Vm foundVm = findAppropriateVm(tasks.get(taskCursor));
                if (foundVm != null)
                    foundVm.assignTask(tasks.get(taskCursor), false);
                else {
                    createNewVm(simulationTime).assignTask(tasks.get(taskCursor), false);
                }
                taskCursor++;
            } else
                break;
        }
        return taskCursor;
    }

    private double printEnergyConsumption() {
        double energyConsumption = 0;
        for (Vm v : Vms.values()) {
            energyConsumption += 100 + v.getUtilization();
        }
        log.info("energy consumption: " + energyConsumption);
        return energyConsumption;
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
    private void consolidateVm(Vm vm, boolean ftm) {
        if (ftm) {
            double totalSpace = 0;
            for (Vm v : Vms.values()) {
                totalSpace += v.getUtilization();
            }
            if (totalSpace < 200) {// all vms have same capacity
                log.info("Consolidation is rejected according to ftm metric");
                return;
            }
        }
        UUID vmId = vm.getVmId();
        log.info("Consolidating Vm#" + vmId);
        if (checkOtherVmsForMigration(vm)) {
            deleteVm(vm);
        } else
            log.info("Vm#" + vmId + " CANNOT be consolidated");
    }

    private void deleteVm(Vm vm) {
        vm.getTasks().clear();
        Vms.remove(vm.getVmId());
        numberOfVm--;
        log.info("Consolidated Vm#" + vm.getVmId());
    }

    private boolean checkOtherVmsForMigration(Vm vm) {
        Map<UUID, Vm> assignableVmList = new HashMap<>();
        Map<UUID, String> assignedTasks = new HashMap<>();
        for (Task task : vm.getTasks().values()) {
            Vm foundVm = findMigrateVm(task, assignableVmList);
            if (foundVm == null) {
                return false;
            } else {
                assignableVmList.put(foundVm.getVmId(), Vm.builder().capacities(foundVm.getCapacities()).usedResources(foundVm.getUsedResources()).tasks(foundVm.getTasks()).VmId(foundVm.getVmId()).build());
                assignableVmList.get(foundVm.getVmId()).assignTask(task, true);
                assignedTasks.put(foundVm.getVmId(), task.getTaskId());
            }
        }
        assignableVmList.forEach((key, value) -> value.getTasks().get(assignedTasks.get(key)).setVmId(key));
        assignableVmList.forEach((key, value) -> Vms.replace(key, value));
        return true;
    }

    private Vm createNewVm(int i) {
        UUID vmId = UUID.randomUUID();
        numberOfVm++;
        for (int j = i; j < i+ 6 && j < 1000  ; j++)
            energyConsumptionArray[j] += 100;
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
