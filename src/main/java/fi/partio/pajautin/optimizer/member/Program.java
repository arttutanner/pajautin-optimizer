package fi.partio.pajautin.optimizer.member;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Program {

    private static final Logger log = LogManager.getLogger(Program.class);

    private Integer countinueOnSlot;

    public int MAX_SCORE = 30;

    private boolean isDummy;

    private Map<Object,Object> JSONData;

    String name;
    int id;

    // Timeslots where this program is possible to be
    boolean[] possibleTimeSlots;

    // Timeslots where this program has been allocated
    boolean[] allocatedTimeSlots;

    int minPlaces;

    int maxPlaces;

    // How many times this program can repeat
    int maxOccurance;

    // How many time this program actually repeats
    int realizedOccurance;

    // How many different combinations of time slot allocation this program can have
    int combinations;

    int possibleTimeSlotCount;

    int primaryPreferenceCount;

    boolean isResolved;

    int[] slotPreference;

    List<Participant> facilitators;


    HashMap<String, Participant>[] assignedParticipants;

    public Program (int id, String name) {
        this.id = id;
        this.name = name;
        this.isDummy = true;
    }

    public Program(Map<Object, Object> JSONData) {
        this.name = JSONData.get("name") + "";
        this.allocatedTimeSlots = new boolean[]{false, false, false};
        this.possibleTimeSlots = new boolean[]{false, false, false};
        possibleTimeSlots[0] = "TRUE".equals(JSONData.get("slot1"));
        possibleTimeSlots[1] = "TRUE".equals(JSONData.get("slot2"));
        possibleTimeSlots[2] = "TRUE".equals(JSONData.get("slot3"));
        this.maxPlaces = safeParseInt(JSONData.get("maxSize"), 30);
        this.minPlaces = safeParseInt(JSONData.get("minSize"), 5);
        this.maxOccurance = safeParseInt(JSONData.get("availableSlots"), 1);
        this.id = Integer.parseInt(JSONData.get("id") + "");
        if (JSONData.containsKey("countinueInSlot") && !JSONData.get("countinueInSlot").equals(""))
            this.countinueOnSlot = Integer.parseInt(JSONData.get("countinueInSlot") + "");
        this.JSONData = JSONData;

        possibleTimeSlotCount = 0;
        for (boolean b : possibleTimeSlots)
            if (b) possibleTimeSlotCount++;

        combinations = calculateCombinations(maxOccurance, possibleTimeSlotCount);
        assignedParticipants = new HashMap[]{new HashMap<>(), new HashMap<>(), new HashMap<>()};
        slotPreference = new int[]{0, 0, 0};
        facilitators = new ArrayList<>();
    }


    public void calculatePrimaryPreferenceCount(List<Participant> participantList) {
        primaryPreferenceCount = 0;
        for (Participant p : participantList)
            if (p.preferences.size() > 0 && p.preferences.peek().getProgramId() == id) primaryPreferenceCount++;
    }


    public int getPrimaryPreferenceCount() {
        return primaryPreferenceCount;
    }

    public int getFirstPossibleSlot() {
        for (int i = 0; i < possibleTimeSlots.length; i++)
            if (possibleTimeSlots[i]) return i;
        return -1;
    }

    public void assignToSlot(int slot) {
        int assignedSlotCount = 0;
        for (boolean as : allocatedTimeSlots)
            if (as) assignedSlotCount++;
        if (assignedSlotCount >= possibleTimeSlotCount) {
            log.info("Could not assign program with id " + id + " to slot " + slot + 1 + " since the program has already been assigned to maximum number of slots it can have.");
            return;
        }
        if (possibleTimeSlots[slot] == false) {
            log.info("Could not assign program with id " + id + " to slot " + slot + 1 + " since that slot is not possible for this program.");
            return;
        }
        allocatedTimeSlots[slot] = true;

        // If facilitator is assigned to this program, remove him from other programs and set him as not present
        for (Participant p : facilitators) {
            p.unAssignSlot(slot);
            p.getPresent()[slot] = false;
        }
    }

    /**
     * Calculate how many combinations this program can have
     * (i.e. how many ways it can appear in the time slots)
     */
    private int calculateCombinations(int occurance, int possibilities) {
        // Return trivial solution for 3 time slots
        if (possibilities == 1) return 1;
        if (possibilities == occurance) return 1;
        if (possibilities == 2 && occurance == 1) return 2;
        return 3; // 3 combinations for both cases 3-1 and 3-2

    }

    /**
     * Calculates how many participants wants (and is able to) to go to this program in each of the timeslots available

     *
     * @param participants Participant list used
     */
    public void calulateTimeslotPreference(ArrayList<Participant> participants) {
        for (int i=0; i<slotPreference.length; i++)
            slotPreference[i]=0;

        for (Participant participant : participants) {
            if (participant.getPreferences().size() > 0 && participant.peekTopPreference().getProgramId() == this.id) {
                for (int slot = 0; slot < possibleTimeSlots.length; slot++) {
                    if (possibleTimeSlots[slot] && participant.isCurrentlyFreeAtSlot(slot)) slotPreference[slot]++;
                }
            }
        }
    }

    /**
     * Finds most popular available time slot for this program, accoring to slotPreference
     * Assumes that calculateTimeslotPreference has been called before, otherwise just allocates first
     * available timeslot in the timeslot order
     * Finds timeslot that is:
     * - Most popular of the matching timeslots
     * - Not yet allocated
     * - Available timeslot for that program
     *
     * @return number of most popular timeslot, or -1 if there is no more timeslots available
     */
    public int findMostPopularFreeAndUnallocatedTimeSlot() {

        // No more occurances can be created.
        if (getAllocatedTimeSlotCount()>=maxOccurance) return  -1;

        int mostPopularSlot = -1;
        int mostPopularCount = 0;
        for (int i = 0; i < possibleTimeSlots.length; i++) {
            if (hasSpace(i) && possibleTimeSlots[i] && !allocatedTimeSlots[i] && slotPreference[i] > mostPopularCount) {
                mostPopularSlot = i;
                mostPopularCount = slotPreference[i];
            }
        }
        if (mostPopularCount==0) {
            if (getAllocatedTimeSlotCount()==0) {
                // if no timeslots have been allocated yet, allocate one timeslot at random
                ArrayList<Integer> possibleSlots = new ArrayList<>();
                for (int i = 0; i < possibleTimeSlots.length; i++) {
                    if (hasSpace(i) && possibleTimeSlots[i] && !allocatedTimeSlots[i]) {
                        possibleSlots.add(i);
                    }
                }
                if (possibleSlots.size()>0) {
                    mostPopularSlot = possibleSlots.get((int) (Math.random() * possibleSlots.size()));
                }
                else {
                    log.info("No more slots available for program with id " + id);
                    mostPopularSlot = -1;
                }
            }
            else {
                // if unpopular program has already been allocated to one timeslot, it is enough
                mostPopularSlot = -1;
            }

        }
        return mostPopularSlot;
    }

    public void increseSlotPreference(int slot, int increment) {
        slotPreference[slot] += increment;
    }

    public int getSlotPreference(int slot) {
        return slotPreference[slot];
    }

    public void resetSlotPreferences() {
        slotPreference[0] = 0;
        slotPreference[1] = 0;
        slotPreference[2] = 0;
    }

    public boolean hasSpace(int slot) {
        return assignedParticipants[slot].size() < maxPlaces;
    }

    public void assignParticipant(Participant p, int slot) {
        assignedParticipants[slot].put(p.getId(), p);
    }

    public void setResolved() {
        isResolved = true;
    }

    public void setResolved(boolean[] allocatedTimeSlots) {
        this.allocatedTimeSlots = allocatedTimeSlots;
        this.isResolved = true;
    }


    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean[] getPossibleTimeSlots() {
        return possibleTimeSlots;
    }

    public boolean[] getAllocatedTimeSlots() {
        return allocatedTimeSlots;
    }

    public int getAllocatedTimeSlotCount() {
        int count =0;
        for (int i=0; i<allocatedTimeSlots.length; i++) {
            if (allocatedTimeSlots[i]) count ++;
        }
        return count;
    }

    public int getParticipantsInSlot(int slot) {
        if (assignedParticipants[slot]==null) return 0;
        return assignedParticipants[slot].size();
    }

    public int getMinPlaces() {
        return minPlaces;
    }

    public int getMaxPlaces() {
        return maxPlaces;
    }

    public int getMaxOccurance() {
        return maxOccurance;
    }

    public int getRealizedOccurance() {
        return realizedOccurance;
    }

    public int getPossibleTimeSlotCount() {
        return possibleTimeSlotCount;
    }

    public int getCombinations() {

        return combinations;
    }

    public int getTotalAssignedParticipants() {
        int count = 0;
        for (int i=0; i<assignedParticipants.length; i++) {
            count+=assignedParticipants[i].size();
        }
        return count;
    }

    public boolean isResolved() {
        return isResolved;
    }

    @Override
    public String toString() {
        return "Program{" +
                "id=" + id +
                ", assigned participants=" + assignedParticipants[0].size() + "," + assignedParticipants[1].size() + "," + assignedParticipants[2].size() +
                ", maxPlaces=" + maxPlaces +
                ", minPlaces=" + minPlaces +

                ", activeTimeSlots=" + Arrays.toString(allocatedTimeSlots) +
                ", possibleTimeSlots=" + Arrays.toString(possibleTimeSlots) +
                ", timeSlotPreference=" + Arrays.toString(slotPreference) +
                ", occurances=" + maxOccurance +
                ", primaryPreferenceCount=" + primaryPreferenceCount +
                ", name='" + name + '\'' +
                '}';
    }


    private int safeParseInt(Object o, int def) {
        try {
            return Integer.parseInt("" + o);
        } catch (Exception e) {
            log.info("Warning, could not parse " + o + " using default value " + def);
            return def;
        }
    }

    public HashMap<String, Participant> getAssignedParticipants(int slot) {
        return assignedParticipants[slot];
    }

    public void unAssignParticipant(Participant participant, int slot) {
        assignedParticipants[slot].remove(participant.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Program program = (Program) o;
        return id == program.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public int[] getSlotPreference() {
        return slotPreference;
    }

    public List<List<String>> getAssignedParticipants() {
        List<List<String>> result = new ArrayList<>();
        for (int i=0; i<assignedParticipants.length; i++) {
            List<String> slot = new ArrayList<>();
            for (Participant p : assignedParticipants[i].values()) {
                slot.add(p.getId());
            }
            result.add(slot);
        }
        return result;
    }


    public void addFacilitator(Participant facilitator) {
        facilitators.add(facilitator);
    }

    public List<String> getFacilitators() {
        return facilitators.stream().map(f -> f.getId()).collect(Collectors.toList());
    }

    public Integer getCountinueOnSlot() {
        return countinueOnSlot;
    }

    public boolean isDummy() {
        return isDummy;
    }


    public Map<Object, Object> getJSONData() {
        return JSONData;
    }

    public void setJSONData(Map<Object, Object> JSONData) {
        this.JSONData = JSONData;
    }
}
