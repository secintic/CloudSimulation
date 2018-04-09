package Simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;

import java.util.Map;

@Data
@Builder
@ToString
class Task {
    private double startTime;
    private double duration;
    private double totalProcessTime;
    private String taskId;
    private String vmId;

    @Singular
    private Map<String, Double> resources;

    public void increaseTotalProcessTime() {
        this.totalProcessTime++;
    }
}
