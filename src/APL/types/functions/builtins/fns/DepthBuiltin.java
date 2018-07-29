package APL.types.functions.builtins.fns;

import APL.types.*;
import APL.types.functions.Builtin;

public class DepthBuiltin extends Builtin {
  public DepthBuiltin() {
    super("≡");
    valid = 0x011;
  }
  public Obj call(Value w) {
    int depth = 0;
    while (!w.primitive()) {
      w = w.first();
      depth++;
    }
    return new Num(depth);
  }
  public Obj call(Value a, Value w) {
    return null;
  }
}