package BQN;

import BQN.errors.*;
import BQN.tokenizer.*;
import BQN.tokenizer.types.*;
import BQN.tools.*;
import BQN.types.*;
import BQN.types.arrs.*;
import BQN.types.callable.builtins.fns.*;
import BQN.types.callable.builtins.md1.*;
import BQN.types.callable.builtins.md2.*;
import BQN.types.callable.trains.*;
import BQN.types.mut.*;

import java.util.*;

public class Comp {
  public final int[] bc;
  public final Value[] objs;
  public final BlockTok[] blocks;
  public final Token[] ref;
  private final Token tk;
  
  public static int compileStart = 1; // at which iteration of calling the function should it be compiled to Java bytecode; negative for never, 0 for always
  
  public Comp(int[] bc, Value[] objs, BlockTok[] blocks, Token[] ref, Token tk) {
    this.bc = bc;
    this.objs = objs;
    this.blocks = blocks;
    this.ref = ref;
    this.tk = tk;
  }
  
  public static final byte PUSH =  0; // N; push object from objs[N]
  public static final byte VARO =  1; // N; push variable with name strs[N]
  public static final byte VARM =  2; // N; push mutable variable with name strs[N]
  public static final byte ARRO =  3; // N; create a vector of top N items
  public static final byte ARRM =  4; // N; create a mutable vector of top N items
  public static final byte FN1C =  5; // monadic function call ⟨…,x,f  ⟩ → F x
  public static final byte FN2C =  6; //  dyadic function call ⟨…,x,f,w⟩ → w F x
  public static final byte OP1D =  7; // derive 1-modifier to function; ⟨…,  _m,f⟩ → (f _m) 
  public static final byte OP2D =  8; // derive 2-modifier to function; ⟨…,g,_m,f⟩ → (f _m_ g)
  public static final byte TR2D =  9; // derive 2-train aka atop; ⟨…,  g,f⟩ → (f g)
  public static final byte TR3D = 10; // derive 3-train aka fork; ⟨…,h,g,f⟩ → (f g h)
  public static final byte SETN = 11; // set new; _  ←_; ⟨…,x,  mut⟩ → mut←x
  public static final byte SETU = 12; // set upd; _  ↩_; ⟨…,x,  mut⟩ → mut↩x
  public static final byte SETM = 13; // set mod; _ F↩_; ⟨…,x,F,mut⟩ → mut F↩x
  public static final byte POPS = 14; // pop object from stack
  public static final byte DFND = 15; // N; push dfns[N], derived to current scope
  public static final byte FN1O = 16; // optional monadic call (FN1C but checks for · at 𝕩)
  public static final byte FN2O = 17; // optional  dyadic call (FN2C but checks for · at 𝕩 & 𝕨)
  public static final byte CHKV = 18; // throw error if top of stack is ·
  public static final byte TR3O = 19; // TR3D but creates an atop if F is ·
  public static final byte OP2H = 20; // derive 2-modifier to 1-modifier ⟨…,g,_m_⟩ → (_m_ g)
  public static final byte LOCO = 21; // N0,N1; push variable at depth N0 and position N1
  public static final byte LOCM = 22; // N0,N1; push mutable variable at depth N0 and position N1
  public static final byte VFYM = 23; // push a mutable version of ToS that fails if set to a non-equal value (for header assignment)
  public static final byte SETH = 24; // set header; acts like SETN, but it doesn't push to stack, and, instead of erroring in cases it would, it skips to the next body
  public static final byte RETN = 25; // returns top of stack
  public static final byte FLDO = 26; // N; get field objs[N] of ToS
  public static final byte FLDM = 27; // N; set field objs[N] from ToS
  public static final byte NSPM = 28; // N0,N1; create a destructible namespace from top N0 items, with the keys objs[N1]
  public static final byte RETD = 29; // return a namespace of exported items
  
