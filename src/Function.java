import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class Function {
   private String name;
   private Type returnType;
   private SymbolTable vars;
   private HashMap<String, Integer> registers;
   private Block entry, exit;
   private int currentRegister;

   public Function(String name, Type returnType, SymbolTable vars) {
      this.name = name;
      this.returnType = returnType;
      this.vars = vars;

      this.entry = new Block(name, "entry");
      this.exit = new Block(name, "exit");

      currentRegister = 0;
      registers = new HashMap<String, Integer>();

      for (String var : vars.getLocals())
         putVarRegister(var, getNextRegister());
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

   public int getParamIndex(String name) {
      int ndx = 0;
      for (String s : vars.getParamNames()) {
         if (s.equals(name))
            return ndx;
         ++ndx;
      }
      throw new RuntimeException("param " + name + " doesn't exist");
   }

   public int getNextRegister() {
      return currentRegister++;
   }

   public int peekNextRegister() {
      return currentRegister;
   }

   public Integer getVarRegister(String varName) {
      return registers.get(varName);
   }

   public void putVarRegister(String varName, int reg) {
      registers.put(varName, reg);
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

   public String[] getCode() {
      StringWriter iloc = new StringWriter();
      StringWriter asm = new StringWriter();
      Stack<Block> toVisit = new Stack<Block>();
      HashSet<Block> visited = new HashSet<Block>();
      String[] code;

      String[] prefix = InstructionFactory.functionStart(name).toAssembly();


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

         code = tmp == entry ? tmp.getCode(prefix) : tmp.getCode();
         iloc.append(code[0]);
         asm.append(code[1]);
      }
      code = exit.getCode();
      iloc.append(code[0]);
      asm.append(code[1]);

      return new String[] {iloc.toString(), asm.toString()};
   }

   public void cleanBlocks() {
      // remove empty blocks
      // add backward nodes
   }
}