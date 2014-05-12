public class Register {
   private int iloc;
   private Integer asm;

   public Register(int iloc) {
      this.iloc = iloc;
      this.asm = null;
   }

   public void setASM(int asm) {
      this.asm = asm;
   }

   public int getILOC() {
      return iloc;
   }

   public int getASM() {
      if (asm == null)
         throw new RuntimeException("assembly register not yet set");
      return asm;
   }
}