import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;

public class InstructionFactory {
   private static final String[] ops = {"eq", "ge", "gt", "le", "lt", "ne"};
   private static final String[] asmOps = {"e", "ge", "g", "le", "l", "ne"};
   private static final String[] paramRegs = {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};

   public static final String[] registers = {"10", "11", "12", "13", "14", "15"};
   public static final String TEMP_REG = "bx";

   public static final int NUM_PARAM_REGS = paramRegs.length;

   public static final String printName = ".LC0";
   public static final String printlnName = ".LC1";
   public static final String readName = ".LC2";

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
            +printName + ":\n"
            +"\t.string \"%ld \"\n"
            +printlnName + ":\n"
            +"\t.string \"%ld\\n\"\n"
            +readName + ":\n"
            +"\t.string \"%ld\"\n"
            +"\t.text\n"
            +".globl main\n"
            +"\t.type main, @function\n";
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

   public static Instruction functionStart(final String name) {
      final int stackSize = funcs.get(name).getStackSize();
      return new Instruction("",
                              new Register[] {},
                              null) {
         protected String[] getBareAsm() {
            return null;
         }

         public List<String> toAssembly() {
            List<String> commands = new ArrayList<String>();
            commands.add("pushq %rbp");
            commands.add("movq %rsp, %rbp");
            if (! name.equals("main")) {
               commands.add("push %r12");
               commands.add("push %r13");
               commands.add("push %r14");
               commands.add("push %r15");
            }
            commands.add("subq $" + stackSize + ", %rsp");
            return commands;
         }
      };                       
   }

   private static Instruction mul(final Long imm, Register left, Register right, Register result) {
      String iloc = "mult r" + left.getILOC() + ", " + (imm == null ? ("r" + right.getILOC()) : imm) + ", r" + result.getILOC();
      if (imm == null) {
         return new Instruction(iloc,
                                 new Register[] {left, right},
                                 result) {
            protected String[] getBareAsm() {
               if (sources[0].getASM() == target.getASM()) {
                  return new String[] {
                     "imul %r" + sources[1].getASM() + ", %r" + target.getASM()
                  };
               } else if (sources[1].getASM() == target.getASM()) {
                  return new String[] {
                     "imul %r" + sources[0].getASM() + ", %r" + target.getASM()
                  };
               } else {
                  return new String[] {
                     "mov %r" + sources[0].getASM() + ", %r" + target.getASM(),
                     "imul %r" + sources[1].getASM() + ", %r" + target.getASM()
                  };
               }
            }
         };
      } else {
         return new Instruction(iloc,
                                 new Register[] {left},
                                 result) {
            protected String[] getBareAsm() {
               if (sources[0].getASM() == target.getASM()) {
                  return new String[] {
                     "imul $" + imm + ", %r" + target.getASM()
                  };
               }
               else {
                  return new String[] {
                     "imul $" + imm + ", %r" + sources[0].getASM() + ", %r" + target.getASM()
                  };
               }
            }
         };
      }
   }

   private static Instruction div(Register left, Register right, Register result) {
      String iloc = "div r" + left.getILOC() + ", r" + right.getILOC() + ", r" + result.getILOC();
      return new Instruction(iloc,
                              new Register[] {left, right},
                              result) {
         protected String[] getBareAsm() {
            if (target.getASM().equals("ax")) {
               return new String[] {
                  "push %rdx",
                  "mov %r" + sources[0].getASM() + ", %rax",
                  "mov %r" + sources[0].getASM() + ", %rdx",
                  "sarq $63, %rdx",
                  "idiv %r" + sources[1].getASM(),
                  "pop %rdx"
               };
            } else {
               return new String[] {
                  "push %rdx",
                  "push %rax",
                  "mov %r" + sources[0].getASM() + ", %rax",
                  "mov %r" + sources[0].getASM() + ", %rdx",
                  "sarq $63, %rdx",
                  "idiv %r" + sources[1].getASM(),
                  "mov %rax, %r" + target.getASM(),
                  "pop %rax",
                  "pop %rdx"
               };
            }
         }
      };
   }

