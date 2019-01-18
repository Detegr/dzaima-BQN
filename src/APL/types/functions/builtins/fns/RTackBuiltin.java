package APL.types.functions.builtins.fns;

import APL.types.*;
import APL.types.functions.Builtin;

public class RTackBuiltin extends Builtin {
  public RTackBuiltin () {
    super("⊢");
  }
  
  public Obj call(Value w) { return w; }
  public Obj call(Value a, Value w) { return w; }
}