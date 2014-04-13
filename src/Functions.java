import java.util.HashMap;

public class Functions {
   private HashMap<String, Function> funcs;

   public Functions() {
      funcs = new HashMap<String, Function>();
   }

   public boolean isDefined(String name) {
      return funcs.containsKey(name);
   }

   public Function get(String name) {
      if (! isDefined(name))
         throw new RuntimeException(name + " not defined");
      return funcs.get(name);
   }

   public void newFunction(Function function) {
      System.out.println("adding function " + function.getName());
      funcs.put(function.getName(), function);
   }
}