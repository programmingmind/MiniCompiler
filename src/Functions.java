import java.util.List;
import java.util.HashMap;

public class Functions {
   private class Function {
      public Type returnType;
      public SymbolTable vars;

      public Function(Type returnType, SymbolTable vars) {
         this.returnType = returnType;
         this.vars = vars;
      }
   }

   private HashMap<String, Function> funcs;

   public Functions() {
      funcs = new HashMap<String, Function>();
   }

   public boolean isDefined(String name) {
      return funcs.containsKey(name);
   }

   public void newFunction(String name, Type returnType, SymbolTable vars) {
      System.out.println("adding function " + name);
      funcs.put(name, new Function(returnType, vars));
   }

   public Type getReturnType(String name) {
      System.out.println("getting return type of " + name + ", isDefined: " + isDefined(name));
      return isDefined(name) ? funcs.get(name).returnType : null;
   }

   public void checkParams(String name, List<Type> args) {
      if (! isDefined(name))
         throw new RuntimeException(name + " not defined");
      funcs.get(name).vars.checkParams(args);
   }
}