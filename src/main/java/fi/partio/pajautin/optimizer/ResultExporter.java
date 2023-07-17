package fi.partio.pajautin.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.partio.pajautin.optimizer.member.Problem;
import fi.partio.pajautin.optimizer.member.Program;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Collectors;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ResultExporter {

    public static void exportProblem(Problem problem) {
        // if directory "results" does not exist, create it
        File dir = new File("results");
        if (!dir.exists()) {
            dir.mkdir();
        }

        String currentDateInISOFormat = java.time.LocalDateTime.now().toString().replace(":", "_").replace(".", "_");

        // Create directory for this problem
        String problemDir = "results/result_ua-" + problem.getUnallocated().size() + "_cancel-" + problem.getProgramsWithTooFewParticipants().size() + "_fit-" + problem.calculateFitness() + "_t-" + currentDateInISOFormat;
        dir = new File(problemDir);
        dir.mkdir();

        // Export problem data
        writeParticipantsAsJson(problem, problemDir);
        writeProgramsAsJson(problem, problemDir);
        writeParticipantsAsCSV(problem, problemDir);
        writeProgramsAsCSV(problem, problemDir);
        writeProgramAsSQL(problem, problemDir);
        writeParticipantRegistrationsAsSQL(problem, problemDir);


    }

    private static void writeParticipantRegistrationsAsSQL(Problem problem, String problemDir) {

        try {
            File sqlFile = new File(problemDir + "/participant_registrations.sql");
            PrintStream ps = new PrintStream(sqlFile);
            ps.println("INSERT INTO participant_registration (program_id,participant_id,slot) VALUES");
            boolean first = true;
            for (var prg : problem.getPrograms()) {
                for (int slot =0; slot<prg.getAllocatedTimeSlots().length; slot++) {
                    for (var part : prg.getAssignedParticipants(slot).values()) {
                        if (first) {
                            first = false;
                        } else {
                            ps.println();
                            ps.print(",");
                        }
                        ps.print("(" + prg.getId() + ",'" + part.getId() + "'," + (slot+1) + ")");
                    }
                }
            }
            ps.println(";");
            ps.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void writeProgramsAsCSV(Problem problem, String problemDir) {
        try {
            File csvFile = new File(problemDir + "/programs.csv");
            PrintStream ps = new PrintStream(csvFile);
            ps.println("id;name;max;min;occurance;possible1;possible2;possible3;active1;active2;active3;participants1;participants2;participants3;empty");
            for (int i = 0; i < problem.getPrograms().size(); i++) {
                ps.print(problem.getPrograms().get(i).getId() + ";");
                ps.print(problem.getPrograms().get(i).getName().replace(';', ' ') + ";");
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
                ps.print((problem.getPrograms().get(i).getAssignedParticipants().stream().map(a -> a.size()).collect(Collectors.summingInt(s -> s)).intValue()==0 ? "TRUE" : "") + ";");
                ps.println();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void writeParticipantsAsCSV(Problem problem, String problemDir) {
        try {
            File csvFile = new File(problemDir + "/participants.csv");
            PrintStream ps = new PrintStream(csvFile);

            ps.println("id;fitness;slot1;slot2;slot3;present1;present2;present3;fitness1;fitness2;fitness3;best;preferences;");
            for (int i = 0; i < problem.getParticipants().size(); i++) {
                ps.print(problem.getParticipants().get(i).getId() + ";");
                ps.print(problem.getParticipants().get(i).getFitness() + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedProgramIds().get(0) + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedProgramIds().get(1) + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedProgramIds().get(2) + ";");
                ps.print(problem.getParticipants().get(i).getPresent()[0] + ";");
                ps.print(problem.getParticipants().get(i).getPresent()[1] + ";");
                ps.print(problem.getParticipants().get(i).getPresent()[2] + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedPreferences()[0] == null ? ";" : problem.getParticipants().get(i).getAllocatedPreferences()[0].getOrder() + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedPreferences()[1] == null ? ";" : problem.getParticipants().get(i).getAllocatedPreferences()[1].getOrder() + ";");
                ps.print(problem.getParticipants().get(i).getAllocatedPreferences()[2] == null ? ";" : problem.getParticipants().get(i).getAllocatedPreferences()[2].getOrder() + ";");
                ps.print(problem.getParticipants().get(i).getBestPreference() + ";");
                ps.print(problem.getParticipants().get(i).getOriginalPreferences().stream().map(p -> p.getProgram().getId() + "").collect(Collectors.joining(";")));
                ps.println();
            }
            ps.close();

        } catch (IOException e) {
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


    public static void writeProgramAsSQL(Problem problem, String problemDir) {

        try {

            File sqlFilePath = new File(problemDir + "/programs.sql");
            // Create the SQL file
            FileWriter writer = new FileWriter(sqlFilePath);



            // Iterate over JSON array
            for (Program prg : problem.getPrograms()) {
                var jsonObject = prg.getJSONData();

                // Extract data from JSON object
                String id = (String) jsonObject.get("id");
                String name = (String) jsonObject.get("name");
                String keywords = (String) jsonObject.get("keywords");
                String author = (String) jsonObject.get("author");
                String description = (String) jsonObject.get("description");
                String minSize = prg.getMinPlaces() + "";
                String maxSize = prg.getMaxPlaces() + "";
                String roverRecommended = "TRUE".equals(jsonObject.get("roverRecommended")) ? "1" : "0";
                String availableSlots = prg.getMaxOccurance()+ "";
                String type = (String) jsonObject.get("type");
                String countinueInSlot = (String) jsonObject.get("countinueInSlot");
                String slot3 = "TRUE".equals(jsonObject.get("slot3")) ? "1" : "0";
                String slot2 = "TRUE".equals(jsonObject.get("slot2")) ? "1" : "0";
                String slot1 = "TRUE".equals(jsonObject.get("slot1")) ? "1" : "0";
                String act3 = prg.getAllocatedTimeSlots()[2] ? "1" : "0";
                String act2 = prg.getAllocatedTimeSlots()[1] ? "1" : "0";
                String act1 = prg.getAllocatedTimeSlots()[0] ? "1" : "0";

                if (author!=null) author = author.replace("'", "\\'");
                if (name!=null) name = name.replace("'", "\\'");
                if (keywords!=null) keywords = keywords.replace("'", "\\'");
                if (description!=null) description = description.replace("'", "\\'");

                // Generate the SQL INSERT statement
                String insertQuery = String.format("INSERT INTO program (keywords, author, description, maxSize, roverRecommended, " +
                                "availableSlots, type, countinueInSlot, slot3, slot2, slot1, act3, act2, act1, name, minSize, id) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');\n",
                        keywords, author, description, maxSize, roverRecommended, availableSlots, type, countinueInSlot,
                        slot3, slot2, slot1, act3,act2,act1, name, minSize, id);

                // Write the SQL INSERT statement to the file
                writer.write(insertQuery);
            }

            // Close the file writer
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
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
