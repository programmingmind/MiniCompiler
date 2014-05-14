import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GraphColorer {
   private class Node {
      private Register reg;
      private Set<Node> links;

      public Node(Register reg) {
         this.reg = reg;
         links = new HashSet<Node>();
      }

      public Register getReg() {
         return reg;
      }

      public void addLink(Node other) {
         links.add(other);
      }

      public Set<Node> getLinks() {
         return links;
      }

      public boolean equals(Node other) {
         return reg == other.reg && links.equals(other.links);
      }
   }

   private Set<Node> graph;

   public GraphColorer(Collection<Register> registers) {
      graph = new HashSet<Node>();
      
      for (Register reg : registers)
         graph.add(new Node(reg));

      for (Node node : graph) {
         for (Node link : graph) {
            if (node.getReg().overlaps(link.getReg())) {
               node.addLink(link);
               link.addLink(node);
            }
         }
      }
   }

   public void color() {
      for (Node node : graph) {
         if (! node.getReg().isASMSet()) {
            ArrayList<String> regs = new ArrayList<String>(Arrays.asList(InstructionFactory.registers));
            for (Node link : node.getLinks())
               if (link.getReg().isASMSet())
                  regs.remove(link.getReg().getASM());

            if (regs.size() > 0)
               node.getReg().setASM(regs.get(0));
            else {
               System.err.println("WARNING: need to spill, unimplemented");
               node.getReg().setASM("SPILL");
            }
         }
      }
   }
}