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
    int clock = 0;

    Queue<Instruction> instructions = new LinkedList<>();

    Queue<RInstruction> FPAddersReservation = new LinkedList<>();
    Queue<RInstruction> FPAdders = new LinkedList<>();

    Queue<RInstruction> FPMultipliersReservation = new LinkedList<>();
    Queue<RInstruction> FPMultipliers = new LinkedList<>();

    Queue<LDSTInstruction> LDBuffer = new LinkedList<>();
    Queue<LDSTInstruction> STBuffer = new LinkedList<>();
    Queue<LDSTInstruction> memoryUnit = new LinkedList<>();

    Map<String, Map<Boolean, Queue>> locateInstruction = new HashMap<>(){{
        put(InstructionBase.ADD, new HashMap<Boolean, Queue>(){{
            put(true, FPAddersReservation);
            put(false, FPAdders);
        }});
        put(InstructionBase.SUB, new HashMap<Boolean, Queue>(){{
            put(true, FPAddersReservation);
            put(false, FPAdders);
        }});
        put(InstructionBase.MUL, new HashMap<Boolean, Queue>(){{
            put(true, FPMultipliersReservation);
            put(false, FPMultipliers);
        }});
        put(InstructionBase.DIV, new HashMap<Boolean, Queue>(){{
            put(true, FPMultipliersReservation);
            put(false, FPMultipliers);
        }});
        put(InstructionBase.LOAD, new HashMap<Boolean, Queue>(){{
            put(true, LDBuffer);
            put(false, memoryUnit);
        }});
        put(InstructionBase.STORE, new HashMap<Boolean, Queue>(){{
            put(true, STBuffer);
            put(false, memoryUnit);
        }});
    }};

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
                !FPMultipliersReservation.isEmpty() || !LDBuffer.isEmpty() ||
                !STBuffer.isEmpty() || !memoryUnit.isEmpty()) {
            clock++;

            executeInstruction(FPAdders);
            executeInstruction(FPMultipliers);
            executeInstruction(memoryUnit);

            if (!instructions.isEmpty()) popAndMoveInstruction();

            moveInstructionToExecutionUnit(FPAddersReservation, FPAdders);
            moveInstructionToExecutionUnit(FPMultipliersReservation, FPMultipliers);
            moveInstructionToExecutionUnit(LDBuffer, memoryUnit);
            moveInstructionToExecutionUnit(STBuffer, memoryUnit);

            print();
        }
    }

    private <T> void executeInstruction(Queue<T> queue) {
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

    private <T> void moveInstructionToExecutionUnit(Queue<T> buffer, Queue<T> executionUnit) {
        List<Instruction> toRemove = new ArrayList<>();

        for (T instruction : buffer) {
            if (((Instruction) instruction).noneSrcRegsEmpty()) {
                executionUnit.add(instruction);
                toRemove.add(((Instruction) instruction));
            }
        }

        for (Instruction _toRemove : toRemove) {
            buffer.remove(_toRemove);
        }
    }

    private void popAndMoveInstruction() {
        Instruction instruction = instructions.remove();
        boolean toBuffer = instruction.anySrcRegEmpty() || instruction.anyRegBusy();

        Queue<Instruction> queueSrc = locateInstruction.get(instruction.getOp()).get(toBuffer);
        queueSrc.add(instruction);
        instruction.setRdBusy(true);
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
        instructions.add(new RInstruction(InstructionBase.ADD, registers.get("F1"), registers.get("F2"), registers.get("F3")));
        instructions.add(new RInstruction(InstructionBase.MUL, registers.get("F4"), registers.get("F1"), registers.get("F2")));
        instructions.add(new RInstruction(InstructionBase.ADD, registers.get("F2"), registers.get("F1"), registers.get("F3")));
        instructions.add(new RInstruction(InstructionBase.ADD, registers.get("F5"), registers.get("F2"), registers.get("F4")));
        instructions.add(new LDSTInstruction(InstructionBase.LOAD, registers.get("F9"), registers.get("F5")));
        instructions.add(new LDSTInstruction(InstructionBase.STORE, registers.get("F9"), registers.get("F0")));
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

class InstructionBase {
    final static String ADD = "ADD";
    final static String SUB = "SUB";
    final static String DIV = "DIV";
    final static String MUL = "MUL";
    final static String STORE = "STORE";
    final static String LOAD = "LOAD";

    final HashMap<String, Integer> clockPerInstruction = new HashMap<>(){{
        put(ADD, 1);
        put(SUB, 1);
        put(MUL, 1);
        put(DIV, 1);
        put(STORE, 1);
        put(LOAD, 1);
    }};

    public String op;
    public int remainingClock;
    public Register rd;

    Map<String, Consumer<Register[]>> operations = new HashMap<>(){{
        put(ADD, (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() + rgs[2].getValue()));
        put(SUB, (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() - rgs[2].getValue()));
        put(MUL, (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() * rgs[2].getValue()));
        put(DIV, (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue() / rgs[2].getValue()));
        put(LOAD, (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue()));
        put(STORE, (Register[] rgs) -> rgs[0].setValue(rgs[1].getValue()));
    }};

    public InstructionBase(String op, Register rd) {
        this.op = op;
        this.rd = rd;
        this.remainingClock = clockPerInstruction.get(op);
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


class LDSTInstruction extends InstructionBase implements Instruction {
    private Register rs;

    public LDSTInstruction(String op, Register rd, Register rs) {
        super(op, rd);

        if (op.equals(InstructionBase.STORE)) {
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

class RInstruction extends InstructionBase implements Instruction {
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
