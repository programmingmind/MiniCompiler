import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class Function {
   private class VariableList {
      private VariableList prev;
      private HashMap<String, Register> registers;
      private HashMap<String, Long> immediates;

      public VariableList(VariableList prev) {
         this.prev = prev;
         this.registers = new HashMap<String, Register>();
         this.immediates = new HashMap<String, Long>();
      }

      public VariableList getPrev() {
         return prev;
      }

      public Register getVarRegister(String varName) {
         if (registers.containsKey(varName))
            return registers.get(varName);
         else if (prev != null)
            return prev.getVarRegister(varName);
         else
            return null;
      }

      public Long getVarImmediate(String varName) {
         if (immediates.containsKey(varName))
            return immediates.get(varName);
         else if (prev != null)
            return prev.getVarImmediate(varName);
         else
            return null;
      }

      public void putVarRegister(String varName, Register reg) {
         registers.put(varName, reg);
         immediates.remove(varName);
      }

      public Set<String> getImmediateVars() {
         return new HashSet<String>(immediates.keySet());
      }

      public void putVarImmediate(String varName, Long imm) {
         immediates.put(varName, imm);
         registers.remove(varName);
      }
   }

   private String name;
   private Type returnType;
   private SymbolTable vars;

   private VariableList variables; 
   
   private Block entry, exit, preLoop;

   private int currentRegister;
   private ArrayList<Register> usedRegs;

   private int loopDepth, conditionalDepth;

   public Function(String name, Type returnType, SymbolTable vars) {
      this.name = name;
      this.returnType = returnType;
      this.vars = vars;

      this.entry = new Block(name, "entry");
      this.exit = new Block(name, "exit");

      currentRegister = 0;
      
      this.variables = new VariableList(null);

      usedRegs = new ArrayList<Register>();

      for (String var : vars.getLocals())
         putVarRegister(var, getNextRegister());

      loopDepth = 0;
      conditionalDepth = 0;

      preLoop = entry;
   }

   public String getName() {
      return name;
   }

   public Block getEntry() {
      return entry;
   }

   public Block getExit() {
      return exit;
   }

   public int getStackSize() {
      // make room for all possible variables, and 1 extra spot for scanf
      return 8 * (vars.getParamNames().size() + vars.getLocals().size() + 1);
   }

   public int getOffset(String var) {
      return vars.getOffset(var);
   }

   public Type getReturnType() {
      System.out.println("getting return type of " + name);
      return returnType;
   }

   public void checkParams(List<Type> args) {
      vars.checkParams(args);
   }

   public String getParamName(int num) {
      return vars.getParamName(num);
   }

   public List<String> getParamNames() {
      return vars.getParamNames();
   }

   public int getParamIndex(String name) {
      int ndx = 0;
      for (String s : vars.getParamNames()) {
         if (s.equals(name))
            return ndx;
         ++ndx;
      }
      throw new RuntimeException("param " + name + " doesn't exist");
   }

   public Register getNextRegister() {
      Register reg = new Register(currentRegister++);
      usedRegs.add(reg);
      return reg;
   }

   public void enterLoop() {
      variables = new VariableList(variables);
      ++loopDepth;
   }

   public void exitLoop() {
      variables = variables.getPrev();
      --loopDepth;
   }

   public int getLoopDepth() {
      return loopDepth;
   }

   public void enterConditional() {
      variables = new VariableList(variables);
      ++conditionalDepth;
   }

   public void exitConditional() {
      variables = variables.getPrev();
      --conditionalDepth;
   }

   public int getConditionalDepth() {
      return conditionalDepth;
   }

   public Register getVarRegister(String varName) {
      return variables.getVarRegister(varName);
   }

   public Long getVarImmediate(String varName) {
      return variables.getVarImmediate(varName);
   }

   public void putVarRegister(String varName, Register reg) {
      variables.putVarRegister(varName, reg);
   }

   public Set<String> getImmediateVars() {
      return variables.getImmediateVars();
   }

   public void putVarImmediate(String varName, Long imm) {
      variables.putVarImmediate(varName, imm);
   }

   private void spillAll() {

   }

   public void allocateRegisters() {
      boolean spillAll = false;
      if (usedRegs.size() < InstructionFactory.registers.length) {
         for (int i = 0; i < usedRegs.size(); i++)
            usedRegs.get(i).setASM(InstructionFactory.registers[i]);
      } else {
         if (spillAll)
            spillAll();

         computeLiveRanges(spillAll);
         for (Block b : sortBlocks())
            b.allocateRegisters();
      }
   }

   public void setPreLoop(Block b) {
      preLoop = b;
   }

   public void addBeforeLoop(Instruction inst) {
      preLoop.addInstruction(inst);
   }

   public void saveDot() {
      ArrayList<Block> blocks = new ArrayList<Block>();
      blocks.add(entry);
      blocks.add(exit);

      for (int i = 0; i < blocks.size(); i++)
         for (Block block : blocks.get(i).getLinks())
            if (! blocks.contains(block))
               blocks.add(block);

      try {
         File file = new File("dot/" + name + ".dot");
         if (!file.exists())
            file.createNewFile();
 
         FileWriter fw = new FileWriter(file.getAbsoluteFile());
         BufferedWriter bw = new BufferedWriter(fw);

         bw.write("digraph " + name + " {");
         bw.newLine();

         for (Block block : blocks)
            bw.write(block.printLinks());

         bw.write("}");
         bw.close();
         fw.close();
      } catch (IOException e) {
         ;
      }
   }

   private List<Block> sortBlocks() {
      Stack<Block> toVisit = new Stack<Block>();
      ArrayList<Block> visited = new ArrayList<Block>();

      toVisit.push(entry);
      while (! toVisit.empty()) {
         Block tmp = toVisit.pop();
         visited.add(tmp);

         List<Block> successors = tmp.getLinks();
         for (int i = successors.size() - 1; i >= 0; i--) {
            Block b = successors.get(i);
            if (toVisit.search(b) == -1 && !visited.contains(b) && b != exit) {
               Block[] keep = new Block[0];
               if (!toVisit.empty() && tmp.doesEndBranch(b)) {
                  int len = successors.size() - i;
                  keep = new Block[len];
                  while (len > 0)
                     keep[keep.length - len--] = toVisit.pop();
               }
               toVisit.push(b);
               for (Block k : keep)
                  toVisit.push(k);
            }
         }
      }
      visited.add(exit);

      return visited;
   }

   public String[] getCode() {
      StringWriter iloc = new StringWriter();
      StringWriter asm = new StringWriter();
      String[] code;

      String[] prefix = InstructionFactory.functionStart(name).toAssembly();

      for (Block tmp : sortBlocks()) {
         code = tmp == entry ? tmp.getCode(prefix) : tmp.getCode();
         iloc.append(code[0]);
         asm.append(code[1]);
      }

      return new String[] {iloc.toString(), asm.toString()};
   }

   public void cleanBlocks() {
      boolean change = true;
      while (change) {
         change = false;
         List<Block> blocks = sortBlocks();

         for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).verifyLinks((i + 1 < blocks.size()) ? blocks.get(i + 1) : null)) {
               change = true;
               break;
            }
         }
      }
   }

   private void replaceIfPossible(Instruction i) {
      Register source = i.getSources()[0];
      Register target = i.getTarget();

      if (! target.canBePropogated())
         return;

      List<Block> blocks = sortBlocks();
      boolean seen = false;
      boolean newlySet = false;

      Register tmp = null, other = null;

      ArrayList<Instruction> toReplace = new ArrayList<Instruction>();
      toReplace.add(i);

      for (Block b : blocks) {
         for (Instruction inst : b.getInstructions()) {
            if (! seen) {
               seen = inst == i;
               continue;
            }

            if (inst.getTarget() == target && inst.toIloc().startsWith("mov ")) {
               target.disablePropogation();
               return;
            }

            if (newlySet) {
               if (inst.usesReg(other) || inst.getTarget() == other)
                  return;

               if (inst.usesReg(tmp) || inst.getTarget() == tmp)
                  toReplace.add(inst);

               continue;
            }

            if (inst.usesReg(target))
               toReplace.add(inst);

            if (inst.getTarget() == source || inst.getTarget() == target) {
               tmp = inst.getTarget();
               other = tmp == source ? target : source;
               newlySet = true;
            }
         }
      }

      System.out.println("replacing " + target.getILOC() + " with " + source.getILOC());

      for (Instruction inst : toReplace)
         inst.replace(target, source);
   }

   public void performCopyPropogation() {
      List<Block> blocks = sortBlocks();

      for (Block b : blocks)
         if (b.getDepth() == 0)
            for (Instruction i : b.getInstructions())
               if (i.toIloc().startsWith("mov "))
                  replaceIfPossible(i);
   }

   public void performTargetPropogation() {
      List<Block> blocks = sortBlocks();

      for (Block b : blocks) {
         List<Instruction> insts = b.getInstructions();
         List<Instruction> toRemove = new ArrayList<Instruction>();

         for (int i = 1; i < insts.size(); i++) {
            if (insts.get(i).toIloc().startsWith("mov ") &&
                insts.get(i).getSources()[0] == insts.get(i-1).getTarget()
                && !usedAfter(insts.get(i - 1).getTarget(), insts.get(i))) {

               insts.get(i - 1).replace(insts.get(i - 1).getTarget(), insts.get(i).getTarget());
               toRemove.add(insts.get(i));
            }
         }

         for (Instruction i : toRemove)
            b.removeInstruction(i);
      }
   }

   private boolean usedAfter(Register target, Instruction i) {
      if (target == null)
         return true;

      boolean seen = false;
      List<Block> blocks = sortBlocks();
      Block orig = null;

      for (Block b : blocks) {
         for (Instruction inst : b.getInstructions()) {
            if (! seen) {
               seen = inst == i;
               if (seen)
                  orig = b;
               continue;
            }

            for (Register r : inst.getSources()) {
               if (r == target)
                  return true;

            if (inst.getTarget() == target && ! inst.isConditionalTarget())
               return orig != b;
            }
         }
      }

      return false;
   }

   private boolean usedBeforeNextSet(Instruction i) {
      return usedAfter(i.getTarget(), i);
   }

   public void removeUselessInstructions() {
      List<Block> blocks = sortBlocks();
      boolean change = true;

      while (change) {
         change = false;

         for (Block b : blocks) {
            Instruction toRemove = null;

            for (Instruction inst : b.getInstructions()) {
               if (inst.toIloc().startsWith("storeai"))
                  continue;

               if (! usedBeforeNextSet(inst)) {
                  change = true;
                  toRemove = inst;
                  break;
               }
            }

            if (toRemove != null) {
               b.removeInstruction(toRemove);
               break;
            }
         }
      }

      // remove useless movs
      for (Block b : blocks) {
         ArrayList<Instruction> toRemove = new ArrayList<Instruction>();
         for (Instruction i : b.getInstructions())
            if (i.toIloc().startsWith("mov ") && i.getSources()[0] == i.getTarget())
               toRemove.add(i);

         for (Instruction i : toRemove)
            b.removeInstruction(i);
      }
   }

   private void computeLiveRanges(boolean spillAll) {
      List<Block> blocks = sortBlocks();
      boolean change = !spillAll;

      while (change) {
         change = false;
         for (int i = blocks.size() - 1; i >= 0; i--)
            change |= blocks.get(i).findPriorRegisters();
      }

      for (Block b : blocks)
         b.computeLiveRanges();
   }
}