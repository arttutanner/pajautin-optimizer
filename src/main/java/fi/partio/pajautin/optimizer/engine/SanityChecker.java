package fi.partio.pajautin.optimizer.engine;

import fi.partio.pajautin.optimizer.member.Problem;

public class SanityChecker {

    public static boolean checkSanity(Problem problem) {
        return checkProgramsParametersFulfilled(problem) && checkParticipantsRequirementsFulfilled(problem);


    }

    private static boolean checkParticipantsRequirementsFulfilled(Problem problem) {
        return true;
    }

    public static boolean checkProgramsParametersFulfilled(Problem problem) {
        return true;
    }

}
