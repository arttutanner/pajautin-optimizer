package fi.partio.pajautin.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.partio.pajautin.optimizer.member.Problem;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Collectors;

public class ResultExporter {

    public static void exportProblem(Problem problem) {
        // if directory "results" does not exist, create it
        File dir = new File("results");
        if (!dir.exists()) {
            dir.mkdir();
        }

        // Create directory for this problem
        String problemDir = "results/result_ua-"+problem.getUnallocated().size()+"_cancel-"+problem.getProgramsWithTooFewParticipants().size()+"_fit-"+problem.calculateFitness() +"_t-"+ System.currentTimeMillis();
        dir = new File(problemDir);
        dir.mkdir();

        // Export problem data
        writeParticipantsAsJson(problem, problemDir);
        writeProgramsAsJson(problem, problemDir);
        writeParticipantsAsCSV(problem, problemDir);
        writeProgramsAsCSV(problem, problemDir);


    }

    private static void writeProgramsAsCSV(Problem problem, String problemDir) {
        try {
            File csvFile = new File(problemDir + "/programs.csv");
            PrintStream ps = new PrintStream(csvFile);
            ps.println("id;name;max;min;occurance;possible1;possible2;possible3;active1;active2;active3;participants1;participants2;participants3;");
            for (int i = 0; i < problem.getPrograms().size(); i++) {
                ps.print(problem.getPrograms().get(i).getId() + ";");
                ps.print(problem.getPrograms().get(i).getName() + ";");
                ps.print(problem.getPrograms().get(i).getMaxPlaces() + ";");
                ps.print(problem.getPrograms().get(i).getMinPlaces() + ";");
                ps.print(problem.getPrograms().get(i).getMaxOccurance() + ";");
                ps.print(problem.getPrograms().get(i).getPossibleTimeSlots()[0] + ";");
                ps.print(problem.getPrograms().get(i).getPossibleTimeSlots()[1] + ";");
                ps.print(problem.getPrograms().get(i).getPossibleTimeSlots()[2] + ";");
                ps.print(problem.getPrograms().get(i).getAllocatedTimeSlots()[0] + ";");
                ps.print(problem.getPrograms().get(i).getAllocatedTimeSlots()[1] + ";");
                ps.print(problem.getPrograms().get(i).getAllocatedTimeSlots()[2] + ";");
                ps.print(problem.getPrograms().get(i).getAssignedParticipants().get(0).size() + ";");
                ps.print(problem.getPrograms().get(i).getAssignedParticipants().get(1).size() + ";");
                ps.print(problem.getPrograms().get(i).getAssignedParticipants().get(2).size() + ";");
                ps.println();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void writeParticipantsAsCSV(Problem problem, String problemDir) {
        try {
            File csvFile = new File(problemDir + "/participants.csv");
            PrintStream ps = new PrintStream(csvFile);

            ps.println("id;fitness;slot1;slot2;slot3;present1;present2;present3;fitness1;fitness2;fitness3;preferences;");
            for (int i = 0; i < problem.getParticipants().size(); i++) {
                ps.print(problem.getParticipants().get(i).getId() + ";");
                ps.print(problem.getParticipants().get(i).getFitness() + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedProgramIds().get(0)+ ";");
                ps.print(problem.getParticipants().get(i).getAllocatedProgramIds().get(1)+ ";");
                ps.print(problem.getParticipants().get(i).getAllocatedProgramIds().get(2)+ ";");
                ps.print(problem.getParticipants().get(i).getPresent()[0] + ";");
                ps.print(problem.getParticipants().get(i).getPresent()[1]+ ";");
                ps.print(problem.getParticipants().get(i).getPresent()[2] + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedPreferences()[0]==null ? "" :  problem.getParticipants().get(i).getAllocatedPreferences()[0].getOrder() + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedPreferences()[1]==null ? "" :  problem.getParticipants().get(i).getAllocatedPreferences()[1].getOrder() + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedPreferences()[2]==null ? "" :  problem.getParticipants().get(i).getAllocatedPreferences()[2].getOrder() + ";");

                ps.print(problem.getParticipants().get(i).getOriginalPreferences().stream().map(p -> p.getProgram().getId()+"").collect(Collectors.joining(";")));
                ps.println();
            }
            ps.close();

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

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
            mapper.writeValue(new File(problemDir + "/participants.json"), problem.getParticipants());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
