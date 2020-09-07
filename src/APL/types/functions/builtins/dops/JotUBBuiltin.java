package APL.types.functions.builtins.dops;

import APL.types.Value;
import APL.types.functions.*;

public class JotUBBuiltin extends Dop {
  @Override public String repr() {
    return "⍛";
  }
  
  public Value call(Value f, Value g, Value w, Value x, DerivedDop derv) {
    return g.call(f.call(w), x);
  }
  
  public Value callInvX(Value f, Value g, Value w, Value x) {
    return g.callInvX(f.call(w), x);
  }
  
  public Value callInvW(Value f, Value g, Value w, Value x) {
    return f.callInv(g.callInvW(w, x));
  }
}