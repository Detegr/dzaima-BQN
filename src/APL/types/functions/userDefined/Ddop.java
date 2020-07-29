package APL.types.functions.userDefined;

import APL.*;
import APL.tokenizer.types.DfnTok;
import APL.types.*;
import APL.types.functions.*;



public class Ddop extends Dop {
  public final DfnTok code;
  public final Scope sc;
  
  public Ddop(DfnTok t, Scope sc) {
    this.sc = sc;
    code = t;
  }
  
  public Fun derive(Value f, Value g) { // ···𝕣𝕗𝕘
    if (!code.immediate) return super.derive(f, g);
    Main.printdbg("ddop immediate call", f, g);
    Scope nsc = new Scope(sc);
    int s = code.find(nsc, null, f, g, null, this);
    nsc.set(0, this);
    nsc.set(1, f);
    nsc.set(2, g);
    return code.comp.exec(nsc, s).asFun();
  }
  
  public Mop derive(Value g) {
    if (!code.immediate) return super.derive(g);
    Main.printdbg("ddop immediate half-derive", g);
    return new HalfDerivedDdop(g, this);
  }
  
  public static class HalfDerivedDdop extends Mop {
    public final Value g;
    public final Ddop op;
    
    public HalfDerivedDdop(Value g, Ddop op) {
      this.g = g;
      this.op = op;
    }
    
    public Fun derive(Value f) {
      return op.derive(f, g);
    }
    
    public String repr() {
      String gs = g.oneliner();
      if (!(g instanceof Arr) && gs.length() != 1) gs = "("+gs+")";
      return op.repr()+gs;
    }
  }
  
  public Value call(Value f, Value g, Value x, DerivedDop derv) { // 𝕊𝕩𝕨𝕣𝕗𝕘
    Main.printdbg("ddop call", x);
    Scope nsc = new Scope(sc);
    int s = code.find(nsc, null, f, g, x, this);
    nsc.set(0, derv);
    nsc.set(1, x);
    nsc.set(2, Nothing.inst);
    nsc.set(3, this);
    nsc.set(4, f);
    nsc.set(5, g);
    return code.comp.exec(nsc, s);
  }
  
  public Value call(Value f, Value g, Value w, Value x, DerivedDop derv) { // 𝕊𝕩𝕨𝕣𝕗𝕘
    Main.printdbg("ddop call", w, x);
    Scope nsc = new Scope(sc);
    int s = code.find(nsc, w, f, g, x, this);
    nsc.set(0, derv);
    nsc.set(1, x);
    nsc.set(2, w);
    nsc.set(3, this);
    nsc.set(4, f);
    nsc.set(5, g);
    return code.comp.exec(nsc, s);
  }
  
  public String repr() {
    return code.toRepr();
  }
}