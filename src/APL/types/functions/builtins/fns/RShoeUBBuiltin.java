package APL.types.functions.builtins.fns;

import APL.*;
import APL.types.*;
import APL.types.arrs.*;
import APL.types.functions.Builtin;

public class RShoeUBBuiltin extends Builtin {
  @Override public String repr() {
    return "⊇";
  }
  
  public RShoeUBBuiltin(Scope sc) {
    super(sc);
  }
  
  public Obj call(Value w) {
    if (w.rank == 0) return w.get(0);
    return w;
  }
  
  public Obj call(Value a, Value w) {
//    return new EachBuiltin().derive(new SquadBuiltin(sc)).call(a, (Value)new LShoeBuiltin().call(w));
    if (a.ia == 0) return EmptyArr.SHAPE0;
    if (w instanceof APLMap) {
      Value[] res = new Value[a.ia];
      APLMap map = (APLMap) w;
      Value[] vs = a.values();
      for (int i = 0; i < a.ia; i++) {
        res[i] = (Value) map.getRaw(vs[i].asString());
      }
      return Arr.create(res, a.shape);
    }
    if (a instanceof Primitive) return w.get((int) a.asDouble());
    if (w.quickDoubleArr()) {
      double[] wv = w.asDoubleArr();
      double[] res = new double[a.ia];
      if (a.quickDoubleArr()) {
        double[] da = a.asDoubleArr();
        for (int i = 0; i < a.ia; i++) {
          res[i] = wv[Indexer.fromShape(w.shape, new int[]{(int)da[i]}, sc.IO)];
        }
      } else {
        for (int i = 0; i < a.ia; i++) {
          res[i] = wv[Indexer.fromShape(w.shape, a.get(i).asIntVec(), sc.IO)];
        }
      }
      return new DoubleArr(res, a.shape);
    }
    Value[] res = new Value[a.ia];
    for (int i = 0; i < a.ia; i++) {
      res[i] = w.at(a.get(i).asIntVec(), sc.IO);
    }
    return Arr.create(res, a.shape);
  }
}