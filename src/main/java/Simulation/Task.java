package Simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@ToString
class Task {
    private double startTime;
    private double endTime;
    private String taskId;
    private UUID vmId;

    @Singular
    private Map<String, Double> resources;
}
