package Simulation;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

@Builder
@Slf4j
class SimulationEngine {
    @Builder.Default
    private HashMap<String, Vm> Vms = new HashMap<>();
    private int numberOfVm;
    private List<Task> tasks;
    private double[] energyConsumptionArray;

    void run(int simulationDuration, int threshold, int errorFreq, int experiment, String fileName) {
        createVms();
        //tasks = ReadGoogleData.read();
        tasks = ReadGoogleData.generatePoisson(simulationDuration);
        double[] numberOfActiveApplication = new double[simulationDuration];
        for (int simulationTime = 0; simulationTime < simulationDuration; simulationTime++) {
            log.info("Simulation time: " + simulationTime);
            assignNewTasks(simulationTime);
            removeDoneTasks(simulationTime);
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
            energyConsumptionArray[simulationTime] += calculateEnergyConsumption();
            numberOfActiveApplication[simulationTime] = countNumberOfActiveApplications();
        }
        writeToFile(fileName, energyConsumptionArray);
        writeToFile("apps.txt", numberOfActiveApplication);
        Vms.clear();
        tasks.clear();
        numberOfVm = 0;
        for (int i = 0; i < simulationDuration; i++) {
            energyConsumptionArray[i] = 0;
        }
    }

    private int countNumberOfActiveApplications() {
        int numberOfActiveApplications = 0;
        for (Vm v : Vms.values()) {
            numberOfActiveApplications += v.getTasks().size();
        }
        return numberOfActiveApplications;
    }

    private void writeToFile(String fileName, double[] values) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileName), "utf-8"))) {
            writer.append(Arrays.toString(values));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void consolidateLowUtilVms(int threshold, int simulationTime, boolean ftm) {
        Set<Vm> toBeConsolidated = new HashSet<>();
        for (Map.Entry<String, Vm> entry : Vms.entrySet()) {
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
            }
            deleteVm(vm);
        }
    }

    private void removeDoneTasks(int simulationTime) {
        for (int j = 0; j < tasks.size(); j++) {
            if (tasks.get(j).getEndTime() <= simulationTime) {
                removeFromVm(tasks.get(j));
                tasks.remove(j);
                j--;
            } else if (simulationTime < tasks.get(j).getStartTime() + 50)
                break;
        }
    }

    private void assignNewTasks(int simulationTime) {
        for (Task task : tasks) {
            if (task.getStartTime() <= simulationTime) {
                if (task.getVmId() == null) {
                    Vm foundVm = findAppropriateVm(task);
                    if (foundVm != null)
                        foundVm.assignTask(task, false);
                    else {
                        createNewVm(simulationTime).assignTask(task, false);
                    }
                }
            } else {
                break;
            }
        }
    }

    private double calculateEnergyConsumption() {
        double energyConsumption = 0;
        for (Vm v : Vms.values()) {
            energyConsumption += 50 + v.getUtilization();
        }
        log.info("energy consumption: " + energyConsumption);
        return energyConsumption;
    }

    private void createVms() {
        for (int i = 0; i < numberOfVm; i++) {
            UUID vmId = UUID.randomUUID();
            Vms.put(vmId.toString(), Vm.builder().VmId(vmId.toString()).capacity("CPU", 2.0).capacity("Memory", 2.0).build());
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
        String vmId = vm.getVmId();
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
        Map<String, Vm> assignableVmList = new HashMap<>();
        Map<String, String> assignedTasks = new HashMap<>();
        for (Task task : vm.getTasks().values()) {
            Vm foundVm = findMigrateVm(task, assignableVmList);
            if (foundVm == null) {
                return false;
            } else {
                if (!(assignableVmList.containsKey(foundVm.getVmId()))) {
                    assignableVmList.put(foundVm.getVmId(), Vm.builder().capacities(foundVm.getCapacities()).usedResources(foundVm.getUsedResources()).tasks(foundVm.getTasks()).VmId(foundVm.getVmId()).build());
                }
                assignableVmList.get(foundVm.getVmId()).assignTask(task, true);
                assignedTasks.put(task.getTaskId(), foundVm.getVmId());
            }
        }
        for (Map.Entry<String, String> entry : assignedTasks.entrySet()) {
            assignableVmList.get(entry.getValue()).getTasks().get(entry.getKey()).setVmId(entry.getValue());
        }

        assignableVmList.forEach((key, value) -> Vms.replace(key, value));
        return true;
    }

    private Vm createNewVm(int i) {
        UUID vmId = UUID.randomUUID();
        numberOfVm++;
        for (int j = i; j < i + 10 && j < 1000; j++)
            energyConsumptionArray[j] += 100;
        Vms.put(vmId.toString(), Vm.builder().VmId(vmId.toString())
                .capacity("CPU", 2.0).capacity("Memory", 2.0).build());
        return Vms.get(vmId.toString());
    }

    private Vm findAppropriateVm(Task task) {
        for (Vm vm : Vms.values()) {
            if (vm.checkVmSpace(task)) {
                return vm;
            }
        }
        return null;
    }

    private Vm findMigrateVm(Task task, Map<String, Vm> pseudoList) {
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
        log.info(task.toString());
        log.info(Vms.toString());
        Vms.get(task.getVmId()).removeTask(task);
    }
}
