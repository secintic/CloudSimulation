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

    public static List<Task> readDataFromCsv() {
        String line;
        List<Task> tasks = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("data.csv"))) {
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                tasks.add(Task.builder()
                        .startTime(Double.parseDouble(columns[0]))
                        .endTime(Double.parseDouble(columns[1]))
                        .taskId(columns[2])
                        .resource("CPU", Double.parseDouble(columns[3]))
                        .resource("Memory", Double.parseDouble(columns[4])).build());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    private static void writeDataToCsv(List<Task> tasks) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new File("data.csv"));
        StringBuilder sb = new StringBuilder();
        for (Task t : tasks) {
            sb.append(t.getStartTime());
            sb.append(',');
            sb.append(t.getEndTime());
            sb.append(',');
            sb.append(t.getTaskId());
            sb.append(',');
            sb.append(t.getResources().get("CPU"));
            sb.append(',');
            sb.append(t.getResources().get("Memory"));
            sb.append('\n');
        }
        pw.write(sb.toString());
        pw.close();
    }

    public static List<Task> generatePoisson(int simulationDuration) throws FileNotFoundException {
        List<Task> tasks = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < simulationDuration - 75; i++) {
            int numberOfTasksInTheCycle = r.nextInt(11);
            if (i == 200)
                numberOfTasksInTheCycle += 250;
            if (i % 200 == 0 || i % 200 == 25 || i % 200 == 50 || i % 200 == 75)
                numberOfTasksInTheCycle += 60;
            while (numberOfTasksInTheCycle > 0) {
                tasks.add(Task.builder()
                        .startTime(i)
                        .endTime(i + (r.nextInt(51) + 50))
                        .taskId(Integer.toString(tasks.size() + 1))
                        .resource("CPU", (r.nextInt(21) + 10) / 100.0)
                        .resource("Memory", (r.nextInt(21) + 10) / 100.0).build());
                numberOfTasksInTheCycle--;
            }
        }
        writeDataToCsv(tasks);
        return tasks;
    }
}
