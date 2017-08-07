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
                        .startTime(Double.parseDouble(columns[1]))
                        .duration(Double.parseDouble(columns[2]) - Double.parseDouble(columns[1]))
                        .taskId(columns[3] + columns[4])
                        .resource("CPU", Double.parseDouble(columns[6]))
                        .resource("Memory", Double.parseDouble(columns[8])).build());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tasks;
    }
}
