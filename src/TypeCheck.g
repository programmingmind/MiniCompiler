tree grammar TypeCheck;

options
{
   tokenVocab=Mini;
   ASTLabelType=CommonTree;
}

@header
{
   import java.io.BufferedWriter;
   import java.io.File;
   import java.io.FileWriter;
   import java.io.IOException;

   import java.util.ArrayList;
   import java.util.Map;
   import java.util.HashMap;
   import java.util.Vector;
   import java.util.Iterator;
   import java.util.Set;
}

@members
{
   private StructTypes stypes = new StructTypes();
   private Functions funcs = new Functions();
   private Function func;
   private Stack<SymbolTable> stables;
   private Stack<Type> returns;
   private Stack<Block> next;
   private Stack<Block> current;

   private File ilocFile = null;
   private File asmFile = null;
   private FileWriter ilocFW = null;
   private FileWriter asmFW = null;
   private BufferedWriter ilocBW = null;
   private BufferedWriter asmBW = null;

   private boolean inAssign;

   private boolean preserveConstants;
}

error0 [String text]
   : error[0,text]
   ;

error [int line, String text]
   : { if (true) throw new RuntimeException($line + " : " + $text); }
   ;

verify [boolean outputIloc, String fileName, boolean removeDeadCode, boolean performCopyPropogation, boolean preserveConstants]
   : program [outputIloc, fileName, removeDeadCode, performCopyPropogation, preserveConstants]
      {
         System.out.print("Type checking passed");
         if (outputIloc)
            System.out.print(", iloc generated,");
         System.out.println(" and assembly instructions generated");

         if (!funcs.isDefined("main"))
            System.err.println("WARNING: missing main()");
      }
   | { System.out.println("not a valid AST"); }
   ;

