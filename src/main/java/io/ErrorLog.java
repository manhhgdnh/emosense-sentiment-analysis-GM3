package io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;


public class ErrorLog {
    private final String input;
    private final String userComment;
    private double machineScore;

    public ErrorLog(String input, String userComment, double machineScore) {
        this.input = input;
        this.userComment = userComment;
        this.machineScore = machineScore;
    }

    public ErrorLog(String input, String userComment) {
        this.input = input;
        this.userComment = userComment;
    }

    public void saveToFile() {
        File out = new File("results/errors.log");
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();

        try (FileWriter writer = new FileWriter(out, true)) {
            String logEntry = String.format("[%s] Input: '%s' | Score: %.2f | User says: %s\n", 
                                            LocalDateTime.now(), input, machineScore, userComment);
            
            writer.write(logEntry);
            System.out.println("Error feedback has been noted !");

        } catch (IOException e) {
            System.err.println("Error logs not recorded: " + e.getMessage());
        }
    }
    
}
