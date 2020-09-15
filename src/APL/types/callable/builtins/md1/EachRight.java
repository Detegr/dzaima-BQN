package APL.types.callable.builtins.md1;

import APL.types.*;
import APL.types.arrs.SingleItemArr;
import APL.types.callable.Md1Derv;
import APL.types.callable.builtins.Md1Builtin;

public class EachRight extends Md1Builtin {
  @Override public String repr() {
    return "ᑈ";
  }
  
  public Value call(Value f, Value w, Value x, Md1Derv derv) {
    Value[] n = new Value[x.ia];
    for (int i = 0; i < n.length; i++) {
      n[i] = f.call(w, x.get(i)).squeeze();
    }
    return Arr.create(n, x.shape);
  }
  
  public Value underW(Value f, Value o, Value w, Value x, Md1Derv derv) {
    return EachBuiltin.underW(f, o, SingleItemArr.r0(w), x, this);
  }
}