  public static final byte SPEC = 30; // special
  public static final byte   EVAL = 0; // ⍎
  public static final byte   STDIN = 1; // •
  public static final byte   STDOUT = 2; // •←
  
  
  static class Stk {
    private Obj[] vals = new Obj[4];
    private int sz = 0;
    void push(Obj o) {
      if (sz>=vals.length) vals = Arrays.copyOf(vals, vals.length<<1);
      vals[sz++] = o;
    }
    Obj pop() {
      Obj val = vals[--sz];
      vals[sz] = null;
      return val;
    }
    Obj peek() {
      return vals[sz-1];
    }
  }
  
  
  public static boolean JCOMP = !System.getProperty("org.graalvm.nativeimage.kind", "-").equals("executable");
  public Value exec(Scope sc, Body body) {
    int i = body.start;
    try {
      if (JCOMP) {
        if (body.gen!=null) return body.gen.get(sc, body);
        if (body.iter++>=compileStart && compileStart>=0) {
          body.gen = new JBQNComp(this, body.start).r;
          if (body.gen!=null) return body.gen.get(sc, body);
          else body.iter = Integer.MIN_VALUE;
        }
      }
      Stk s = new Stk();
      exec: while (true) {
        int c = i;
        if (i >= bc.length) break;
        c++;
        switch (bc[i]) {
          case PUSH: {
            int n = bc[c++];
            s.push(objs[n]);
            break;
          }
          case VARO: {
            int n = bc[c++];
            Value got = sc.getC(objs[n].asString());
            s.push(got);
            break;
          }
          case VARM: {
            int n = bc[c++];
            s.push(new Variable(objs[n].asString()));
            break;
          }
          case LOCO: {
            int depth = bc[c++];
            int index = bc[c++];
            Value got = sc.getL(depth, index);
            if (got==null) throw new ValueError("Reading variable before creation");
            s.push(got);
            break;
          }
          case LOCM: {
            int depth = bc[c++];
            int index = bc[c++];
            s.push(new Local(depth, index));
            break;
          }
          case ARRO: {
            int n = bc[c++];
            Value[] vs = new Value[n];
            for (int j = 0; j < n; j++) {
              vs[n-j-1] = (Value) s.pop();
            }
            s.push(Arr.create(vs));
            break;
          }
          case ARRM: {
            int n = bc[c++];
            Settable[] vs = new Settable[n];
            for (int j = 0; j < n; j++) {
              vs[n-j-1] = (Settable) s.pop();
            }
            s.push(new SettableArr(vs));
            break;
          }
          case FN1C: {
            Value f = (Value) s.pop();
            Value x = (Value) s.pop();
            s.push(f.call(x));
            break;
          }
          case FN2C: {
            Value w = (Value) s.pop();
            Value f = (Value) s.pop();
            Value x = (Value) s.pop();
            s.push(f.call(w, x));
            break;
          }
          case FN1O: {
            Value f = (Value) s.pop();
            Value x = (Value) s.pop();
            if (x instanceof Nothing) s.push(x);
            else s.push(f.call(x));
            break;
          }
          case FN2O: {
            Value w = (Value) s.pop();
            Value f = (Value) s.pop();
            Value x = (Value) s.pop();
            if (x instanceof Nothing) s.push(x);
            else if (w instanceof Nothing) s.push(f.call(x));
            else s.push(f.call(w, x));
            break;
          }
          case OP1D: {
            Value f = (Value) s.pop();
            Value r = (Value) s.pop();
            if (!(r instanceof Md1)) throw new SyntaxError("Cannot interpret "+r.humanType(true)+" as a 1-modifier");
            Value d = ((Md1) r).derive(f); if (d instanceof Callable) ((Callable) d).token = ref[i];
            s.push(d);
            break;
          }
          case OP2D: {
            Value f = (Value) s.pop();
            Value r = (Value) s.pop();
            Value g = (Value) s.pop();
            if (!(r instanceof Md2)) throw new SyntaxError("Cannot interpret "+r.humanType(true)+" as a 2-modifier");
            Value d = ((Md2) r).derive(f, g); if (d instanceof Callable) ((Callable) d).token = ref[i];
            s.push(d);
            break;
          }
          case OP2H: {
            Value r = (Value) s.pop();
            Value g = (Value) s.pop();
            if (!(r instanceof Md2)) throw new SyntaxError("Cannot interpret "+r.humanType(true)+" as a 2-modifier");
            Md1 d = ((Md2) r).derive(g); d.token = ref[i];
            s.push(d);
            break;
          }
          case TR2D: {
            Value f = (Value) s.pop();
            Value g = (Value) s.pop();
            Atop d = new Atop(f, g);
            s.push(d);
            break;
          }
          case TR3D: {
            Value f = (Value) s.pop();
            Value g = (Value) s.pop();
            Value h = (Value) s.pop();
            Fork d = new Fork(f, g, h);
            s.push(d);
            break;
          }
          case TR3O: {
            Value f = (Value) s.pop();
            Value g = (Value) s.pop();
            Value h = (Value) s.pop();
            Obj d = f instanceof Nothing? new Atop(g, h) : new Fork(f, g, h);
            s.push(d);
            break;
          }
          case SETN: {
            Settable k = (Settable) s.pop();
            Value    v = (Value   ) s.pop();
            k.set(v, false, sc, null);
            s.push(v);
            break;
          }
          case SETU: {
            Settable k = (Settable) s.pop();
            Value    v = (Value   ) s.pop();
            k.set(v, true, sc, null);
            s.push(v);
            break;
          }
          case SETM: {
            Settable k = (Settable) s.pop();
            Value    f = (Value   ) s.pop();
            Value    v = (Value   ) s.pop();
            Value    r = f.call(k.get(sc), v);
            k.set(r, true, sc, null);
            s.push(r);
            break;
          }
          case SETH: {
            Settable k = (Settable) s.pop();
            Value    v = (Value   ) s.pop();
            if (!k.seth(v, sc)) return null;
            break;
          }
          case VFYM: {
            Value x = (Value) s.pop();
            s.push(new MatchSettable(x));
            break;
          }
          case POPS: {
            s.pop();
            break;
          }
          case DFND: {
            int n = bc[c++];
            s.push(blocks[n].eval(sc));
            break;
          }
          case CHKV: {
            Obj v = s.peek();
            if (v instanceof Nothing) throw new SyntaxError("didn't expect · here");
            break;
          }
          case RETN: {
            break exec;
          }
          case RETD: {
            s.push(new Namespace(sc, body.exp));
            break exec;
          }
          case FLDO: {
            int n = bc[c++];
            Obj m = s.pop();
            String k = objs[n].asString();
            if (!(m instanceof BQNObj)) throw new DomainError("Expected value to the left of '.' to be a map");
            s.push(((BQNObj) m).getChk(k));
            break;
          }
          case FLDM: {
            int n = bc[c++];
            Obj m = s.pop();
            String k = objs[n].asString();
            if (!(m instanceof BQNObj)) throw new DomainError("Expected value to the left of '.' to be a map");
            s.push(((BQNObj) m).getMut(k));
            break;
          }
          case NSPM: {
            int n0 = bc[c++];
            int n1 = bc[c++];
            Settable[] vs = new Settable[n0];
            for (int j = 0; j < n0; j++) {
              vs[n0-j-1] = (Settable) s.pop();
            }
            s.push(new SettableNS(vs, objs[n1]));
            break;
          }
          case SPEC: {
            switch(bc[c++]) {
              case EVAL:
                s.push(new EvalBuiltin(sc));
                break;
              case STDOUT:
                s.push(new Quad());
                break;
              case STDIN:
                s.push(new Quad().get(sc));
                break;
              default:
                throw new InternalError("Unknown special "+bc[c-1]);
            }
            break;
          }
          default: throw new InternalError("Unknown bytecode "+bc[i]);
        }
        i = c;
      }
      return (Value) s.peek();
    } catch (Throwable t) {
      APLError e;
      if (t instanceof APLError) e = (APLError) t;
      else {
        if (t instanceof OutOfMemoryError || t instanceof StackOverflowError) e = new SystemError(t instanceof StackOverflowError? "Stack overflow" : "Out of memory", t);
        else e = new ImplementationError(t);
      }
      Tokenable fn = body.gen!=null? null : ref[i];
      if (e.blame == null) {
        e.blame = fn!=null? fn : tk;
      }
      ArrayList<APLError.Mg> mgs = new ArrayList<>();
      APLError.Mg.add(mgs, tk, '¯');
      APLError.Mg.add(mgs, fn, '^');
      e.trace.add(new APLError.Frame(sc, mgs, this, body.start));
      throw e;
    }
  }
  
  public String fmt() {
    return fmt(-1, 10);
  }
  
