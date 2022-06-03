public class RInstruction extends Instruction implements IInstruction {
    private Register rs1;
    private Register rs2;
    
    public RInstruction(String op, Register rd, Register rs1, Register rs2) {
        super(op, rd);
        this.rs1 = rs1;
        this.rs2 = rs2;
    }

    public void execute() {
        super.operations.get(super.op).accept(new Register[]{ rd, rs1, rs2 });
    }

    public boolean anyRegBusy() {
        return super.rd.isBusy() || this.rs1.isBusy() || this.rs2.isBusy();
    }

    public boolean noneSrcRegsEmpty() {
        return !rs1.isEmpty() && !rs2.isEmpty();
    }

    public boolean anySrcRegEmpty() {
        return rs1.isEmpty() || rs2.isEmpty();
    }

    @Override
    public String toString() {
        return "[ op = " + super.op + " | rd = " + super.rd +
               " | rs1 = " + rs1 + " | rs2 = " +
               rs2 + " | clock = " + Integer.toString(super.remainingClock) + " ]";
    }
}   