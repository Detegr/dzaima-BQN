package APL.types.functions.builtins.fns;

import APL.errors.DomainError;
import APL.types.*;
import APL.types.arrs.*;
import APL.types.functions.Builtin;

public class SemiUBBuiltin extends Builtin {
  @Override public String repr() {
    return "⍮";
  }
  
  
  
  @Override
  public Value call(Value w) {
    return new Shape1Arr(w);
  }
  
  public Value call(Value a, Value w) {
    if (a instanceof Num && w instanceof Num) {
      return new DoubleArr(new double[]{((Num) a).num, ((Num) w).num});
    }
    if (a instanceof Char && w instanceof Char) {
      return new ChrArr(((Char) a).chr +""+ ((Char) w).chr);
    }
    return Arr.create(new Value[]{a, w});
  }
  
  public Value callInv(Value w) {
    if (w.rank!=1 || w.shape[0]!=1) throw new DomainError("⍮⁼: argument must be a length 1 vector", this, w);
    return w.first();
  }
  
  public Value callInvW(Value a, Value w) {
    if (w.rank!=1 || w.shape[0]!=2) throw new DomainError("⍮⁼: 𝕩 must be a length 2 vector", this, w);
    if (!w.get(0).equals(a)) throw new DomainError("⍮⁼: expected 𝕨≡⊃𝕩", this, w);
    return w.get(1);
  }
  public Value callInvA(Value a, Value w) {
    if (a.rank!=1 || a.shape[0]!=2) throw new DomainError("⍮˜⁼: 𝕨 must be a length 2 vector", this, a);
    if (!a.get(1).equals(w)) throw new DomainError("⍮˜⁼: expected 𝕩≡⊃⌽𝕨", this, a);
    return a.get(0);
  }
}