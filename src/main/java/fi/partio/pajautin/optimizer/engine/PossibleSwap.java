package fi.partio.pajautin.optimizer.engine;

import fi.partio.pajautin.optimizer.member.Participant;
import fi.partio.pajautin.optimizer.member.Preference;

public class PossibleSwap {

    Participant fromParticipant;
    Participant toParticipant;
    Preference fromPreference;
    Preference toPreference;
    int slot;
    int fitnessChange;

    public PossibleSwap(Participant fromParticipant, Participant toParticipant, Preference fromPreference, Preference toPreference, int slot, int fitnessChange) {
        this.fromParticipant = fromParticipant;
        this.toParticipant = toParticipant;
        this.fromPreference = fromPreference;
        this.toPreference = toPreference;
        this.slot = slot;
        this.fitnessChange = fitnessChange;
    }

    public Participant getFromParticipant() {
        return fromParticipant;
    }

    public void setFromParticipant(Participant fromParticipant) {
        this.fromParticipant = fromParticipant;
    }

    public Participant getToParticipant() {
        return toParticipant;
    }

    public void setToParticipant(Participant toParticipant) {
        this.toParticipant = toParticipant;
    }

    public Preference getFromPreference() {
        return fromPreference;
    }

    public void setFromPreference(Preference fromPreference) {
        this.fromPreference = fromPreference;
    }

    public Preference getToPreference() {
        return toPreference;
    }

    public void setToPreference(Preference toPreference) {
        this.toPreference = toPreference;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getFitnessChange() {
        return fitnessChange;
    }

    public void setFitnessChange(int fitnessChange) {
        this.fitnessChange = fitnessChange;
    }
}
