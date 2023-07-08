package fi.partio.pajautin.optimizer.engine;

import fi.partio.pajautin.optimizer.member.Participant;
import fi.partio.pajautin.optimizer.member.Preference;
import fi.partio.pajautin.optimizer.member.Problem;
import fi.partio.pajautin.optimizer.member.Program;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EagerOptimizer extends Optimizer {

    static final boolean LEAST_POPULAR_FIRST = true;

    private static final Logger log = LogManager.getLogger(EagerOptimizer.class);


    public EagerOptimizer(Problem problem) {
        super(problem);
    }

    @Override
    public void optimize() {


        // Resolve trivial cases until there are no more
        for (int i = resolveTrivialCases(); i > 0; i = resolveTrivialCases()) {
            log.info("Resolved " + i + " trivial cases");
            problem.printStats();
            System.out.println("-----------------------------------");
        }


        pruneResolvedParticipants();
        pruneResolvedPrograms();


        // Resolve non-trivial but timeslot-fixed cases until there are no more
        for (int i = resolveNonTrivialFixedCases(); i > 0; i = resolveNonTrivialFixedCases()) {
            log.info("Resolved " + i + " non-trivial fixed cases");
            problem.printStats();
            System.out.println("-----------------------------------");
        }

        pruneResolvedParticipants();
        pruneResolvedPrograms();

        // Allocate time slots for all the programs and assign participants to them when possible
        while (problem.getUnprocessedPrograms().size() > 0) {
            allocateMostPopularProgram();
            pruneResolvedParticipants();
            pruneResolvedPrograms();
        }


        // Assign remaining participants to programs
        // Begin with participants with the worst fitness (i.e. most unfairly treated)
        int i = 0;
        while (problem.getUnprocessedParticipants().size() > 0 || i > 10000) {
            i++;
            assignWorstOfParticipantToProgram();
            pruneResolvedParticipants();
            pruneResolvedPrograms();
        }
        //System.out.println("Iterations: " + i);


        tryToAllocateUnallocatedParticipants();

        printStuff();

    }

    private void tryToAllocateUnallocatedParticipants() {
        problem.getParticipants().stream().filter(p -> p.getAllocatedCount() < p.getPresentCount() && p.getOriginalPreferences().size() > 9).forEach(
                participant -> {
                    for (int i=0; i<participant.getPresentCount(); i++) {
                        if (participant.getPresent()[i] && participant.getAllocatedPreferences()[i] == null) {
                            tryToAllocateParticipantForSlot(participant, i);
                        }
                    }

                }
        );

    }

    private void tryToAllocateParticipantForSlot(Participant participant, int slot) {
        // Get preferences that match the time slot
        List<Preference> preferencesInSlot = participant.getOriginalPreferences().stream().filter(p -> p.getProgram().getAllocatedTimeSlots()[slot]).collect(Collectors.toList());
        if (preferencesInSlot.size()==0) {
            log.info("Hopeless case for re-allocation. No preferences for slot " + slot + " for participant " + participant);
            return;
        }

        // Find possible swaps that could be made to fill the slot
        ArrayList<PossibleSwap> possibleSwaps = new ArrayList<>();
        for (Preference preference : preferencesInSlot) {
            for (Participant otherParticipant : preference.getProgram().getAssignedParticipants(slot).values()) {
                otherParticipant.getPreferences().stream().filter(p -> p.getProgram().getAllocatedTimeSlots()[slot]).forEach(
                        otherPreference -> {
                            if (otherPreference.getProgram().getAssignedParticipants(slot).size() < otherPreference.getProgram().getMaxPlaces()) {
                                int fitnessChangeForParticipant=13-preference.getOrder(); // assume that the participant has the worst possible fitness for this slot
                                int fitnessChangeForOtherParticipant=otherParticipant.getAllocatedPreferences()[slot].getOrder()-otherPreference.getOrder();
                                possibleSwaps.add(new PossibleSwap(otherParticipant, participant, otherPreference, preference, slot, fitnessChangeForOtherParticipant+fitnessChangeForParticipant));
                            }
                        }
                );
            }
        }

        // Sort the swaps by fitness change

        possibleSwaps.sort((s1, s2) -> Integer.compare(s2.getFitnessChange(), s1.getFitnessChange()));
        if (possibleSwaps.size()>0)
            executeSwap(possibleSwaps.get(0));
        else
            log.info("No possible swaps for participant " + participant + " for slot " + slot);

    }


    private void executeSwap(PossibleSwap swap) {
        swap.getFromParticipant().unAssignSlot(swap.getSlot());
        // now there should be free space
        swap.getToParticipant().assignPreference(swap.getToPreference(), swap.getSlot());
        // and the other guy should also fit somewhere...
        if (!allocateOneSlotForParticipant(swap.getFromParticipant())) {
            log.error("Could not allocate slot for (from) participant " + swap.getFromParticipant() + " after swap");
        }


    }


    private void printStuff() {

        for (Program p : problem.getPrograms()) {
            System.out.println(p);
        }
        AtomicInteger ct = new AtomicInteger();
        problem.getParticipants().stream().filter(p -> p.getAllocatedCount() < p.getPresentCount() && p.getOriginalPreferences().size() > 9).forEach(
                p -> {
                    System.out.println(p);
                    ct.getAndIncrement();
                });
        System.out.println("Unallocated: " + ct);

        System.out.println("PROGRAMS WITH LESS THAN MINIMUM PARTICIPANTS");
        problem.getPrograms().stream().filter(p -> (p.getAllocatedTimeSlots()[0] && p.getAssignedParticipants(0).size() < p.getMinPlaces())
                || (p.getAllocatedTimeSlots()[1] && p.getAssignedParticipants(1).size() < p.getMinPlaces())
                || (p.getAllocatedTimeSlots()[2] && p.getAssignedParticipants(2).size() < p.getMinPlaces())).forEach(
                p -> {
                    System.out.println(p);
                });

        problem.printStats();
    }

    private void assignWorstOfParticipantToProgram() {

        problem.getUnprocessedParticipants().stream().forEach(participant -> participant.setRandom(getRandom().nextFloat()));
        // Find participant with the worst fitness + random
        Optional<Participant> worst = problem.getUnprocessedParticipants().stream().sorted((p2, p1) -> Float.compare(p1.getFitness() + p1.getRandom(), p2.getFitness() + p2.getRandom())).findFirst();

        if (worst.isPresent()) allocateOneSlotForParticipant(worst.get());


    }

    private boolean allocateOneSlotForParticipant(Participant participant) {
        boolean success = false;
        while (!success && !participant.isResolved()) {
            success = participant.assignFirstPreference();
        }
        return success;
    }


    /**
     * Resolves trivial cases, i.e. cases where first choice can be fulfilled and the program has only one possible time slot.
     *
     * @return number of resolved trivial cases
     */
    private int resolveTrivialCases() {

        problem.calculatePrimaryPreferenceCountForProgram();
        AtomicInteger count = new AtomicInteger();
        problem.getUnprocessedPrograms()
                .stream()
                .filter(p -> p.getCombinations() == 1 && p.getPossibleTimeSlotCount() == 1 && p.getPrimaryPreferenceCount() <= p.getMaxPlaces() && p.getPrimaryPreferenceCount() >= p.getMinPlaces())
                .forEach(prog -> {

                    int firstPossibleSlot = prog.getFirstPossibleSlot();
                    prog.assignToSlot(firstPossibleSlot);
                    prog.setResolved();
                    problem.getUnprocessedParticipants().stream().filter(participant -> participant.getPreferences().size() > 0
                                    && participant.getPreferences().peek().getProgramId() == prog.getId())
                            .forEach(participant -> {
                                        if (participant.assignFirstPreference(firstPossibleSlot)) count.getAndIncrement();

                                    }
                            );

                });


        return count.get();
    }


    /**
     * Resolves trivial cases, i.e. cases where first choice can be fulfilled and the program has only one possible time slot.
     *
     * @return number of resolved trivial cases
     */
    private int resolveNonTrivialFixedCases() {
        problem.sortProgramsByPrimaryPreferenceCount();
        AtomicInteger count = new AtomicInteger();
        problem.getUnprocessedPrograms().stream().filter(p -> p.getCombinations() == 1 && p.getPossibleTimeSlotCount() == 1).forEach(prog -> {

            int slot = prog.getFirstPossibleSlot();
            prog.assignToSlot(slot);
            prog.setResolved();
            count.getAndAdd(allocateFairly(prog, problem.getUnprocessedParticipants().stream().filter(participant -> participant.getPreferences().size() > 0
                            && participant.getPreferences().peek().getProgramId() == prog.getId())
                    .collect(Collectors.toList()), slot));
        });

        return count.get();
    }

    /**
     * Allocates most popular program to participants and removes it from the list of unprocessed programs.
     * Also fix the program's time slot.
     */
    private void allocateMostPopularProgram() {
        problem.sortProgramsByPrimaryPreferenceCount();
        Program program;
        if (LEAST_POPULAR_FIRST)
            program = problem.getUnprocessedPrograms().get(problem.getUnprocessedPrograms().size() - 1);
        else
            program = problem.getUnprocessedPrograms().get(0);

        int slot;
        do {
            program.calulateTimeslotPreference(problem.getUnprocessedParticipants());
            slot = program.findMostPopularFreeAndUnallocatedTimeSlot();
            log.debug("Most popular free slot for program " + program.getId() + " is " + slot);
            if (slot != -1) {
                program.assignToSlot(slot);
                int allocated = 0;
                do {
                    allocated = allocateFairly(program, problem.getUnprocessedParticipants().stream().filter(participant -> participant.getPreferences().size() > 0
                                    && participant.getPreferences().peek().getProgramId() == program.getId())
                            .collect(Collectors.toList()), slot);
                    log.debug("Allocated " + allocated + " participants to program " + program.getId() + " in slot " + slot);
                } while (allocated > 0);
            }
        } while (slot != -1);
        program.setResolved();

        distributeEqually(program);

    }


    private void distributeEqually(Program program) {
        // If there is 0 or 1 active slots, nothing to see here
        if (program.getAllocatedTimeSlotCount() < 2) return;
    }


    private void pruneResolvedParticipants() {
        // Prune resolved participants
        int pruned = problem.pruneResolvedParticipants();
        log.info("Pruned " + pruned + " resolved participants");
    }


    private void pruneResolvedPrograms() {
        // Prune resolved programs
        int pruned = problem.pruneResolvedPrograms();
        problem.pruneResolvedPrograms();
        log.info("Pruned " + pruned + " resolved programs");
    }


}
