import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Instruction {
    final static String ADD = "ADD";
    final static String SUB = "SUB";
    final static String DIV = "DIV";
    final static String MUL = "MUL";
    final static String STORE = "STORE";
    final static String LOAD = "LOAD";

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

    public Instruction(String op, Register rd) {
        this.op = op;
        this.rd = rd;
        this.remainingClock = Data.clockPerInstruction.get(op);
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
