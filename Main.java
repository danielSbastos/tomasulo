import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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

    Queue<LDInstruction> LDBuffers = new LinkedList();
    Queue<LDInstruction> memoryUnit = new LinkedList();

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

    private void print(String info) { System.out.println(info); }

    public void execute() {
        populateRInstructions();

        while (!instructions.isEmpty() || !FPAdders.isEmpty() ||
                    !FPAddersReservation.isEmpty() || !FPMultipliers.isEmpty() ||
                        !FPMultipliersReservation.isEmpty() || !LDBuffers.isEmpty() ||
				!memoryUnit.isEmpty()) {
            clock++;

            // Check ALUs and execute operation if clock count is done
            List<RInstruction> toRemoveAdders = new ArrayList<>();
            List<RInstruction> toRemoveMultipliers = new ArrayList<>();
            List<LDInstruction> toRemoveMemoryUnit = new ArrayList<>();
	    
            for (RInstruction instruction : FPAdders) {
                instruction.decrementClock();
                if (instruction.getRemainingClock() == 0) {
                    instruction.execute();
                    instruction.setRdBusy(false);
                    toRemoveAdders.add(instruction);
                }
            }
            for (RInstruction instruction : FPMultipliers) {
                instruction.decrementClock();
                if (instruction.getRemainingClock() == 0) {
                    instruction.execute();
                    instruction.setRdBusy(false);
                    toRemoveMultipliers.add(instruction);
                }
            }
            for (LDInstruction instruction : memoryUnit) {
                instruction.decrementClock();
                if (instruction.getRemainingClock() == 0) {
                    instruction.execute();
                    instruction.setRdBusy(false);
                    toRemoveMemoryUnit.add(instruction);
                }
            }
            for (RInstruction toRemove : toRemoveAdders) {
                FPAdders.remove(toRemove);
            }
            for (RInstruction toRemove : toRemoveMultipliers) {
                FPMultipliers.remove(toRemove);
            }
            for (LDInstruction toRemove : toRemoveMemoryUnit) {
                memoryUnit.remove(toRemove);
            }

            // Dequeue instructions one by one. If the registers are available, add instruction
            // to ALU, if not, add to the Reservation unit
            if (!instructions.isEmpty()) {
                Instruction instruction = instructions.remove();
                if (instruction.anySrcRegEmpty() || instruction.anyRegBusy()) {
                    if (instruction.getOp().equals("ADD") || instruction.getOp().equals("SUB")) {
                        FPAddersReservation.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("MUL") || instruction.getOp().equals("DIV")) {
                        FPMultipliersReservation.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("LOAD")) {
                        LDBuffers.add((LDInstruction) instruction);
		    }
                } else {
                    if (instruction.getOp().equals("ADD") || instruction.getOp().equals("SUB")) {
                        FPAdders.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("MUL") || instruction.getOp().equals("DIV")) {
                        FPMultipliers.add((RInstruction) instruction);
                    } else if (instruction.getOp().equals("LOAD")) {
			memoryUnit.add((LDInstruction) instruction);
		    }
                }

                instruction.setRdBusy(true);
            }

            // Check Reservation units, if regs are available, move instruction to ALU.
            List<RInstruction> toRemoveAddReserv = new ArrayList<>();
            List<RInstruction> toRemoveMulReserv = new ArrayList<>();
            List<LDInstruction> toRemoveLDBuffers = new ArrayList<>();
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
            for (LDInstruction instruction : LDBuffers) {
               if (instruction.noneSrcRegsEmpty()) {
		   print("adding to memory unit");
		   memoryUnit.add(instruction);
                   toRemoveLDBuffers.add(instruction);
                }
            }

            for (RInstruction toRemove : toRemoveAddReserv) {
                FPAddersReservation.remove(toRemove);
            }
            for (RInstruction toRemove : toRemoveMulReserv) {
                FPMultipliersReservation.remove(toRemove);
            }
            for (LDInstruction toRemove : toRemoveLDBuffers) {
                LDBuffers.remove(toRemove);
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
        System.out.println("============ LDBuffers ===========");
        System.out.println(LDBuffers);
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

    private void populateRInstructions() {
        instructions.add(new RInstruction("ADD", registers.get("F1"), registers.get("F2"), registers.get("F3"), 2));
        instructions.add(new RInstruction("MUL", registers.get("F4"), registers.get("F1"), registers.get("F2"), 1));
        instructions.add(new RInstruction("ADD", registers.get("F2"), registers.get("F1"), registers.get("F3"), 4));
        instructions.add(new RInstruction("ADD", registers.get("F5"), registers.get("F2"), registers.get("F4"), 1));
        instructions.add(new LDInstruction("LOAD", registers.get("F9"), registers.get("F5"), 1));
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

class LDInstruction implements Instruction {
    private String op;
    private int remainingClock;
    private Register rd;
    private Register rs;

    public LDInstruction(String op, Register rd, Register rs, int remainingClock) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.remainingClock = remainingClock;
    }

    public void execute() {
	if (op.equals("LOAD")) {
	    rd.setValue(rs.getValue());
	} 
    }

    public String getOp() { return op; }

    public void setRdBusy(boolean value) {}

    public boolean anySrcRegEmpty() { 
	return rs.isEmpty();
    }

    public boolean anyRegBusy() { 
	return rs.isBusy() || rd.isBusy();
    }

    public boolean noneSrcRegsEmpty() { 
        return !rs.isEmpty();
    }

    public void decrementClock() {
        remainingClock--;
    }

    public int getRemainingClock() {
        return remainingClock;
    }

    @Override
    public String toString() {
        return "[ op = " + op + " | rd = " + rd +
               " | rs = " + rs + " | clock = " + Integer.toString(remainingClock) + " ]";
    }
}

class RInstruction implements Instruction {
    private String op;
    private int remainingClock;
    private Register rd;
    private Register rs1;
    private Register rs2;

    public RInstruction(String op, Register rd, Register rs1, Register rs2, int remainingClock) {
        this.op = op;
        this.rd = rd;
        this.rs1 = rs1;
        this.rs2 = rs2;
        this.remainingClock = remainingClock;
    }

    public String getOp() { return op; }

    public void execute() {
        if (op.equals("ADD")) {
            rd.setValue(rs1.getValue() + rs2.getValue());
        } else if (op.equals("SUB")) {
            rd.setValue(rs1.getValue() - rs2.getValue());
        } else if (op.equals("MUL")) {
            rd.setValue(rs1.getValue() * rs2.getValue());
        } else if (op.equals("DIV")) {
            rd.setValue(rs1.getValue() / rs2.getValue());
        }
    }

    public boolean anyRegBusy() {
        return rd.isBusy() || rs1.isBusy() || rs2.isBusy();
    }

    public boolean noneSrcRegsEmpty() {
        return !rs1.isEmpty() && !rs2.isEmpty();
    }

    public boolean anySrcRegEmpty() {
        return rs1.isEmpty() || rs2.isEmpty();
    }

    public void setRdBusy(boolean status) {
        this.rd.setBusy(status);
    }

    public void decrementClock() {
        remainingClock--;
    }

    public int getRemainingClock() {
        return remainingClock;
    }

    @Override
    public String toString() {
        return "[ op = " + op + " | rd = " + rd +
               " | rs1 = " + rs1 + " | rs2 = " +
               rs2 + " | clock = " + Integer.toString(remainingClock) + " ]";
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
