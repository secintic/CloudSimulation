package Simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;

import java.util.Map;

@Data
@Builder
@ToString
public class Task {
    double startTime;
    double endTime;
    String taskId;
    int vmId;

    @Singular
    Map<String, Double> resources;
}
