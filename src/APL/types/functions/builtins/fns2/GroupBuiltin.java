package APL.types.functions.builtins.fns2;

import APL.Main;
import APL.errors.*;
import APL.tools.MutVal;
import APL.types.*;
import APL.types.arrs.*;
import APL.types.functions.Builtin;

import java.util.Arrays;

public class GroupBuiltin extends Builtin {
  public String repr() {
    return "⊔";
  }
  
  
  private static class MutIA {
    int[] ds = new int[4];
    int sz;
    void add(int i) {
      if (sz >= ds.length) {
        ds = Arrays.copyOf(ds, ds.length*2);
      }
      ds[sz] = i;
      sz++;
    }
  }
  
  public Value call(Value x) {
    if (x.rank != 1) throw new RankError("⊔: argument must be a vector", this, x);
    int depth = MatchBuiltin.full(x);
    if (depth == 1) {
      int[] xi = x.asIntVec();
      int sz = 0;
      for (int d : xi) sz = Math.max(sz, d);
      sz++;
      
      MutIA[] ds = new MutIA[sz];
      for (int i = 0; i < sz; i++) ds[i] = new MutIA();
      for (int i = 0; i < xi.length; i++) {
        int c = xi[i];
        if (c>=0) ds[c].add(i);
        else if (c!=-1) throw new DomainError("⊔: didn't expect "+c+" in argument", this, x);
      }
      Value[] res = new Value[sz];
      for (int i = 0; i < sz; i++) res[i] = new IntArr(Arrays.copyOf(ds[i].ds, ds[i].sz));
      return new HArr(res);
    }
    if (depth != 2) throw new DomainError("⊔: argument must be depth 1 or 2 (was "+depth+")", this, x);
    int[] args = new int[x.ia];
    for (int i = 0; i < args.length; i++) {
      Value c = x.get(i);
      if (c.rank != 1) throw new DomainError("⊔: expected items of argument to be vectors (contained item with shape "+Main.formatAPL(c.shape)+")", this, x);
      args[i] = c.ia;
    }
    return call(x, UDBuiltin.on(new IntArr(args), null)); // gives strange errors but whatever
  }
  
  
  