  public String fmt(int hl, int am) {
    StringBuilder b = new StringBuilder(hl<0? "code:\n" : "");
    int i = 0;
    try {
      while (i != bc.length) {
        int pi = i;
        i++;
        String cs;
        switch (bc[pi]) {
          case PUSH: cs = "PUSH " + safeObj(l7dec(bc, i)); i = l7end(bc, i); break;
          case VARO: cs = "VARO " + safeObj(l7dec(bc, i)); i = l7end(bc, i); break;
          case VARM: cs = "VARM " + safeObj(l7dec(bc, i)); i = l7end(bc, i); break;
          case DFND: cs = "DFND " +         l7dec(bc, i) ; i = l7end(bc, i); break;
          case ARRO: cs = "ARRO " +         l7dec(bc, i) ; i = l7end(bc, i); break;
          case ARRM: cs = "ARRM " +         l7dec(bc, i) ; i = l7end(bc, i); break;
          case FN1C: cs = "FN1C"; break;
          case FN2C: cs = "FN2C"; break;
          case OP1D: cs = "OP1D"; break;
          case OP2D: cs = "OP2D"; break;
          case TR2D: cs = "TR2D"; break;
          case TR3D: cs = "TR3D"; break;
          case SETN: cs = "SETN"; break;
          case SETU: cs = "SETU"; break;
          case SETM: cs = "SETM"; break;
          case SETH: cs = "SETH"; break;
          case POPS: cs = "POPS"; break;
          case FN1O: cs = "FN1O"; break;
          case FN2O: cs = "FN2O"; break;
          case CHKV: cs = "CHKV"; break;
          case TR3O: cs = "TR3O"; break;
          case OP2H: cs = "OP2H"; break;
          case VFYM: cs = "VFYM"; break;
          case LOCO: cs = "LOCO " + l7dec(bc, i) + " " +         l7dec(bc, i=l7end(bc, i)) ; i = l7end(bc, i); break;
          case LOCM: cs = "LOCM " + l7dec(bc, i) + " " +         l7dec(bc, i=l7end(bc, i)) ; i = l7end(bc, i); break;
          case NSPM: cs = "NSPM " + l7dec(bc, i) + " " + safeObj(l7dec(bc, i=l7end(bc, i))); i = l7end(bc, i); break;
          case RETN: cs = "RETN"; break;
          case RETD: cs = "RETD"; break;
          case FLDO: cs = "FLDO " + safeObj(l7dec(bc, i)); i = l7end(bc, i); break;
          case FLDM: cs = "FLDM " + safeObj(l7dec(bc, i)); i = l7end(bc, i); break;
          case SPEC: cs = "SPEC " + (bc[i++]&0xff); break;
          default  : cs = "unknown";
        }
        if (hl<0 || Math.abs(hl-pi) < am || Math.abs(hl-i) < am) {
          if (hl<0) b.append(' ');
          else b.append(hl==pi? ">>" : "  ");
          for (int j = pi; j < i; j++) {
            int c = bc[j]&0xff;
            b.append(Integer.toHexString(c/16).toUpperCase());
            b.append(Integer.toHexString(c%16).toUpperCase());
            if (j+1 != i) b.append(' ');
          }
          b.append(Main.repeat(" ", Math.max(1, (3-(i-pi))*3 + 1)));
          b.append(cs);
          b.append('\n');
        }
      }
      if (hl>=0) return b.toString();
    } catch (Throwable t) {
      b.append("#ERR#\n");
    }
    if (objs.length > 0) {
      b.append("objs:\n");
      for (int j = 0; j < objs.length; j++) b.append(' ').append(j).append(": ").append(objs[j].ln(FmtInfo.def)).append('\n');
    }
    if (blocks.length > 0) {
      b.append("blocks:\n");
      for (int j = 0; j < blocks.length; j++) { // TODO move this around so it can also show for the top-level block
        BlockTok blk = blocks[j];
        char tp = blk.type;
        b.append(' ').append(j).append(": ").append(tp=='a'? "immediate block" : (blk.immediate?"immediate ":"") + (tp=='f'? "function" : tp=='d'? "2-modifier" : tp=='m'? "1-modifier" : tp+"")).append(" \n");
        b.append("  flags: ").append(blk.flags).append('\n');
        HashSet<Body> mb = new HashSet<>(); mb.addAll(Arrays.asList(blk.bdM)); mb.addAll(Arrays.asList(blk.bdMxi));
        HashSet<Body> db = new HashSet<>(); db.addAll(Arrays.asList(blk.bdD)); db.addAll(Arrays.asList(blk.bdDxi)); db.addAll(Arrays.asList(blk.bdDwi));
        HashSet<Body> done = new HashSet<>();
        for (int k = 0; k < 3; k++) {
          for (Body[] bs : new Body[][][]{{blk.bdM, blk.bdD}, {blk.bdMxi, blk.bdDxi}, {blk.bdDwi}}[k]) {
            for (Body bd : bs) {
              if (done.contains(bd)) continue;
              boolean mq = mb.contains(bd);
              boolean dq = db.contains(bd);
              b.append("  ").append(mq&&dq? "ambivalent" : mq? "monadic" : "dyadic").append(k==0?"" : k==1? " inverse" : " 𝕨-inverse").append(" body: ").append('\n');
              b.append("    start: ").append(bd.start).append('\n');
              if (bd.vars.length!=0) b.append("    vars: ").append(Arrays.toString(bd.vars)).append('\n');
              done.add(bd);
            }
          }
        }
        if (blk.comp != this) {
          b.append("  ");
          b.append(blk.comp.fmt().replace("\n", "\n  "));
          b.append('\n');
        }
      }
    }
    b.deleteCharAt(b.length()-1);
    return b.toString();
  }
  
  
  public int next(int i) {
    switch (bc[i]) {
      case PUSH: case DFND:
      case VARO: case VARM:
      case ARRO: case ARRM:
      case FLDO: case FLDM:
        return l7end(bc, i+1);
      case FN1C: case FN2C: case FN1O: case FN2O:
      case OP1D: case OP2D: case OP2H:
      case TR2D: case TR3D: case TR3O:
      case SETN: case SETU: case SETM: case SETH:
      case POPS: case CHKV: case VFYM: case RETN: case RETD:
        return i+1;
      case SPEC: return i+2;
      case LOCO: case LOCM: case NSPM:
        return l7end(bc, l7end(bc, i+1));
      default  : return -1;
    }
  }
  
  
  private int l7dec(int[] bc, int i) {
    return bc[i];
  }
  private int l7end(int[] bc, int i) { // returns first index after end
    return i+1;
  }
  
  private String safeObj(int l) {
    if (l>=objs.length) return l+" INVALID";
    return l+": "+objs[l].ln(FmtInfo.def);
  }
  
  
  /* types:
    a - array
    A - array or ·
    f - function
    d - 2-modifier
    m - 1-modifier
    
    ← - new var
    ↩ - upd var
    
    _ - empty
    
   */
  
  
  
  public static class Mut {
    Mut p;
    public Mut(Mut p) { this.p = p; }
  
    ArrayList<Value> objs = new ArrayList<>(16);
    ArrayList<BlockTok> blocks = new ArrayList<>(16);
    MutIntArr bc = new MutIntArr(16);
    ArrayList<Token> ref = new ArrayList<>(16);
    
    HashMap<String, Integer> vars; // map of varName→index
    ArrayList<String> varnames;
    ArrayList<BlockTok> cBlocks = new ArrayList<>(16);
    HashSet<String> exported;
    public void newBody(String[] preset) {
      for (BlockTok c : cBlocks) c.compile(this);
      cBlocks.clear();
      varnames = new ArrayList<>(preset.length);
      Collections.addAll(varnames, preset);
      exported = null;
      vars = new HashMap<>(preset.length);
      for (int i = 0; i < preset.length; i++) vars.put(preset[i], i);
    }
    
    public void export(String name) {
      if (exported == null) exported = new HashSet<>();
      exported.add(name);
    }
    
    public String[] getVars() {
      return varnames.toArray(new String[0]);
    }
    public int[] getExp() {
      if (exported == null) return null;
      int[] exp = new int[exported.size()];
      int i = 0;
      for (String name : exported) {
        Integer pos = vars.get(name);
        if (pos == null) throw new SyntaxError("Exporting non-defined variable "+name);
        exp[i++] = pos;
      }
      return exp;
    }
    
    public void push(Token t, Value o) {
      add(t, PUSH);
      add(addObj(o));
    }
    public int addObj(Value o) {
      int r = objs.size();
      objs.add(o);
      return r;
    }
    
    public void push(BlockTok o) {
      add(o, DFND);
      add(blocks.size());
      blocks.add(o);
      cBlocks.add(o);
    }
    
    public void nvar(String name) {
      vars.put(name, varnames.size());
      varnames.add(name);
    }
    public void var(Token t, String s, boolean mut) {
      Mut c = this;
      int d = 0;
      do {
        Integer pos = c.vars.get(s);
        if (pos!=null) {
          add(t, mut? LOCM : LOCO);
          add(d);
          add(pos);
          return;
        }
        d++;
        c = c.p;
      } while (c!=null);
      add(t, mut? VARM : VARO);
      add(addObj(new ChrArr(s)));
    }
    
    
    public void add(int nbc) {
      bc.add(nbc); ref.add(null);
    }
    public void add(Token tk, int nbc) {
      bc.add(nbc); ref.add(tk);
    }
    public void add(Token tk, int... nbc) {
      for (int b : nbc) {
        bc.add(b); ref.add(tk);
      }
    }
    
