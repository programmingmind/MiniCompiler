import java.io.StringWriter;

import java.util.List;

public class InstructionFactory {
   private static final String[] ops = {"eq", "ge", "gt", "le", "lt", "ne"};
   private static final String[] asmOps = {"e", "ge", "g", "le", "l", "ne"};
   private static final String[] paramRegs = {"edi", "esi", "edx", "ecx", "r8d", "r9d"};

   private static final String printName = ".LLC0";
   private static final String printlnName = ".LLC1";
   private static final String readName = ".LLC0";

   private static Functions funcs;
   private static String currentFunction;

   public static void setFunctions(Functions functions) {
      funcs = functions;
   }

   public static void setFunction(String name) {
      currentFunction = name;
   }

   public static String getProgramHeader() {
      return "\t.section .rodata\n"
            +printName + "\n"
            +"\t.string \"%ld\"\n"
            +printlnName + "\n"
            +"\t.string \"%ld\\n\"\n"
            +"\t.text\n";
   }

   public static String globalHeader(SymbolTable globals) {
      StringWriter sw = new StringWriter();
      List<String> globalNames = globals.getLocals();
      for (String name : globalNames)
         sw.append("\t.comm glob_" + name + ", 8, 8\n");
      return sw.toString();
   }

   private static int getCompNdx(int type, boolean reverse) {
      int ndx = 0;
      switch (type) {
         case TypeCheck.EQ:
            ndx = 0;
            break;
         case TypeCheck.GE:
            ndx = 1;
            break;
         case TypeCheck.GT:
            ndx = 2;
            break;
         case TypeCheck.LE:
            ndx = 3;
            break;
         case TypeCheck.LT:
            ndx = 4;
            break;
         case TypeCheck.NE:
            ndx = 5;
            break;
         default:
            throw new RuntimeException("unkown comp op");
      }

      if (reverse && ndx > 0 && ndx < 5)
         ndx = 5 - ndx;

      return ndx;
   }

   private static String getCompOp(int type, boolean reverse) {
      return ops[getCompNdx(type, reverse)];
   }

   private static String getCompAsmOp(int type, boolean reverse) {
      return asmOps[getCompNdx(type, reverse)];
   }

   public static Instruction functionStart(String name) {
      return new Instruction("",
                              new int[] {},
                              null,
                              "pushq %ebp",
                              "movq %esp, %ebp",
                              "subq $" + funcs.get(name).getStackSize() + ", %rsp");
   }

   public static Instruction arithmetic(int type, Integer imm, int left, Integer right, int result) {
      String op = null;
      switch (type) {
         case TypeCheck.PLUS:
            op = imm == null ? "add" : "addi";
            break;
         case TypeCheck.MINUS:
            op = imm == null ? "sub" : "rsubi";
            break;
         case TypeCheck.TIMES:
            op = "mult";
            break;
         case TypeCheck.DIVIDE:
            op = "div";
            break;
         default:
            throw new RuntimeException("unkown arithmetic op");
      }

      String iloc = op + " r" + left + ", " + (imm == null ? ("r" + right) : imm) + ", r" + result;
      if (imm == null) {
         String asm = op;
         if (type == TypeCheck.TIMES)
            asm = "imul";
         else if (type == TypeCheck.DIVIDE)
            asm = "idiv";

         if (left == result) {
            return new Instruction(iloc,
                                    new int[] {left, right},
                                    result,
                                    asm + "q %r" + right + ", %r" + result);
         } else if (right == result) {
            if (type == TypeCheck.PLUS || type == TypeCheck.TIMES) {
               return new Instruction(iloc,
                                       new int[] {left, right},
                                       result,
                                       asm + "q %r" + left + ", %r" + result);
            } else {
               return new Instruction(iloc,
                                       new int[] {left, right},
                                       result,
                                       "NOT SUPPORTED YET");
            }
         } else {
            return new Instruction(iloc,
                                    new int[] {left, right},
                                    result,
                                    "mov %r" + left + ", %r" + result,
                                    asm + "q %r" + right + ", %r" + result);
         }
      } else {
         if (left == result) {
            return new Instruction(iloc,
                                    new int[] {left},
                                    result,
                                    (type == TypeCheck.PLUS ? "addq" : "subq") + " $" + imm + ", %r" + result);
         } else {
            return new Instruction(iloc,
                                    new int[] {left},
                                    result,
                                    "mov %r" + left + ", %r" + result,
                                    (type == TypeCheck.PLUS ? "addq" : "subq") + " $" + imm + ", %r" + result);
         }
      }
   }

