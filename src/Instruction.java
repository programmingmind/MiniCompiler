public abstract class Instruction {
   private String text;
   protected Register[] sources;
   protected Register target;

   public Instruction(String text, Register[] sources, Register target) {
      this.text = text;
      this.sources = sources;
      this.target = target;
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

      text = text.replaceAll("r" + from.getILOC(), "r" + to.getILOC());
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

   public boolean usesReg(Register reg) {
      for (Register r : sources)
         if (r == reg)
            return true;
      return false;
   }

   public abstract String[] toAssembly();
}