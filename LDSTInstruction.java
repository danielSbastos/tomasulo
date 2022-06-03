public class LDSTInstruction extends Instruction implements IInstruction {
    private Register rs;

    public LDSTInstruction(String op, Register rd, Register rs) {
        super(op, rd);

        if (op.equals(Instruction.STORE)) {
            super.rd = rs;
            this.rs = rd;
        }  else {
            this.rs = rs;
        }
    }

    public void execute() {
        super.operations.get(super.op).accept(new Register[]{ rd, rs });
    }

    public boolean anySrcRegEmpty() { 
        return rs.isEmpty();
    }

    public boolean anyRegBusy() { 
	    return rs.isBusy() || super.rd.isBusy();
    }

    public boolean noneSrcRegsEmpty() { 
        return !rs.isEmpty();
    }

    @Override
    public String toString() {
        return "[ op = " + super.op + " | rd = " + super.rd +
               " | rs = " + rs + " | clock = " + Integer.toString(super.remainingClock) + " ]";
    }
}


