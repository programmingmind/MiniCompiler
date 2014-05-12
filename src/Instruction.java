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

   public abstract String[] toAssembly();
}