public class Instruction {
   private String text;
   private String[] asm;
   private int[] sources;
   private Integer target;

   public Instruction(String text, int[] sources, Integer target, String... asm) {
      this.text = text;
      this.asm = asm;
      this.sources = sources;
      this.target = target;
   }

   public Instruction(String text, int[] sources, Integer target, String asm) {
      this(text, sources, target, new String[] {asm});
   }

   public String toIloc() {
      return text;
   }

   public String[] toAssembly() {
      return asm;
   }
}