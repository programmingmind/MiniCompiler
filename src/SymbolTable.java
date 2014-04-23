import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SymbolTable {
   private static SymbolTable global;

   private SymbolTable last;
   private LinkedHashMap<String, Type> params;
   private LinkedHashMap<String, Type> locals;

   public SymbolTable() {
      this(null);
   }

   public SymbolTable(SymbolTable last) {
      System.out.println("new symbol table");

      if (global == null) {
         System.out.println("these are the global vars");
         global = this;
         this.last = null;
      } else {
         this.last = last == null ? global : last;
      }

      params = new LinkedHashMap<String, Type>();
      locals = new LinkedHashMap<String, Type>();
   }

   private boolean isGlobal() {
      return this == global;
   }

   public boolean isFormal(String name) {
      return params.containsKey(name);
   }

   public boolean redef(String name) {
      return locals.containsKey(name);
   }

   public boolean isLocallyDefined(String name) {
      return isFormal(name) || redef(name);
   }

   public void putParam(String name, Type type) {
      if (isLocallyDefined(name))
         throw new RuntimeException(name + " already defined");

      System.out.println("putting in parameter " + name + " : " + type);
      params.put(name, type);
   }

   public void put(String name, Type type) {
      if (isLocallyDefined(name))
         throw new RuntimeException(name + " already defined");

      System.out.println("putting in " + name + " : " + type);
      locals.put(name, type);
   }

   public boolean isDefined(String name) {
      return get(name) != null;
   }

   public Type get(String name) {
      if (params.containsKey(name))
         return params.get(name);

      if (locals.containsKey(name))
         return locals.get(name);

      if (last != null)
         return last.get(name);

      return null;
   }

   public void checkParams(List<Type> args) {
      if (args.size() != params.size())
         throw new RuntimeException("number of args/params don't match");

      List<String> paramNames = getParamNames();
      for (int i = 0; i < args.size(); i++)
         if (! params.get(paramNames.get(i)).equals(args.get(i)))
            throw new RuntimeException("argument " + i + "(" + paramNames.get(i) + ") doesn't match. Required: " + params.get(paramNames.get(i)) + ", given: " + args.get(i));
   }

   public String getParamName(int num) {
      return getParamNames().get(num);
   }

   public List<String> getParamNames() {
      ArrayList<String> names = new ArrayList<String>();
      names.addAll(params.keySet());
      return names;
   }

   public List<String> getLocals() {
      ArrayList<String> locs = new ArrayList<String>();
      locs.addAll(locals.keySet());
      return locs;
   }

   public int getOffset(String varName) {
      if (! locals.containsKey(varName))
         throw new RuntimeException(varName + " not defined in symbol table");
      
      int offset = 0;
      for (String var : locals.keySet()) {
         if (var.equals(varName))
            return offset;
         offset += 8;
      }

      return offset;
   }
}