    public Comp finish(Token tk) {
      assert bc.sz == ref.size() : bc.sz +" "+ ref.size();
      for (BlockTok c : cBlocks) c.compile(this);
      return new Comp(bc.get(), objs.toArray(new Value[0]), blocks.toArray(new BlockTok[0]), ref.toArray(new Token[0]), tk);
    }
  }
  
  
  public static class SingleComp {
    public final Comp c; public final Body b;
    public SingleComp(Comp c, Body b) { this.c = c; this.b = b; }
    public Value exec(Scope sc) { return c.exec(sc, b); }
    public String fmt() { return c.fmt(); }
  }
  public static SingleComp comp(TokArr lns, Scope sc, boolean allowEmpty) { // non-block
    Mut mut = new Mut(null);
    mut.newBody(sc.varNames);
    int sz = lns.tokens.size();
    if (!allowEmpty && sz==0) throw new SyntaxError("Executing empty code");
    for (int i = 0; i < sz; i++) {
      Token ln = lns.tokens.get(i); typeof(ln); flags(ln);
      compO(mut, ln);
      if (i!=sz-1) mut.add(POPS);
    }
    sc.varNames = mut.getVars();
    sc.varAm = sc.varNames.length;
    if (sc.vars.length < sc.varAm) sc.vars = Arrays.copyOf(sc.vars, sc.varAm);
    sc.removeMap();
    return new SingleComp(mut.finish(lns), new Body(new ArrayList<>(lns.tokens), 'a', false));
  }
  public static SingleComp compN(TokArr lns, Scope sc) { // non-block
    Mut mut = new Mut(null);
    Body b = new Body(new ArrayList<>(lns.tokens), 'a', false);
    mut.newBody(sc.varNames);
    int sz = lns.tokens.size();
    if (sz==0) throw new SyntaxError("Executing empty code");
    LineTok last = null;
    for (int i = 0; i < sz; i++) {
      if (last!=null) mut.add(POPS);
      LineTok ln = (LineTok) lns.tokens.get(i);
      if (ln.tokens.size()==2 && ln.tokens.get(1).type=='⇐') {
        compE(mut, ln.tokens.get(0));
        last = null;
      } else {
        typeof(ln); flags(ln);
        compO(mut, ln);
        last = ln;
      }
    }
    b.vars = mut.getVars();
    b.setExp(mut.getExp());
    if (b.exp==null) {
      if (last!=null && last.type=='A') mut.add(CHKV);
      mut.add(RETN);
    } else {
      if (last!=null) mut.add(POPS);
      mut.add(RETD);
    }
    
    sc.varNames = mut.getVars();
    sc.varAm = sc.varNames.length;
    if (sc.vars.length < sc.varAm) sc.vars = Arrays.copyOf(sc.vars, sc.varAm);
    sc.removeMap();
    
    return new SingleComp(mut.finish(lns), b);
  }
  
  public static Comp comp(Mut mut, ArrayList<Body> parts, BlockTok tk) { // block
    for (Body b : parts) {
      b.start = mut.bc.sz;
      mut.newBody(tk.defNames());
      b.addHeader(mut);
      int sz = b.lns.size();
      if (sz==0) throw new SyntaxError("Executing empty code");
      LineTok last = null;
      for (int j = 0; j < sz; j++) {
        if (last!=null) mut.add(POPS);
        LineTok ln = (LineTok) b.lns.get(j);
        if (ln.tokens.size()==2 && ln.tokens.get(1).type=='⇐') {
          compE(mut, ln.tokens.get(0));
          last = null;
        } else {
          typeof(ln); flags(ln);
          compO(mut, ln);
          last = ln;
        }
      }
      b.vars = mut.getVars();
      b.setExp(mut.getExp());
      if (b.exp==null) {
        if (last!=null && last.type=='A') mut.add(CHKV);
        mut.add(RETN);
      } else {
        if (last!=null) mut.add(POPS);
        mut.add(RETD);
      }
    }
    return mut.finish(tk);
  }
  
  private static boolean isE(DQ tps, String pt, int lim, boolean last) { // O=[aAf] in non-!, A ≡ a
    if (tps.size() > lim) return false;
    int pi = pt.length()-1;
    int ti = tps.size()-1;
    boolean qex = false;
    while (pi>=0) {
      char c = pt.charAt(pi--);
      if (c=='|') {
        if (last) qex = true;
      } else {
        if (ti==-1) return qex;
        char t = norm(tps.get(ti--).type);
        if (c=='!') {
          if (pt.charAt(pi) == ']') {
            do { pi--;
              if (t == pt.charAt(pi)) return false;
            } while(pt.charAt(pi) != '['); pi--;
          } else {
            if (t == pt.charAt(pi--)) return false;
          }
        } else if (c=='O') {
          if (t!='f' && t!='a') return false;
        } else if (c == ']') {
          boolean any = false;
          do {
            if (t == pt.charAt(pi)) any = true;
            pi--;
          } while(pt.charAt(pi) != '['); pi--;
          if (!any) return false;
        } else {
          if (t != c) return false;
        }
      }
    }
    return true;
  }
  
  static abstract class Res {
    char type;
    Value c;
    public Res(char type) {
      this.type = type;
    }
    
    abstract void add(Mut m);
    Res mut(boolean create, boolean export) { throw new SyntaxError("This cannot be mutated", lastTok()); }
    
    public abstract Token lastTok();
  }
  
  static class ResTk extends Res {
    Token tk;
    private boolean mut;
    private boolean create;
    private boolean export;
    
    public ResTk(Token tk) {
      super(tk.type);
      this.tk = tk;
      this.c = (tk.flags&1)!=0? constFold(tk) : null;
      type = tk.type;
    }
    
    void add(Mut m) {
      if (export) compE(m, tk);
      if (mut) compM(m, tk, create, false);
      else compO(m, tk);
    }
    
    Res mut(boolean create, boolean export) {
      assert !mut;
      mut = true;
      this.create = create;
      this.export = export;
      return this;
    }
    
    public Token lastTok() {
      return tk;
    }
    
    public String toString() {
      return tk==null? type+"" : tk.source();
    }
  }
  static class ResChk extends Res {
    private final boolean check;
    private final Token tk;
    
    public ResChk(boolean check) {
      super('\0');
      this.check = check;
      this.tk = null;
    }
    public ResChk(Token tk, boolean check) {
      super('\0');
      this.check = check;
      this.tk = tk;
    }
    
    void add(Mut m) {
      if (check) m.add(tk, CHKV);
    }
    
    public Token lastTok() {
      return null;
    }
    
    public String toString() {
      return check? "CHKV" : "NOOP";
    }
  }
  static class ResBC extends Res {
    private final int bc;
    private final Token tk;
    
    public ResBC(int bc) {
      super('\0');
      this.bc = bc;
      this.tk = null;
    }
    public ResBC(Token tk, int bc) {
      super('\0');
      this.bc = bc;
      this.tk = tk;
    }
    
    void add(Mut m) {
      m.add(tk, bc);
    }
    
    public Token lastTok() {
      return null;
    }
    
    public String toString() {
      return String.valueOf(bc);
    }
  }
  static class ResGet extends Res {
    private final Res o;
    private final String k;
    private final Token tk;
    private boolean mut;
    public ResGet(Res o, String k, char t, Token tk) {
      super(t);
      this.o = o;
      this.k = k;
      this.tk = tk;
    }
    void add(Mut m) {
      o.add(m);
      m.add(tk, mut? FLDM : FLDO);
      m.add(m.addObj(new ChrArr(k)));
    }
  
    Res mut(boolean create, boolean export) { // TODO use create?
      mut = true;
      if (export) throw new SyntaxError("Cannot export field access", tk);
      return this;
    }
  
    public Token lastTok() {
      return tk;
    }
  }
  static class ResMix extends Res {
    private final Res[] all;
    
    public ResMix(char type, Res... all) {
      super(type);
      this.all = all;
    }
    
    void add(Mut m) {
      for (Res r : all) r.add(m);
    }
    
    public Token lastTok() {
      for (int i = all.length-1; i >= 0; i--) {
        Res r = all[i];
        Token tk = r.lastTok();
        if (tk != null) return tk;
      }
      return null;
    }
    
    public String toString() {
      return Arrays.toString(all);
    }
  }
  static class ResCf extends Res {
    private final Token last;
  
    public ResCf(char type, Value val, Token last) {
      super(type);
      this.c = val;
      this.last = last;
    }
  
    void add(Mut m) {
      m.push(last, c);
    }
  
    public Token lastTok() {
      return last;
    }
  
    public String toString() {
      return "C:"+c.ln(FmtInfo.def);
    }
  }
  
