package APL.types.functions.builtins.fns;

import APL.types.Value;
import APL.types.arrs.EmptyArr;
import APL.types.functions.builtins.FnBuiltin;
import APL.types.functions.builtins.fns2.JoinBuiltin;

public class CommaBarBuiltin extends FnBuiltin {
  @Override public String repr() {
    return "⍪";
  }
  
  
  
  public Value call(Value x) {
    if (x.rank==1 && x.shape[0]==0) return new EmptyArr(new int[]{0, 1}, x.safePrototype());
    if (x.rank==0) return x.ofShape(new int[]{1, 1});
    int[] nsh = new int[]{x.shape[0], x.ia/x.shape[0]};
    return x.ofShape(nsh);
  }
  
  public Value call(Value w, Value x) {
    return JoinBuiltin.on(w, x, this);
  }
}