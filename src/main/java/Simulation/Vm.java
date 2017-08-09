package Simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.Tolerate;


import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@ToString
public class Vm {
    private int VmId;
    @Builder.Default
    private Map<String, Task> tasks = new HashMap<>();
    @Builder.Default
    private Map<String, Double> usedResources = new HashMap<>();
    @Singular
    private Map<String, Double> capacities;


    @Tolerate
    Vm() {
    }

    public void removeTask(Task task) {
        usedResources.put("CPU", usedResources.get("CPU") - task.resources.get("CPU"));
        usedResources.put("Memory", usedResources.get("Memory") - task.resources.get("Memory"));
        tasks.remove(task.taskId);
    }

    public void assignTask(Task task) {
        usedResources.put("CPU", usedResources.getOrDefault("CPU",0.0) + task.resources.get("CPU"));
        usedResources.put("Memory", usedResources.getOrDefault("Memory",0.0) + task.resources.get("Memory"));
        tasks.put(task.taskId, task);
    }
}
