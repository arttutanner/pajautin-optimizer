package fi.partio.pajautin.optimizer.member;

public class Preference {

    int order;
    int programId;

    boolean resolved;

    Program program;

    int realizedTimeSlot;

    public Preference(Program program, int order) {
        this.order = order;
        this.programId = program.getId();
        this.resolved=false;
        this.program=program;
    }

    public void setResolved(int timeSlot) {
        this.resolved=true;
        this.realizedTimeSlot=timeSlot;
    }

    public int getOrder() {
        return order;
    }

    public int getProgramId() {
        return programId;
    }

    public boolean isResolved() {
        return resolved;
    }

    public Program getProgram() {
        return program;
    }

    public int getRealizedTimeSlot() {
        return realizedTimeSlot;
    }
}
