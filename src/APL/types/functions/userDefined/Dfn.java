package APL.types.functions.userDefined;

import APL.*;
import APL.tokenizer.types.DfnTok;
import APL.types.*;



public class Dfn extends Fun {
  public final DfnTok code;
  public final Scope sc;
  
  public Dfn(DfnTok t, Scope sc) {
    this.sc = sc;
    code = t;
  }
  
  public Value call(Value x) {
    Main.printdbg("dfn call", x);
    Scope nsc = new Scope(sc);
    int s = code.start(nsc, null, null, null, x, this);
    nsc.set("𝕨", Nothing.inst);
    nsc.set("𝕩", x);
    nsc.set("𝕤", this);
    return code.comp.exec(nsc, s);
  }
  
  public Value call(Value w, Value x) {
    Main.printdbg("dfn call", w, x);
    Scope nsc = new Scope(sc);
    int s = code.start(nsc, w, null, null, x, this);
    nsc.set("𝕨", w);
    nsc.set("𝕩", x);
    nsc.set("𝕤", this);
    return code.comp.exec(nsc, s);
  }
  
  public String repr() {
    return code.toRepr();
  }
  
  public String name() { return "dfn"; }
}