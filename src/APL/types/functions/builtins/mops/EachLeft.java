package APL.types.functions.builtins.mops;

import APL.types.*;
import APL.types.arrs.Rank0Arr;
import APL.types.functions.*;

public class EachLeft extends Mop {
  @Override public String repr() {
    return "ᐵ";
  }
  
  public Value call(Value f, Value w, Value x, DerivedMop derv) {
    Fun ff = f.asFun();
    Value[] n = new Value[w.ia];
    for (int i = 0; i < n.length; i++) {
      n[i] = ff.call(w.get(i), x).squeeze();
    }
    return Arr.create(n, w.shape);
  }
  
  public Value underW(Value aa, Value o, Value w, Value x, DerivedMop derv) {
    return EachBuiltin.underW(aa.asFun(), o, w, new Rank0Arr(x), this);
  }
}