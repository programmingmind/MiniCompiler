public class Instruction {
   private String text;
   private int[] source;
   private int target;

   public Instruction(String text) {
      this.text = text;
   }

   public String toString() {
      return text;
   }
}