   public static Instruction arithmetic(final int type, final Long imm, Register left, Register right, Register result) {
      String op = null;
      switch (type) {
         case TypeCheck.PLUS:
            op = imm == null ? "add" : "addi";
            break;
         case TypeCheck.MINUS:
            op = imm == null ? "sub" : "rsubi";
            break;
         case TypeCheck.TIMES:
            return mul(imm, left, right, result);
         case TypeCheck.DIVIDE:
            return div(left, right, result);
         default:
            throw new RuntimeException("unkown arithmetic op");
      }

      String iloc = op + " r" + left.getILOC() + ", " + (imm == null ? ("r" + right.getILOC()) : imm) + ", r" + result.getILOC();
      if (imm == null) {
         final String asm = op;

         if (left.getILOC() == result.getILOC()) {
            return new Instruction(iloc,
                                    new Register[] {left, right},
                                    result) {
               protected String[] getBareAsm() {
                  return new String[] {
                     asm + "q %r" + sources[1].getASM() + ", %r" + target.getASM()
                  };
               }
            };
         } else if (right.getILOC() == result.getILOC()) {
            if (type == TypeCheck.PLUS || type == TypeCheck.TIMES) {
               return new Instruction(iloc,
                                       new Register[] {left, right},
                                       result) {
                  protected String[] getBareAsm() {
                     return new String[] {
                        asm + "q %r" + sources[0].getASM() + ", %r" + target.getASM()
                     };
                  }
               };
            } else {
               return new Instruction(iloc,
                                       new Register[] {left, right},
                                       result) {
                  protected String[] getBareAsm() {
                     return new String[] {
                        "NOT SUPPORTED YET"
                     };
                  }
               };
            }
         } else {
            return new Instruction(iloc,
                                    new Register[] {left, right},
                                    result) {
               protected String[] getBareAsm() {
                  return new String[] {
                     "mov %r" + sources[0].getASM() + ", %r" + target.getASM(),
                     asm + "q %r" + sources[1].getASM() + ", %r" + target.getASM()
                  };
               }
            };
         }
      } else {
         if (left.getILOC() == result.getILOC()) {
            return new Instruction(iloc,
                                    new Register[] {left},
                                    result) {
               protected String[] getBareAsm() {
                  return new String[] {
                     (type == TypeCheck.PLUS ? "addq" : "subq") + " $" + imm + ", %r" + target.getASM()
                  };
               }
            };
         } else {
            return new Instruction(iloc,
                                    new Register[] {left},
                                    result) {
               protected String[] getBareAsm() {
                  return new String[] {
                     "mov %r" + sources[0].getASM() + ", %r" + target.getASM(),
                     (type == TypeCheck.PLUS ? "addq" : "subq") + " $" + imm + ", %r" + target.getASM()
                  };
               }
            };
         }
      }
   }

   public static Instruction bool(final int type, Register left, Register right, Register result) {
      final String iloc = (type == TypeCheck.AND ? "and" : "or") + " r" + left.getILOC() + ", r" + right.getILOC() + ", r" + result.getILOC();
      if (left.getILOC() == result.getILOC()) {
         return new Instruction(iloc,
                                 new Register[] {left, right},
                                 result) {
            protected String[] getBareAsm() {
               return new String[] {
                  (type == TypeCheck.AND ? "and" : "or") + " %r" + sources[1].getASM() + ", %r" + target.getASM()
               };
            }
         };
      } else if (right.getILOC() == result.getILOC()) {
         return new Instruction(iloc,
                                 new Register[] {left, right},
                                 result) {
            protected String[] getBareAsm() {
               return new String[] {
                  (type == TypeCheck.AND ? "and" : "or") + " %r" + sources[0].getASM() + ", %r" + target.getASM()
               };
            }
         };
      } else {
         return new Instruction(iloc,
                                 new Register[] {left, right},
                                 result) {
            protected String[] getBareAsm() {
               return new String[] {
                  "mov %r" + sources[0].getASM() + ", %r" + target.getASM(),
                  (type == TypeCheck.AND ? "and" : "or") + " %r" + sources[1].getASM() + ", %r" + target.getASM()
               };
            }
         };
      }
   }

   public static Instruction xori(Register reg, final long imm, Register result) {
      if (reg.getILOC() == result.getILOC()) {
         return new Instruction("xori r" + reg.getILOC() + ", " + imm + ", r" + result.getILOC(),
                                 new Register[] {reg},
                                 result) {
            protected String[] getBareAsm() {
               return new String[] {
                  "xor $" + imm + ", %r" + target.getASM()
               };
            }
         };
      } else {
         return new Instruction("xori r" + reg.getILOC() + ", " + imm + ", r" + result.getILOC(),
                                 new Register[] {reg},
                                 result) {
            protected String[] getBareAsm() {
               return new String[] {
                  "xor $" + imm + ", %r" + target.getASM()
               };
            }
         };
      }
   }

