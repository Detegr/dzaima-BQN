package APL.types.functions.builtins.fns;

import APL.errors.DomainError;
import APL.types.Num;
import APL.types.Obj;
import APL.types.Value;
import APL.types.functions.Builtin;

public class UTackBuiltin extends Builtin {
  static UTackBuiltin copy = new UTackBuiltin();
  public UTackBuiltin() {
    super("⊥");
    valid = 0x011;
  }
  
  public Obj call(Value w) {
    return call(Num.TWO, w);
  }
  
  public Obj callInv(Value w) {
    return DTackBuiltin.copy.call(w);
  }
  public Obj callInvW(Value a, Value w) {
    return DTackBuiltin.copy.call(a, w);
  }
  
  public Obj call(Value a, Value w) {
    Num res = Num.ZERO;
    Num base = ((Num)a);
    if (w.rank != 1) throw new DomainError("⊥ on rank "+w.rank, this, w);
    for (int i = 0; i < w.ia; i++) {
      res = res.times(base).plus((Num) w.arr[i]);
    }
    return res;
  }
}