program [boolean outputIloc, String fileName, boolean removeDeadCode, boolean performCopyPropogation, boolean preserveConstants]
   @init
   {
      this.preserveConstants = preserveConstants;

      stables = new Stack<SymbolTable>();
      returns = new Stack<Type>();
      SymbolTable stable = new SymbolTable();
      stables.push(stable);
      returns.push(Type.voidType());

      InstructionFactory.setFunctions(funcs);

      String name = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
      name = name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;

      System.out.println("saving to asm/" + name + ".s");
      asmFile = new File("asm/" + name + ".s");
      try {
         if (!asmFile.exists())
            asmFile.createNewFile();
 
         asmFW = new FileWriter(asmFile.getAbsoluteFile());
         asmBW = new BufferedWriter(asmFW);

         asmBW.write(InstructionFactory.getProgramHeader());

         if (outputIloc) {
            System.out.println("saving to iloc/" + name + ".il");
            ilocFile = new File("iloc/" + name + ".il");
            
            if (!ilocFile.exists())
               ilocFile.createNewFile();
    
            ilocFW = new FileWriter(ilocFile.getAbsoluteFile());
            ilocBW = new BufferedWriter(ilocFW);
         }
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }
   : ^(PROGRAM types[stable] declarations[stable]
         {
            try {
               asmBW.write(InstructionFactory.globalHeader(stable));
            } catch (IOException ioe) {
               ioe.printStackTrace();
            }
         }
         functions[removeDeadCode, performCopyPropogation])
      {
         try {
            asmBW.close();
            asmFW.close();

            if (outputIloc) {
               ilocBW.close();
               asmFW.close();
            }

         } catch (IOException ioe) {
            ioe.printStackTrace();
         }
      }
   ;

types [SymbolTable stable]
   : ^(TYPES (types_sub[stable]))
   | TYPES { System.out.println("no struct types"); }
   ;

types_sub [SymbolTable stable]
   : type_declaration[stable] types_sub[stable]?
   ;

type_declaration [SymbolTable stable]
   : ^(STRUCT id=ID { stypes.newStruct($id.text); } nested_decl[stypes.getStructMembers($id.text)])
   ;

nested_decl[SymbolTable stable]
   : (decl[stable,false])+
   ;

decl [SymbolTable stable,boolean isParam]
   : ^(DECL ^(TYPE t=type) id=ID)
      {
         if (isParam)
            $stable.putParam($id.text, t.t);
         else
            $stable.put($id.text, t.t);
      }
   ;

type returns [Type t = null]
   : INT { $t = Type.intType(); }
   | BOOL { $t = Type.boolType(); }
   | VOID { $t = Type.voidType(); }
   | ^(STRUCT id=ID)
      {
         if (!stypes.isDefined($id.text))
         {
            error($id.line, "undefined struct type '" + $id + "'");
         }
         $t = Type.structType($id.text);
      }
   ;

declarations [SymbolTable stable]
   : ^(DECLS declaration[stable])
   | { System.out.println("There are no declarations"); }
   ;

declaration [SymbolTable stable]
   : (decl_list[stable])*
   ;

decl_list [SymbolTable stable]
   : ^(DECLLIST ^(TYPE t=type) id_list[t.t,stable])
   ;

id_list [Type t, SymbolTable stable]
   : (
         id=ID
         {
            if ($stable.redef($id.text))
            {
               error($id.line, "redefinition of variable '" + $id + "'");
            }
               else if ($stable.isFormal($id.text))
            {
               error($id.line, "redefinition of parameter '" + $id + "'");
            }
            $stable.put($id.text, $t);
         }
      )+
   ;

functions [boolean removeDeadCode, boolean performCopyPropogation]
   : ^(FUNCS function[removeDeadCode, performCopyPropogation]*)
   ;

function [boolean removeDeadCode, boolean performCopyPropogation]
   @init
   {
      SymbolTable vars = new SymbolTable();
      next = new Stack<Block>();
      current = new Stack<Block>();
      inAssign = false;
   }
   : ^(
         FUN id=ID parameters[vars] ^(RETTYPE rt=return_type) declarations[vars]
         {
            funcs.newFunction(func = new Function($id.text,rt.t,vars));
            InstructionFactory.setFunction($id.text);
            stables.push(vars);
            returns.push(rt.t); 
            current.push(func.getEntry());
            next.push(func.getExit());

            for (String arg : func.getParamNames()) {
               Register reg;
               func.putVarRegister(arg, reg = func.getNextRegister());
               current.peek().addInstruction(InstructionFactory.loadArg(arg, func.getParamIndex(arg), reg, false));
            }
         }
         sl=statement_list
      )
      {
         if (sl.t == null && !returns.peek().equals(Type.voidType()))
            error0($id.text + " missing return value");

         func.getExit().addInstruction(InstructionFactory.realRet());

         stables.pop();
         returns.pop();
         current.peek().addNext(func.getExit());

         func.cleanBlocks();
         if (performCopyPropogation)
            func.performCopyPropogation();
         
         if (removeDeadCode)
            func.removeUselessInstructions();

         if (performCopyPropogation && removeDeadCode)
            func.performTargetPropogation();

         func.allocateRegisters();

         String[] code = func.getCode();
         try {
            asmBW.write(code[1] + "\n");

            if (ilocBW != null) {
               func.saveDot();

               ilocBW.write(code[0] + "\n");
            }
         } catch (IOException ioe) {
            ioe.printStackTrace();
         }
      }
   ;

parameters [SymbolTable params]
   : ^(PARAMS decl[params,true]*)
   ;

return_type returns [Type t = null]
   : tmp=type { $t = tmp.t; System.out.println("has return: " + $t); }
   | { System.out.println("void return"); $t = Type.voidType(); }
   ;

store_reg [lvalue_return l] returns [Register reg]
   : {$reg = ((l.global || l.dl != null) && (!inAssign || func.getLoopDepth() == 0)) ? func.getNextRegister() : l.reg;}
   ;

store_val [Register reg, lvalue_return l]
   : {if (l.global) {
         current.peek().addInstruction(InstructionFactory.storeGlobal(reg, l.name));
      } else if (l.dl != null) {
         current.peek().addInstruction(InstructionFactory.storeai(reg, l.dl.reg, stypes.getStructMembers(l.dl.t.getName()).getOffset(l.name)));
      }}
   ;

statement returns [Type t = null]
   :  {
         next.push(new Block(func.getName()));
      }
      b=block
      {
         $t = b.t;
         b.last.addNext(next.peek());
         current.pop().addNext(b.blk);
         current.push(next.pop());
      }
   | ^(ASSIGN {inAssign = true;} a=expression l=lvalue)
      {
         System.out.println("Here I be assigning");
         if (! a.t.equals(l.t))
            error($ASSIGN.line, "assignments need the same types (" + l.t + " vs " + a.t + ")");
 
         if (l.wasStruct)
            current.peek().removeUnnecessaryLVal();

         if (! l.wasStruct && !l.global && a.imm != null && func.getConditionalDepth() == 0 && func.getLoopDepth() == 0) {
            func.putVarImmediate(l.name, a.imm);
         } else {
            Register reg = store_reg(l).reg;
            if (reg == null) {
               reg = func.getNextRegister();
               func.putVarRegister(l.name, reg);
            }

            if (a.imm != null) {
               current.peek().addInstruction(InstructionFactory.loadi(a.imm, reg));
            } else {
               current.peek().addInstruction(InstructionFactory.mov(a.reg, reg));
            }
            store_val(reg, l);
         }

         inAssign = false;
      }
   | ^(PRINT a=expression ENDL?)
      {
         if (! a.t.isInt())
            error0("can only print ints");

         if (a.reg == null)
            current.peek().addInstruction(InstructionFactory.loadi(a.imm, a.reg = func.getNextRegister()));

         Instruction tmp = null;
         if ($ENDL != null)
            tmp = InstructionFactory.println(a.reg);
         else
            tmp = InstructionFactory.print(a.reg);

         current.peek().addInstruction(tmp);
      }
   | ^(READ l=lvalue)
      {
         if (! l.t.isInt())
            error0("can only read in ints");

         Register reg = store_reg(l).reg;
         if (reg == null) {
            reg = func.getNextRegister();
            func.putVarRegister(l.name, reg);
         }

         current.peek().addInstruction(InstructionFactory.read(reg));
         store_val(reg, l);
      }
   | ^(
         token=IF
         {
            next.push(new Block(func.getName()));
         }
         a=expression
         {
            func.enterConditional();
         }
         b=block
         {
            func.exitConditional();
            func.enterConditional();
         }
         b2=block?
         {
            func.exitConditional();
         }
      )
      {
         if (! a.t.isBool())
            error0("if statements need bool expression");
         if (b2 != null && b.t != null && b.t.equals(b2.t))
            $t = b.t;

         if (a.imm != null) {
            boolean flag = a.imm != 0;
            System.err.println("WARNING: if statement always " + flag + " at line " + $token.line);
            if (flag)
               current.peek().addInstruction(InstructionFactory.jump(b.blk.getLabel()));
            else if (b2 != null)
               current.peek().addInstruction(InstructionFactory.jump(b2.blk.getLabel()));
            else
               current.peek().addInstruction(InstructionFactory.jump(next.peek().getLabel()));
         } else {
            current.peek().addInstruction(InstructionFactory.compi(a.reg, 1));
            current.peek().addInstruction(InstructionFactory.ccbranch(EQ, false, b.blk.getLabel(), (b2 == null ? next.peek() : b2.blk).getLabel()));
         }

         current.peek().addNext(b.blk);
         b.last.addNext(next.peek(), true);
         if (b2 == null) {
            current.peek().addNext(next.peek(), true);
         } else {
            current.peek().addNext(b2.blk);
            b.last.addInstruction(InstructionFactory.jump(next.peek().getLabel()));
            b2.last.addNext(next.peek(), true);
         }
         current.pop();
         current.push(next.pop());
      }
   | ^(
         token=WHILE
         {
            Set<String> imms = func.getImmediateVars();
            for (String name : imms) {
               Register nextR = func.getNextRegister();
               current.peek().addInstruction(InstructionFactory.loadi(func.getVarImmediate(name), nextR));
               func.putVarRegister(name, nextR);
            }
            next.push(new Block(func.getName()));
            func.enterLoop();
         }
         a=expression b=block {current.push(b.last);} e=expression {current.pop();}
      )
      {
         if (! (a.t.isBool() && e.t.isBool()))
            error0("while statements need bool expressions");

         if (a.imm != null) {
            boolean flag = a.imm != 0;
            System.err.println("WARNING: while statement always " + flag + " at line " + $token.line);
            if (flag) {
               current.peek().addInstruction(InstructionFactory.jump(b.blk.getLabel()));
               b.last.addInstruction(InstructionFactory.jump(b.blk.getLabel()));
            }
            else {
               current.peek().addInstruction(InstructionFactory.jump(next.peek().getLabel()));
               b.last.addInstruction(InstructionFactory.jump(next.peek().getLabel()));
            }
         } else {
            current.peek().addInstruction(InstructionFactory.compi(a.reg, 1));
            current.peek().addInstruction(InstructionFactory.ccbranch(EQ, false, b.blk.getLabel(), next.peek().getLabel()));
            b.last.addInstruction(InstructionFactory.compi(e.reg, 1));
            b.last.addInstruction(InstructionFactory.ccbranch(EQ, false, b.blk.getLabel(), next.peek().getLabel()));
         }

         b.last.addNext(b.blk);
         b.last.addNext(next.peek(), true);
         current.peek().addNext(b.blk);
         current.peek().addNext(next.peek(), true);
         current.pop();
         current.push(next.pop());

         func.exitLoop();
      }
   | ^(DELETE a=expression)
      {
         if (! a.t.isStruct())
            error0("can only delete a struct");
         current.peek().addInstruction(InstructionFactory.del(a.reg));
      }
   | r=ret
      {
         if (! r.t.equals(returns.peek()))
            error0("return type doesn't match");
         $t = r.t;
         current.peek().addInstruction(InstructionFactory.ret());
         current.peek().addNext(func.getExit());
      }
   | invocation { System.out.println("invoking expression"); }
   ;

block returns [Type t = null, Block blk = null, Block last = null]
   @init
   {
      Block blk = new Block(func.getName());
      $blk = blk;
   }
   : ^(
         BLOCK
         {
            current.peek().addNext(blk);
            current.push(blk);
         }
         sl=statement_list
      )
      {
         System.out.println("Heres a block");
         $t = sl.t;
         $last = current.pop();
      }
   ;

statement_list returns [Type t = null]
   : ^(
         STMTS
         (
            tmp=statement
            {
               if ($t != null)
                  System.err.println("WARNING: statements after return");
               else
                  $t = tmp.t;
            }
         )*
      )
   ;
 
ret returns [Type t = null]
   : ^(RETURN tmp=expression)
      {
         System.out.println("returned expression");
         $t = tmp.t;

         if (tmp.imm != null)
            current.peek().addInstruction(InstructionFactory.loadi(tmp.imm, tmp.reg = func.getNextRegister()));
         current.peek().addInstruction(InstructionFactory.storeRet(tmp.reg));
      }
   | RETURN
      {
         System.out.println("no return expression");
         $t = Type.voidType();
      }
   ;

invocation returns [Type t = null]
   : ^(INVOKE id=ID args=arguments)
      {
         funcs.get($id.text).checkParams(args.argTypes);
         for (int i = 0; i < args.registers.size(); i++)
            current.peek().addInstruction(InstructionFactory.storeInArg(args.registers.get(i), funcs.get($id.text).getParamName(i), i));

         current.peek().addInstruction(InstructionFactory.call(funcs.get($id.text).getEntry().getLabel()));
         System.out.println("invoked " + $id.text);
         $t = funcs.get($id.text).getReturnType();
      }
   ;

expression returns [Type t = null, Register reg = null, Long imm = null]
   : ^(op=(PLUS | TIMES | DIVIDE) a=expression b=expression)
      {
         if (! (a.t.isInt() && b.t.isInt()))
            error0("not an int");
         $t=Type.intType();

         Long tmpi = null;
         Register lReg = null, rReg = null;

         if (a.imm != null && b.imm != null) {
            switch (op.getType()) {
               case PLUS:
                  $imm = a.imm + b.imm;
                  break;
               case TIMES:
                  $imm = a.imm * b.imm;
                  break;
               case DIVIDE:
                  $imm = a.imm / b.imm;
                  break;
            }
         } else {
            if (a.imm != null) {
               lReg = b.reg;
               tmpi = a.imm;
            } else if (b.imm != null) {
               lReg = a.reg;
               tmpi = b.imm;
            } else {
               lReg = a.reg;
               rReg = b.reg;
            }

            if (tmpi != null && op.getType() == DIVIDE) {
               rReg = func.getNextRegister();
               current.peek().addInstruction(InstructionFactory.loadi(tmpi, rReg));
               tmpi = null;
            }

            $reg = func.getNextRegister();
            current.peek().addInstruction(InstructionFactory.arithmetic(op.getType(), tmpi, lReg, rReg, $reg));
         }
      }
   | ^(MINUS a=expression b=expression?)
      {
         if (! (a.t.isInt() && (b == null || b.t.isInt())))
            error0("not an int");
         $t=Type.intType();

         if (b == null) {
            if (a.imm != null)
               $imm = -a.imm;
            else {
               Register tmp = func.getNextRegister();
               current.peek().addInstruction(InstructionFactory.loadi(0, tmp));
               current.peek().addInstruction(InstructionFactory.arithmetic(MINUS, null, tmp, a.reg, $reg = func.getNextRegister()));
            }
         } else if (a.imm != null && b.imm != null) {
            $imm = a.imm - b.imm;
         } else if (a.imm != null) {
            current.peek().addInstruction(InstructionFactory.arithmetic(PLUS, -a.imm, b.reg, null, $reg = func.getNextRegister()));
            current.peek().addInstruction(InstructionFactory.arithmetic(TIMES, -1L, $reg, null, $reg));
         } else if (b.imm != null) {
            current.peek().addInstruction(InstructionFactory.arithmetic(MINUS, b.imm, a.reg, null, $reg = func.getNextRegister()));
         } else {
            current.peek().addInstruction(InstructionFactory.arithmetic(MINUS, null, a.reg, b.reg, $reg = func.getNextRegister()));
         }
      }
   | ^(op=(AND | OR) a=expression b=expression)
      {
         if (! (a.t.isBool() && b.t.isBool()))
            error0("not a bool");
         $t=Type.boolType();

         if (a.imm != null && b.imm != null) {
            $imm = op.getType() == AND ? (a.imm & b.imm) : (a.imm | b.imm);
         } else if (a.imm != null || b.imm != null) {
            Long imm;
            Register reg;
            if (a.imm != null) {
               imm = a.imm;
               reg = b.reg;
            } else {
               imm = b.imm;
               reg = a.reg;
            }

            if (op.getType() == AND) {
               if (imm == 0) {
                  System.err.println($op.line + ": expression is always false");
                  $imm = 0L;
               } else {
                  $reg = reg;
               }
            } else {
               if (imm == 0) {
                  $reg = reg;
               } else {
                  System.err.println($op.line + ": expression is always true");
                  $imm = 1L;
               }
            }
         } else {
            Register lReg = a.reg;
            Register rReg = b.reg;
            if (a.imm != null)
               current.peek().addInstruction(InstructionFactory.loadi(a.imm, lReg = func.getNextRegister()));
            if (b.imm != null)
               current.peek().addInstruction(InstructionFactory.loadi(b.imm, rReg = func.getNextRegister()));
            current.peek().addInstruction(InstructionFactory.bool(op.getType(), lReg, rReg, $reg = func.getNextRegister()));
         }
      }
   | ^(op=(LT | GT | LE | GE) a=expression b=expression)
      {
         if (! (a.t.isInt() && b.t.isInt()))
            error($op.line, "numeric comparisons need ints");
         $t=Type.boolType();

         if (a.imm != null && b.imm != null) {
            switch (op.getType()) {
               case LT:
                  $imm = a.imm < b.imm ? 1L : 0L;
                  break;
               case GT:
                  $imm = a.imm > b.imm ? 1L : 0L;
                  break;
               case LE:
                  $imm = a.imm <= b.imm ? 1L : 0L;
                  break;
               case GE:
                  $imm = a.imm >= b.imm ? 1L : 0L;
                  break;
            }
         } else if (a.imm == null && b.imm == null) {
            $reg = func.getNextRegister();
            Register tmp = func.getNextRegister();
            current.peek().addInstruction(InstructionFactory.loadi(0, $reg));
            current.peek().addInstruction(InstructionFactory.loadi(1, tmp));
            current.peek().addInstruction(InstructionFactory.comp(a.reg, b.reg));
            current.peek().addInstruction(InstructionFactory.ccmove(op.getType(), false, tmp, $reg));
         } else {
            Register tmpReg;
            Long tmpImm;
            boolean reverse;
            if (a.imm != null) {
               tmpReg = b.reg;
               tmpImm = a.imm;
               reverse = true;
            } else {
               tmpReg = a.reg;
               tmpImm = b.imm;
               reverse = false;
            }
            $reg = func.getNextRegister();
            Register tmp = func.getNextRegister();
            current.peek().addInstruction(InstructionFactory.loadi(0, $reg));
            current.peek().addInstruction(InstructionFactory.loadi(1, tmp));
            current.peek().addInstruction(InstructionFactory.compi(tmpReg, tmpImm));
            current.peek().addInstruction(InstructionFactory.ccmove(op.getType(), reverse, tmp, $reg));
         }
      }
   | ^(op = (EQ | NE) a=expression b=expression)
      {
         if (! (a.t.equals(b.t)))
            error($op.line, "types are wrong");
         $t=Type.boolType();

         if (a.imm != null && b.imm != null) {
            $imm = a.imm.equals(b.imm) == (op.getType() == EQ) ? 1L : 0L;
         } else if (a.imm == null && b.imm == null) {
            $reg = func.getNextRegister();
            Register tmp = func.getNextRegister();
            current.peek().addInstruction(InstructionFactory.loadi(0, $reg));
            current.peek().addInstruction(InstructionFactory.loadi(1, tmp));
            current.peek().addInstruction(InstructionFactory.comp(a.reg, b.reg));
            current.peek().addInstruction(InstructionFactory.ccmove(op.getType(), false, tmp, $reg));
         } else {
            Register tmpReg;
            Long tmpImm;
            if (a.imm != null) {
               tmpReg = b.reg;
               tmpImm = a.imm;
            } else {
               tmpReg = a.reg;
               tmpImm = b.imm;
            }
            $reg = func.getNextRegister();
            Register tmp = func.getNextRegister();
            current.peek().addInstruction(InstructionFactory.loadi(0, $reg));
            current.peek().addInstruction(InstructionFactory.loadi(1, tmp));
            current.peek().addInstruction(InstructionFactory.compi(tmpReg, tmpImm));
            current.peek().addInstruction(InstructionFactory.ccmove(op.getType(), false, tmp, $reg));
         }
      }
   | l=lvalue { $t=l.t; $reg=l.reg; $imm=l.imm;}
   | ^(NEG a=expression)
      {
         if (! a.t.isInt())
            error0("not an int");
         $t=Type.intType();

         if (a.imm != null)
            $imm = -a.imm;
         else {
            Register tmp = func.getNextRegister();
            current.peek().addInstruction(InstructionFactory.loadi(0, tmp));
            current.peek().addInstruction(InstructionFactory.arithmetic(MINUS, null, tmp, a.reg, $reg = func.getNextRegister()));
         }
      }
   | ^(NOT a=expression)
      {
         if (! a.t.isBool())
            error0("not a bool");
         $t=Type.boolType();

         if (a.imm != null)
            $imm = a.imm == 0L ? 1L : 0L;
         else
            current.peek().addInstruction(InstructionFactory.xori(a.reg, 1, $reg = func.getNextRegister()));
      }
   | f=factor { $t=f.t; $reg=f.reg; $imm=f.imm; }
   ;

lvalue returns [Type t = null, Register reg = null, Long imm = null, String name = null, boolean global = false, dot_load_return dl = null, boolean wasStruct = false]
   : ^(DOT d=dot_load id=ID)
      {
         if (! d.t.isStruct())
            error0("can only dot from a struct");

         SymbolTable structTable = stypes.getStructMembers(d.t.getName());
         $t=structTable.get($id.text);

         current.peek().addInstruction(InstructionFactory.loadai(d.reg, structTable.getOffset($id.text), $reg = func.getNextRegister()));
         $dl = d;
         $name = $id.text;
         $wasStruct = true;
      }
   | id=ID
      {
         System.out.println("looking up " + $id.text);
         SymbolTable stable = stables.peek();
         if (! stable.isDefined($id.text))
            error0("not defined: " + $id.text);

         $t=stable.get($id.text);
         $reg = func.getVarRegister($id.text);
         $imm = func.getVarImmediate($id.text);

         $name = $id.text;

         if ($reg == null && $imm == null) {
            if (stable.isFormal($id.text)) {
               func.putVarRegister($id.text, $reg = func.getNextRegister());
               current.peek().addInstruction(InstructionFactory.loadArg($id.text, func.getParamIndex($id.text), $reg, inAssign));
            } else {
               current.peek().addInstruction(InstructionFactory.loadGlobal($id.text, $reg = func.getNextRegister()));
               $global = true;
            }
         }
      }
   ;

dot_load returns [Type t = null, Register reg = null, Long imm = null]
   : ^(DOT d=dot_load id=ID)
      {
         if (! d.t.isStruct())
            error0("can only dot from a struct");

         SymbolTable structTable = stypes.getStructMembers(d.t.getName());
         $t=structTable.get($id.text);

         current.peek().addInstruction(InstructionFactory.loadai(d.reg, structTable.getOffset($id.text), $reg = func.getNextRegister()));
      }
   | f=factor { $t=f.t; $reg=f.reg; }
   | id=ID
      {
         System.out.println("looking up " + $id.text);
         SymbolTable stable = stables.peek();
         if (! stable.isDefined($id.text))
            error0("not defined: " + $id.text);

         $t=stable.get($id.text);
         $reg = func.getVarRegister($id.text);
         $imm = func.getVarImmediate($id.text);

         if ($reg == null && $imm == null) {
            if (stable.isFormal($id.text)) {
               func.putVarRegister($id.text, $reg = func.getNextRegister());
               current.peek().addInstruction(InstructionFactory.loadArg($id.text, func.getParamIndex($id.text), $reg, inAssign));
            }
            else if ($t.isStruct())
               current.peek().addInstruction(InstructionFactory.loadGlobal($id.text, $reg = func.getNextRegister()));
            else
               current.peek().addInstruction(InstructionFactory.globalAddr($id.text, $reg = func.getNextRegister()));
         }
      }
   ;

factor returns [Type t = null, Register reg = null, Long imm = null]
   : LPAREN! tmp=expression RPAREN! { $t = tmp.t; $reg = tmp.reg; $imm = tmp.imm; }
   | i=invocation
      {
         System.out.println("invoking factor");
         $t = i.t;
         current.peek().addInstruction(InstructionFactory.loadRet($reg = func.getNextRegister()));
      }
   | INTEGER
      {
         $t=Type.intType();

         if (preserveConstants) {
            current.peek().addInstruction(InstructionFactory.loadi(Long.parseLong($INTEGER.text), $reg = func.getNextRegister()));
         } else {
            $imm = Long.parseLong($INTEGER.text);
         }
      }
   | TRUE
      {
         $t = Type.boolType();

         if (preserveConstants) {
            current.peek().addInstruction(InstructionFactory.loadi(1, $reg = func.getNextRegister()));
         } else {
            $imm = 1L;
         }
      }
   | FALSE
      {
         $t = Type.boolType();

         if (preserveConstants) {
            current.peek().addInstruction(InstructionFactory.loadi(0, $reg = func.getNextRegister()));
         } else {
            $imm = 0L;
         }
      }
   | ^(NEW id=ID)
      {
         $t=Type.structType($id.text);
         current.peek().addInstruction(InstructionFactory.newAlloc(stypes.getStructSize($id.text), $reg = func.getNextRegister()));
      }
   | NULL
      {
         $t=Type.structType("null");
         
         if (preserveConstants) {
            current.peek().addInstruction(InstructionFactory.loadi(0, $reg = func.getNextRegister()));
         } else {
            $imm = 0L;
         }
      }
   ;

arguments returns [ArrayList<Type> argTypes, ArrayList<Register> registers]
   @init
   {
      $argTypes = new ArrayList<Type>();
      $registers = new ArrayList<Register>();
   }
   : ^(ARGS (t=expression
      {
         $argTypes.add(t.t);
         
         if (t.imm != null)
            current.peek().addInstruction(InstructionFactory.loadi(t.imm, t.reg = func.getNextRegister()));
         $registers.add(t.reg);
      })+)
   | ARGS
   ;