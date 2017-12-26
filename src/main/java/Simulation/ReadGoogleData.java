package Simulation;

import java.io.*;
import java.util.*;

public class ReadGoogleData {
    public static List<Task> read() {
        String line;
        List<Task> tasks = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("part-00000-of-00500.csv"))) {
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                tasks.add(Task.builder()
                        .startTime(Double.parseDouble(columns[0]) / (1000.0 * 1000.0))
                        .endTime(Double.parseDouble(columns[1]) / (1000.0 * 1000.0))
                        .taskId(columns[2] + columns[3])
                        .resource("CPU", Double.parseDouble(columns[5]))
                        .resource("Memory", Double.parseDouble(columns[10])).build());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public static void writeDataToText(List<Task> tasks) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("tasks.txt"), "utf-8"))) {
            writer.append(tasks.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Task> generatePoisson(int simulationDuration) {
        List<Task> tasks = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < simulationDuration - 75; i++) {
            int numberOfTasksInTheCycle = r.nextInt(21);
            while (numberOfTasksInTheCycle > 0) {
                tasks.add(Task.builder()
                        .startTime(i)
                        .endTime(i + (r.nextInt(101) + 50))
                        .taskId(Integer.toString(tasks.size()+1))
                        .resource("CPU", (r.nextInt(21) + 10) / 100.0)
                        .resource("Memory", (r.nextInt(21) + 10) / 100.0).build());
                numberOfTasksInTheCycle--;
            }
        }
        //writeDataToText(tasks);
        return tasks;
    }
}
