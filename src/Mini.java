import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;
import org.antlr.stringtemplate.*;

import java.io.*;
import java.util.Vector;
import java.util.HashMap;

public class Mini
{
   public static void main(String[] args)
   {
      parseParameters(args);

      CommonTokenStream tokens = new CommonTokenStream(createLexer());
      MiniParser parser = new MiniParser(tokens);
      CommonTree tree = parse(parser);

      if (_displayAST && tree != null)
      {
         DOTTreeGenerator gen = new DOTTreeGenerator();
         StringTemplate st = gen.toDOT(tree);
         System.out.println(st);
      }
      else if (!parser.hasErrors())
      {
         // To use a different tree parser, modify the code within validate.
         validate(tree, tokens);
      }
   }

   public static boolean shouldSpillAll() {
      return _spillAll;
   }

   private static final String DISPLAYAST = "-displayAST";
   private static final String OUTPUTILOC = "-dumpIL";
   private static final String KEEPDEADCODE = "-keepDead";
   private static final String NOCOPY = "-noCopyProp";
   private static final String PRESERVECONST = "-preserveConst";
   private static final String SPILLALL = "-spillAll";

   private static String _inputFile = null;
   private static boolean _displayAST = false;
   private static boolean _outputIloc = false;
   private static boolean _removeDead = true;
   private static boolean _copyProp = true;
   private static boolean _preserveConst = false;
   private static boolean _spillAll = false;

   private static void parseParameters(String [] args)
   {
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals(DISPLAYAST))
         {
            _displayAST = true;
         }
         else if (args[i].equals(OUTPUTILOC)) {
            _outputIloc = true;
         }
         else if (args[i].equals(KEEPDEADCODE)) {
            _removeDead = false;
         }
         else if (args[i].equals(NOCOPY)) {
            _copyProp = false;
         }
         else if (args[i].equals(PRESERVECONST)) {
            _preserveConst = true;
         }
         else if (args[i].equals(SPILLALL)) {
            _spillAll = true;
         }
         else if (args[i].charAt(0) == '-')
         {
            System.err.println("unexpected option: " + args[i]);
            System.exit(1);
         }
         else if (_inputFile != null)
        {
            System.err.println("too many files specified");
            System.exit(1);
         }
         else
         {
            _inputFile = args[i];
         }
      }
   }

   private static CommonTree parse(MiniParser parser)
   {
      try
      {
         MiniParser.program_return ret = parser.program();

         return (CommonTree)ret.getTree();
      }
      catch (org.antlr.runtime.RecognitionException e)
      {
         error(e.toString());
      }
      catch (Exception e)
      {
         System.exit(-1);
      }

      return null;
   }

   private static void validate(CommonTree tree, CommonTokenStream tokens)
   {
      try
      {
         CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
         nodes.setTokenStream(tokens);
         TypeCheck tparser = new TypeCheck(nodes);

         tparser.verify(_outputIloc, _inputFile, _removeDead, _copyProp, _preserveConst);
      }
      catch (org.antlr.runtime.RecognitionException e)
      {
         error(e.toString());
      }
   }

   private static void error(String msg)
   {
      System.err.println(msg);
      System.exit(1);
   }

   private static MiniLexer createLexer()
   {
      try
      {
         ANTLRInputStream input;
         if (_inputFile == null)
         {
            input = new ANTLRInputStream(System.in);
         }
         else
         {
            input = new ANTLRInputStream(
               new BufferedInputStream(new FileInputStream(_inputFile)));
         }
         return new MiniLexer(input);
      }
      catch (java.io.IOException e)
      {
         System.err.println("file not found: " + _inputFile);
         System.exit(1);
         return null;
      }
   }
}
