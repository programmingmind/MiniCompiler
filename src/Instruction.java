import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public abstract class Instruction {
   public static enum Types {
      NORMAL, IMMOVEABLE, CALL
   };

   private String text;
   private Types type;
   protected Register[] sources;
   protected Register target;

   public Instruction(String text, Register[] sources, Register target, Types type) {
      this.text = text;
      this.sources = sources;
      this.target = target;
      this.type = type;
   }

   public Instruction(String text, Register[] sources, Register target) {
      this(text, sources, target, Types.NORMAL);
   }

   public String toIloc() {
      return text;
   }

   public Register[] getSources() {
      return sources;
   }

   public Register getTarget() {
      return target;
   }

   public void replace(Register from, Register to) {
      for (int i = 0; i < sources.length; i++)
         if (sources[i] == from)
            sources[i] = to;

      if (target == from)
         target = to;

      text = text.replaceAll("r" + from.getILOC() + "(\\D|$)", "r" + to.getILOC() + "$1");
   }

   public boolean isConditionalTarget() {
      return text.matches("mov\\w+ ccr, r\\d+, r\\d+");
   }

   public boolean isJump() {
      return isJump(false);
   }

   public boolean isJump(boolean jump) {
      return text.startsWith("jumpi ") || (!jump && text.startsWith("cbr"));
   }

   public boolean jumpsToLabel(String label) {
      return jumpsToLabel(label, false);
   }

   public boolean jumpsToLabel(String label, boolean jump) {
      return isJump(jump) && text.contains(label);
   }

   public boolean isMoveable() {
      return type != Types.IMMOVEABLE;
   }

   public boolean isCall() {
      return type == Types.CALL;
   }

   public boolean usesReg(Register reg) {
      for (Register r : sources)
         if (r == reg)
            return true;
      return false;
   }

   private List<String> loadSpilled() {
      List<String> commands = new ArrayList<String>();

      if (sources.length > 0) {
         if (sources[0].doesSpill() && sources[0] != null)
            commands.addAll(sources[0].getRegister(InstructionFactory.TEMP_REG));

         if (sources.length == 2 && sources[1] != sources[0] && sources[1] != target && sources[1].doesSpill()) {
            if (sources[0].doesSpill()) {
               throw new RuntimeException("Currently only 1 temp reg");
            }
            commands.addAll(sources[1].getRegister(InstructionFactory.TEMP_REG));
         }
      }

      if (target != null) {
         commands.addAll(target.getRegister(InstructionFactory.TEMP_REG));
      }

      return commands;
   }

   private List<String> restoreSpilled() {
      List<String> commands = new ArrayList<String>();

      if (target != null && target.doesSpill())
         commands.addAll(target.restoreSpill(true));

      for (Register src : sources)
         if (src.doesSpill())
            commands.addAll(src.restoreSpill(false));

      return commands;
   }

   protected abstract String[] getBareAsm();

   public List<String> toAssembly()  {
      List<String> commands = loadSpilled();
      commands.addAll(Arrays.asList(getBareAsm()));
      commands.addAll(restoreSpilled());
      return commands;
   }

   public String toString() {
      return "iloc: " + text;
   }
}