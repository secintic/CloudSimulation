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
    @Builder.Default
    private Queue<Task> taskQueue = new LinkedList<>();
    private int numberOfVm;
    private List<Task> tasks;
    private double[] energyConsumptionArray;
    private double[] numberOfMigration;
    private List<Integer> faultTimesAccordingToWeibullDist;
    private int VmLimit;


    void run(int simulationDuration, int threshold, int experiment, String fileName) throws IOException {
        createVms();
        tasks = ReadGoogleData.readDataFromCsv();
        double[] numberOfVms = new double[simulationDuration];
        numberOfMigration = new double[simulationDuration];
        for (int simulationTime = 0; simulationTime < simulationDuration && !tasks.isEmpty(); simulationTime++) {
            log.info("Simulation time: " + simulationTime);
            increaseProcessTimes();
            assignNewTasks(simulationTime);
            removeDoneTasks();

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
            if (faultTimesAccordingToWeibullDist.contains(simulationTime))
                faultOccurred(simulationTime, experiment);
            energyConsumptionArray[simulationTime] += calculateEnergyConsumption();
            numberOfVms[simulationTime] = Vms.size();
        }
        writeToFile(fileName, energyConsumptionArray);
        writeToFile("vms_" + fileName, numberOfVms);
        for (int i = 1; i < simulationDuration; i++) {
            numberOfMigration[i] += numberOfMigration[i - 1];
        }
        writeToFile("migration_" + fileName, numberOfMigration);
        Vms.clear();
        tasks.clear();
        numberOfVm = 0;
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

    private void faultOccurred(int simulationTime, int experiment) {
        Vm vm = Vms.get(findSpecificUtilVmUtil("max"));
        log.info("An Error Occurred On VM: " + vm.getVmId());
        if (!checkOtherVmsForMigration(vm, simulationTime) || experiment == 0) {
            Vm v = createNewVm(simulationTime, true);
            numberOfMigration[simulationTime] += vm.getTasks().size();
            vm.getTasks().values().forEach(task -> v.assignTask(task, false));
        }
        deleteVm(vm);
    }

    private void removeDoneTasks() {
        for (int j = 0; j < tasks.size(); j++) {
            if (tasks.get(j).getDuration() - tasks.get(j).getTotalProcessTime() <= 0) {
                removeFromVm(tasks.get(j));
                tasks.remove(j);
                j--;
            }
        }
    }

    private void increaseProcessTimes() {
        for (Task task : tasks) {
            if (task.getVmId() != null) {
                task.increaseTotalProcessTime();
            }
        }
    }

    private void assignNewTasks(int simulationTime) {
        for (Task task : tasks) {
            if (task.getStartTime() <= simulationTime) {
                if (task.getVmId() == null) {
                    taskQueue.add(task);
                }
            } else {
                break;
            }
        }
        while (!taskQueue.isEmpty()) {
            Task t = taskQueue.peek();
            Vm foundVm = findAppropriateVm(t);
            if (foundVm != null) {
                foundVm.assignTask(t, false);
                taskQueue.poll();
            } else {
                if (Vms.size() < VmLimit) {
                    createNewVm(simulationTime, false).assignTask(t, false);
                    taskQueue.poll();
                } else
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
        if (ftm && !checkOtherVmsForMigrationAfterRemoval(Vms.get(findSpecificUtilVmUtil("max" +
                "")), vm)) {
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

    private String findSpecificUtilVmUtil(String type) {
        List<Vm> vms = new ArrayList<>(Vms.values());
        vms.sort(Comparator.comparing(Vm::getUtilization));
        switch (type) {
            case "median":
                return vms.get(vms.size() / 2).getVmId();
            case "min":
                return vms.get(0).getVmId();

            default:
                return vms.get(vms.size() - 1).getVmId();
        }
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
            if (checkOtherVms(assignableVmList, assignedTasks, task, foundVm)) return false;
        }

        for (Map.Entry<String, String> entry : assignedTasks.entrySet()) {
            Vms.get(entry.getValue()).assignTask(vm.getTasks().get(entry.getKey()), false);
        }
        this.numberOfMigration[time] += assignedTasks.size();

        return true;
    }

    private boolean checkOtherVms(Map<String, Vm> assignableVmList, Map<String, String> assignedTasks, Task task, Vm foundVm) {
        if (foundVm == null) {
            return true;
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
                            .duration(t.getDuration())
                            .totalProcessTime(t.getTotalProcessTime())
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
        return false;
    }

    private boolean checkOtherVmsForMigrationAfterRemoval(Vm vm, Vm targetVm) {
        Map<String, Vm> assignableVmList = new HashMap<>();
        Map<String, String> assignedTasks = new HashMap<>();
        for (Task task : vm.getTasks().values()) {
            Vm foundVm = findMigrateVmExcept(task, assignableVmList, targetVm);
            if (checkOtherVms(assignableVmList, assignedTasks, task, foundVm)) return false;
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
