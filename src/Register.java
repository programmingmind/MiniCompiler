import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Register {
   private int iloc;
   private String asm;

   private boolean canPropogate = true;

   private boolean isSpilled = false;
   private int spillCount;

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
      return asm != null || isSpilled;
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
      if (r == this)
         return false;
      
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

   public void spill(int spillCount) {
      isSpilled = true;
      this.spillCount = spillCount;
   }

   public boolean doesSpill() {
      return isSpilled;
   }

   public int getSpillCount() {
      if (!isSpilled)
         throw new RuntimeException("Register isn't spilled");
      return spillCount;
   }

   public List<String> getRegister(String tmp) {
      List<String> insts = new ArrayList<String>();

      if (isSpilled) {
         insts.add("movq " + (-8 * spillCount) + "(%rbp), %r" + tmp);
         this.asm = tmp;
      }

      return insts;
   }

   public List<String> restoreSpill(boolean save) {
      List<String> insts = new ArrayList<String>();

      if (isSpilled && this.asm != null) {
         if (save) {
            insts.add("movq %r" + this.asm + ", " + (-8 * spillCount) + "(%rbp)");
         }

         this.asm = null;
      }

      return insts;
   }
}