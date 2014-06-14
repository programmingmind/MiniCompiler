import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

   private List<Node> graph;
   private Function function;

   public GraphColorer(Collection<Register> registers, Function function) {
      this.function = function;

      Set<Node> tmpGraph = new HashSet<Node>();
      
      for (Register reg : registers)
         tmpGraph.add(new Node(reg));

      graph = new ArrayList<Node>(tmpGraph);
      Collections.sort(graph, new Comparator<Node>() {
         public int compare(Node n1, Node n2) {
            return n1.getReg().totalRange() - n2.getReg().totalRange();
         }
      });

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
            ArrayList<String> regs = Mini.shouldSpillAll() ? new ArrayList<String>() : new ArrayList<String>(Arrays.asList(InstructionFactory.registers));
            for (Node link : node.getLinks())
               if (link.getReg().isASMSet() && !link.getReg().doesSpill())
                  regs.remove(link.getReg().getASM());

            if (regs.size() > 0)
               node.getReg().setASM(regs.get(0));
            else {
               node.getReg().spill(function.nextSpill());
            }
         }
      }
   }
}