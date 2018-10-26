package APL;

import APL.types.*;
import APL.errors.*;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collectors;

public class Main {
  public static final String CODEPAGE = "\0\0\0\0\0\0\0\0\0\t\n\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~÷×↑↓⌈⌊≠∊⍺⍴⍵⍳∍⋾⎕⍞⌸⌺⍇⍁⍂⌻⌼⍃⍄⍈⍌⍍⍐⍓⍔⍗⍯⍰⍠⌹⊆⊇⍶⍸⍹⍘⍚⍛⍜⍊≤≥⍮ϼ⍷⍉⌽⊖⊙⌾○∘⍟⊗¨⍨⍡⍥⍩⍣⍢⍤⊂⊃∩∪⊥⊤∆∇⍒⍋⍫⍱⍲∨∧⍬⊣⊢⌷⍕⍎←→⍅⍆⍏⍖⌿⍀⍪≡≢⍦⍧⍭‽⍑∞…√";
  public static boolean debug = false;
  public static boolean noBoxing = false;
  public static boolean quotestrings = false;
  public static boolean colorful = true;
  static final Arr alphabet = toAPL("ABCDEFGHIJKLMNOPQRSTUVWXYZ", null);
  static final Arr lowercaseAlphabet = toAPL("abcdefghijklmnopqrstuvwxyz", null);
  static int printlvl = 0;
  public static final Error up = null;//new Error("A problem has been detected and APL has been shut down to prevent damage to your computer.");
  static final long startingMillis = System.currentTimeMillis();
  public static Scanner console;
  public static void main(String[] args) {
    colorful = System.console() != null && System.getenv().get("TERM") != null;
    console = new Scanner(System.in);
    Scope global = new Scope();
    Throwable lastError = null;
    boolean REPL = false;
    boolean silentREPL = false;
    if (args.length > 0) {
      try {
        for (int i = 0; i < args.length; i++) {
          String p = args[i];
          if (p.length() >= 2 && p.charAt(0) == '-') {
            if (p.charAt(1) == '-') {
              switch (p) {
                case "--help":
                  println("Usage: APL [options]");
                  println("Options:");
                  println("-f file: execute the contents of the file");
                  println("-e code: execute the argument as APL");
                  println("-p code: execute the argument as APL and print its result");
                  println("-i     : execute STDIN as APL");
                  println("-r     : start the REPL after everything else");
                  println("-s     : start the REPL without \">\" after everything else");
                  println("-d     : enable debug mode");
                  println("-q     : enable quoting of strings");
                  println("-b     : disable boxing");
                  println("-c     : disable colorful printing");
                  println("-D file: run the file as SBCS");
                  println("-E a b : encode the file A in the SBCS, save as B");
                  println("If given no arguments, an implicit -r will be added");
                  System.exit(0);
                  break;
                default:
                  throw new DomainError("Unknown command-line argument " + p);
              }
            } else {
              for (char c : p.substring(1).toCharArray()) {
                switch (c) {
                  case 'f':
                    String name = args[++i];
                    exec(readFile(name), global);
                    break;
                  case 'e':
                    String code = args[++i];
                    exec(code, global);
                    break;
                  case 'p':
                    code = args[++i];
                    println(exec(code, global).toString());
                    break;
                  case 'i':
                    StringBuilder s = new StringBuilder();
                    while (console.hasNext()) {
                      s.append(console.nextLine()).append('\n');
                    }
                    exec(s.toString(), global);
                    break;
                  case 'r':
                    REPL = true;
                    break;
                  case 's':
                    REPL = true;
                    silentREPL^= true;
                    break;
                  case 'd':
                    debug = true;
                    break;
                  case 'q':
                    quotestrings = true;
                    break;
                  case 'b':
                    noBoxing = true;
                    break;
                  case 'c':
                    colorful = false;
                    break;
                  case 'E': {
                    String origS = readFile(args[++i]);
                    byte[] res = new byte[origS.length()];
                    for (int j = 0; j < origS.length(); j++) {
                      char chr = origS.charAt(j);
                      int index = CODEPAGE.indexOf(chr);
                      if (index == -1) throw new DomainError("error encoding character " + chr + " (" + (+chr) + ")");
                      res[j] = (byte) index;
                    }
                    String conv = args[++i];
                    try (FileOutputStream stream = new FileOutputStream(conv)) {
                      stream.write(res);
                    } catch (IOException e) {
                      e.printStackTrace();
                      throw new DomainError("couldn't write file");
                    }
                    break;
                  }
                  case 'D':
                    try {
                      byte[] bytes = Files.readAllBytes(new File(args[++i]).toPath());
                      StringBuilder res = new StringBuilder();
                      for (byte b : bytes) {
                        res.append(CODEPAGE.charAt(b & 0xff));
                      }
                      exec(res.toString(), global);
                    } catch (IOException e) {
                      e.printStackTrace();
                      throw new DomainError("couldn't read file");
                    }
                    break;
                  default:
                    throw new DomainError("Unknown command-line argument -" + c);
                }
              }
            }
          } else {
            throw new DomainError("Unknown command-line argument " + p);
          }
        }
      } catch (APLError e) {
        e.print();
        throw e;
      } catch (Throwable e) {
        e.printStackTrace();
        colorprint(e + ": " + e.getMessage(), 246);
        throw e;
      }
    }
    if (args.length == 0 || REPL) {
      if (!silentREPL) print("> ");
      REPL: while (console.hasNext()) {
        try {
          String cr = console.nextLine();
          if (cr.equals("exit")) break;
          if (cr.startsWith(")")) {
            String[] parts = cr.split(" ");
            String t = parts[0].toUpperCase();
            String rest = cr.substring(t.length());
            switch (t) {
              case ")EX":
                exec(readFile(parts[1]), global);
                break;
              case ")DEBUG":
                debug = !debug;
                break;
              case ")QUOTE":
                quotestrings = !quotestrings;
                break;
              case ")ONELINE":
                noBoxing = !noBoxing;
                break;
              case ")OFF": case ")EXIT": case ")STOP":
                break REPL;
              case ")TOKENIZE"    : println(Tokenizer.tokenize(rest).toTree("")); break;
              case ")TOKENIZEREPR": println(Tokenizer.tokenize(rest).toRepr()); break;
              case ")TYPE"        : println(Main.human(exec(rest, global).type())); break;
              case ")ERR"         : new NotErrorError("", exec(rest, global)).print(); break;
              case ")CLASS"       : println(exec(rest, global).getClass().getCanonicalName()); break;
              case ")ATYPE"       : println(exec(rest, global).humanType(false)); break;
              case ")STACK":
                if (lastError != null) {
                  lastError.printStackTrace();
                }
                break;
              default:
                throw new SyntaxError("Undefined user command");
            }
          } else {
            Obj r = exec(cr, global);
            println(r.toString());
          }
        } catch (APLError e) {
          e.print();
          lastError = e;
        } catch (Throwable e) {
          e.printStackTrace();
          colorprint(e + ": " + e.getMessage(), 246);
        }
        if (!silentREPL) print("> ");
      }
    }
  }
  
  
  public static void print(String s) {
    System.out.print(s);
  }
  
