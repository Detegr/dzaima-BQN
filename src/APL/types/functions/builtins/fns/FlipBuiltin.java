package APL.types.functions.builtins.fns;

import APL.types.*;
import APL.types.dimensions.DimMFn;
import APL.types.functions.Builtin;

public class FlipBuiltin extends Builtin implements DimMFn {
  @Override public String repr() {
    return "⊖";
  }
  
  
  @Override
  public Obj call(Value w, int dim) {
    return ((Arr) w).reverseOn(dim);
  }
  public Obj call(Value w) {
    if (w instanceof Primitive) return w;
    return ((Arr) w).reverseOn(0);
  }
}