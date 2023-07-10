package fi.partio.pajautin.optimizer.engine;

import fi.partio.pajautin.optimizer.member.Participant;
import fi.partio.pajautin.optimizer.member.Problem;
import fi.partio.pajautin.optimizer.member.Program;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SanityChecker {

    private static final Logger log = LogManager.getLogger(SanityChecker.class);

    public static boolean checkSanity(Problem problem) {
        return checkProgramsParametersFulfilled(problem)
                && checkParticipantsRequirementsFulfilled(problem)
                && crossCheckProgramAndParticipant(problem);

    }

    private static boolean checkParticipantsRequirementsFulfilled(Problem problem) {

        return allProgramsFromThePreferences(problem) &&
                noDuplicatePrograms(problem) &&
                noProgramsWhileNotPresent(problem) &&
                noProgramAllocationWithProgramNotActive(problem);


    }

    public static boolean checkProgramsParametersFulfilled(Problem problem) {
        return noActiveMoreThanMaxOccurances(problem) &&
                noTooMuchParticipants(problem) &&
                noTooLittleParticipants(problem) &&
                noDuplicateParticipants(problem) ;


    }

    public static boolean crossCheckProgramAndParticipant(Problem problem) {
        for (Participant p : problem.getParticipants()) {
            for (int i = 0; i < p.getAllocatedPreferences().length; i++) {
                if (p.getAllocatedPreferences()[i] != null) {
                    if (!p.getAllocatedPreferences()[i].getProgram().getAssignedParticipants(i).containsKey(p.getId())) {
                        log.error("Participant " + p.getId() + " allocated to program " + p.getAllocatedPreferences()[i].getProgram().getId() + " while not present in the program");
                        return false;
                    }
                }
            }
        }

        for (Program p : problem.getPrograms()) {
            for (int i = 0; i < p.getAllocatedTimeSlots().length; i++) {
                if (p.getAllocatedTimeSlots()[i]) {
                    for (Participant participant : p.getAssignedParticipants(i).values()) {
                        if (!participant.getAllocatedPreferences()[i].getProgram().equals(p)) {
                            log.error("Participant " + participant.getId() + " allocated to program " + participant.getAllocatedPreferences()[i].getProgram().getId() + " while not present in the program");
                            return false;
                        }
                    }
                }
            }
        }
        log.info("Cross check of program and participant allocations passed");
        return true;
    }

    private static boolean noDuplicateParticipants(Problem problem) {

        for (Program p : problem.getPrograms()) {
            for (int slot = 0; slot < p.getAllocatedTimeSlots().length; slot++) {

                var possibleDuplicate = getFirstDuplicateEntry(p.getAssignedParticipants(slot).values());
                if (possibleDuplicate != null) {
                    log.error("Program " + p.getId() + " has duplicate participant " + ((Participant)possibleDuplicate).getId() + " in timeslot " + slot);
                    return false;
                }

            }
        }
        log.info("No duplicate participants in programs");
        return true;
    }

    private static boolean noTooLittleParticipants(Problem problem) {

        for (Program p : problem.getPrograms()) {
            for (int i = 0; i < p.getAllocatedTimeSlots().length; i++) {
                if (p.getAllocatedTimeSlots()[i]) {
                    if (p.getAssignedParticipants(i).size() < p.getMinPlaces()) {
                        log.error("Program " + p.getId() + " has too little participants in timeslot " + i);
                        // Let's not stop here, because this is not a fatal error
                        // return false;
                    }
                }
            }
        }
        //log.info("No too few participants in programs");
        return true;
    }

    private static boolean noTooMuchParticipants(Problem problem) {
        for (Program p : problem.getPrograms()) {
            for (int i = 0; i < p.getAllocatedTimeSlots().length; i++) {
                if (p.getAllocatedTimeSlots()[i]) {
                    if (p.getAssignedParticipants(i).size() > p.getMaxPlaces()) {
                        log.error("Program " + p.getId() + " has too much participants in timeslot " + i);
                        return false;
                    }
                }
            }
        }
        log.info("No too many participants in programs");
        return true;
    }

    private static boolean noActiveMoreThanMaxOccurances(Problem problem) {
        for (Program p : problem.getPrograms()) {
            int activeCount = 0;
            for (boolean b : p.getAllocatedTimeSlots()) {
                if (b) {
                    activeCount++;
                }
            }
            if (activeCount > p.getMaxOccurance()) {
                log.error("Program " + p.getId() + " has too many active timeslots");
                return false;
            }
        }
        log.info("No programs with too many active timeslots");
        return true;
    }



    private static boolean noProgramAllocationWithProgramNotActive(Problem problem) {
        for (Participant p : problem.getParticipants()) {
            for (int i = 0; i < p.getAllocatedPreferences().length; i++) {
                if (p.getAllocatedPreferences()[i] != null) {
                    if (!p.getAllocatedPreferences()[i].getProgram().getAllocatedTimeSlots()[i]) {
                        log.error("Program " + p.getAllocatedPreferences()[i].getProgram().getId() + " allocated to participant " + p.getId() + " while not active in the timeslot " + i);
                        return false;
                    }
                }
            }
        }
        log.info("No program allocations with program not active");
        return true;
    }

    private static boolean noProgramsWhileNotPresent(Problem problem) {
        for (Participant p : problem.getParticipants()) {
            for (int i = 0; i < p.getAllocatedPreferences().length; i++) {
                if (p.getAllocatedPreferences()[i] != null) {
                    if (!p.getPresent()[i]) {
                        log.error("Program " + p.getAllocatedPreferences()[i].getProgram().getId() + " allocated to participant " + p.getId() + " while not present");
                        return false;
                    }
                }
            }
        }
        log.info("No program allocations while not present");
        return true;
    }

    private static boolean noDuplicatePrograms(Problem problem) {
        for (Participant p : problem.getParticipants()) {
            for (int i = 0; i < p.getAllocatedPreferences().length; i++) {
                if (p.getAllocatedPreferences()[i] != null) {
                    for (int j = i + 1; j < p.getAllocatedPreferences().length; j++) {
                        if (p.getAllocatedPreferences()[j] != null) {
                            if (p.getAllocatedPreferences()[i].getProgram().getId() == p.getAllocatedPreferences()[j].getProgram().getId()) {
                                log.error("Duplicate program " + p.getAllocatedPreferences()[i].getProgram().getId() + " allocated to participant " + p.getId());
                                //return false;
                            }
                        }
                    }
                }
            }
        }
        log.info("No duplicate programs allocated to participants");
        return true;
    }

    private static boolean allProgramsFromThePreferences(Problem problem) {
        for (Participant p : problem.getParticipants()) {
            for (int i = 0; i < p.getAllocatedPreferences().length; i++) {
                if (p.getAllocatedPreferences()[i] != null) {
                    boolean found = false;
                    for (int j = 0; j < p.getOriginalPreferences().size(); j++) {
                        if (p.getAllocatedPreferences()[i].getProgram().getId() == p.getOriginalPreferences().get(j).getProgram().getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        log.error("Program " + p.getAllocatedPreferences()[i].getProgram().getId() + " allocated to participant " + p.getId() + " not in the preferences");
                        return false;
                    }
                }
            }
        }
        log.info("All programs allocated to participants are from their preferences");
        return true;
    }

    public static Object getFirstDuplicateEntry(Collection<? extends Object> objCol) {
        List<Object> list;
        if (objCol instanceof List) list = (List) objCol;
        else {
            list = new ArrayList<>(objCol);

        }


        for (int i=0; i<list.size(); i++) {
            for (int j=i+1; j<list.size(); j++) {
                if (list.get(i).equals(list.get(j))) {
                    return list.get(i);
                }
            }
        }
        return null;
    }

}
