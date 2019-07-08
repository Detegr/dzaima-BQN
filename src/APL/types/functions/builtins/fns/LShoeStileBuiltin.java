package APL.types.functions.builtins.fns;

import APL.errors.RankError;
import APL.types.*;
import APL.types.arrs.*;
import APL.types.functions.Builtin;

import java.util.*;

public class LShoeStileBuiltin extends Builtin {
  
  @Override public Obj call(Value a, Value w) {
    HashMap<Value, Integer> counts = new HashMap<>();
    for (Value ca : a) counts.put(ca, 0);
    for (Value cw : w) {
      Integer pv = counts.get(cw);
      if (pv != null) counts.put(cw, pv + 1);
    }
    double[] res = new double[a.ia];
    int i = 0;
    for (Value ca : a) {
      res[i] = counts.get(ca);
      i++;
    }
    return new DoubleArr(res, a.shape);
  }
  
  @Override public Obj call(Value w) {
    if (w.rank != 1) throw new RankError("rank of ⍵ should be 1", w);
    HashSet<Value> encountered = new HashSet<>();
    BitArr.BC res = new BitArr.BC(w.shape);
    for (Value cv : w) {
      if (encountered.contains(cv)) res.add(false);
      else {
        encountered.add(cv);
        res.add(true);
      }
    }
    return res.finish();
  }
  
  @Override public String repr() {
    return "⍧";
  }
}