import java.util.HashMap;

public class Register {
   private int iloc;
   private String asm;

   private boolean canPropogate = true;

   private HashMap<Block, int[]> range;

   public Register(int iloc) {
      this.iloc = iloc;
      this.asm = null;

      range = new HashMap<Block, int[]>();
   }

   public boolean canBePropogated() {
      return canPropogate;
   }

   public void disablePropogation() {
      canPropogate = false;
   }

   public void setASM(String asm) {
      this.asm = asm;
   }

   public int getILOC() {
      return iloc;
   }

   public boolean isASMSet() {
      return asm != null;
   }

   public String getASM() {
      if (asm == null)
         throw new RuntimeException("assembly register not yet set");
      return asm;
   }

   public void setRange(Block b, int start, int end) {
      range.put(b, new int[] {start, end});
   }

   public boolean overlaps(Register r) {
      for (Block b : range.keySet()) {
         int[] blockRange = range.get(b);
         int[] otherRange = r.range.get(b);
         if (otherRange != null) {
            if ((blockRange[0] <= otherRange[1] && blockRange[1] >= otherRange[1]) ||
                (otherRange[0] <= blockRange[1] && otherRange[1] >= blockRange[1]))
               return true;
         }
      }
      return false;
   }
}