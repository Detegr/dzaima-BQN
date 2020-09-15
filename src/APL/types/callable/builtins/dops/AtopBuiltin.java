package APL.types.callable.builtins.dops;

import APL.types.Value;
import APL.types.callable.Md2Derv;
import APL.types.callable.builtins.Md2Builtin;

public class AtopBuiltin extends Md2Builtin {
  public String repr() {
    return "∘";
  }
  
  public Value call(Value f, Value g, Value x, Md2Derv derv) {
    return f.call(g.call(x));
  }
  public Value call(Value f, Value g, Value w, Value x, Md2Derv derv) {
    return f.call(g.call(w, x));
  }
  
  // +TODO inverses (from OldJotDiaeresisBuiltin)
}