  private static final int[] NOINTS = EmptyArr.NOINTS;
  private static final int[] CHKV_A = new int[]{CHKV};
  
  
  private static void printlvl(String s) {
    System.out.println(Main.repeat(" ", Main.printlvl*2) + s);
  }
  public static void collect(DQ tps, boolean train, boolean last) {
    while (true) {
      if (Main.debug) printlvl(tps.toString());
      if (tps.size() <= 1) break;
      if (tps.peekFirst().type=='.' || tps.get(1).type=='.'  &&  !last) break;
      char fType = tps.peekFirst().type;
      char lType = tps.peekLast().type;
      if (tps.size()>=3 && (
           last && tps.get(1).type=='.' 
        || tps.get(2).type=='.')
      ) {
        int s = tps.get(1).type=='.'? 0 : 1;
        do {
          Res m = tps.remove(s);
          Res d = tps.remove(s); assert d.type=='.';
          Res k = tps.remove(s);
          if (m.type!='a' && m.type!='A') throw new SyntaxError("expected token before '.' to be a subject", m.lastTok());
          if (!(k instanceof ResTk)) throw new SyntaxError("expected name after '.'", k.lastTok());
          Token tk = ((ResTk) k).tk;
          if (!(tk instanceof NameTok)) throw new SyntaxError("expected name after '.'", tk);
          String ks = ((NameTok) tk).name;
          tps.add(s, new ResGet(m, ks, k.type, d.lastTok()));
        } while (tps.size()>=s+3 && tps.get(s+1).type=='.');
      }
      if (tps.sz>=(last?2:3)) {
        if (train) { // trains only
          if (lType=='f' && tps.getL(1).type=='f') {
            if (isE(tps, "d!|Off", 4, last)) {
              if (Main.debug) printlvl("match F F F");
              Res h = tps.removeLast();
              Res g = tps.removeLast();
              Res f = tps.removeLast();
              if (h.c!=null && g.c!=null && f.c!=null) {
                if (f.c instanceof Nothing) tps.addLast(new ResCf('f', new Atop(g.c, h.c), h.lastTok()));
                else tps.addLast(new ResCf('f', new Fork(f.c, g.c, h.c), h.lastTok()));
              } else {
                tps.addLast(new ResMix('f', h, g, f, new ResBC(f.type=='A'? TR3O : TR3D) ));
              }
              continue;
            }
            if (isE(tps, "[⇐←↩]|ff", 4, last)) {
              if (Main.debug) printlvl("match F F");
              Res h = tps.removeLast();
              Res g = tps.removeLast();
              if (h.c!=null && g.c!=null) tps.addLast(new ResCf('f', new Atop(g.c, h.c), h.lastTok()));
              else tps.addLast(new ResMix('f', h, g, new ResBC(TR2D)));
              continue;
            }
          }
        } else { // value expressions
          if (tps.getL(1).type=='f' && isArr(lType)) {
            char g3 = tps.sz>2? norm(tps.getL(2).type) : 0;
            if (g3=='a') {
              if (tps.sz>3? tps.getL(3).type!='d' : last) {
                if (Main.debug) printlvl("match a F a");
                Res x = tps.removeLast();
                Res f = tps.removeLast();
                Res w = tps.removeLast();
                tps.addLast(new ResMix(x.type,
                  x, f, w,
                  new ResBC(f.lastTok(), x.type=='A' | w.type=='A'? FN2O : FN2C)
                ));
                continue;
              }
            } else {
              if (g3!='d') {
                if (Main.debug) printlvl("match F a");
                Res x = tps.removeLast();
                Res f = tps.removeLast();
                tps.addLast(new ResMix(x.type,
                  x, f,
                  new ResBC(f.lastTok(), x.type=='A'? FN1O : FN1C)
                ));
                continue;
              }
            }
          }
          // if (isE(tps, "d!|afa", 4, last)) {
          //   if (Main.debug) printlvl("match a F a");
          //   Res x = tps.removeLast();
          //   Res f = tps.removeLast();
          //   Res w = tps.removeLast();
          //   tps.addLast(new ResMix(x.type,
          //     x, f, w,
          //     new ResBC(f.lastTok(), x.type=='A' | w.type=='A'? FN2O : FN2C)
          //   ));
          //   continue;
          // }
          // if (isE(tps, "[da]!|fa", 4, last)) {
          //   if (Main.debug) printlvl("match F a");
          //   Res x = tps.removeLast();
          //   Res f = tps.removeLast();
          //   tps.addLast(new ResMix(x.type,
          //     x, f,
          //     new ResBC(f.lastTok(), x.type=='A'? FN1O : FN1C)
          //   ));
          //   continue;
          // }
        }
      }
      // all
      
      int i = last? 0 : 1; // hopefully this doesn't need to be looping
      if (tps.sz >= 2+i) {
        char o1Type = last? fType : tps.get(1).type;
        char o2Type = tps.get(i+1).type;
        if (fType!='d' && isOperand(o1Type)) {
          if (o2Type=='m') {
            if (Main.debug) printlvl("match O m");
            Res c=tps.remove(i+1);
            Res f=tps.remove(i  );
            if (c.c!=null && f.c!=null) {
              tps.add(i, new ResCf('f', ((Md1) c.c).derive(f.c), c.lastTok()));
            } else tps.add(i, new ResMix('f', c, f,
              new ResChk(f.lastTok(), f.type=='A'),
              new ResBC(c.lastTok(), OP1D)
            ));
            continue;
          }
          if (tps.sz>=3+i && o2Type=='d' && isOperand(tps.get(i + 2).type)) {
            if (Main.debug) printlvl("match O d O "+i);
            Res g=tps.remove(i+2);
            Res c=tps.remove(i+1);
            Res f=tps.remove(i  );
            if (g.c!=null && c.c!=null && f.c!=null) {
              if (g.c instanceof Nothing || f.c instanceof Nothing) throw new SyntaxError("didn't expect · here", g.c instanceof Nothing? g.lastTok() : f.lastTok() );
              tps.add(i, new ResCf('f', ((Md2) c.c).derive(f.c, g.c), f.lastTok()));
            } else tps.add(i, new ResMix('f',
              g, new ResChk(g.lastTok(), g.type=='A'),
              c,
              f, new ResChk(f.lastTok(), f.type=='A'),
              new ResBC(c.lastTok(), OP2D)
            ));
            continue;
          }
        }
        if (o1Type=='d' && isOperand(o2Type)) { // isS(tps, "dO", i)
          if (i==0 || !isOperand(fType)) {
            if (Main.debug) printlvl("match dO");
            Res f;
            tps.add(i, new ResMix('m',
              (f=tps.remove(i+1)),
              new ResChk(f.type=='A'),
              (  tps.remove(i  )),
              new ResBC(f.lastTok(), OP2H)
            ));
            continue;
          }
        }
      }
      
      if (isE(tps, ".!|af↩a", 5, last)) { //tps.sz>=4+i && isArr(lType) && tps.getL(1).type=='↩' && tps.getL(2).type=='f' && isArr(tps.getL(3).type) 
        if (Main.debug) printlvl("af↩a");
        Res val = tps.removeLast();
                  tps.removeLast(); // ↩
        Res fn  = tps.removeLast();
        Res mut = tps.removeLast().mut(false, false);
        tps.addLast(new ResMix('a', val, fn, mut, new ResBC(SETM) ));
        continue;
      }
      set: if (tps.size() >= (last? 3 : 4)) {
        char a = tps.getL(1).type;
        if (a=='←' || a=='↩' || a=='⇐') {
          char k = tps.getL(2).type;
          char v = lType;
          char p = tps.size()>=4? tps.getL(3).type : 0;
          if (p=='d') break set;
          char ov = v;
          v = norm(v);
          k = norm(k); // 𝕨↩ is a possibility
          if (k==v) {
            if (Main.debug) printlvl(k+" "+a+" "+v);
            Res val = tps.removeLast();
                      tps.removeLast(); // ↩/←/⇐
            Res mut = tps.removeLast().mut(a!='↩', a=='⇐');
            tps.addLast(new ResMix(v,
              val,
              new ResChk(ov=='A'),
              mut,
              new ResBC(a=='↩'? SETU : SETN)
            ));
            continue;
          } else if (last) throw new SyntaxError(a+": Cannot assign with different types", ((ResTk) tps.getL(1)).tk);
        }
      }
      break;
    }
  }
  static boolean isOperand(char c) {
    return c=='a' | c=='A' | c=='f';
  }
  static boolean isArr(char c) {
    return c=='a' | c=='A';
  }
  
