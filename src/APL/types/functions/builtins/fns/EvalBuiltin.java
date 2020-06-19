package APL.types.functions.builtins.fns;

import APL.*;
import APL.types.Value;
import APL.types.functions.Builtin;

public class EvalBuiltin extends Builtin {
  @Override public String repr() {
    return "⍎";
  }
  
  public EvalBuiltin(Scope sc) {
    super(sc);
  }
  
  public Value call(Value w) {
    return Main.exec(w.asString(), sc);
  }
}