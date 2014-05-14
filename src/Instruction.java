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

   public abstract String[] toAssembly();
}