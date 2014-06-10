import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Block {
   private static HashMap<String, Integer> counts;

   private ArrayList<Instruction> instructions;
   private String label;
   private ArrayList<Block> predecessors;
   private ArrayList<Block> successors;
   private HashSet<Block> endBranchSuccessors;

   private HashSet<Register> leaving;
   private Set<Register> registers;

   private boolean hasLeft;

   private String function;
   private int depth;

   private void init(String func, String which) {
      this.label = func + "_" + which;
      if (func.equals("main") && which.equals("entry"))
         this.label = "main";

      instructions = new ArrayList<Instruction>();
      predecessors = new ArrayList<Block>();
      successors = new ArrayList<Block>();
      endBranchSuccessors = new HashSet<Block>();

      leaving = new HashSet<Register>();

      hasLeft = false;

      this.function = func;
      this.depth = 0;

      if (Functions.isDefined(func)) {
         if (Functions.get(func).getLoopDepth() == 0)
            Functions.get(func).setPreLoop(this);

         this.depth = Functions.get(func).getLoopDepth() + Functions.get(func).getConditionalDepth();
      }
   }

   public Block(String func, String which) {
      init(func, which);
   }

   public Block(String func) {
      if (counts == null)
         counts = new HashMap<String, Integer>();

      Integer count = counts.get(func);
      count = count == null ? 0 : (count + 1);
      counts.put(func, count);

      init(func, count.toString());
   }

   public String getLabel() {
      return label;
   }

   public List<Instruction> getInstructions() {
      return Collections.unmodifiableList(instructions);
   }

   public int getDepth() {
      return depth;
   }

   public void removeInstruction(Instruction inst) {
      instructions.remove(inst);
   }

   public void addNext(Block next) {
      addNext(next, false);
   }

   public void addNext(Block next, boolean endBranch) {
      if (!successors.contains(next))
         successors.add(next);
      if (endBranch)
         endBranchSuccessors.add(next);

      if (! next.predecessors.contains(this))
         next.predecessors.add(this);
   }

   public boolean doesEndBranch(Block b) {
      return endBranchSuccessors.contains(b);
   }

   public void addInstruction(Instruction inst) {
      if (!hasLeft) {
         instructions.add(inst);
         hasLeft = inst.isJump();
      } else {
         System.err.println("WARNING: ignoring instruction: " + inst.toIloc());
      }
   }

   public void removeUnnecessaryLVal() {
      if (instructions.size() == 0)
         return;

      Instruction tmp = instructions.remove(instructions.size() - 1);
      if (! tmp.toIloc().startsWith("loadai")) {
         System.err.println("WARNING: last instruction (" + tmp.toIloc() + ") was neccessary l-value related instruction");
         instructions.add(tmp);
      }
   }

   public String[] getCode() {
      return getCode(new ArrayList<String>());
   }

   public String[] getCode(List<String> prefix) {
      StringWriter iloc = new StringWriter();
      StringWriter asm = new StringWriter();

      iloc.append(label + ":\n");
      asm.append(label + ":\n");

      for (String s : prefix)
         asm.append("\t" + s + "\n");
      
      for (Instruction instruction : instructions) {
         iloc.append("\t" + instruction.toIloc() + "\n");
         for (String s : instruction.toAssembly())
            asm.append("\t" + s + "\n");
      }

      return new String[] {iloc.toString(), asm.toString()};
   }

   public List<Block> getLinks() {
      return new ArrayList<Block>(successors);
   }

   public String printLinks() {
      StringWriter sw = new StringWriter();
      for (Block block : successors)
         sw.append(label + " -> " + block.label + "\n");
      return sw.toString();
   }

   public boolean verifyLinks(Block next) {
      List<Block> toRemove = getLinks();
      boolean hasJump = false;

      for (Instruction inst : instructions) {
         if (inst.isJump()) {
            hasJump = true;

            for (Block block : successors)
               if (inst.jumpsToLabel(block.getLabel()))
                  toRemove.remove(block);
         }
      }

      if (next != null && !hasJump)
         toRemove.remove(next);

      if (instructions.size() > 0 && next != null) {
         Instruction last = instructions.get(instructions.size() - 1);
         if (last.jumpsToLabel(next.getLabel(), true))
            instructions.remove(last);
      }

      boolean change = false;
      for (Block b : toRemove) {
         successors.remove(b);
         endBranchSuccessors.remove(b);
         b.predecessors.remove(this);

         change = true;
      }

      return change;
   }

   public boolean findPriorRegisters() {
      HashSet<Register> setBefore = new HashSet<Register>();
      HashSet<Register> targetsSet = new HashSet<Register>();

      for (Instruction inst : instructions) {
         for (Register src : inst.getSources())
            if (! targetsSet.contains(src))
               setBefore.add(src);
         
         Register target = inst.getTarget();
         if (target != null)
            targetsSet.add(target);
      }

      for (Register src : leaving)
         if (! targetsSet.contains(src))
            setBefore.add(src);

      boolean change = false;
      for (Block pre : predecessors)
         change |= pre.leaving.addAll(setBefore);

      return change;
   }

   public void computeLiveRanges() {
      HashMap<Register, Integer> start = new HashMap<Register, Integer>();
      HashMap<Register, Integer> end = new HashMap<Register, Integer>();

      for (Block pre : predecessors) {
         for (Register reg : pre.leaving) {
            start.put(reg, -1);
            end.put(reg, 0);
         }
      }

      for (int i = 0; i < instructions.size(); i++) {
         for (Register reg : instructions.get(i).getSources())
            end.put(reg, i);

         Register target = instructions.get(i).getTarget();
         if (target != null) {
            if (! start.containsKey(target))
               start.put(target, i);
            end.put(target, i);
         }
      }

      for (Register reg : leaving)
         end.put(reg, instructions.size());

      registers = new HashSet<Register>(start.keySet());
      registers.addAll(end.keySet());

      for (Register register : registers) {
         if (start.get(register) == null) {
            System.err.println("WARNING (" + label + "): could not compute start of live range for r" + register.getILOC());
            start.put(register, -1);
         }
         if (end.get(register) == null) {
            System.err.println("WARNING (" + label + "): could not compute end of live range for r" + register.getILOC());
            end.put(register, instructions.size());
         }

         register.setRange(this, start.get(register), end.get(register));
      }
   }

   public void allocateRegisters() {
      GraphColorer colorer = new GraphColorer(registers, Functions.get(function));
      colorer.color();
   }
}