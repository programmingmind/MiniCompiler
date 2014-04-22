public class InstructionFactory {
   private static final String[] ops = {"eq", "ge", "gt", "le", "lt", "ne"};

   private static String getCompOp(int type, boolean reverse) {
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

      return ops[ndx];
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

      return new Instruction(op + " r" + left + ", " + (imm == null ? ("r" + right) : imm) + ", r" + result);
   }

   public static Instruction bool(int type, int left, int right, int result) {
      return new Instruction((type == TypeCheck.AND ? "and" : "or") + " r" + left + ", r" + right + ", r" + result);
   }

   public static Instruction xori(int reg, int imm, int result) {
      return new Instruction("xori r" + reg + ", " + imm + ", r" + result);
   }

   public static Instruction comp(int left, int right) {
      return new Instruction("comp r" + left + ", r" + right);
   }

   public static Instruction compi(int reg, int imm) {
      return new Instruction("comp r" + reg + ", " + imm);
   }

   public static Instruction ccbranch(int type, boolean reverse, String label1, String label2) {    
      return new Instruction("cbr" + getCompOp(type, reverse) + " ccr, " + label1 + ", " + label2);
   }

   public static Instruction jump(String label) {
      return new Instruction("jumpi " + label);
   }

   public static Instruction loadi(int immediate, int result) {
      return new Instruction("loadi " + immediate + ", r" + result);
   }

   public static Instruction loadai(int reg, int imm, int result) {
      return new Instruction("loadai r" + reg + ", " + imm + ", r" + result);
   }

   public static Instruction loadGlobal(String var, int result) {
      return new Instruction("loadglobal " + var + ", r" + result);
   }

   public static Instruction loadArg(String var, int num, int reg) {
      return new Instruction("loadinargument " + var + ", " + num + ", r" + reg);
   }

   public static Instruction loadRet(int reg) {
      return new Instruction("loadret " + reg);
   }

   public static Instruction formalAddr(String var, int num, int reg) {
      return new Instruction("computeformaladdress " + var + ", " + num + ", r" + reg);
   }

   public static Instruction restoreFormal(String var, int num) {
      return new Instruction("restoreformal " + var + ", " + num);
   }

   public static Instruction globalAddr(String var, int result) {
      return new Instruction("computeglobaladdress " + var + ", r" + result);
   }

   public static Instruction storeai(int reg, int result, int imm) {
      return new Instruction("storeai r" + reg + ", r" + result + ", " + imm);
   }

   public static Instruction storeGlobal(int reg, String var) {
      return new Instruction("storeglobal r" + reg + ", " + var);
   }

   public static Instruction storeInArg(int reg, String var, int num) {
      return new Instruction("storeinargument r" + reg + ", " + var + ", " + num);
   }

   public static Instruction storeOutArg(int reg, int num) {
      return new Instruction("storeoutargument r" + reg + ", " + num);
   }

   public static Instruction storeRet(int reg) {
      return new Instruction("storeret " + reg);
   }

   public static Instruction call(String label) {
      return new Instruction("call " + label);
   }

   public static Instruction ret() {
      return new Instruction("ret");
   }

   public static Instruction newAlloc(int num, int reg) {
      return new Instruction("new " + num + ", r" + reg);
   }

   public static Instruction del(int reg) {
      return new Instruction("del r" + reg);
   }

   public static Instruction print(int reg) {
      return new Instruction("print r" + reg);
   }

   public static Instruction println(int reg) {
      return new Instruction("println r" + reg);
   }

   public static Instruction read(int reg) {
      return new Instruction("read r" + reg);
   }

   public static Instruction mov(int r1, int r2) {
      return new Instruction("mov r" + r1 + ", r" + r2);
   }

   public static Instruction ccmove(int type, boolean reverse, int imm, int reg) {
      return new Instruction("mov"+ getCompOp(type, reverse) + "i ccr, " + imm + ", r" + reg);
   }
}