  public Value call(Value w, Value x) {
    int depth = MatchBuiltin.full(w);
    int[][] wp;
    int wsz;
    int xsz = x.rank;
    if (depth == 1) {
      wsz = 1;
      if (w.rank != 1) throw new RankError("⊔: depth 1 𝕨 must have rank 1 "+(w.rank==0? "(was a scalar)" : "(had shape "+Main.formatAPL(w.shape)+")"), this, w);
      if (xsz==0) throw new RankError("⊔: 𝕩 cannot be scalar if 𝕨 has depth 1", this, w);
      if (w.ia != x.shape[0]) throw new LengthError("⊔: length of 𝕨 must be ⊑≢𝕩 ("+w.ia+" ≡ ≠𝕨; "+Main.formatAPL(x.shape)+" ≡ ≢𝕩)", this, w);
      wp = new int[][]{w.asIntArr()};
    } else if (depth == 2) {
      wsz = w.ia;
      if (w.rank > 1) throw new RankError("⊔: depth 2 𝕨 must have rank ≤1 (had shape "+Main.formatAPL(w.shape)+")", this, w);
      if (wsz > xsz) throw new DomainError("⊔: length of depth 2 𝕨 must be greater than rank of 𝕩 ("+wsz+" ≡ ≠𝕨; "+Main.formatAPL(x.shape)+" ≡ ≢𝕩)", this, w);
      wp = new int[w.ia][];
      for (int i = 0; i < w.ia; i++) {
        Value c = w.get(i);
        if (c.rank!=1) throw new RankError("⊔: items of 𝕨 must be of rank 1", this, w);
        wp[i] = c.asIntArr();
        if (c.ia != x.shape[i]) { int[] shs = new int[w.ia]; for (int j = 0; j < w.ia; j++) shs[j] = w.get(j).ia;
          throw new LengthError("⊔: lengths of 𝕨 must be a prefix of ≢𝕩 ("+Main.formatAPL(shs)+" ≡ ≠¨𝕨; "+Main.formatAPL(x.shape)+" ≡ ≢𝕩)", this, w); }
      }
    } else throw new DomainError("⊔: depth of 𝕨 must be 1 or 2 (was "+depth+")", this, w);
    
    
    
    if (x.rank==1) { // fast path
      int[] poss = wp[0];
      int sz = -1;
      for (int i : poss) sz = Math.max(sz, i);
      sz++;
      int[] rshs = new int[sz];
      for (int c : poss) {
        if (c>=0) rshs[c]++;
        else if (c!=-1) throw new DomainError("⊔: didn't expect "+c+" in 𝕨", this, w);
      }
      if (x.quickIntArr()) {
        int[] xi = x.asIntArr();
        int[] idxs = new int[sz];
        int[][] vs = new int[sz][];
        for (int i = 0; i < sz; i++) vs[i] = new int[rshs[i]];
        for (int i = 0; i < x.ia; i++) {
          int c = poss[i];
          if (c>=0) vs[c][idxs[c]++] = xi[i];
        }
        Value[] res = new Value[sz];
        for (int i = 0; i < sz; i++) res[i] = new IntArr(vs[i]);
        return new HArr(res);
      }
      int[] idxs = new int[sz];
      Value[][] vs = new Value[sz][];
      for (int i = 0; i < sz; i++) vs[i] = new Value[rshs[i]];
      for (int i = 0; i < x.ia; i++) {
        int c = poss[i];
        if (c>=0) vs[c][idxs[c]++] = x.get(i);
      }
      Value[] res = new Value[sz];
      for (int i = 0; i < sz; i++) res[i] = Arr.create(vs[i]);
      return new HArr(res);
    }
  
    int csz = Arr.prod(x.shape, wsz, xsz);
    int[] rsh = new int[wsz];
    for (int i = 0; i < wsz; i++) {
      int max = -1;
      for (int c : wp[i]) max = Math.max(max, c);
      rsh[i] = max+1;
    }
    int sz = Arr.prod(rsh);
    int[][] rshs = new int[sz][xsz];
    int repl = 1;
    for (int i = wsz-1; i >= 0; i--) {
      int[] ca = new int[rsh[i]];
      for (int c : wp[i]) {
        if (c>=0) ca[c]++;
        else if (c!=-1) throw new DomainError("⊔: didn't expect "+c+" in 𝕨", this, w);
      }
      int rp = 0;
      while (rp < sz) {
        for (int c : ca) {
          for (int k = 0; k < repl; k++) rshs[rp++][i] = c;
        }
      }
      repl*= rsh[i];
    }
    for (int[] c : rshs) {
      System.arraycopy(x.shape, wsz, c, wsz, xsz-wsz);
    }
  
    MutVal[] vs = new MutVal[sz];
    for (int i = 0; i < sz; i++) vs[i] = new MutVal(rshs[i]);
    recIns(vs, new int[sz], rsh, 0, 0, 0, wp, x, csz);
    
    Value[] res = new Value[sz];
    for (int i = 0; i < sz; i++) res[i] = vs[i].get();
    return new HArr(res, rsh);
  }
  
  private void recIns(MutVal[] vs, int[] ram, int[] rsh, int rp, int k, int ip, int[][] w, Value x, int csz) {
    if (k == rsh.length) {
      vs[rp].copy(x, ip*csz, ram[rp], csz);
      ram[rp]+= csz;
    } else {
      rp*= rsh[k];
      ip*= x.shape[k];
      int[] c = w[k];
      for (int i = 0; i < c.length; i++) {
        if (c[i] >= 0) recIns(vs, ram, rsh, rp+c[i], k+1, ip+i, w, x, csz);
      }
    }
  }
}