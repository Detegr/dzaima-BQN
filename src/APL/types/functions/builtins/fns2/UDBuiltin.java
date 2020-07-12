package APL.types.functions.builtins.fns2;

import APL.*;
import APL.errors.*;
import APL.types.*;
import APL.types.arrs.*;
import APL.types.functions.Builtin;

public class UDBuiltin extends Builtin {
  public String repr() {
    return "↕";
  }
  
  public Value call(Value x) {
    return on(x, this);
  }
  public static Value on(Value x, Callable blame) {
    if (x instanceof Primitive) {
      if (x instanceof Num) {
        int[] res = new int[x.asInt()];
        for (int i = 0; i < res.length; i++) res[i] = i;
        return new IntArr(res);
      } else if (x instanceof BigValue) {
        Value[] res = new Value[x.asInt()];
        for (int i = 0; i < res.length; i++) {
          res[i] = new BigValue(i);
        }
        return new HArr(res);
      }
    }
    if (Main.vind) { // •VI←1
      if (x.rank != 1) throw new DomainError(blame+": 𝕩 must be a vector ("+Main.formatAPL(x.shape)+" ≡ ≢𝕩)", blame, x);
      int dim = x.ia;
      int[] shape = x.asIntVec();
      int prod = Arr.prod(shape);
      Value[] res = new Value[dim];
      int blockSize = 1;
      for (int i = dim-1; i >= 0; i--) {
        double[] ds = new double[prod];
        int len = shape[i];
        int csz = 0;
        double val = 0;
        for (int k = 0; k < len; k++) {
          for (int l = 0; l < blockSize; l++) ds[csz++] = val;
          val++;
        }
        int j = csz;
        while (j < prod) {
          System.arraycopy(ds, 0, ds, j, csz);
          j+= csz;
        }
        res[i] = new DoubleArr(ds, shape);
        blockSize*= shape[i];
      }
      return new HArr(res);
    } else { // •VI←0
      int[] shape = x.asIntVec();
      int ia = Arr.prod(shape);
      Value[] arr = new Value[ia];
      int i = 0;
      for (int[] c : new Indexer(shape)) {
        arr[i] = new IntArr(c.clone());
        i++;
      }
      return new HArr(arr, shape);
    }
  }
  
  public Value call(Value w, Value x) {
    throw new NYIError("dyadic ↕", this, w);
  }
}