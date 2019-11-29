package APL.types.functions.builtins.fns;

import APL.types.*;
import APL.types.functions.Builtin;

public class TallyBuiltin extends Builtin {
  @Override public String repr() {
    return "≢";
  }
  
  
  public Obj call(Value w) {
    if (w.rank==0) return Num.ONE;
    return Num.of(w.shape[0]);
  }
  public Obj call(Value a, Value w) {
    return a.equals(w)? Num.ZERO : Num.ONE;
  }
}