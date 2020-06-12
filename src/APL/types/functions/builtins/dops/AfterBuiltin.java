package APL.types.functions.builtins.dops;

import APL.types.*;
import APL.types.functions.*;

public class AfterBuiltin extends Dop {
  public String repr() {
    return "⟜";
  }
  
  public Value call(Obj aa, Obj ww, Value w, DerivedDop derv) {
    return call(aa, ww, w, w, derv);
  }
  
  public Value call(Obj aa, Obj ww, Value a, Value w, DerivedDop derv) {
    return aa.asFun().call(a, ww.asFun().call(w));
  }
  
  // +TODO inverses
}
