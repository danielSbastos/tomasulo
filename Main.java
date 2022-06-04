import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        Tomasulo t = new Tomasulo();
        t.execute();
    }
}

class Tomasulo {
    int clock = 0;

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

    public void execute() {
        Data.loadFile();

        while (!Data.instructions.isEmpty() || !FPAdders.isEmpty() ||
                !FPAddersReservation.isEmpty() || !FPMultipliers.isEmpty() ||
                !FPMultipliersReservation.isEmpty() || !LDBuffer.isEmpty() ||
                !STBuffer.isEmpty() || !memoryUnit.isEmpty()) {

            clock++;
            printBefore();

            executeInstruction(FPAdders);
            executeInstruction(FPMultipliers);
            executeInstruction(memoryUnit);

            if (!Data.instructions.isEmpty()) popAndMoveInstruction();

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
        IInstruction instruction = Data.instructions.remove();
        System.out.println("Dequeued instruction: " + instruction);
        boolean toBuffer = instruction.anySrcRegEmpty() || instruction.anyRegBusy();

        Queue<IInstruction> queueSrc = locateInstruction.get(instruction.getOp()).get(toBuffer);
        queueSrc.add(instruction);
        instruction.setRdBusy(true);
    }

    private void printBefore() {
        System.out.println("\n==================================================================");
        System.out.println("Clock: " + clock);
        printRegisters("before");
    }

    private void printAfter() {
        System.out.println("============ Instructions ===========");
        System.out.println(Data.instructions);
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
        String out = "Registers " + str + ":";

        for (Map.Entry<String, Register> reg : Data.registers.entrySet()) {
            out += " | " + reg.getKey() + ": " + reg.getValue();
        }
        System.out.println(out);
    }
}
