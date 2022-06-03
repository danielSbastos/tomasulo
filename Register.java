public class Register {
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