  public static char typeof(Token t) {
    if (t.type != 0) return t.type; // handles NumTok, StrTok, ChrTok, SetTok, ModTok, NameTok & re-evaluations
    
    if (t instanceof ParenTok) {
      return t.type = typeof(((ParenTok) t).ln);
    } else if (t instanceof StrandTok) {
      for (Token c : ((StrandTok) t).tokens) typeof(c);
      return t.type = 'a';
    } else if (t instanceof ArrayTok) {
      for (Token c : ((ArrayTok) t).tokens) typeof(c);
      return t.type = 'a';
    } else if (t instanceof OpTok) {
      OpTok op = (OpTok) t;
      Value b = ((OpTok) t).b;
      if (b==null) {
        String s = op.op;
        switch (s) {
          case "𝕨":
            return t.type = 'A';
          case "𝕘": case "𝕗": case "𝕩": case "𝕤": case "𝕣": case "•":
            return t.type = 'a';
          case "𝔾": case "𝔽": case "𝕏": case "𝕎": case "𝕊": case "⍎":
            return t.type = 'f';
          default: throw new ImplementationError("Undefined built-in "+s, op);
        }
      } else {
        return t.type = b instanceof Fun? 'f' : b instanceof Md1? 'm' : b instanceof Md2? 'd' : 'a';
      }
    } else if (t instanceof LineTok) {
      List<Token> tks = ((LineTok) t).tokens;
      char[] tps = new char[tks.size()];
      if (tps.length == 0) throw new SyntaxError("line with no tokens", t);
      for (int i = 0; i < tks.size(); i++) tps[i] = typeof(tks.get(i));
      char last = tps[tps.length-1];
      if (tps.length == 1) return t.type = last;
      char prev = tps[tps.length-2];
      
      // i hope these guesses are correct..
      if (prev == 'd') { // ends with d[fa], so equivalent to (last == 'm') below
        for (int i = 0; i < tps.length-2; i++) { // must not touch the last [fa] though (and while at it, d neither) 
          char tp = tps[i];
          if (isOperand(tp)) return t.type = 'f';
        }
        return t.type = 'm';
      } else {
        if (last == 'd') return t.type = 'd'; // (_d_←{𝔽𝕘}) should be the only case (+ more variable assignment)
        if (isArr(last)) {
          for (char tp : tps) if (tp=='←' || tp=='↩' || tp=='⇐') return t.type = 'a'; // {x←𝕨} discards the optionality property
          return t.type = last; // not as arg of modifier
        }
        if (last == 'f') return t.type = 'f';
        
        if (last == 'm') { // complicated because (_a←_b←_c) vs (⊢+ ⊢+ +˜)
          for (char tp : tps) {
            if (isOperand(tp)) return t.type = 'f';
          }
          return t.type = 'm';
        }
        if (last == '⇐') return t.type = '\0'; // idk man
      }
      return '\0';
    } else if (t instanceof BasicLines) {
      List<Token> ts = ((BasicLines) t).tokens;
      for (Token c : ts) typeof(c);
      return t.type = ts.get(ts.size()-1).type;
    }
    throw new ImplementationError("can't get type of "+t.getClass().getCanonicalName(), t);
  }
  
  public static byte flags(Token t) {
    if (t.flags != -1) return t.flags;
    if (t instanceof ConstTok || t instanceof NothingTok) return t.flags = 7;
    if (t instanceof ModTok || t instanceof SetTok || t instanceof NameTok || t instanceof ExportTok) return t.flags = 6;
    
    if (t instanceof ParenTok) return t.flags = flags(((ParenTok) t).ln);
    if (t instanceof TokArr) {
      List<? extends Token> ts = ((TokArr) t).tokens;
      if (t instanceof ArrayTok || t instanceof StrandTok 
      ||  t instanceof LineTok && ts.size()==1) {
        t.flags = 7;
      } else t.flags = 6;
      for (Token c : ts) {
        if (c instanceof BlockTok) t.flags&= ~2;
        t.flags&= flags(c);
      }
      return t.flags;
    }
    if (t instanceof OpTok) {
      if (((OpTok) t).op.equals("⍎")) return t.flags = 0;
      if (((OpTok) t).b==null) return t.flags = 6;
      return t.flags = 7;
    }
    throw new ImplementationError("didn't check for "+t.getClass().getSimpleName());
  }
  
  public static void compE(Mut m, Token tk) {
    if (tk instanceof NameTok) {
      String name = ((NameTok) tk).name;
      if (name.charAt(0)=='•' || name.equals("𝕣")) throw new SyntaxError("Cannot export "+name, tk);
      m.export(name);
      return;
    }
    if (tk instanceof StrandTok) {
      for (Token c : ((StrandTok) tk).tokens) compE(m, c);
      return;
    }
    if (tk instanceof ArrayTok) {
      for (Token c : ((ArrayTok) tk).tokens) compE(m, c);
      return;
    }
    if (tk instanceof ParenTok) {
      compE(m, ((ParenTok) tk).ln);
      return;
    }
    if (tk instanceof LineTok) {
      if (((LineTok) tk).tokens.size() == 1) {
        compE(m, ((LineTok) tk).tokens.get(0));
        return;
      }
    }
    throw new SyntaxError("Cannot export "+tk, tk);
  }
  public static void  compM(Mut m, Token tk, boolean create, boolean header) {
    assert tk.type != 0;
    if (tk instanceof NameTok) {
      String name = ((NameTok) tk).name;
      if (create) {
        if (m.vars.containsKey(name)) {
          if (m.p!=null) throw Local.redefine(name, tk);
        } else if (name.charAt(0)!='•') {
          m.nvar(name);
        }
      }
      m.var(tk, name, true);
      return;
    }
    if (tk instanceof StrandTok) {
      List<Token> tks = ((StrandTok) tk).tokens;
      for (Token c : tks) compM(m, c, create, header);
      m.add(tk, ARRM); m.add(tks.size());
      return;
    }
    arraytok: if (tk instanceof ArrayTok) {
      List<Token> tks = ((ArrayTok) tk).tokens;
      boolean dict = false;
      for (Token token : tks) {
        if (!(token instanceof LineTok)) throw new SyntaxError("Didn't expect "+token+" in array literal");
        if (((LineTok) token).tokens.size() == 3) { dict = true; break; }
      }
      if (dict) {
        Value[] nsKeys = new Value[tks.size()];
        for (int i = 0; i < tks.size(); i++) {
          LineTok c = ((LineTok) tks.get(i));
          List<Token> parts = c.tokens;
          if (c.tokens.size() == 3) {
            if (!(parts.get(1) instanceof ExportTok)) break arraytok; // i.e. give generic "cannot be mutated"
          } else if (c.tokens.size()!=1) break arraytok;
          Token name = parts.get(parts.size()-1);
          if (!(name instanceof NameTok)) throw new SyntaxError("Expected name after ⇐ in assigment", name);
          nsKeys[i] = new ChrArr(((NameTok) name).name);
          compM(m, parts.get(0), create, header);
        }
        m.add(tk, NSPM);
        m.add(tks.size());
        m.add(m.addObj(new HArr(nsKeys)));
      } else {
        for (Token c : tks) compM(m, c, create, header);
        m.add(tk, ARRM); m.add(tks.size());
      }
      return;
    }
    if (tk instanceof ParenTok) {
      compM(m, ((ParenTok) tk).ln, create, header);
      return;
    }
    if (tk instanceof LineTok) {
      if (((LineTok) tk).tokens.size() == 1) {
        compM(m, ((LineTok) tk).tokens.get(0), create, header);
        return;
      }
    }
    if (tk instanceof OpTok) {
      String op = ((OpTok) tk).op;
      if (op.equals("•")) {
        m.add(tk, SPEC, STDOUT);
        return;
      }
      int aid = Tokenizer.surrogateOps.indexOf(op);
      if (aid != -1) {
        aid = aid/4*4;
        m.var(tk, Tokenizer.surrogateOps.substring(aid, aid+2), true);
        return;
      }
    }
    if (header && tk instanceof ConstTok) {
      m.push(tk, ((ConstTok) tk).val);
      m.add(tk, VFYM);
      return;
    }
    throw new SyntaxError(tk.toRepr()+" cannot be mutated", tk);
  }
  
