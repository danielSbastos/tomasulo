import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        Tomasulo t = new Tomasulo();
        t.execute();
    }
}


class Tomasulo {
    Queue<Instruction> instructions = new LinkedList<>();

    Queue<RInstruction> FPAddersReservation = new LinkedList<>();
    Queue<RInstruction> FPAdders = new LinkedList<>();

    Queue<RInstruction> FPMultipliersReservation = new LinkedList<>();
    Queue<RInstruction> FPMultipliers = new LinkedList<>();

    Queue<LDSTInstruction> LDBuffer = new LinkedList<>();
    Queue<LDSTInstruction> STBuffer = new LinkedList<>();
    Queue<LDSTInstruction> memoryUnit = new LinkedList<>();

    int clock = 0;

    Map<String, Register> registers = new HashMap<>() {{
        put("F0", new Register("F0", false));
        put("F1", new Register("F1", false));
        put("F2", new Register("F2", false, (float) 2));
        put("F3", new Register("F3", false, (float) 3));
        put("F4", new Register("F4", false));
        put("F5", new Register("F5", false));
        put("F6", new Register("F6", false));
        put("F7", new Register("F7", false));
        put("F8", new Register("F8", false));
        put("F9", new Register("F9", false));
        put("F10", new Register("F10", false));
        put("F11", new Register("F11", false));
    }};

    private <T> void executeAndRemove(Queue<T> queue) {
        List<Instruction> toRemove = new ArrayList<>();

        for (T instruction : queue) {
            ((Instruction) instruction).decrementClock();
            if (((Instruction) instruction).getRemainingClock() == 0) {
                ((Instruction) instruction).execute();
                ((Instruction) instruction).setRdBusy(false);
                toRemove.add(((Instruction) instruction));
            }
        }

        for (Instruction _toRemove : toRemove) {
            queue.remove(_toRemove);
        }
    }

    public void execute() {
        populateInstructions();

        while (!instructions.isEmpty() || !FPAdders.isEmpty() ||
                !FPAddersReservation.isEmpty() || !FPMultipliers.isEmpty() ||
                !FPMultipliersReservation.isEmpty() || !LDBuffer.isEmpty() ||
                !STBuffer.isEmpty() || !memoryUnit.isEmpty()) {
            clock++;

            executeAndRemove(FPAdders);
            executeAndRemove(FPMultipliers);
            executeAndRemove(memoryUnit);

            // Dequeue instructions one by one. If the registers are available, add
            // instruction
            // to ALU, if not, add to the Reservation unit
            if (!instructions.isEmpty()) {
                Instruction instruction = instructions.remove();
                if (instruction.anySrcRegEmpty() || instruction.anyRegBusy()) {
                    if (instruction.getOp().equals("ADD") || instruction.getOp().equals("SUB")) {
                        FPAddersReservation.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("MUL") || instruction.getOp().equals("DIV")) {
                        FPMultipliersReservation.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("LOAD")) {
                        LDBuffer.add((LDSTInstruction) instruction);
                    } else if (instruction.getOp().equals("STORE")) {
                        STBuffer.add((LDSTInstruction) instruction);
                    }
                } else {
                    if (instruction.getOp().equals("ADD") || instruction.getOp().equals("SUB")) {
                        FPAdders.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("MUL") || instruction.getOp().equals("DIV")) {
                        FPMultipliers.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("LOAD") || instruction.getOp().equals("STORE") ) {
                        memoryUnit.add((LDSTInstruction) instruction);
                    }
                }

                instruction.setRdBusy(true);
            }

            // Check Reservation units, if regs are available, move instruction to ALU.
            List<RInstruction> toRemoveAddReserv = new ArrayList<>();
            List<RInstruction> toRemoveMulReserv = new ArrayList<>();
            List<LDSTInstruction> toRemoveLDBuffer = new ArrayList<>();
            List<LDSTInstruction> toRemoveSTBuffer = new ArrayList<>();
            for (RInstruction instruction : FPAddersReservation) {
                if (instruction.noneSrcRegsEmpty()) {
                    FPAdders.add(instruction);
                    toRemoveAddReserv.add(instruction);
                }
            }
            for (RInstruction instruction : FPMultipliersReservation) {
                if (instruction.noneSrcRegsEmpty()) {
                    FPMultipliers.add(instruction);
                    toRemoveMulReserv.add(instruction);
                }
            }
            for (LDSTInstruction instruction : LDBuffer) {
                if (instruction.noneSrcRegsEmpty()) {
                    memoryUnit.add(instruction);
                    toRemoveLDBuffer.add(instruction);
                }
            }
            for (LDSTInstruction instruction : STBuffer) {
                if (instruction.noneSrcRegsEmpty()) {
                    memoryUnit.add(instruction);
                    toRemoveSTBuffer.add(instruction);
                }
            }

            for (RInstruction toRemove : toRemoveAddReserv) {
                FPAddersReservation.remove(toRemove);
            }
            for (RInstruction toRemove : toRemoveMulReserv) {
                FPMultipliersReservation.remove(toRemove);
            }
            for (LDSTInstruction toRemove : toRemoveLDBuffer) {
                LDBuffer.remove(toRemove);
            }
            for (LDSTInstruction toRemove : toRemoveSTBuffer) {
                STBuffer.remove(toRemove);
            }

            print();
        }
    }

