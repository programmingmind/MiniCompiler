import java.util.ArrayList;

public class StructTypes {
   private ArrayList<String> names;
   private ArrayList<SymbolTable> vars;

   public StructTypes() {
      names = new ArrayList<String>();
      vars = new ArrayList<SymbolTable>();
   }

   public boolean isDefined(String name) {
      return names.contains(name);
   }

   public void newStruct(String name) {
      System.out.println("new struct: " + name);
      names.add(name);
      vars.add(new SymbolTable());
   }

   public SymbolTable getStructMembers(String name) {
      System.out.println("Getting members for: " + name + ", isDefined: " + isDefined(name));
      return isDefined(name) ? vars.get(names.indexOf(name)) : null;
   }

   public int getStructSize(String name) {
      return isDefined(name) ? vars.get(names.indexOf(name)).getLocals().size() * 8 : 0;
   }
}