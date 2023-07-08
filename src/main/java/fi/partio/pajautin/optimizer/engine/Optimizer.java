package fi.partio.pajautin.optimizer.engine;

import fi.partio.pajautin.optimizer.member.Participant;
import fi.partio.pajautin.optimizer.member.Problem;
import fi.partio.pajautin.optimizer.member.Program;

import java.util.List;
import java.util.Random;

public abstract class Optimizer {

    Random random;

    protected Problem problem;

    public Optimizer(Problem problem) {
        this.problem = problem;
    }

    public abstract void optimize();

    public Random getRandom() {
        if (random==null) {
            random=new Random();
        }
        return random;
    }

    public int allocateFairly(Program program, List<Participant> participantList, int timeSlot) {

        // Sort participants first by top preference, then by random number
        // This way, participants with the same top preference will be allocated randomly
        // This should make the allocation more fair
        participantList.forEach(participant -> participant.setRandom(getRandom().nextFloat()));
        participantList.sort((p1, p2) -> Float.compare(p1.getRandom()+(float)p1.getTopPreferenceOrder(), p2.getRandom()+(float)p2.getTopPreferenceOrder()));

        // Try to allocate participants to their top preference as long there is space
        // Iterate based on "fairly" sorted list
        int count = 0;
        for (Participant participant : participantList) {
            if (program.hasSpace(timeSlot)) {
                if (participant.assignFirstPreference(timeSlot)) count++;
            }
        }

        return count;

    }



}
