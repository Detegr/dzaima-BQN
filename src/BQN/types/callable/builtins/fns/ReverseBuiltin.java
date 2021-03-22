package BQN.types.callable.builtins.fns;

import BQN.Main;
import BQN.errors.*;
import BQN.tools.*;
import BQN.types.*;
import BQN.types.callable.builtins.FnBuiltin;

import java.util.Arrays;

public class ReverseBuiltin extends FnBuiltin {
  public String ln(FmtInfo f) { return "⌽"; }
  
  public Value call(Value x) {
    return on(x);
  }
  public static Value on(Value x) {
    if (x instanceof Primitive) return x;
    return ((Arr) x).reverseOn(0);
  }
  public Value callInv(Value x) {
    return call(x);
  }
  
  
  public Value call(Value w, Value x) {
    if (x.r()==0) {
      if (w.ia==0) return x;
      throw new RankError("⌽: atom 𝕩 is only allowed when ⟨⟩≡𝕨", this);
    }
    if (w instanceof Primitive) return on(w.asInt(), x);
    int[] wi = w.asIntVec();
    if (wi.length > x.r()) throw new DomainError("⌽: length of 𝕨 was greater than rank of 𝕩 ("+(Main.fArr(x.shape))+" ≡ ≢𝕩, "+Main.fArr(wi)+" ≡ 𝕨)", this);
    wi = Arrays.copyOf(wi, x.r()); // pads with 0s; also creates a mutable copy for moduloing
    if (x.scalar()) return x; // so recursion doesn't have to worry about it
  
    for (int i = 0; i < wi.length; i++) {
      int l = x.shape[i];
      if (l==0) return x;
      int c = wi[i];
      c%= l; if (c<0) c+= l;
      wi[i] = c;
    }
    
    MutVal res = new MutVal(x.shape, x);
    rec(wi, res, x, 0, 0, 0);
    return res.get();
  }
  
  private void rec(int[] w, MutVal res, Value x, int d, int is, int rs) {
    int ax = x.shape[d];
    int mv = w[d];
    is*= ax;
    rs*= ax;
    if (d == x.r()-1) {
      res.copy(x, is   , rs+ax-mv,    mv);
      res.copy(x, is+mv, rs      , ax-mv);
    } else {
      for (int i =  0; i < mv; i++) rec(w, res, x, d+1, is+i, rs+i+ax-mv);
      for (int i = mv; i < ax; i++) rec(w, res, x, d+1, is+i, rs+i   -mv);
    }
  }
  
  @Override public Value callInvX(Value w, Value x) {
    return call(numM(MinusBuiltin.NF, w), x);
  }
  
  
  
  public static Value on(int a, Value x) {
    if (x.ia==0) return x;
    a = Math.floorMod(a, x.shape[0]);
    if (a == 0) return x;
    int csz = Arr.prod(x.shape, 1, x.r());
    int pA = csz*a; // first part
    int pB = x.ia - pA; // second part
    
    MutVal res = new MutVal(x.shape, x);
    res.copy(x, pA,  0, pB);
    res.copy(x,  0, pB, pA);
    return res.get();
  }
}