   public static Instruction comp(Register left, Register right) {
      return new Instruction("comp r" + left.getILOC() + ", r" + right.getILOC(),
                              new Register[] {left, right},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "cmp %r" + sources[0].getASM() + ", %r" + sources[1].getASM()
            };
         }
      };
   }

   public static Instruction compi(Register reg, final long imm) {
      return new Instruction("compi r" + reg.getILOC() + ", " + imm,
                              new Register[] {reg},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "cmp $" + imm + ", %r" + sources[0].getASM()
            };
         }
      };
   }

   public static Instruction ccbranch(int type, boolean reverse, final String label1, final String label2) {
      final String asmComp = getCompAsmOp(type, reverse);
      return new Instruction("cbr" + getCompOp(type, reverse) + " ccr, " + label1 + ", " + label2,
                              new Register[] {},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "j" + asmComp + " " + label1,
               "jmp " + label2
            };
         }
      };
   }

   public static Instruction jump(final String label) {
      return new Instruction("jumpi " + label,
                              new Register[] {},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "jmp " + label
            };
         }
      };
   }

   public static Instruction loadi(final long immediate, Register result) {
      return new Instruction("loadi " + immediate + ", r" + result.getILOC(),
                              new Register[] {},
                              result) {
         protected String[] getBareAsm() {
            return new String[] {
               "movq $" + immediate + ", %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction loadai(Register reg, final long imm, Register result) {
      return new Instruction("loadai r" + reg.getILOC() + ", " + imm + ", r" + result.getILOC(),
                              new Register[] {reg},
                              result) {
         protected String[] getBareAsm() {
            return new String[] {
               "movq " + imm + "(%r" + sources[0].getASM() + "), %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction loadGlobal(final String var, Register result) {
      return new Instruction("loadglobal " + var + ", r" + result.getILOC(),
                              new Register[] {},
                              result) {
         protected String[] getBareAsm() {
            return new String[] {
               "movq glob_" + var + "(%rip), %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction loadArg(String var, int num, Register reg, boolean noMove) {
      if (num < NUM_PARAM_REGS) {
         final String regName = paramRegs[num];
         return new Instruction("loadinargument " + var + ", " + num + ", r" + reg.getILOC(),
                                 new Register[] {},
                                 reg,
                                 noMove ? Instruction.Types.IMMOVEABLE : Instruction.Types.NORMAL) {
            protected String[] getBareAsm() {
               return new String[] {
                  "movq %" + regName + ", %r" + target.getASM()
               };
            }
         };
      } else {
         return new Instruction("loadinargument " + var + ", " + num + ", r" + reg.getILOC(),
                                 new Register[] {},
                                 reg) {
            protected String[] getBareAsm() {
               return new String[] {
                  "NOT SUPPORTED YET"
               };
            }
         };
      }
   }

   public static Instruction loadRet(Register reg) {
      return new Instruction("loadret r" + reg.getILOC(),
                              new Register[] {},
                              reg) {
         protected String[] getBareAsm() {
            return new String[] {
               "mov %rax, %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction formalAddr(String var, int num, Register reg) {
      final int offset = funcs.get(currentFunction).getOffset(var) * 8;
      return new Instruction("computeformaladdress " + var + ", " + num + ", r" + reg.getILOC(),
                              new Register[] {},
                              reg) {
         protected String[] getBareAsm() {
            return new String[] {
               "leaq -" + offset + "(%rbp), %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction restoreFormal(String var, int num) {
      return new Instruction("restoreformal " + var + ", " + num,
                              new Register[] {},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "NOT SUPPORTED YET"
            };
         }
      };
   }

   public static Instruction globalAddr(final String var, Register result) {
      return new Instruction("computeglobaladdress " + var + ", r" + result.getILOC(),
                              new Register[] {},
                              result) {
         protected String[] getBareAsm() {
            return new String[] {
               "movq $glob_" + var + ", %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction storeai(Register reg, Register result, final long imm) {
      return new Instruction("storeai r" + reg.getILOC() + ", r" + result.getILOC() + ", " + imm,
                              new Register[] {reg, result},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "movq %r" + sources[0].getASM() + ", " + imm + "(%r" + sources[1].getASM() + ")"
            };
         }
      };
   }

   public static Instruction storeGlobal(Register reg, final String var) {
      return new Instruction("storeglobal r" + reg.getILOC() + ", " + var,
                              new Register[] {reg},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "movq %r" + sources[0].getASM() + ", glob_" + var + "(%rip)"
            };
         }
      };
   }

   public static Instruction storeInArg(Register reg, String var, int num) {
      if (num < NUM_PARAM_REGS) {
         final String regName = paramRegs[num];
         return new Instruction("storeinargument r" + reg.getILOC() + ", " + var + ", " + num,
                                 new Register[] {reg},
                                 null) {
            protected String[] getBareAsm() {
               return new String[] {
                  "movq %r" + sources[0].getASM() + ", %" + regName
               };
            }
         };
      }
      else {
         return new Instruction("storeinargument r" + reg.getILOC() + ", " + var + ", " + num,
                                 new Register[] {reg},
                                 null) {
            protected String[] getBareAsm() {
               return new String[] {
                  "NOT SUPPORTED YET"
               };
            }
         };
      }
   }

   public static Instruction storeOutArg(Register reg, int num) {
      return new Instruction("storeoutargument r" + reg.getILOC() + ", " + num,
                              new Register[] {reg},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "NOT SUPPORTED YET"
            };
         }
      };
   }

   public static Instruction storeRet(Register reg) {
      return new Instruction("storeret r" + reg.getILOC(),
                              new Register[] {reg},
                              null) {
         protected String[] getBareAsm() {
            return new String[] {
               "mov %r" + sources[0].getASM() + ", %rax"
            };
         }
      };
   }

   public static Instruction call(final String label) {
      return new Instruction("call " + label,
                              new Register[] {},
                              null,
                              Instruction.Types.CALL) {
         protected String[] getBareAsm() {
            return new String[] {
               "push %r10",
               "push %r11",
               "call " + label,
               "pop %r11",
               "pop %r10"
            };
         }
      };
   }

   public static Instruction ret() {
      return jump(funcs.get(currentFunction).getExit().getLabel());
   }

   public static Instruction realRet() {
      final int stackSize = funcs.get(currentFunction).getStackSize();
      final String funcName = funcs.get(currentFunction).getEntry().getLabel();
      return new Instruction("ret",
                              new Register[] {},
                              null) {
         protected String[] getBareAsm() {
            return null;
         }

         public List<String> toAssembly() {
            List<String> commands = new ArrayList<String>();
            commands.add("addq $" + stackSize + ", %rsp");
            if (! funcName.equals("main")) {
               commands.add("pop %r15");
               commands.add("pop %r14");
               commands.add("pop %r13");
               commands.add("pop %r12");
            }
            commands.add("leave");
            commands.add("ret");
            commands.add(".size " + funcName + ", .-" + funcName);
            return commands;
         }
      };
   }

   public static Instruction newAlloc(final int num, Register reg) {
      return new Instruction("new " + num + ", r" + reg.getILOC(),
                              new Register[] {},
                              reg,
                              Instruction.Types.CALL) {
         protected String[] getBareAsm() {
            return new String[] {
               "movq $" + num + ", %rdi",
               "push %r10",
               "push %r11",
               "call malloc",
               "pop %r11",
               "pop %r10",
               "mov %rax, %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction del(Register reg) {
      return new Instruction("del r" + reg.getILOC(),
                              new Register[] {reg},
                              null,
                              Instruction.Types.CALL) {
         protected String[] getBareAsm() {
            return new String[] {
               "mov %r" + sources[0].getASM() + ", %rdi",
               "push %r10",
               "push %r11",
               "call free",
               "pop %r11",
               "pop %r10"
            };
         }
      };
   }

   private static Instruction printInst(Register reg, final boolean newLine) {
      return new Instruction((newLine ? "println" : "print") + " r" + reg.getILOC(),
                              new Register[] {reg},
                              null,
                              Instruction.Types.CALL) {
         protected String[] getBareAsm() {
            return new String[] {
               "push %rsi",
               "push %rdi",
               "movq %r" + sources[0].getASM() + ", %rsi",
               "movq $" + (newLine ? InstructionFactory.printlnName : InstructionFactory.printName) + ", %rdi",
               "movq $0, %rax",
               "push %r10",
               "push %r11",
               "call printf",
               "pop %r11",
               "pop %r10",
               "pop %rdi",
               "pop %rsi"
            };
         }
      };
   }

   public static Instruction print(Register reg) {
      return printInst(reg, false);
   }

   public static Instruction println(Register reg) {
      return printInst(reg, true);
   }

   public static Instruction read(Register reg) {
      return new Instruction("read r" + reg.getILOC(),
                              new Register[] {},
                              reg,
                              Instruction.Types.CALL) {
         protected String[] getBareAsm() {
            return new String[] {
               "leaq -8(%rbp), %r" + target.getASM(),
               "movq $" + InstructionFactory.readName + ", %rdi",
               "movq %r" + target.getASM() + ", %rsi",
               "movq $0, %rax",
               "push %r10",
               "push %r11",
               "call scanf",
               "pop %r11",
               "pop %r10",
               "mov -8(%rbp), %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction mov(Register r1, Register r2) {
      return new Instruction("mov r" + r1.getILOC() + ", r" + r2.getILOC(),
                              new Register[] {r1},
                              r2) {
         protected String[] getBareAsm() {
            return new String[] {
               "mov %r" + sources[0].getASM() + ", %r" + target.getASM()
            };
         }
      };
   }

   public static Instruction ccmove(int type, boolean reverse, Register r1, Register r2) {
      final String asmComp = getCompAsmOp(type, reverse);
      return new Instruction("mov"+ getCompOp(type, reverse) + " ccr, r" + r1.getILOC() + ", r" + r2.getILOC(),
                              new Register[] {r1, r2},
                              r2) {
         protected String[] getBareAsm() {
            return new String[] {
               "cmov" + asmComp + " %r" + sources[0].getASM() + ", %r" + sources[1].getASM()
            };
         }
      };
   }
}