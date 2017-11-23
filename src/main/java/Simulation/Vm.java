package Simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.Tolerate;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@ToString
class Vm {
    private UUID VmId;
    @Builder.Default
    private Map<String, Task> tasks = new HashMap<>();
    @Builder.Default
    private Map<String, Double> usedResources = new HashMap<>();
    @Singular
    private Map<String, Double> capacities;


    @Tolerate
    Vm() {
    }

    void removeTask(Task task) {
        usedResources.put("CPU", usedResources.get("CPU") - task.getResources().get("CPU"));
        usedResources.put("Memory", usedResources.get("Memory") - task.getResources().get("Memory"));
        tasks.remove(task.getTaskId());
    }

    double getUtilization() {
        return usedResources.getOrDefault("CPU", 0.0) / capacities.get("CPU") * 50.0
                + usedResources.getOrDefault("Memory", 0.0) / capacities.get("Memory") * 50.0;
    }

    boolean checkVmSpace(Task task) {
        return (capacities.get("CPU") - usedResources.getOrDefault("CPU", 0.0) > task.getResources().get("CPU")
                && capacities.get("Memory") - usedResources.getOrDefault("Memory", 0.0) > task.getResources().get("Memory"));
    }

    void assignTask(Task task, boolean pseudoAssign) {
        usedResources.put("CPU", usedResources.getOrDefault("CPU", 0.0) + task.getResources().get("CPU"));
        usedResources.put("Memory", usedResources.getOrDefault("Memory", 0.0) + task.getResources().get("Memory"));
        tasks.put(task.getTaskId(), task);
        if (!pseudoAssign)
            task.setVmId(VmId);
    }
}
