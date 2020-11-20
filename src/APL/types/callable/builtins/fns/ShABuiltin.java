package APL.types.callable.builtins.fns;

import APL.errors.RankError;
import APL.tools.*;
import APL.types.Value;
import APL.types.callable.builtins.FnBuiltin;
import APL.types.callable.builtins.md1.CellBuiltin;

public class ShABuiltin extends FnBuiltin {
  public String ln(FmtInfo f) { return "«"; }
  
  public Value call(Value w, Value x) {
    if (x.scalar()) throw new RankError("«: 𝕩 cannot be scalar", this);
    if (w.r() > x.r()) throw new RankError("«: rank of 𝕨 cannot exceed =𝕩", this);
    JoinBuiltin.check(w, x, this);
    MutVal res = new MutVal(x.shape, x);
    int mid = Math.max(0, x.ia-w.ia);
    res.copy(x, x.ia-mid, 0, mid);
    res.copy(w, 0, mid, x.ia-mid);
    return res.get();
  }
  
  public Value call(Value x) {
    if (x.scalar()) throw new RankError("«: argument cannot be scalar", this);
    if (x.ia==0) return x;
    MutVal res = new MutVal(x.shape, x);
    int csz = CellBuiltin.csz(x);
    res.copy(x, csz, 0, x.ia-csz);
    res.fill(x.prototype(), x.ia-csz, x.ia);
    return res.get();
  }
}
