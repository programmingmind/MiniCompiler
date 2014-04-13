tree grammar TypeCheck;

options
{
   tokenVocab=Mini;
   ASTLabelType=CommonTree;
}

@header
{
   import java.util.ArrayList;
   import java.util.Map;
   import java.util.HashMap;
   import java.util.Vector;
   import java.util.Iterator;
}

@members
{
   private StructTypes stypes = new StructTypes();
   private Functions funcs = new Functions();
   private Stack<SymbolTable> stables;
   private Stack<Type> returns;
}

error0 [String text]
   : error[0,text]
   ;

error [int line, String text]
   : { if (true) throw new RuntimeException($line + " : " + $text); }
   ;

verify
   : program
      {
         System.out.println("Type checking passed");
         if (!funcs.isDefined("main"))
            System.err.println("WARNING: missing main()");
      }
   | { System.out.println("not a valid AST"); }
   ;

program
   @init
   {
      stables = new Stack<SymbolTable>();
      returns = new Stack<Type>();
      SymbolTable stable = new SymbolTable();
      stables.push(stable);
      returns.push(Type.voidType());
   }
   :  ^(PROGRAM types[stable] declarations[stable] functions)
   ;

types [SymbolTable stable]
   :  ^(TYPES (types_sub[stable]))
   |  TYPES { System.out.println("no struct types"); }
   ;

types_sub [SymbolTable stable]
   :  type_declaration[stable] types_sub[stable]?
   ;

type_declaration [SymbolTable stable]
   :  ^(STRUCT id=ID { stypes.newStruct($id.text); } nested_decl[stypes.getStructMembers($id.text)])
   ;

nested_decl[SymbolTable stable]
   :  (decl[stable,false])+
   ;

decl [SymbolTable stable,boolean isParam]
   :  ^(DECL ^(TYPE t=type) id=ID) { if (isParam) $stable.putParam($id.text, t.t); else $stable.put($id.text, t.t); }
   ;

type returns [Type t = null]
   : INT { $t = Type.intType(); }
   | BOOL { $t = Type.boolType(); }
   | VOID { $t = Type.voidType(); }
   | ^(STRUCT id=ID)
      {
         if (!stypes.isDefined($id.text))
         {
            error($id.line, "undefined struct type '" + $id + "'");
         }
         $t = Type.structType($id.text);
      }
   ;

declarations [SymbolTable stable]
   : ^(DECLS declaration[stable])
   | { System.out.println("There are no declarations"); }
   ;

declaration [SymbolTable stable]
   : (decl_list[stable])*
   ;

decl_list [SymbolTable stable]
   : ^(DECLLIST ^(TYPE t=type) id_list[t.t,stable])
   ;

id_list [Type t, SymbolTable stable]
   :  (
         id=ID
         {
            if ($stable.redef($id.text))
            {
               error($id.line, "redefinition of variable '" + $id + "'");
            }
               else if ($stable.isFormal($id.text))
            {
               error($id.line, "redefinition of parameter '" + $id + "'");
            }
            $stable.put($id.text, $t);
         }
      )+
   ;

functions
   : ^(FUNCS function*)
   ;

function
   @init
   {
      SymbolTable vars = new SymbolTable();
   }
   : ^(FUN id=ID parameters[vars] ^(RETTYPE rt=return_type) declarations[vars] { funcs.newFunction($id.text,rt.t,vars); stables.push(vars); returns.push(rt.t); } sl=statement_list) { if (sl.t == null && !returns.peek().equals(Type.voidType())) error0($id.text + " missing return value"); stables.pop(); returns.pop(); }
   ;

parameters [SymbolTable params]
   :  ^(PARAMS decl[params,true]*)
   ;

return_type returns [Type t = null]
   : tmp=type { $t = tmp.t; System.out.println("has return: " + $t); }
   |  { System.out.println("void return"); $t = Type.voidType(); }
   ;

