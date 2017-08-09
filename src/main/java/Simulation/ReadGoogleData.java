package Simulation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadGoogleData {
    public static List<Task> read() {
        String line;
        List<Task> tasks = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("part-00000-of-00500.csv"))) {
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                tasks.add(Task.builder()
                        .startTime(Double.parseDouble(columns[0])/(1000.0*1000.0))
                        .endTime( Double.parseDouble(columns[1])/(1000.0*1000.0))
                        .taskId(columns[2] + columns[3])
                        .resource("CPU", Double.parseDouble(columns[5]))
                        .resource("Memory", Double.parseDouble(columns[7])).build());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tasks;
    }
}