  public static void println(String s) {
    System.out.println(s);
  }
  public static String human(Type t) {
    switch (t) {
      case array: return "array";
      case var: return "variable";
      case  fn: case  bfn: return "function";
      case mop: case bmop: return "monadic operator";
      case dop: case bdop: return "dyadic operator";
      default: throw new IllegalStateException();
    }
  }
  public static String formatAPL (int[] ia) {
    return Arrays.stream(ia).mapToObj(String::valueOf).collect(Collectors.joining(" "));
  }
  private static String readFile(String path) {
    try {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new DomainError("File "+path+" not found");
    }
  }
  
  @SuppressWarnings("WeakerAccess")
  public static Obj exec(String s, Scope sc) {
    Token t = Tokenizer.tokenize(s);
    printdbg(t);
    return execLines(t, sc);
  }
  
  static public void printdbg(Object... args) {
    if (!debug) return;
    if (args.length > 0) print(args[0] == null? "null" : args[0].toString());
    for (int i = 1; i < args.length; i++) {
      print(" ");
      print(args[i] == null? "null" : args[i].toString());
    }
    print("\n");
  }
  
  static public Obj execLines(Token lines, Scope sc) {
    assert (lines.type == TType.lines || lines.type == TType.usr);
    Obj res = null;
    for (Token ln : lines.tokens) {
      List<Token> tokens = ln.tokens;
      int guardPos = -1;
      boolean endAfter = tokens.size() > 0 && tokens.get(0).type == TType.set;
      if (endAfter) tokens = tokens.subList(1, tokens.size());
      else for (int i = 0; i < tokens.size(); i++) {
        if (tokens.get(i).type == TType.guard ) {
          guardPos = i;
          if (i == tokens.size()-1) throw new SyntaxError("Guard without success expression");
          if (tokens.get(i+1).type == TType.set) endAfter = true;
          break;
        }
      }
      if (guardPos >= 0) {
        var guard = new Token(ln.type, tokens.subList(0, guardPos), lines);
        if (bool(execTok(guard, sc), sc)) {
          var expr = new Token(ln.type, tokens.subList(guardPos+(endAfter? 2 : 1), tokens.size()), lines);
          res = execTok(expr, sc);
          if (endAfter) return res;
        }
      } else {
        res = execTok(endAfter? new Token(ln.type, tokens, lines) : ln, sc);
        if (endAfter) return res;
      }
    }
    return res;
  }
  