   public static Instruction bool(int type, int left, int right, int result) {
      String iloc = (type == TypeCheck.AND ? "and" : "or") + " r" + left + ", r" + right + ", r" + result;
      if (left == result) {
         return new Instruction(iloc,
                                 new int[] {left, right},
                                 result,
                                 (type == TypeCheck.AND ? "and" : "or") + " %r" + right + ", %r" + result);
      } else if (right == result) {
         return new Instruction(iloc,
                                 new int[] {left, right},
                                 result,
                                 (type == TypeCheck.AND ? "and" : "or") + " %r" + left + ", %r" + result);
      } else {
         return new Instruction(iloc,
                                 new int[] {left, right},
                                 result,
                                 "mov %r" + left + ", %r" + result,
                                 (type == TypeCheck.AND ? "and" : "or") + " %r" + right + ", %r" + result);
      }
   }

   public static Instruction xori(int reg, int imm, int result) {
      if (reg == result) {
         return new Instruction("xori r" + reg + ", " + imm + ", r" + result,
                                 new int[] {reg},
                                 result,
                                 "xor $" + imm + ", %r" + result);
      } else {
         return new Instruction("xori r" + reg + ", " + imm + ", r" + result,
                                 new int[] {reg},
                                 result,
                                 "xor $" + imm + ", %r" + result);
      }
   }

   public static Instruction comp(int left, int right) {
      return new Instruction("comp r" + left + ", r" + right,
                              new int[] {left, right},
                              null,
                              "cmp %r" + left + ", %r" + right);
   }

   public static Instruction compi(int reg, int imm) {
      return new Instruction("compi r" + reg + ", " + imm,
                              new int[] {reg},
                              null,
                              "cmp %r" + reg + ", $" + imm);
   }

   public static Instruction ccbranch(int type, boolean reverse, String label1, String label2) {    
      return new Instruction("cbr" + getCompOp(type, reverse) + " ccr, " + label1 + ", " + label2,
                              new int[] {},
                              null,
                              "j" + getCompAsmOp(type, reverse) + " " + label1,
                              "jmp " + label2);
   }

   public static Instruction jump(String label) {
      return new Instruction("jumpi " + label,
                              new int[] {},
                              null,
                              "jmp " + label);
   }

   public static Instruction loadi(int immediate, int result) {
      return new Instruction("loadi " + immediate + ", r" + result,
                              new int[] {},
                              result,
                              "movq $" + immediate + ", %r" + result);
   }

   public static Instruction loadai(int reg, int imm, int result) {
      return new Instruction("loadai r" + reg + ", " + imm + ", r" + result,
                              new int[] {reg},
                              result,
                              "movq " + imm + "(%r" + reg + "), %r" + result);
   }

   public static Instruction loadGlobal(String var, int result) {
      return new Instruction("loadglobal " + var + ", r" + result,
                              new int[] {},
                              result,
                              "movq glob_" + var + "(%rip), %r" + result);
   }

   public static Instruction loadArg(String var, int num, int reg) {
      if (num < paramRegs.length) {
         return new Instruction("loadinargument " + var + ", " + num + ", r" + reg,
                                 new int[] {},
                                 reg,
                                 "movq %" + paramRegs[num] + ", %r" + reg);
      } else {
         return new Instruction("loadinargument " + var + ", " + num + ", r" + reg,
                                 new int[] {},
                                 reg,
                                 "NOT SUPPORTED YET");
      }
   }

   public static Instruction loadRet(int reg) {
      return new Instruction("loadret r" + reg,
                              new int[] {},
                              reg,
                              "mov %rax, %r" + reg);
   }

   public static Instruction formalAddr(String var, int num, int reg) {
      return new Instruction("computeformaladdress " + var + ", " + num + ", r" + reg,
                              new int[] {},
                              reg,
                              "movq %rsp, %r" + reg,
                              "addq $" + funcs.get(currentFunction).getOffset(var) + ", %r" + reg);
   }