statement returns [Type t = null]
   :  ^(BLOCK sl=statement_list) { System.out.println("Heres a block"); $t = sl.t; }
   |  ^(ASSIGN a=expression l=lvalue) { System.out.println("Here I be assigning"); if (! a.t.equals(l.t)) error0("assignments need the same types"); }
   |  ^(PRINT a=expression ENDL?) { System.out.println("Let us print"); if (! a.t.isInt()) error0("can only print ints"); }
   |  ^(READ l=lvalue) { if (! l.t.isInt()) error0("can only read in ints"); }
   |  ^(IF a=expression s=statement s2=statement?) { if (! a.t.isBool()) error0("if statements need bool expression"); if (s2 != null && s.t != null && s.t.equals(s2.t)) $t = s.t; }
   |  ^(WHILE a=expression statement b=expression) { if (! (a.t.isBool() && b.t.isBool())) error0("while statements need bool expressions"); }
   |  ^(DELETE a=expression) { if (! a.t.isStruct()) error0("can only delete a struct");}
   |  r=ret { if (! r.t.equals(returns.peek())) error0("return type doesn't match"); $t = r.t; }
   |  i=invocation { System.out.println("invoking"); /*$t = i.t; don't need this because an invocation statement does nothing for return*/ }
   ;

statement_list returns [Type t = null]
   : ^(STMTS (tmp=statement { if ($t != null) System.err.println("WARNING: statements after return"); else $t = tmp.t; })*)
   ;
 
ret returns [Type t = null]
   : ^(RETURN tmp=expression) { System.out.println("returned expression"); $t = tmp.t; }
   | RETURN            { System.out.println("no return expression"); $t = Type.voidType(); }
   ;

invocation returns [Type t = null]
   : ^(INVOKE id=ID args=arguments) { System.out.println("invoked " + $id.text); funcs.checkParams($id.text,args.argTypes); $t = funcs.getReturnType($id.text); }
   ;

expression returns [Type t = null]
   : ^((PLUS | TIMES | DIVIDE) a=expression b=expression) { if (! (a.t.isInt() && b.t.isInt())) error0("not an int"); $t=Type.intType(); }
   | ^(MINUS a=expression b=expression?) { if (! ($a.t.isInt() &&  (b == null || $b.t.isInt()))) error0("not an int"); $t=Type.intType(); }
   | ^((AND | OR) a=expression b=expression) { if (! ($a.t.isBool() && $b.t.isBool())) error0("not a bool"); $t=Type.boolType(); }
   | ^((LT | GT | NE | LE | GE) a=expression b=expression) { if (! ($a.t.isInt() && $b.t.isInt())) error0("numeric comparisons need ints"); $t=Type.boolType(); }
   | ^(EQ a=expression b=expression) { if (! ($a.t.equals($b.t) && ($a.t.isInt() || $a.t.isBool()))) error0("types are wrong"); $t=Type.boolType(); }
   | ^(DOT a=expression id=ID) { if (! a.t.isStruct()) error0("not a struct : " + a.t); $t=stypes.getStructMembers(a.t.getName()).get($id.text); }
   | ^(NEG a=expression) { if (! a.t.isInt()) error0("not an int"); $t=Type.intType(); }
   | ^(NOT a=expression) { if (! a.t.isBool()) error0("not a bool"); $t=Type.boolType(); }
   | s=selector { $t=s.t; }
   ;

lvalue returns [Type t = null]
   : ^(DOT l=lvalue id=ID) {if (! l.t.isStruct()) error0("can only dot from a struct"); $t=stypes.getStructMembers(l.t.getName()).get($id.text); }
   | id=ID { SymbolTable stable = stables.peek(); if (! stable.isDefined($id.text)) error0("not defined: " + $id.text); $t=stable.get($id.text); }
   ;

selector returns [Type t = null]
   :  tmp=factor { $t=tmp.t; System.out.println("factor: " + $t); } (DOT^ id=ID { if (! $t.isStruct()) error0("not a struct: " + $t); $t=stypes.getStructMembers($t.getName()).get($id.text);})*
   ;

factor returns [Type t = null]
   :  LPAREN! tmp=expression RPAREN! { $t = tmp.t; }
   |  i=invocation { System.out.println("invoking"); $t = i.t; }
   |  id=ID { System.out.println("looking up " + $id.text); SymbolTable stable = stables.peek(); if (! stable.isDefined($id.text)) error0("not defined: " + $id.text); $t=stable.get($id.text);}
   |  INTEGER { $t=Type.intType(); }
   |  TRUE { $t=Type.boolType(); }
   |  FALSE { $t=Type.boolType(); }
   |  ^(NEW id=ID) { $t=Type.structType($id.text); }
   |  NULL { $t=Type.structType("null"); }
   ;

arguments returns [ArrayList<Type> argTypes]
   : tmp=arg_list {$argTypes=tmp.argTypes;}
   ;

arg_list returns [ArrayList<Type> argTypes]
   @init
   {
      $argTypes = new ArrayList<Type>();
   }
   : ^(ARGS (t=expression {$argTypes.add(t.t);})+)
   | ARGS
   ;