  public static void compO(Mut m, Token tk) { // assumes tk has been typechecked
    if ((tk.flags&1)!=0) { m.push(tk, constFold(tk)); return; } // ConstTok, NothingTok
    if (tk instanceof ParenTok) {
      compO(m, ((ParenTok) tk).ln);
      return;
    }
    if (tk instanceof LineTok) {
      List<Token> ts = ((LineTok) tk).tokens;
      if (ts.size() == 0) return;
      if (ts.size() == 1) { compO(m, ts.get(0)); return; }
      int i = ts.size()-1;
      
      DQ tps = new DQ();
      Res t0 = new ResTk(ts.get(i));
      tps.addFirst(t0);
      i--;
      final boolean train = (t0.type!='a' && t0.type!='A')  ||  (ts.size()>=2 && ts.get(i).type=='d');
      
      
      if (Main.debug) {
        printlvl("parsing "+tk.source());
        Main.printlvl++;
      }
      
      while (i>=0) {
        Res c = new ResTk(ts.get(i));
        tps.addFirst(c);
        collect(tps, train, false);
        i--;
      }
      collect(tps, train, true);
      if (Main.debug) Main.printlvl--;
      
      if (tps.size()!=1) {
        Token t = null;
        for (int j = 0; j < tps.size(); j++) {
          Res tp = tps.get(j);
          if (tp.lastTok() != null) t = tp.lastTok();
          if (j>=1 && norm(tps.get(j).type)=='a' && norm(tps.get(j-1).type)=='a') throw new SyntaxError("failed to parse expression (found two adjacent values, missing `‿`?)", tps.get(j-1).lastTok());
          if (j>=2 && "↩←⇐".indexOf(tps.get(j-1).type)!=-1 && norm(tps.get(j).type)!=norm(tps.get(j-2).type)) throw new SyntaxError(tps.get(j-1)+": cannot assign with different types", tps.get(j-1).lastTok());
        }
        throw new SyntaxError("failed to parse expression", t);
      }
      assert tps.get(0).type == tk.type : tps.get(0).type + "≠" + tk.type;
      tps.get(0).add(m);
      return;
    }
    if (tk instanceof OpTok) {
      OpTok op = (OpTok) tk;
      Callable b = op.b;
      if (b != null) {
        m.push(tk, b);
        return;
      }
      
      String s = op.op;
      switch (s) {
        case "𝕨": case "𝕘": case "𝕗": case "𝕩": case "𝕤": case "𝕣":
          m.var(tk, s, false);
          return;
        case "𝕎": case "𝔾": case "𝔽": case "𝕏": case "𝕊":
          m.var(tk, new String(new char[]{55349, (char) (s.charAt(1)+26)}), false); // lowercase
          return;
        case "⍎": m.add(op, SPEC, EVAL ); return;
        case "•": m.add(op, SPEC, STDIN); return;
        default: throw new ImplementationError("Undefined unknown built-in "+s, op);
      }
    }
    if (tk instanceof NameTok) {
      String n = ((NameTok) tk).name;
      if (((NameTok) tk).val != null) {
        int rel = Scope.rel(n);
        if (rel<=4) {
          m.push(tk, ((NameTok) tk).val);
        } else if (rel==5) {
          m.var(tk, n, false);
          m.push(tk, ((NameTok) tk).val);
          m.add(OP1D);
        } else throw new IllegalStateException();
      }
      else m.var(tk, n, false);
      return;
    }
    if (tk instanceof StrandTok) { // +TODO (+↓) check for type A
      if (Main.debug) { printlvl("parsing "+tk.source()); Main.printlvl++; }
      List<Token> tks = ((StrandTok) tk).tokens;
      for (Token c : tks) compO(m, c);
      if (Main.debug) Main.printlvl--;
      
      m.add(tk, ARRO); m.add(tks.size());
      return;
    }
    if (tk instanceof ArrayTok) {
      if (Main.debug) { printlvl("parsing "+tk.source()); Main.printlvl++; }
      List<Token> tks = ((ArrayTok) tk).tokens;
      for (Token c : tks) compO(m, c);
      if (Main.debug) Main.printlvl--;
      
      m.add(tk, ARRO); m.add(tks.size());
      return;
    }
    if (tk instanceof SetTok || tk instanceof ModTok || tk instanceof ExportTok || tk instanceof StranderTok || tk instanceof ColonTok || tk instanceof SemiTok || tk instanceof DotTok) {
      throw new SyntaxError("Standalone `"+tk.source()+"`");
    }
    if (tk instanceof BlockTok) {
      m.push((BlockTok) tk);
      return;
    }
    throw new ImplementationError("can't compile "+tk.getClass());
  }
  
  public static char norm(char x) {
    return x=='A'? 'a' : x;
  }
  
  public static Value constFold(Token t) {
    assert (t.flags&1)!=0 && t.flags!=-1;
    if (t instanceof ConstTok) return ((ConstTok) t).val;
    if (t instanceof ParenTok) return constFold(((ParenTok) t).ln);
    if (t instanceof LineTok) {
      List<Token> ts = ((LineTok) t).tokens;
      assert ts.size() == 1;
      return constFold(ts.get(0));
    }
    if (t instanceof StrandTok) {
      List<Token> ts = ((StrandTok) t).tokens;
      Value[] ps = new Value[ts.size()];
      for (int i = 0; i < ps.length; i++) ps[i] = constFold(ts.get(i));
      return Arr.create(ps);
    }
    if (t instanceof ArrayTok) {
      List<Token> ts = ((ArrayTok) t).tokens;
      Value[] ps = new Value[ts.size()];
      for (int i = 0; i < ps.length; i++) ps[i] = constFold(ts.get(i));
      return Arr.create(ps);
    }
    if (t instanceof OpTok) {
      Callable b = ((OpTok) t).b;
      if (b == null) throw new ImplementationError(t.source());
      return b;
    }
    if (t instanceof NothingTok) return ((NothingTok) t).val;
    throw new ImplementationError("couldn't constant fold "+t.getClass().getSimpleName());
  }
  
