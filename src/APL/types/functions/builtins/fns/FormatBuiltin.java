package APL.types.functions.builtins.fns;

import APL.*;
import APL.errors.NYIError;
import APL.types.*;
import APL.types.arrs.ChrArr;
import APL.types.functions.Builtin;

public class FormatBuiltin extends Builtin {
  @Override public String repr() {
    return "⍕";
  }
  
  
  
  public Obj call(Value w) {
    if (w.rank == 1) {
      w = w.squeeze();
      if (w instanceof ChrArr) return Main.toAPL(w.asString());
    }
    return Main.toAPL(w.toString());
  }
  
//  public Obj call(Value a, Value w) { TODO
//
//  }
}