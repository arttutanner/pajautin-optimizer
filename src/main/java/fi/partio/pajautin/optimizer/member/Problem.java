package fi.partio.pajautin.optimizer.member;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Problem {
    public static final int UNALLOCATED_FITNESS = 30;

    ArrayList<Participant> participants;
    HashMap<Integer,Program> programs;

    ArrayList<Program> unprocessedPrograms;
    ArrayList<Participant> unprocessedParticipants;

    public Problem(List<Map<Object,Object>> programData, Map<Object,Object> participantData, List<List<Object>> participantSpeakers) {
        programs= new HashMap<Integer,Program>();
        unprocessedPrograms = new ArrayList<Program>();
        for (Map<Object,Object> programDatum : programData) {
            Program program = new Program(programDatum);
            programs.put(program.getId(), program);
            unprocessedPrograms.add(program);
        }

        participants = new ArrayList<Participant>();
        unprocessedParticipants = new ArrayList<Participant>();
        for (HashMap.Entry<Object, Object> participantDatum : participantData.entrySet()) {
            Participant participant = new Participant(participantDatum.getKey().toString(), (Map<Object,Object>)participantDatum.getValue(), this);
            participants.add(participant);
            unprocessedParticipants.add(participant);
        }

        // Add participants who are facilitators to the programs
        for (List<Object> tuple : participantSpeakers) {
            int programId = (Integer)tuple.get(0);
            String participantId = (String)tuple.get(1);
            Optional<Participant> first = participants.stream().filter(p -> p.getId().equals(participantId)).findFirst();
            if (first.isPresent()) {
                Participant participant = first.get();
                Program program = programs.get(programId);
                program.addFacilitator(participant);
            }
        }

    }


    public int calculateFitness() {
        AtomicInteger fitness = new AtomicInteger();
        participants.stream().filter(p -> p.getPresentCount()>0 || p.getOriginalPreferences().size()>9).forEach(p -> fitness.addAndGet(p.getFitness()));


        return fitness.get();
    }

    public Program getProgram(int programId) {
        return programs.get(programId);
    }

    /**
     * Calculate the number of participants who have this program as their first preference.
     */
    public void calculatePrimaryPreferenceCountForProgram() {
        for (Program program : programs.values()) {
            program.calculatePrimaryPreferenceCount(unprocessedParticipants);
        }
    }

    public void sortProgramsByPrimaryPreferenceCount() {
        unprocessedPrograms.stream().forEach(p -> p.calculatePrimaryPreferenceCount(unprocessedParticipants));
        unprocessedPrograms.sort((p1, p2) -> p2.getPrimaryPreferenceCount() - p1.getPrimaryPreferenceCount());
    }

    public int pruneResolvedPrograms() {
        int before = unprocessedPrograms.size();
        unprocessedPrograms.removeIf(program -> program.isResolved());
        return before - unprocessedPrograms.size();
    }

    public int pruneResolvedParticipants() {
        int before = unprocessedParticipants.size();
        unprocessedParticipants.removeIf(participant -> participant.isResolved());
        return before - unprocessedParticipants.size();
    }


    public void printUnprocessedPrograms() {
        for (Program program : unprocessedPrograms) {
            System.out.println(program);
        }
    }


    public ArrayList<Program> getUnprocessedPrograms() {
        return unprocessedPrograms;
    }

    public ArrayList<Participant> getUnprocessedParticipants() {
        return unprocessedParticipants;
    }

    public LinkedHashMap<String,String> getStats() {
        LinkedHashMap stats = new LinkedHashMap<String,String>();
        stats.put("Participants", participants.size());
        stats.put("Unprocessed participants", unprocessedParticipants.size());
        stats.put("Programs", programs.size());
        stats.put("Unprocessed programs", unprocessedPrograms.size());
        stats.put("Total Fitness", calculateFitness());
        stats.put("Average Fitness", (float)calculateFitness()/(float)participants.size());
        stats.put("Total allocated / unalllocated slots", (participants.stream().mapToInt(p -> p.getAllocatedCount()).sum())+ " / " + (participants.stream().mapToInt(p -> p.getUnallocatedCount()).sum()));
        return stats;
    }

    @JsonIgnore
    public List<Participant> getUnallocated() {
        return getParticipants().stream().filter(p -> p.getAllocatedCount() < p.getPresentCount() && p.getOriginalPreferences().size() > 9).collect(Collectors.toList());
    }

    public List<Program> getProgramsWithTooFewParticipants() {
        return getPrograms().stream().filter(p -> (p.getAllocatedTimeSlots()[0] && p.getAssignedParticipants(0).size() < p.getMinPlaces())
                || (p.getAllocatedTimeSlots()[1] && p.getAssignedParticipants(1).size() < p.getMinPlaces())
                || (p.getAllocatedTimeSlots()[2] && p.getAssignedParticipants(2).size() < p.getMinPlaces()))
                .collect(Collectors.toList());
    }

    public void printStats() {
        LinkedHashMap stats = getStats();
        for (Object key : stats.keySet()) {
            System.out.println(key + ": " + stats.get(key));
        }
    }


    public ArrayList<Participant> getParticipants() {
        return participants;
    }

    public List<Program> getPrograms() {
        return new ArrayList<Program>(programs.values());
    }
}
