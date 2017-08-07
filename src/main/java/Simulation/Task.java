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
    double duration;
    String taskId;
    @Singular
    Map<String, Double> resources;
}
