package Simulation;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Builder
@Slf4j
class SimulationEngine {
    @Builder.Default
    private HashMap<String, Vm> Vms = new HashMap<>();
    private int numberOfVm;
    private List<Task> tasks;
    private double[] energyConsumptionArray;
    private double[] numberOfMigration;

    void run(int simulationDuration, int threshold, int errorFreq, int experiment, String fileName) throws IOException {
        createVms();
        tasks = ReadGoogleData.readDataFromCsv();
        double[] numberOfActiveApplication = new double[simulationDuration];
        double[] numberOfVms = new double[simulationDuration];
        numberOfMigration = new double[simulationDuration];
        for (int simulationTime = 0; simulationTime < simulationDuration; simulationTime++) {
            log.info("Simulation time: " + simulationTime);
            assignNewTasks(simulationTime);
            removeDoneTasks(simulationTime);
            checkVmFault(simulationTime, errorFreq, experiment);
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
            log.info("number of application: " + tasks.size() + " number of Vm: " + Vms.size());
            numberOfVms[simulationTime] = Vms.size();
        }
        writeToFile(fileName, energyConsumptionArray);
        writeToFile("apps.csv", numberOfActiveApplication);
        writeToFile("vms_" + fileName, numberOfVms);
        for (int i = 1; i < simulationDuration; i++) {
            numberOfMigration[i] += numberOfMigration[i - 1];
        }
        writeToFile("migration_" + fileName, numberOfMigration);
        Vms.clear();
        tasks.clear();
        numberOfVm = 0;
    }

    private int countNumberOfActiveApplications() {
        int numberOfActiveApplications = 0;
        for (Vm v : Vms.values()) {
            numberOfActiveApplications += v.getTasks().size();
        }

        return numberOfActiveApplications;
    }

