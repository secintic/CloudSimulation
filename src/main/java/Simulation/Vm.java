package Simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.Tolerate;

import java.util.List;
import java.util.Map;

@Data
@Builder
@ToString
public class Vm {
    private int VmId;
    private List<Task> tasks;
    @Singular
    private Map<String, Double> capacities;

    @Tolerate
    Vm() {
    }
}
