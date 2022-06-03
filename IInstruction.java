public interface IInstruction {
    public boolean anySrcRegEmpty();
    public boolean anyRegBusy();
    public boolean noneSrcRegsEmpty();
    public String getOp();
    public void setRdBusy(boolean value);
    public void decrementClock();
    public int getRemainingClock();
    public void execute();
}