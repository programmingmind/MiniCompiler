import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

public class Block {
   private static HashMap<String, Integer> counts;

   private ArrayList<Instruction> instructions;
   private String label;
   private ArrayList<Block> predecessors;
   private ArrayList<Block> successors;
   private HashSet<Block> endBranchSuccessors;

   private void init(String func, String which) {
      this.label = func + "_" + which;
      instructions = new ArrayList<Instruction>();
      predecessors = new ArrayList<Block>();
      successors = new ArrayList<Block>();
      endBranchSuccessors = new HashSet<Block>();
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

   public void addNext(Block next) {
      addNext(next, false);
   }

   public void addNext(Block next, boolean endBranch) {
      if (!successors.contains(next))
         successors.add(next);
      if (endBranch)
         endBranchSuccessors.add(next);
   }

   public boolean doesEndBranch(Block b) {
      return endBranchSuccessors.contains(b);
   }

   public void addInstruction(Instruction inst) {
      instructions.add(inst);
   }

   public String toString() {
      StringWriter sw = new StringWriter();

      sw.append(label + ":\n");
      for (Instruction instruction : instructions)
         sw.append("\t" + instruction + "\n");

      return sw.toString();
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
}