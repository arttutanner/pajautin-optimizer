package fi.partio.pajautin.optimizer.member;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class Participant {

    private static final Logger log = LogManager.getLogger(Participant.class);

    // uuid of the participant
    String id;

    Stack<Preference> preferences;

    List<Preference> originalPreferences;

    // Allocated preferences in each of the time slots [0...2]
    Preference[] allocatedPreferences;

    // When this participant is present
    boolean[] present;



    int fitness;

    public float random;



    public Participant(String id, Map<Object,Object> JSONData, Problem problem) {
        this.id = id;
        Object progs = JSONData.get("prog");
        if (progs instanceof List) {
            int order = 1;
            this.preferences=new Stack<>();
            for (Object pr : ((List)progs))
                this.preferences.add(new Preference(problem.getProgram((Integer)pr),order++));

            originalPreferences=new ArrayList<>(preferences);

            // Preferences will now be in reverse order (in stack), re-reverse the array
            Collections.reverse(preferences);

        }
        else {
            log.warn("Warning: Program list empty for participant "+id);
        }

        allocatedPreferences=new Preference[]{null,null,null};
        fitness=Problem.UNALLOCATED_FITNESS;

        present=new boolean[3];
        List<Boolean> bl = ((List<Boolean>)JSONData.get("present"));
        for (int i=0; i<bl.size(); i++) present[i]=bl.get(i);


    }

    public boolean assignFirstPreference() {
        if (preferences.size()==0) return false;
        //@TODO allocated to "smart" time slot, maybe not full etc
        /*
        if ((new Random().nextBoolean())) {
            for (int i=0; i<present.length; i++) {
                if (present[i]) {
                    if (assignFirstPreference(i)) return true;
                }
            }
        }
        else {
            for (int i=present.length-1; i>=0; i--) {
                if (present[i]) {
                    if (assignFirstPreference(i)) return true;
                }
            }

        }

         */
        Preference pref = preferences.peek();
        ArrayList<Integer> possibleSlots = new ArrayList<>();
        for (int i=0; i<present.length; i++) {
            if (present[i] && allocatedPreferences[i]==null && pref.getProgram().hasSpace(i) && pref.getProgram().getAllocatedTimeSlots()[i]) {
                possibleSlots.add(i);
            }
        }
        if (assingMultiplePreferencesSmartly(possibleSlots,pref.getProgram())) return true;

        // Something went wonky, try to assign to any slot
        log.warn("Could not assign participant "+id+" to any of the preferred slots, trying to assign to any slot.");
        for (int i=0; i<present.length; i++) {
            if (present[i]) {
                if (assignFirstPreference(i)) return true;
            }
        }

        // remove hopelessly unassignable preference
        preferences.pop();
        return false;
    }



    private boolean assingMultiplePreferencesSmartly(ArrayList<Integer> possibleSlots, Program program) {

        if (possibleSlots.size()==0) return false;
        // Easy case, only one possible slot
        if (possibleSlots.size()==1) return assignFirstPreference(possibleSlots.get(0));

        // If all of the slots are above minimum, assign to the one with least participants
        if (possibleSlots.stream().allMatch(slot -> program.getParticipantsInSlot(slot)>=program.getMinPlaces())) {
            int min = Integer.MAX_VALUE;
            int minSlot = -1;
            for (int slot : possibleSlots) {
                if (program.getParticipantsInSlot(slot) < min) {
                    min=program.getParticipantsInSlot(slot);
                    minSlot=slot;
                }
            }
            return assignFirstPreference(minSlot);
        }

        // if none of the slots are above minimum, assign to the one with most participants to fill at least one up
        if (possibleSlots.stream().allMatch(slot -> program.getParticipantsInSlot(slot)<program.getMinPlaces())) {
            int max = 0;
            int maxSlot = -1;
            for (int slot : possibleSlots) {
                if (program.getParticipantsInSlot(slot) >= max) {
                    max = program.getParticipantsInSlot(slot);
                    maxSlot = slot;
                }
            }
            return assignFirstPreference(maxSlot);
        }

        // See the next preference and assign to slot that does not interfere with it
        if (preferences.size()>2) {
            var nextPref = preferences.get(preferences.size()-2);

            for (int i=0; i<present.length; i++) {
                if (present[i] && allocatedPreferences[i]==null && nextPref.getProgram().hasSpace(i) && nextPref.getProgram().getAllocatedTimeSlots()[i]) {
                    // If there is more than one possible slots and the next preference can be assigned to one of them
                    // remove that slot from the possible slots
                    if (possibleSlots.size()>1) {
                        int finalI = i;
                        possibleSlots.removeIf(slot -> slot == finalI);
                    }
                }
            }
        }

        // Pick a random slot from the remaining possible ones
        int slot = possibleSlots.get(new Random().nextInt(possibleSlots.size()));
        return assignFirstPreference(slot);

    }

    public boolean assignFirstPreference(int timeSlot) {
        if (assignPreference(preferences.peek(),timeSlot)) {
            preferences.pop();
            return true;
        }
        else return false;
    }

    public boolean assignPreference(Preference pref, int timeSlot) {

        Program program = pref.getProgram();

        if (!present[timeSlot]) {
            log.info("Could not assign participant "+id+" to program with id "+program.getId()+" because participant is not present at timeslot "+(timeSlot+1));
            return false;
        }

        if (!program.allocatedTimeSlots[timeSlot]) {
            log.info("Could not assign participant "+id+" to program with id "+program.getId()+" because program is not active at timeslot "+(timeSlot+1));
            return false;
        }

        if (allocatedPreferences[timeSlot]!=null) {
            log.info("Could not assign participant "+id+" to program with id "+program.getId()+" because participant is already allocated at timeslot "+(timeSlot+1)+" to program "+allocatedPreferences[timeSlot].getProgramId());
            return false;
        }

        if (!program.hasSpace(timeSlot)) {
            log.warn("Could not assign participant "+id+" to program with id "+program.getId()+" in timeslot "+timeSlot+ " because program is full.");
            return false;
        }



        // If the program has countinueToSlot set, assign the participant to the next slot as well
        if (program.getCountinueOnSlot()!=null) {
            int cntSlot = program.getCountinueOnSlot()-1;
            if (!present[cntSlot]) {
                log.warn("Could not assign participant "+id+" to COUNTINUATION OF program with id "+program.getId()+" in timeslot "+cntSlot+ " because participant is not present.");
                return false;
            }
            // Create a preference that does not exist
            Program dummyPrg = new Program(program.getId()+1000,"Jatkuu: "+program.getName());
            Preference dummyPref = new Preference(dummyPrg,pref.order);
            if (getAllocatedPreferences()[cntSlot]!=null) {
                unAssignSlot(cntSlot);
            }
            allocatedPreferences[cntSlot]=dummyPref;

        }

        allocatedPreferences[timeSlot]=pref;
        program.assignParticipant(this,timeSlot);
        log.debug("Assigned participant "+id+" to program with id "+program.getId()+" on slot "+(timeSlot+1));
        return true;
    }

    public void unAssignSlot(int slot) {
        if (allocatedPreferences[slot]==null) return;
        allocatedPreferences[slot].getProgram().unAssignParticipant(this,slot);
        allocatedPreferences[slot]=null;
    }

    public Preference peekTopPreference() {
        if (preferences.size()==0) return null;
        return preferences.peek();
    }

    /**
     * Returns the preference order number of the top preference. If there is no top preference, return
     * int.max
     * @return
     */
    public int getTopPreferenceOrder() {
        if (preferences.size()==0) return Integer.MAX_VALUE;
        if (preferences.peek()==null) return Integer.MAX_VALUE;
        return preferences.peek().getOrder();
    }


    public int getFitness() {
        calculateFitness();
        return fitness;
    }

    private void calculateFitness() {
        // Fitness for unresolved slot  = number of wishes + 3, maximum 13
        int unalloc = originalPreferences.size() + 3;
        if (unalloc>13) unalloc=13;

        fitness = 0;
        for (int i = 0; i < allocatedPreferences.length; i++) {
            var pref = allocatedPreferences[i];
            if (pref==null && present[i]) {
                fitness += unalloc;
                continue;
            }
            else if (pref!=null)
                fitness+= pref.getOrder();

        }


    }

    public boolean hasProgramWithId(int id) {
        for (var p : allocatedPreferences)
            if (p!=null && p.getProgramId()==id) return true;
        return false;
    }

    public String getId() {
        return id;
    }

    @JsonIgnore
    public Stack<Preference> getPreferences() {
        return preferences;
    }

    @JsonIgnore
    public Preference[] getAllocatedPreferences() {
        return allocatedPreferences;
    }

    public List<Integer> getAllocatedProgramIds() {
        List<Integer> ids = new ArrayList<>();
        for (var p : allocatedPreferences) {
            if (p != null) ids.add(p.getProgramId());
            else ids.add(null);
        }
        return ids;
    }

    public boolean[] getPresent() {
        return present;
    }


    public boolean isCurrentlyFreeAtSlot(int slot) {
        if (!present[slot]) return false;
        return allocatedPreferences[slot]==null;
    }

    public int getAllocatedCount() {
        int allocatedCount =0;
        for (var p : allocatedPreferences)
            allocatedCount+= p == null ? 0 : 1;
        return allocatedCount;
    }

    public int getUnallocatedCount() {
        int unAllocatedCount =0;
        for (var p : allocatedPreferences)
            unAllocatedCount+= p == null ? 1 : 0;
        return unAllocatedCount;
    }

    public int getPresentCount() {
        int count =0;
        for (var p : present)  count+= p  ? 1 : 0;
        return count;
    }




    public boolean isResolved() {

        // Hopeless == resolved
        if (preferences.size()==0) return true;

        for (var p : allocatedPreferences)
            if (p==null) return false;
        return true;
    }

    public float getRandom() {
        return random;
    }

    public void setRandom(float random) {
        this.random = random;
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id='" + id + '\'' +
                ", preferencesCount=" + originalPreferences.size() +
                ", present=" + Arrays.toString(present) +
                ", fitness=" + getFitness() +
                ", allocatedCount=" + getAllocatedCount() +
                ", isResolved=" + isResolved() +
                ", assignedToPrograms=" + getAllocatedPrograms() +
                '}';
    }

    public String getAllocatedPrograms() {
        String ret = "[";
        for (var p : allocatedPreferences)
            ret += p == null ? "null," : p.getProgramId() + ", ";
        return ret + "]";
    }

    @JsonIgnore
    public List<Preference> getOriginalPreferences() {
        return originalPreferences;
    }

    public List<Integer> getOriginalPreferenceIds() {
        return originalPreferences.stream().map(p -> p.getProgramId()).collect(Collectors.toList());
    }

    public int getBestPreference() {
        int best = 999;
        for (var p : allocatedPreferences) {
            if (p != null) {
                if (p.getOrder() < best) best = p.getOrder();
            }
        }
        return best;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
