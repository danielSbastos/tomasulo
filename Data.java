import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Data {
    public static HashMap<String, Integer> clockPerInstruction = new HashMap<>();
    public static HashMap<String, Register> registers = new HashMap<>();
    public static Queue<IInstruction> instructions = new LinkedList<>();
    
    public static void loadFile() {
        try {
            JSONParser parser = new JSONParser();
            JSONObject config = (JSONObject) parser.parse(new FileReader("./data.json"));
            JSONArray registersJson = (JSONArray) config.get("registers");
            JSONArray clocksPerOperationJson = (JSONArray) config.get("clocksPerOperation");
            JSONArray instructionsJson = (JSONArray) config.get("instructions");

            for (Object register : registersJson) {
                String name = String.valueOf(((JSONObject) register).get("name"));
                Long value = (Long) (((JSONObject) register).get("value"));

                if (value == null) {
                    registers.put(name, new Register(name, false));
                } else {
                    registers.put(name, new Register(name, false, value.floatValue()));
                }
            }

            for (Object clockOp : clocksPerOperationJson) {
                String op = String.valueOf(((JSONObject) clockOp).get("op"));
                Long value = (Long) (((JSONObject) clockOp).get("value"));

                clockPerInstruction.put(op, value.intValue());
            }

            for (Object instruction : instructionsJson) {
                String[] parts = String.valueOf(instruction).split(" ");
                if (parts[0].equals("LOAD") || parts[0].equals("STORE")) {
                    instructions.add(new LDSTInstruction(parts[0], registers.get(parts[1]), registers.get(parts[2])));
                } else {
                    instructions.add(new RInstruction(parts[0], registers.get(parts[1]), registers.get(parts[2]), registers.get(parts[3])));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
