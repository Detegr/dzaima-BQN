package APL.types.functions.builtins.dops;

import APL.errors.*;
import APL.types.*;
import APL.types.functions.*;
import APL.types.functions.builtins.fns2.MatchBuiltin;

public class DepthBuiltin extends Dop {
  public String repr() {
    return "⚇";
  }
  
  public Value call(Value f, Value g, Value x, DerivedDop derv) {
    int d = g.asInt();
    if (d < 0) throw new NYIError("negative 𝕘 for ⚇", this, g);
    return on(f, d, x, derv);
  }
  
  public static Value on(Value f, int d, Value w, Fun blame) {
    int ld = MatchBuiltin.lazy(w);
    if (ld==d || ld <= -d) {
      int fd = MatchBuiltin.full(w);
      if (d>0 && d!=fd) throw new DomainError(blame+": can't match a depth "+fd+" array", blame, w);
      if (d <= fd) {
        return f.call(w);
      }
    }
    if (d>0 && ld < d) throw new DomainError(blame+": can't match a depth "+MatchBuiltin.full(w)+" array", blame, w);
    Value[] res = new Value[w.ia];
    for (int i = 0; i < res.length; i++) {
      res[i] = on(f, d, w.get(i), blame);
    }
    return Arr.create(res, w.shape);
  }
}