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
    Queue<Instruction> FPAddersReservation = new LinkedList<>();
    Queue<Instruction> FPAdders = new LinkedList<>();
    Queue<Instruction> FPMultipliersReservation = new LinkedList<>();
    Queue<Instruction> FPMultipliers = new LinkedList<>();

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

    public void execute() {
        populateInstructions();

        while (!instructions.isEmpty() || !FPAdders.isEmpty() ||
                    !FPAddersReservation.isEmpty() || !FPMultipliers.isEmpty() ||
                        !FPMultipliersReservation.isEmpty()) {
            clock++;

            // Check ALUs and execute operation if clock count is done
            List<Instruction> toRemoveAdders = new ArrayList<>();
            List<Instruction> toRemoveMultipliers = new ArrayList<>();
            for (Instruction instruction : FPAdders) {
                instruction.decrementClock();
                if (instruction.getRemainingClock() == 0) {
                    instruction.execute();
                    instruction.setRdBusy(false);
                    toRemoveAdders.add(instruction);
                }
            }
            for (Instruction instruction : FPMultipliers) {
                instruction.decrementClock();
                if (instruction.getRemainingClock() == 0) {
                    instruction.execute();
                    instruction.setRdBusy(false);
                    toRemoveMultipliers.add(instruction);
                }
            }
            for (Instruction toRemove : toRemoveAdders) {
                FPAdders.remove(toRemove);
            }
            for (Instruction toRemove : toRemoveMultipliers) {
                FPMultipliers.remove(toRemove);
            }

            // Dequeue instructions one by one. If the registers are available, add instruction
            // to ALU, if not, add to the Reservation unit
            if (!instructions.isEmpty()) {
                Instruction instruction = instructions.remove();
                if (instruction.anySrcRegEmpty() || instruction.anyRegBusy()) {
                    if (instruction.isAdd()) {
                        FPAddersReservation.add(instruction);
                    } else {
                        FPMultipliersReservation.add(instruction);
                    }
                } else {
                    if (instruction.isAdd()) {
                        FPAdders.add(instruction);
                    } else {
                        FPMultipliers.add(instruction);
                    }
                }

                instruction.setRdBusy(true);
            }

            // Check Reservation units, if regs are available, move instruction to ALU.
            List<Instruction> toRemoveAddReserv = new ArrayList<>();
            List<Instruction> toRemoveMulReserv = new ArrayList<>();
            for (Instruction instruction : FPAddersReservation) {
                if (instruction.noneSrcRegsEmpty()) {
                    FPAdders.add(instruction);
                    toRemoveAddReserv.add(instruction);
                }
            }
            for (Instruction instruction : FPMultipliersReservation) {
               if (instruction.noneSrcRegsEmpty()) {
                    FPMultipliers.add(instruction);
                    toRemoveMulReserv.add(instruction);
                }
            }

            for (Instruction toRemove : toRemoveAddReserv) {
                FPAddersReservation.remove(toRemove);
            }
            for (Instruction toRemove : toRemoveMulReserv) {
                FPMultipliersReservation.remove(toRemove);
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
        instructions.add(new Instruction("ADD", registers.get("F1"), registers.get("F2"), registers.get("F3"), 2));
        instructions.add(new Instruction("MUL", registers.get("F4"), registers.get("F1"), registers.get("F2"), 1));
        instructions.add(new Instruction("ADD", registers.get("F2"), registers.get("F1"), registers.get("F3"), 4));
        instructions.add(new Instruction("ADD", registers.get("F5"), registers.get("F2"), registers.get("F4"), 1));

    }
}

class Instruction {
    private String op;
    private Register rd;
    private Register rs1;
    private Register rs2;
    private int remainingClock;

    public Instruction(String op, Register rd, Register rs1, Register rs2, int remainingClock) {
        this.op = op;
        this.rd = rd;
        this.rs1 = rs1;
        this.rs2 = rs2;
        this.remainingClock = remainingClock;
    }

    public boolean isAdd() {
        return op == "ADD" || op == "SUB";
    }

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
