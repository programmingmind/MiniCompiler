import java.util.HashMap;

public class Functions {
   private static HashMap<String, Function> funcs;

   public Functions() {
      if (funcs == null)
         funcs = new HashMap<String, Function>();
   }

   public static boolean isDefined(String name) {
      return funcs.containsKey(name);
   }

   public static Function get(String name) {
      if (! isDefined(name))
         throw new RuntimeException(name + " not defined");
      return funcs.get(name);
   }

   public static void newFunction(Function function) {
      System.out.println("adding function " + function.getName());
      funcs.put(function.getName(), function);
   }
}