public class Type {
   public static final int NONE = -1;
   public static final int VOID = 0;
   public static final int INT = 1;
   public static final int BOOL = 2;

   private boolean isStruct;
   private int val;
   private String name;

   public Type(boolean isStruct, int val, String name) {
      this.isStruct = isStruct;
      this.val = val;
      this.name = name;
   }

   public static Type voidType() {
      return new Type(false, VOID, null);
   }

   public static Type intType() {
      return new Type(false, INT, null);
   }

   public static Type boolType() {
      return new Type(false, BOOL, null);
   }

   public static Type structType(String name) {
      return new Type(true, NONE, name);
   }

   public String toString() {
      if (isStruct)
         return "struct " + name;
      else {
         if (val == INT)
            return "int";
         else if (val == BOOL)
            return "bool";
         else if (val == NONE)
            return "none";
      }
      return "void";
   }

   public boolean equals(Type other) {
      if (other == null)
         return false;
      if (isStruct != other.isStruct)
         return false;
      if (isStruct)
         return name.equals(other.name) || other.name.equals("null") || name.equals("null");
      else
         return val == other.val;
   }

   public boolean isInt() {
      return !isStruct && val == INT;
   }

   public boolean isBool() {
      return !isStruct && val == BOOL;
   }

   public boolean isStruct() {
      return isStruct;
   }

   public String getName() {
      return name;
   }
}