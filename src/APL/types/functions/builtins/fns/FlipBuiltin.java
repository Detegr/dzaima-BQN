package APL.types.functions.builtins.fns;

import APL.errors.DomainError;
import APL.types.*;
import APL.types.functions.Builtin;

public class FlipBuiltin extends Builtin {
  public FlipBuiltin() {
    super("⊖");
    valid = 0x001;
  }
  
  public Obj call(Value w) {
    return ((Arr) w).reverseOn(0);
  }
}