  public static Obj execTok(Token ln, Scope sc) {
    return new Exec(ln, sc).exec();
  }
  
  public static void colorprint(String s, int col) {
    if (colorful) println("\u001b[38;5;" + col + "m" + s + "\u001b[0m");
    else println(s);
  }
  
  public static Arr toAPL(int[] arr) {
    var va = new Value[arr.length];
    for (int i = 0; i < arr.length; i++) {
      va[i] = new Num(arr[i]);
    }
    return new Arr(va);
  }
  
  public static Arr toAPL(int[] arr, int[] sh) {
    var va = new Value[arr.length];
    for (int i = 0; i < arr.length; i++) {
      va[i] = new Num(arr[i]);
    }
    return new Arr(va, sh);
  }
  
  public static Arr toAPL(String s, Token t) {
    var vs = new ArrayList<Value>();
    for (char c : s.toCharArray()) {
      Char chr = new Char(c);
      chr.token = t;
      vs.add(chr);
    }
    Arr a = new Arr(vs);
    a.token = t;
    a.prototype = Char.SPACE;
    return a;
  }
  public static boolean bool(Obj v, Scope sc) {
    if (v instanceof Settable) v = ((Settable) v).get();
    String cond = ((Arr) sc.get("⎕COND")).string(false);
    assert cond != null;
    if (cond.endsWith(" ")) {
      if (v instanceof Char) {
        return ((Char) v).chr != ' ';
      }
      cond = cond.substring(0, cond.length()-1);
    }
    if (!(v instanceof Num)) throw new DomainError("⎕COND='01' but got type "+human(v.type()));
    Num n = (Num) v;
    switch (cond) {
      case "01":
        if (n.equals(Num.ZERO)) return false;
        if (n.equals(Num.ONE)) return true;
        throw new DomainError("⎕COND='01' expected condition to be 0 or 1, got "+n.toInt(null));
      case ">0":
        return n.compareTo(Num.ZERO)>0;
      case "≠0":
        return n.compareTo(Num.ZERO)!=0;
      default: throw new IllegalStateException("unknown ⎕COND "+cond);
    }
  }
}
