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
    int clock = 0;

    Queue<IInstruction> instructions = new LinkedList<>();

    Queue<RInstruction> FPAddersReservation = new LinkedList<>();
    Queue<RInstruction> FPAdders = new LinkedList<>();

    Queue<RInstruction> FPMultipliersReservation = new LinkedList<>();
    Queue<RInstruction> FPMultipliers = new LinkedList<>();

    Queue<LDSTInstruction> LDBuffer = new LinkedList<>();
    Queue<LDSTInstruction> STBuffer = new LinkedList<>();
    Queue<LDSTInstruction> memoryUnit = new LinkedList<>();

    Map<String, Map<Boolean, Queue>> locateInstruction = new HashMap<>(){{
        put(Instruction.ADD, new HashMap<Boolean, Queue>(){{
            put(true, FPAddersReservation);
            put(false, FPAdders);
        }});
        put(Instruction.SUB, new HashMap<Boolean, Queue>(){{
            put(true, FPAddersReservation);
            put(false, FPAdders);
        }});
        put(Instruction.MUL, new HashMap<Boolean, Queue>(){{
            put(true, FPMultipliersReservation);
            put(false, FPMultipliers);
        }});
        put(Instruction.DIV, new HashMap<Boolean, Queue>(){{
            put(true, FPMultipliersReservation);
            put(false, FPMultipliers);
        }});
        put(Instruction.LOAD, new HashMap<Boolean, Queue>(){{
            put(true, LDBuffer);
            put(false, memoryUnit);
        }});
        put(Instruction.STORE, new HashMap<Boolean, Queue>(){{
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
            printBefore();

            executeInstruction(FPAdders);
            executeInstruction(FPMultipliers);
            executeInstruction(memoryUnit);

            if (!instructions.isEmpty()) popAndMoveInstruction();

            moveInstructionToExecutionUnit(FPAddersReservation, FPAdders);
            moveInstructionToExecutionUnit(FPMultipliersReservation, FPMultipliers);
            moveInstructionToExecutionUnit(LDBuffer, memoryUnit);
            moveInstructionToExecutionUnit(STBuffer, memoryUnit);

            printAfter();
        }
    }

    private <T> void executeInstruction(Queue<T> queue) {
        List<IInstruction> toRemove = new ArrayList<>();

        for (T instruction : queue) {
            ((IInstruction) instruction).decrementClock();
            if (((IInstruction) instruction).getRemainingClock() == 0) {
                ((IInstruction) instruction).execute();
                ((IInstruction) instruction).setRdBusy(false);
                toRemove.add(((IInstruction) instruction));
            }
        }

        for (IInstruction _toRemove : toRemove) {
            System.out.println("Executed:  " + _toRemove);
            queue.remove(_toRemove);
        }
    }

    private <T> void moveInstructionToExecutionUnit(Queue<T> buffer, Queue<T> executionUnit) {
        List<IInstruction> toRemove = new ArrayList<>();

        for (T instruction : buffer) {
            if (((IInstruction) instruction).noneSrcRegsEmpty()) {
                executionUnit.add(instruction);
                toRemove.add(((IInstruction) instruction));
            }
        }

        for (IInstruction _toRemove : toRemove) {
            buffer.remove(_toRemove);
        }
    }

    private void popAndMoveInstruction() {
        IInstruction instruction = instructions.remove();
        System.out.println("Dequeued instruction: " + instruction);
        boolean toBuffer = instruction.anySrcRegEmpty() || instruction.anyRegBusy();

        Queue<IInstruction> queueSrc = locateInstruction.get(instruction.getOp()).get(toBuffer);
        queueSrc.add(instruction);
        instruction.setRdBusy(true);
    }

    private void populateInstructions() {
        instructions.add(new RInstruction(Instruction.ADD, registers.get("F1"), registers.get("F2"), registers.get("F3")));
        instructions.add(new RInstruction(Instruction.MUL, registers.get("F4"), registers.get("F1"), registers.get("F2")));
        instructions.add(new RInstruction(Instruction.ADD, registers.get("F2"), registers.get("F1"), registers.get("F3")));
        instructions.add(new RInstruction(Instruction.ADD, registers.get("F5"), registers.get("F2"), registers.get("F4")));
        instructions.add(new LDSTInstruction(Instruction.LOAD, registers.get("F9"), registers.get("F5")));
        instructions.add(new LDSTInstruction(Instruction.STORE, registers.get("F9"), registers.get("F0")));
    }


    private void printBefore() {
        System.out.println("\n==================================================================");
        System.out.println("Clock: " + clock);
        printRegisters("before");
    }

    private void printAfter() {
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
        System.out.println("===================================");
        printRegisters("after");
    }

    private void printRegisters(String str) {
        System.out.println(
            "Registers " + str + ": F0: " + registers.get("F0") + 
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
}