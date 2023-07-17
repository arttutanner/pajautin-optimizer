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
    static final int FORCE_MOVE_TRESHOLD = 5;

    private static final Logger log = LogManager.getLogger(EagerOptimizer.class);


    public EagerOptimizer(Problem problem) {
        super(problem);
    }

    @Override
    public void optimize() {


        // Resolve trivial cases until there are no more
        for (int i = resolveTrivialCases(); i > 0; i = resolveTrivialCases()) {
            log.info("Resolved " + i + " trivial cases");

        }


        pruneResolvedParticipants();
        pruneResolvedPrograms();


        // Resolve non-trivial but timeslot-fixed cases until there are no more
        for (int i = resolveNonTrivialFixedCases(); i > 0; i = resolveNonTrivialFixedCases()) {
            log.info("Resolved " + i + " non-trivial fixed cases");

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
        if (i == 10000) {
            log.error("Reached maximum number of iterations. Probably stuck in a loop.");
        }


        tryToAllocateUnallocatedParticipants();

        tryToFindPeopleForProgramsUnderMinimum();

        printStuff();

    }

    private void tryToFindPeopleForProgramsUnderMinimum() {
        List<Program> programsWithTooFewParticipants = problem.getProgramsWithTooFewParticipants();
        for (var program : programsWithTooFewParticipants) {
                    if (program.getAllocatedTimeSlotCount()>1) {
                        log.warn("Program " + program + " has too few participants, but is allocated to multiple time slots. Consider decreasing number of maximum slot for program.");
                    }

                    for (int i=0; i<program.getAllocatedTimeSlots().length; i++) {
                        if (program.getAllocatedTimeSlots()[i] && program.getAssignedParticipants(i).size()<program.getMinPlaces()) {
                            tryToFindPersonsForProgram(program, i);
                        }
                    }
                }

    }

    private void tryToFindPersonsForProgram(Program program, int slot) {

        // check how many people we need to make the program above minimum
        int missing = program.getMinPlaces() - program.getAssignedParticipants(slot).size();
        log.debug("Trying to find "+missing+" persons for program " + program + " for slot " + slot+ " to make it above minimum.");
        if (missing > FORCE_MOVE_TRESHOLD) {
            log.warn("Program "+program+" has too few participants, but the number of missing participants is too high to force move people. Consider decreasing number of minimum slot for program.");
            return;
        }
        // Find all participants who
        // -are not in the program already
        // -have the program in their preferences
        // -moving them to this program would not make their original program below minimum
        List<PossibleSwap> swaps = problem.getParticipants().stream()
                .filter(p ->
                        !p.hasProgramWithId(program.getId()) &&
                        p.getOriginalPreferences().stream().anyMatch(pref ->
                                        pref.getProgramId() == program.getId()

                                ) &&
                        (p.getAllocatedPreferences()[slot]!=null &&
                                p.getAllocatedPreferences()[slot].getProgram().getAssignedParticipants(slot).size()
                                        >= p.getAllocatedPreferences()[slot].getProgram().getMinPlaces() + 1)
                ).map(p ->
                        new PossibleSwap(p, null,
                                p.getAllocatedPreferences()[slot],
                                p.getOriginalPreferences().stream().filter(pref -> pref.getProgramId() == program.getId()).findFirst().get(),
                                slot, 0)
                )
                .collect(Collectors.toList());

        // Calculate the fitness change for swaps
        swaps.forEach(swap -> {
            swap.setFitnessChange(swap.getFromPreference().getOrder()-swap.getToPreference().getOrder());
        });

        swaps.sort((s1, s2) -> s2.getFitnessChange() - s1.getFitnessChange());

        if (swaps.size()<missing) {
            log.warn("Program "+program+" has too few participants, but there are not enough people to move to it. Consider decreasing number of minimum slot for program.");
            return;
        }
        for (int i=0; i<missing; i++) {
            PossibleSwap swap = swaps.get(i);
            log.debug("Trying to move participant " + swap.getFromParticipant() + " to program " + program + " for slot " + slot);
            executeLocalSwap(swap);
        }


    }

    private void executeLocalSwap(PossibleSwap swap) {

        swap.getFromParticipant().getAllocatedPreferences()[swap.getSlot()] = swap.getToPreference();
        swap.getToPreference().getProgram().getAssignedParticipants(swap.getSlot()).put(swap.getFromParticipant().getId(), swap.getFromParticipant());
        swap.getFromPreference().getProgram().getAssignedParticipants(swap.getSlot()).remove(swap.getFromParticipant().getId());

    }

    private void tryToAllocateUnallocatedParticipants() {
        problem.getParticipants().stream().filter(p -> p.getAllocatedCount() < p.getPresentCount() && p.getOriginalPreferences().size() > 9).forEach(
                participant -> {
                    log.debug("Trying to allocate unallocated participant " + participant);
                    for (int i=0; i<participant.getPresent().length; i++) {
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

                                // Make sure that none of the participant already have the program that they are being swapped to
                                if (otherParticipant.hasProgramWithId(otherPreference.getProgramId()) || participant.hasProgramWithId(preference.getProgramId())) {
                                    log.debug("Swap is not possible because one of the participants already has the program that they are being swapped to");
                                }
                                else {
                                    possibleSwaps.add(new PossibleSwap(otherParticipant, participant, otherPreference, preference, slot, fitnessChangeForOtherParticipant + fitnessChangeForParticipant));
                                }
                            }
                        }
                );
            }
        }

        // Sort the swaps by fitness change

        possibleSwaps.sort((s1, s2) -> Integer.compare(s2.getFitnessChange(), s1.getFitnessChange()));
        if (possibleSwaps.size()>0) {
            log.debug("Executing swap"+possibleSwaps.get(0).toString()+" for participant "+participant+" for slot "+slot+" with fitness change "+possibleSwaps.get(0).getFitnessChange());
            executeSwap(possibleSwaps.get(0));
        }
        else
            log.warn("No possible swaps for participant " + participant + " for slot " + slot);

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
        problem.getProgramsWithTooFewParticipants().stream().forEach(p -> System.out.println(p));

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
