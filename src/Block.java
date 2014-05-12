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
      if (func.equals("main") && which.equals("entry"))
         this.label = "main";
      
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

   public String[] getCode() {
      return getCode(new String[] {});
   }

   public String[] getCode(String[] prefix) {
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
}