    private void writeToFile(String fileName, double[] values) throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter(fileName));
        StringBuilder sb = new StringBuilder();
        for (double element : values) {
            sb.append(element);
            sb.append(",");
        }
        br.write(sb.toString());
        br.close();
    }

    private void consolidateLowUtilVms(int threshold, int simulationTime, boolean ftm) {
        if (simulationTime > 50) {
            Set<Vm> toBeConsolidated = new HashSet<>();
            for (Map.Entry<String, Vm> entry : Vms.entrySet()) {
                if (entry.getValue().getUtilization() < threshold) {
                    toBeConsolidated.add(entry.getValue());
                }
            }
            for (Vm vm : toBeConsolidated) {
                consolidateVm(vm, ftm, simulationTime);
            }
            toBeConsolidated.clear();
        }
    }

    private void checkVmFault(int simulationTime, int errorFreq, int experiment) {
        if ((simulationTime + 1) % errorFreq == 0 & numberOfVm > 4) {
            Vm vm = Vms.get(findMaxUtilVmUtil());
            log.info("An Error Occurred On VM: " + vm.getVmId());
            if (!checkOtherVmsForMigration(vm, simulationTime) || experiment == 0) {
                Vm v = createNewVm(simulationTime, true);
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
            }
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
                        createNewVm(simulationTime, false).assignTask(task, false);
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
            energyConsumption += 100 + v.getUtilization();
        }
        return energyConsumption;
    }

    private void createVms() {
        for (int i = 0; i < numberOfVm; i++) {
            UUID vmId = UUID.randomUUID();
            Vms.put(vmId.toString(), Vm.builder().VmId(vmId.toString()).capacity("CPU", 20.0).capacity("Memory", 20.0).build());
        }
    }

    private void consolidateVm(Vm vm, boolean ftm, int time) {
        if (ftm && !checkOtherVmsForMigrationAfterRemoval(Vms.get(findMaxUtilVmUtil()), vm)) {
            log.info("Consolidation is rejected according to ftm metric");
            return;
        }
        String vmId = vm.getVmId();
        log.info("Consolidating Vm#" + vmId);
        if (checkOtherVmsForMigration(vm, time)) {
            deleteVm(vm);
        } else
            log.info("Vm#" + vmId + " CANNOT be consolidated");
    }


    private String findMaxUtilVmUtil() {
        double util = 0;
        String vmID = "";
        for (Vm v : Vms.values()) {
            if (util < v.getUtilization()) {
                util = v.getUtilization();
                vmID = v.getVmId();
            }
        }
        return vmID;
    }

    private void deleteVm(Vm vm) {
        vm.getTasks().clear();
        Vms.remove(vm.getVmId());
        numberOfVm--;
        log.info("Consolidated Vm#" + vm.getVmId());
    }

    private boolean checkOtherVmsForMigration(Vm vm, int time) {
        Map<String, Vm> assignableVmList = new HashMap<>();
        Map<String, String> assignedTasks = new HashMap<>();
        for (Task task : vm.getTasks().values()) {
            Vm foundVm = findMigrateVm(task, assignableVmList);
            if (foundVm == null) {
                return false;
            } else {
                if (!(assignableVmList.containsKey(foundVm.getVmId()))) {
                    HashMap<String, Double> usedResources = new HashMap<>();
                    usedResources.put("CPU", foundVm.getUsedResources().get("CPU"));
                    usedResources.put("Memory", foundVm.getUsedResources().get("Memory"));
                    assignableVmList.put(foundVm.getVmId(),
                            Vm.builder().capacities(foundVm.getCapacities())
                                    .usedResources(usedResources)
                                    .VmId(foundVm.getVmId()).build());
                    HashMap<String, Task> tasks = new HashMap<>();
                    for (Task t : foundVm.getTasks().values()) {
                        tasks.put(t.getTaskId(), Task.builder()
                                .startTime(t.getStartTime())
                                .endTime(t.getEndTime())
                                .taskId(t.getTaskId())
                                .vmId(t.getVmId())
                                .resource("CPU", t.getResources().get("CPU"))
                                .resource("Memory", t.getResources().get("Memory")).build());
                    }
                    assignableVmList.get(foundVm.getVmId()).setTasks(tasks);
                }
                assignableVmList.get(foundVm.getVmId()).assignTask(task, true);
                assignedTasks.put(task.getTaskId(), foundVm.getVmId());
            }
        }

        for (Map.Entry<String, String> entry : assignedTasks.entrySet()) {
            Vms.get(entry.getValue()).assignTask(vm.getTasks().get(entry.getKey()), false);
        }
        this.numberOfMigration[time] += assignedTasks.size();

        return true;
    }

    private boolean checkOtherVmsForMigrationAfterRemoval(Vm vm, Vm targetVm) {
        Map<String, Vm> assignableVmList = new HashMap<>();
        Map<String, String> assignedTasks = new HashMap<>();
        for (Task task : vm.getTasks().values()) {
            Vm foundVm = findMigrateVmExcept(task, assignableVmList, targetVm);
            if (foundVm == null) {
                return false;
            } else {
                if (!(assignableVmList.containsKey(foundVm.getVmId()))) {
                    HashMap<String, Double> usedResources = new HashMap<>();
                    usedResources.put("CPU", foundVm.getUsedResources().get("CPU"));
                    usedResources.put("Memory", foundVm.getUsedResources().get("Memory"));
                    assignableVmList.put(foundVm.getVmId(),
                            Vm.builder().capacities(foundVm.getCapacities())
                                    .usedResources(usedResources)
                                    .VmId(foundVm.getVmId()).build());
                    HashMap<String, Task> tasks = new HashMap<>();
                    for (Task t : foundVm.getTasks().values()) {
                        tasks.put(t.getTaskId(), Task.builder()
                                .startTime(t.getStartTime())
                                .endTime(t.getEndTime())
                                .taskId(t.getTaskId())
                                .vmId(t.getVmId())
                                .resource("CPU", t.getResources().get("CPU"))
                                .resource("Memory", t.getResources().get("Memory")).build());
                    }
                    assignableVmList.get(foundVm.getVmId()).setTasks(tasks);
                }
                assignableVmList.get(foundVm.getVmId()).assignTask(task, true);
                assignedTasks.put(task.getTaskId(), foundVm.getVmId());
            }
        }
        return true;
    }


    private Vm createNewVm(int i, boolean fault) {
        UUID vmId = UUID.randomUUID();
        numberOfVm++;
        Vms.put(vmId.toString(), Vm.builder().VmId(vmId.toString())
                .capacity("CPU", 20.0).capacity("Memory", 20.0).build());
        log.info("Creating a new Vm: " + vmId.toString());
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
            if (!(vm.getVmId().equals(task.getVmId()))) {
                if (pseudoList.containsKey(vm.getVmId())) {
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

    private Vm findMigrateVmExcept(Task task, Map<String, Vm> pseudoList, Vm targetVm) {
        for (Vm vm : Vms.values()) {
            if (!(vm.getVmId().equals(task.getVmId())) && vm.getVmId().equals(targetVm.getVmId())) {
                if (pseudoList.containsKey(vm.getVmId())) {
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
