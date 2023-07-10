package fi.partio.pajautin.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.partio.pajautin.optimizer.member.Problem;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;

public class ResultExporter {

    public static void exportProblem(Problem problem) {
        // if directory "results" does not exist, create it
        File dir = new File("results");
        if (!dir.exists()) {
            dir.mkdir();
        }

        // Create directory for this problem
        String problemDir = "results/result_" + System.currentTimeMillis();
        dir = new File(problemDir);
        dir.mkdir();

        // Export problem data
        writeParticipantsAsJson(problem, problemDir);
        writeProgramsAsJson(problem, problemDir);


    }

    private static void writeProgramsAsJson(Problem problem, String problemDir) {


        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(problemDir + "/programs.json"), problem.getPrograms());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void writeParticipantsAsJson(Problem problem, String problemDir) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(problemDir + "/programs.json"), problem.getParticipants());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