  public static Callable builtin(char c, OpTok t) {
    switch (c) {
      // fns
      // case '⍲': return new NandBuiltin(sc);
      // case '⍱': return new NorBuiltin(sc);
      // case '⊥': return new UTackBuiltin();
      // case '⊤': return new DTackBuiltin();
      // case '!': return new ExclBuiltin();
      
      // case '?': return new RandBuiltin(sc);
      // case '⍪': return new CommaBarBuiltin();
      
      // case '…': return new EllipsisBuiltin();
      // case '⍮': return new SemiUBBuiltin();
      // case '⍧': return new LShoeStileBuiltin();
      // case '%': return new MergeBuiltin();
  
  
  
  
      case '⍕': return new FormatBuiltin();
      case '!': return new AssertBuiltin();
      case '+': return new PlusBuiltin();
      case '-': return new MinusBuiltin();
      case '×': return new MulBuiltin();
      case '÷': return new DivBuiltin();
      case '⋆':
      case '*': return new StarBuiltin();
      case '|': return new StileBuiltin();
      case '∧': return new AndBuiltin();
      case '∨': return new OrBuiltin();
      case '⌈': return new CeilingBuiltin();
      case '⌊': return new FloorBuiltin();
      case '√': return new RootBuiltin();
      case '¬': return new NotBuiltin();
      
      
      case '⊢': return new RTackBuiltin();
      case '⊣': return new LTackBuiltin();
      
      case '⥊': return new ShapeBuiltin();
      case '↑': return new UpArrowBuiltin();
      case '↓': return new DownArrowBuiltin();
      case '∾': return new JoinBuiltin();
      case '≍': return new LaminateBuiltin();
      case '⍉': return new TransposeBuiltin();
      case '⌽': return new ReverseBuiltin();
      case '»': return new ShBBuiltin();
      case '«': return new ShABuiltin();
      
      case '/': return new SlashBuiltin();
      case '⊏': return new LBoxBuiltin();
      case '⊔': return new GroupBuiltin();
      case '⊑': return new LBoxUBBuiltin();
      case '⊐': return new RBoxBuiltin();
      case '⊒': return new RBoxUBBuiltin();
      case '↕': return new UDBuiltin();
      case '∊': return new EpsBuiltin();
      case '⍷': return new FindBuiltin();
      case '⍋': return new GradeUpBuiltin();
      case '⍒': return new GradeDownBuiltin();
      case '≢': return new TallyBuiltin();
      case '≡': return new MatchBuiltin();
      
      
      
      // comparisons
      case '<': return new LTBuiltin();
      case '≤': return new LEBuiltin();
      case '=': return new EQBuiltin();
      case '≥': return new GEBuiltin();
      case '>': return new GTBuiltin();
      case '≠': return new NEBuiltin();
      
      // 1-modifiers
      case '´': return new FoldBuiltin();
      case '˝': return new InsertBuiltin();
      case '`': return new ScanBuiltin();
      case '¨': return new EachBuiltin();
      case '˜': return new SelfieBuiltin();
      case '˙': return new ConstBultin();
      case '⌜': return new TableBuiltin();
      case '⁼': return new InvBuiltin();
      case '˘': return new CellBuiltin();
      // case '⍩':
      // case 'ᐵ': return new EachLeft();
      // case 'ᑈ': return new EachRight();
      
      // 2-modifiers
      // case '.': return new DotBuiltin();
      // case '⍡': return new CRepeatBuiltin(sc);
      case '○': return new OverBuiltin();
      case '∘': return new AtopBuiltin();
      case '⊸': return new BeforeBuiltin();
      case '⟜': return new AfterBuiltin();
      case '⌾': return new UnderBuiltin();
      case '⍟': return new RepeatBuiltin();
      case '⚇': return new DepthBuiltin();
      case '⊘': return new AmbivalentBuiltin();
      case '◶': return new CondBuiltin();
      case '⎉': return new NCellBuiltin();
      case '⎊': return new CatchBuiltin();
      
      
      // case '@': return new AtBuiltin(sc);
      // case '⍬': return new DoubleArr(DoubleArr.EMPTY);
  
  
      case '⍎': case '•': // the lone double-struck
      case 55349: // double-struck surrogate pair
        return null;
  
      default: throw new SyntaxError("Unknown token `"+t.toRepr()+"` (\\u"+Tk2.hex4(c)+")", t);
    }
  }
  
  /*
  static final class DQ extends LinkedList<Res> {
    
  }
  /*/
  static final class DQ { // double-ended queue
    // static final Object[] none = new Object[1];
    // Object[] es = none;
    private Res[] es = new Res[16];
    private int s, e; // s - 1st elem; e - after last elem (but still a valid index in es)
    private int sz;
    int size() {
      return sz;
    }
    Res get(int i) {
      i+= s;
      if (i>=es.length) return es[i-es.length];
      return es[i];
    }
    Res getL(int i) {
      i = e-i-1;
      if (i>=0) return es[i];
      return es[i+es.length];
    }
    Res peekFirst() {
      return es[s];
    }
    Res peekLast() {
      return e==0? es[es.length-1] : es[e-1];
    }
    
    
    
    Res remove(int is) {
      int ai = is+s; if (ai>=es.length) ai-= es.length;
      Res ret = es[ai];
      if (is < sz/2) {
        if (ai >= s) {
          System.arraycopy(es, s, es, s+1, is);
        } else {
          System.arraycopy(es, 0, es, 1, ai);
          es[0] = es[es.length-1];
          System.arraycopy(es, s, es, s+1, is-ai-1);
        }
        s++;
        if (s >= es.length) s = 0;
      } else {
        int ie = sz-is-1;
        int ne = e-1; if (ne<0) ne = es.length-1;
        if (ai <= ne) {
          System.arraycopy(es, ai+1, es, ai, ie);
        } else {
          System.arraycopy(es, ai+1, es, ai, es.length-ai-1);
          es[es.length-1] = es[0];
          System.arraycopy(es, 1, es, 0, e-1);
        }
        e = ne;
      }
      sz--;
      // System.out.println("rm @"+ri+": -- "+ret);
      // System.out.println(this);
      return ret;
    }
    
    
    Res removeLast() {
      if (e > 0) e--;
      else e = es.length-1;
      sz--;
      return es[e];
    }
    void addFirst(Res t) {
      if (++sz == es.length) dc();
      if (s > 0) s--;
      else s = es.length-1;
      es[s] = t;
    }
    void addLast(Res t) {
      if (++sz == es.length) dc();
      es[e] = t;
      if(e+1 >= es.length) e = 0;
      else e++;
    }
    void add(int i, Res t) {
      // System.out.println(this);
      // System.out.println("add "+t+"@"+i+":     "+hashCode());
      if (++sz == es.length) dc();
      
      int ai;
      if (i>sz/2) {
        ai = i+s;
        if(ai>=es.length) ai-= es.length; // pos of new item
        if (ai<=e) {
          System.arraycopy(es, ai, es, ai+1, e-ai);
        } else {
          System.arraycopy(es, 0, es, 1, e);
          es[0] = es[es.length-1];
          System.arraycopy(es, ai, es, ai+1, es.length-ai-1);
        }
        e++; if (e==es.length) e=0;
      } else {
        ai = i+s-1;
        if(ai<0)ai=es.length-1; if(ai>=es.length) ai-=es.length; // pos of new item
        if (i==0) {
        } else if (s==0) {
          es[es.length-1] = es[0];
          System.arraycopy(es, 1, es, 0, ai);
        } else if (ai==i+s-1) {
          System.arraycopy(es, s, es, s-1, ai-s+1);
        } else {
          System.arraycopy(es, s, es, s-1, es.length-s);
          es[es.length-1] = es[0];
          System.arraycopy(es, 1, es, 0, ai);
        }
        s--; if(s<0) s=es.length-1;
      }
      es[ai] = t;
      // System.out.println(this);
    }
    void dc() {
      int ol = es.length;
      Res[] nes = Arrays.copyOf(es, ol*2);
      System.arraycopy(es, 0, nes, ol, e);
      if (e<s) e+= ol;
      es = nes;
      // System.out.println("DC");
      // System.out.println(this);
    }
    public String toString() {
      StringBuilder b = new StringBuilder("[");
      for (int i = s; i != e; i=++i==es.length?0:i) {
        if (i!=s) b.append(", ");
        b.append(es[i]);
      }
      b.append("]");
      // b.append(" ").append(s).append("-").append(e).append(" sz=").append(sz).append(" -- ").append(Arrays.toString(es));
      return b.toString();
    }
  } //*/
}