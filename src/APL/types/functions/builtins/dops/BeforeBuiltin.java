package APL.types.functions.builtins.dops;

import APL.types.*;
import APL.types.functions.*;

public class BeforeBuiltin extends Dop {
  public String repr() {
    return "⊸";
  }
  
  public Value call(Obj aa, Obj ww, Value w, DerivedDop derv) {
    return call(aa, ww, w, w, derv);
  }
  
  public Value call(Obj aa, Obj ww, Value a, Value w, DerivedDop derv) {
    return ww.asFun().call(aa.asFun().call(a), w);
  }
  
  // +TODO inverses
}