   public static Instruction restoreFormal(String var, int num) {
      return new Instruction("restoreformal " + var + ", " + num,
                              new int[] {},
                              null,
                              "NOT SUPPORTED YET");
   }

   public static Instruction globalAddr(String var, int result) {
      return new Instruction("computeglobaladdress " + var + ", r" + result,
                              new int[] {},
                              result,
                              "movq $glob_" + var + ", %r" + result);
   }

   public static Instruction storeai(int reg, int result, int imm) {
      return new Instruction("storeai r" + reg + ", r" + result + ", " + imm,
                              new int[] {reg},
                              result,
                              "movq %r" + reg + ", " + imm + "(%r" + result + ")");
   }

   public static Instruction storeGlobal(int reg, String var) {
      return new Instruction("storeglobal r" + reg + ", " + var,
                              new int[] {reg},
                              null,
                              "movq %r" + reg + ", glob_" + var + "(%rip)");
   }

   public static Instruction storeInArg(int reg, String var, int num) {
      if (num < paramRegs.length) {
         return new Instruction("storeinargument r" + reg + ", " + var + ", " + num,
                                 new int[] {reg},
                                 null,
                                 "movq %r" + reg + ", %" + paramRegs[num]);
      }
      else {
         return new Instruction("storeinargument r" + reg + ", " + var + ", " + num,
                                 new int[] {reg},
                                 null,
                                 "NOT SUPPORTED YET");
      }
   }

   public static Instruction storeOutArg(int reg, int num) {
      return new Instruction("storeoutargument r" + reg + ", " + num,
                              new int[] {reg},
                              null,
                              "NOT SUPPORTED YET");
   }

   public static Instruction storeRet(int reg) {
      return new Instruction("storeret r" + reg,
                              new int[] {reg},
                              null,
                              "mov %r" + reg + ", %rax");
   }

   public static Instruction call(String label) {
      return new Instruction("call " + label,
                              new int[] {},
                              null,
                              "call " + label);
   }

   public static Instruction ret() {
      return new Instruction("ret",
                              new int[] {},
                              null,
                              "addq $" + funcs.get(currentFunction).getStackSize() + ", %rsp",
                              "leave",
                              "ret");
   }

   public static Instruction newAlloc(int num, int reg) {
      return new Instruction("new " + num + ", r" + reg,
                              new int[] {},
                              reg,
                              "movq $" + num + ", %edi",
                              "call malloc",
                              "mov %rax, %r" + reg);
   }

   public static Instruction del(int reg) {
      return new Instruction("del r" + reg,
                              new int[] {reg},
                              null,
                              "mov %r" + reg + ", %edi",
                              "call free");
   }

   public static Instruction print(int reg) {
      return new Instruction("print r" + reg,
                              new int[] {reg},
                              null,
                              "movq %r" + reg + ", %esi",
                              "movq " + printName + ", %edi",
                              "movq $0, %rax",
                              "call printf");
   }

   public static Instruction println(int reg) {
      return new Instruction("println r" + reg,
                              new int[] {reg},
                              null,
                              "movq %r" + reg + ", %esi",
                              "movq " + printlnName + ", %edi",
                              "movq $0, %rax",
                              "call printf");
   }

   public static Instruction read(int reg) {
      return new Instruction("read r" + reg,
                              new int[] {},
                              reg,
                              "movq %rsp, %r" + reg,
                              "addq $8, %r" + reg,
                              "movq " + readName + ", %edi",
                              "movq %r" + reg + ", %esi",
                              "movq $0, %rax",
                              "call scanf",
                              "mov (%r" + reg + "), %r" + reg);
   }

   public static Instruction mov(int r1, int r2) {
      return new Instruction("mov r" + r1 + ", r" + r2,
                              new int[] {r1},
                              r2,
                              "mov %r" + r1 + ", %r" + r2);
   }

   public static Instruction ccmove(int type, boolean reverse, int r1, int r2) {
      return new Instruction("mov"+ getCompOp(type, reverse) + " ccr, r" + r1 + ", r" + r2,
                              new int[] {r1, r2},
                              r2,
                              "cmov" + getCompAsmOp(type, reverse) + " %r" + r1 + ", %r" + r2);
   }
}