    private void print() {
        System.out.println("\n======================================================");
        System.out.println("Clock: " + clock);
        System.out.println("============ Instructions ===========");
        System.out.println(instructions);
        System.out.println("============ FPAdders ===========");
        System.out.println(FPAdders);
        System.out.println("============ FPAddersReservation ===========");
        System.out.println(FPAddersReservation);
        System.out.println("============ FPMultipliers ===========");
        System.out.println(FPMultipliers);
        System.out.println("============ FPMultipliersReservation ===========");
        System.out.println(FPMultipliersReservation);
        System.out.println("============ LDBuffer ===========");
        System.out.println(LDBuffer);
        System.out.println("============ STBuffer ===========");
        System.out.println(STBuffer);
        System.out.println("============ MemoryUnit ===========");
        System.out.println(memoryUnit);
        printRegisters();
    }

    private void printRegisters() {
        System.out.println(
            "F0: " + registers.get("F0") + 
            " | F1: " + registers.get("F1") +
            " | F2: " + registers.get("F2") +
            " | F3: " + registers.get("F3") +
            " | F4: " + registers.get("F4") +
            " | F5: " + registers.get("F5") +
            " | F6: " + registers.get("F6") +
            " | F7: " + registers.get("F7") +
            " | F8: " + registers.get("F8") +
            " | F9: " + registers.get("F9") +
            " | F10: " + registers.get("F10") +
            " | F11: " + registers.get("F11")
        );
    }

    private void populateInstructions() {
        instructions.add(new RInstruction("ADD", registers.get("F1"), registers.get("F2"), registers.get("F3"), 2));
        instructions.add(new RInstruction("MUL", registers.get("F4"), registers.get("F1"), registers.get("F2"), 1));
        instructions.add(new RInstruction("ADD", registers.get("F2"), registers.get("F1"), registers.get("F3"), 4));
        instructions.add(new RInstruction("ADD", registers.get("F5"), registers.get("F2"), registers.get("F4"), 1));
        instructions.add(new LDSTInstruction("LOAD", registers.get("F9"), registers.get("F5"), 1));
        instructions.add(new LDSTInstruction("STORE", registers.get("F9"), registers.get("F0"), 1));
    }
}

interface Instruction {
    public boolean anySrcRegEmpty();
    public boolean anyRegBusy();
    public boolean noneSrcRegsEmpty();
    public String getOp();
    public void setRdBusy(boolean value);
    public void decrementClock();
    public int getRemainingClock();
    public void execute();
}

class InstructionC {
    String op;
    int remainingClock;
    Register rd;
    Map<String, Consumer<Register[]>> operations = new HashMap<>(){{
        put("ADD", (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() + rgs[2].getValue()));
        put("SUB", (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() - rgs[2].getValue()));
        put("MUL", (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() * rgs[2].getValue()));
        put("DIV", (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() / rgs[2].getValue()));
    }};

    public InstructionC(String op, Register rd, int remainingClock) {
        this.op = op;
        this.rd = rd;
        this.remainingClock = remainingClock;
    }

    public String getOp() { return op; }

    public void setRdBusy(boolean value) {
        rd.setBusy(value);
    }

    public void decrementClock() {
        remainingClock--;
    }

    public int getRemainingClock() {
        return remainingClock;
    }
}

class LDSTInstruction extends InstructionC implements Instruction {
    private Register rs;

    public LDSTInstruction(String op, Register rd, Register rs, int remainingClock) {
        super(op, rd, remainingClock);

        if (op.equals("STORE")) {
            super.rd = rs;
            this.rs = rd;
        }  else {
            this.rs = rs;
        }
    }

    public void execute() {
	    super.rd.setValue(rs.getValue());
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

class RInstruction extends InstructionC implements Instruction {
    private Register rs1;
    private Register rs2;
    
    public RInstruction(String op, Register rd, Register rs1, Register rs2, int remainingClock) {
        super(op, rd, remainingClock);
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

class Register {
    private Float value;
    private boolean busy;
    private String name;

    public Register(String name, boolean busy) {
        this.name = name;
        this.busy = busy;
    }

    public Register(String name, boolean busy, Float value) {
        this.name = name;
        this.busy = busy;
        this.value = value;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean val) {
        this.busy = val;
    }

    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public String toString() {
        return value == null ? name : value.toString();
    }
}
