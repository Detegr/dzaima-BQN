package BQN.types.callable.builtins.md1;

import BQN.tools.FmtInfo;
import BQN.types.*;
import BQN.types.arrs.SingleItemArr;
import BQN.types.callable.Md1Derv;
import BQN.types.callable.builtins.Md1Builtin;

public class EachLeft extends Md1Builtin {
  public String ln(FmtInfo f) { return "ᐵ"; }
  
  public Value call(Value f, Value w, Value x, Md1Derv derv) {
    Value[] n = new Value[w.ia];
    for (int i = 0; i < n.length; i++) {
      n[i] = f.call(w.get(i), x);
    }
    return Arr.create(n, w.shape);
  }
  
  public Value underW(Value f, Value o, Value w, Value x, Md1Derv derv) {
    return EachBuiltin.underW(f, o, w, SingleItemArr.r0(x), this);
  }
}