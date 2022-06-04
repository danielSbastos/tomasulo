# Tomasulo's Algorithm

Group members: [@danielSbastos](https://github.com/danielSbastos), [@rossanaoliveirasouza](https://github.com/rossanaoliveirasouza), [@isabelaaaguilar](https://github.com/isabelaaaguilar) and [@Kronomant](https://github.com/Kronomant).

Software implementation of Tomasulo's algorithm, which is defined as:

> computer architecture hardware algorithm for dynamic scheduling of instructions that allows out-of-order execution and enables more efficient use of multiple execution units. - [Wikipedia](https://en.wikipedia.org/wiki/Tomasulo_algorithm)

This implementation is heavly based on the following Youtube [video](https://www.youtube.com/watch?v=jyjE6NHtkiA) by [J.R.](https://www.youtube.com/user/jreis999).

## Pre-requisites

- Java 11 (previous versions were not tested, so they might work, try your luck)

## Compilation and execution

```sh
javac -cp ".:json-simple-1.1.1.jar" *.java
java -cp ".:json-simple-1.1.1.jar" Main
```
## Implementation details

- Only the following instructions are supported: `ADD`, `SUB`, `MUL`, `DIV`, `STORE` and `LOAD`;
- `MUL` and `DIV` only need one register (instead of two) to store the result;
- The instructions are dispatched one at time;
- There is no limit on how many instructions a buffer/reservation unit can hold;
- In the same way, there is no limit on how many instructions a memory unit (ST/LD instructions) and ALU (R instructions) (execution units) unit can execute at a time, i.e., a pseudo-paralellism can be achieved;
- In each clock cycle, no concurrency is present, the steps taken are ordered as followed:
  - 1. Execute instructions from the execution unit that are ready to be executed, i.e., their clock count has reached 0;
  - 2. Pop the next instruction and direct either to the buffer or execution unit;
  - 3. For each buffer, check if the instruction can be moved to the execution unit, meaning that the busy registers are now free.

## Configuration

The file `data.json` contains information regarding the registers, clocks per instruction and instructions. Below is an explanation for each field.

### Registers

For each register to be used in the instructions, add the following entry under the `registers` array value, where `R1` is the register name.

```json
"registers" : [
    { "name": "R1" }
    // ...
]
```

If you wish that the register contain an initial value (int), change to the following:

```json
"registers" : [
    { "name": "R1", "value": 2 }
    // ...
]
```

### Clocks per Instruction

Define the clocks for each instruction (`ADD`, `SUB`, `MUL`, `DIV`, `STORE` and `LOAD`) by adding the following entry under the `clocksPerInstruction` array value, where `name` is the instruction name and `value` is the clock.

```json
"clocksPerInstruction" : [
    { "name": "ADD", "value": 2 }
    // ...
]
```

### Instructions

For each instruction, add an entry to the `instructions` array value. For example:

**Make sure the registers used are being defined in the `registers` entry**

```json
"instructions" : [
    "ADD F1 F2 F3",
    "MUL F4 F1 F2",
    "LOAD F9 F5",
    "STORE F9 F0"
    // ...
]
```
