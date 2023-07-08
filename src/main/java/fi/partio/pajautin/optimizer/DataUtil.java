package fi.partio.pajautin.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataUtil {
    /*
     * Read a JSON file to a Map.
     */
    public static Map<Object, Object> readJsonFileToMap(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<Object, Object> map = mapper.readValue(new File(filename), HashMap.class);
            return map;
        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + e.getMessage());
            return null;
        }

    }

    public static List<Map<Object, Object>> readJsonFileToList(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<Object, Object>> map = mapper.readValue(new File(filename), ArrayList.class);
            return map;
        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + e.getMessage());
            return null;
        }

    }

    public static List<List<Object>> readJsonFileToDoubleList(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(filename), ArrayList.class);

        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + e.getMessage());
            return null;
        }

    }


    public static void writeMapToJsonFile(Map<Object, Object> map, String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // set pretty printing to true
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), map);

        } catch (Exception e) {
            System.out.println("Error writing JSON file: " + e.getMessage());
        }
    }

    public static void writeProjectedData(int projectedCount) {
        Map<Object, Object> map = readJsonFileToMap("results.json");
        // get entryset as vector
        Object[] entries = map.entrySet().toArray();
        HashMap<Object,Object> projectedMap = new HashMap<Object,Object>();
        float multiplier =  (float)entries.length / (float)projectedCount;
        for (int i = 0; i < projectedCount; i++) {
            var entry = (Map.Entry<Object,Object>)entries[(int)((float)i*multiplier)];
            projectedMap.put(java.util.UUID.randomUUID().toString(), entry.getValue());
        }
        System.out.println("Projected " + projectedMap.size() + " entries");
        writeMapToJsonFile(projectedMap, "projected.json");

    }

    public static void writeCsvFile(String inputFileName, String outputFileName) {
        // open outputfile for writing
        File outputFile = new File(outputFileName);
        try {
            outputFile.createNewFile();
            PrintStream ps = new PrintStream(outputFile);
            Map<Object, Object> participants = readJsonFileToMap(inputFileName);
            // get entryset as vector
            Object[] entries = participants.entrySet().toArray();

            for (int i = 0; i < entries.length; i++) {
                var entry = (Map.Entry<Object,Object>)entries[i];
                ps.print(entry.getKey()+";");
                List<Object> present = (List<Object>)((Map<Object,Object>)entry.getValue()).get("present");
                for (int j = 0; j < present.size(); j++) {
                    ps.print(present.get(j)+";");
                }
                List<Object> preferences = (List<Object>)((Map<Object,Object>)entry.getValue()).get("prog");
                for (int j = 0; j < preferences.size(); j++) {
                    ps.print(preferences.get(j)+";");
                }
                ps.println();
            }
            ps.close();

        } catch (Exception e) {
            System.out.println("Error creating output file: " + e.getMessage());
            return;
        }
    }
}
