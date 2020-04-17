package APL.types.functions.builtins.fns;

import APL.*;
import APL.errors.*;
import APL.types.*;
import APL.types.arrs.*;
import APL.types.dimensions.*;
import APL.types.functions.Builtin;

public class UpArrowBuiltin extends Builtin implements DimDFn {
  @Override public String repr() {
    return "↑";
  }
  
  public Value call(Value w) {
    if (w instanceof Arr) {
      if (w instanceof DoubleArr || w instanceof ChrArr || w instanceof BitArr) return w;
      Value[] subs = w.values();
      if (subs.length == 0) return w;
      
      int[] def = new int[subs[0].rank];
      System.arraycopy(subs[0].shape, 0, def, 0, def.length);
      for (Value v : subs) {
        if (v.rank != def.length) throw new RankError("↑: expected equal ranks of items", this, v);
        for (int i = 0; i < def.length; i++) def[i] = Math.max(v.shape[i], def[i]);
      }
      int subIA = Arr.prod(def);
      int totalIA = subIA * Arr.prod(w.shape);
      int[] totalShape = new int[def.length + w.rank];
      System.arraycopy(w.shape, 0, totalShape, 0, w.rank);
      System.arraycopy(def, 0, totalShape, w.rank, def.length);
      
      boolean allNums = true;
      for (Value v : subs) {
        if (!v.quickDoubleArr()) {
          allNums = false;
          break;
        }
      }
      if (allNums) {
        double[] allVals = new double[totalIA];
        
        int i = 0;
        for (Value v : subs) {
          double[] c = v.asDoubleArr();
          int k = 0;
          for (int j : new SimpleIndexer(def, v.shape)) {
            allVals[i+j] = c[k++];
          }
          // automatic zero padding
          i+= subIA;
        }
        
        return new DoubleArr(allVals, totalShape);
      }
      Value[] allVals = new Value[totalIA];
      
      int i = 0;
      for (Value v : subs) {
        Value proto = v.prototype();
        for (int[] sh : new Indexer(def, 0)) {
          // System.out.println(v +" "+ Arrays.toString(sh) +" "+ v.at(sh, v.prototype) +" "+ Arrays.toString(v.shape));
          allVals[i++] = v.at(sh, proto);
        }
      }
      
      return Arr.create(allVals, totalShape);
    } else return w;
  }
  
  public static Value merge(Value[] w, Callable blame) {
    int[] def = new int[w[0].rank];
    System.arraycopy(w[0].shape, 0, def, 0, def.length);
    for (Value v : w) {
      if (v.rank != def.length) throw new RankError("↑: expected equal ranks of items", blame, v);
      for (int i = 0; i < def.length; i++) def[i] = Math.max(v.shape[i], def[i]);
    }
    int subIA = Arr.prod(def);
    int totalIA = subIA * w.length;
    int[] totalShape = new int[def.length + 1];
    totalShape[0] = w.length;
    System.arraycopy(def, 0, totalShape, 1, def.length);
    
    boolean allNums = true;
    for (Value v : w) {
      if (!v.quickDoubleArr()) {
        allNums = false;
        break;
      }
    }
    if (allNums) {
      double[] allVals = new double[totalIA];
      
      int i = 0;
      for (Value v : w) {
        double[] c = v.asDoubleArr();
        int k = 0;
        for (int j : new SimpleIndexer(def, v.shape)) {
          allVals[i+j] = c[k++];
        }
        // automatic zero padding
        i+= subIA;
      }
      
      return new DoubleArr(allVals, totalShape);
    } else {
      Value[] allVals = new Value[totalIA];
      
      int i = 0;
      for (Value v : w) {
        Value proto = v.prototype();
        for (int[] sh : new Indexer(def, 0)) {
          // System.out.println(v +" "+ Arrays.toString(sh) +" "+ v.at(sh, v.prototype) +" "+ Arrays.toString(v.shape));
          allVals[i++] = v.at(sh, proto);
        }
      }
      return Arr.create(allVals, totalShape);
    }
  }
  
  
  
  
  public Value call(Value a, Value w) {
    int[] gsh = a.asIntVec();
    if (gsh.length == 0) return w;
    if (gsh.length > w.rank) throw new DomainError("↑: ≢⍺ should be less than ⍴⍴⍵ ("+gsh.length+" = ≢⍺; "+Main.formatAPL(w.shape)+" ≡ ⍴⍵)", this);
    int[] sh = new int[w.rank];
    System.arraycopy(gsh, 0, sh, 0, gsh.length);
    System.arraycopy(w.shape, gsh.length, sh, gsh.length, sh.length - gsh.length);
    int[] off = new int[sh.length];
    for (int i = 0; i < gsh.length; i++) {
      int d = gsh[i];
      if (d < 0) {
        sh[i] = -d;
        off[i] = w.shape[i]-sh[i];
      } else off[i] = 0;
    }
    return on(sh, off, w, this);
  }
  
  public Value call(Value a, Value w, DervDimFn dims) {
    int[] axV = a.asIntVec();
    int[] axK = dims.dims(w.rank);
    if (axV.length != axK.length) throw new DomainError("↑: expected ⍺ and axis specification to have equal number of items (⍺≡"+Main.formatAPL(axV)+"; axis≡"+dims.format()+")", this, dims);
    int[] sh = w.shape.clone();
    int[] off = new int[sh.length];
    for (int i = 0; i < axV.length; i++) {
      int ax = axK[i];
      int am = axV[i];
      sh[ax] = Math.abs(am);
      if (am < 0) off[ax] = w.shape[ax] + am;
    }
    return on(sh, off, w, this);
  }
  
  public static Value on(int[] sh, int[] off, Value w, Callable blame) {
    int rank = sh.length;
    assert rank==off.length && rank==w.rank;
    for (int i = 0; i < rank; i++) {
      if (off[i] < 0) throw new DomainError(blame+": requesting item before first"+(rank>1? " at (0-indexed) axis "+i : ""), blame);
      if (off[i]+sh[i] > w.shape[i]) throw new DomainError(blame+": requesting item after end"+(rank>1? " at (0-indexed) axis "+i : ""), blame);
    }
    if (rank == 1) {
      int s = off[0];
      int l = sh[0];
      if (w instanceof BitArr) {
        BitArr wb = (BitArr) w;
        if (s == 0) {
          long[] ls = new long[BitArr.sizeof(l)];
          System.arraycopy(wb.arr, 0, ls, 0, ls.length);
          return new BitArr(ls, new int[]{l});
        } else {
          BitArr.BA res = new BitArr.BA(l);
          res.add(wb, s, w.ia);
          return res.finish();
        }
      }
      if (w instanceof ChrArr) {
        char[] res = new char[l];
        String ws = ((ChrArr) w).s;
        ws.getChars(s, s+l, res, 0); // ≡ for (int i = 0; i < l; i++) res[i] = ws.charAt(s+i);
        return new ChrArr(res);
      }
      if (w.quickDoubleArr()) {
        double[] res = new double[l];
        double[] wd = w.asDoubleArr();
        System.arraycopy(wd, s, res, 0, l); // ≡ for (int i = 0; i < l; i++) res[i] = wd[s+i];
        return new DoubleArr(res);
      }
      
      Value[] res = new Value[l];
      for (int i = 0; i < l; i++) res[i] = w.get(s+i);
      return Arr.create(res);
    }
    int ia = Arr.prod(sh);
    if (w instanceof ChrArr) {
      char[] arr = new char[ia];
      String s = ((ChrArr) w).s;
      int i = 0;
      for (int[] index : new Indexer(sh, off)) {
        arr[i] = s.charAt(Indexer.fromShape(w.shape, index, 0));
        i++;
      }
      return new ChrArr(arr, sh);
    }
    if (w.quickDoubleArr()) {
      double[] arr = new double[ia];
      double[] wd = w.asDoubleArr();
      int i = 0;
      for (int[] index : new Indexer(sh, off)) {
        arr[i] = wd[Indexer.fromShape(w.shape, index, 0)];
        i++;
      }
      return new DoubleArr(arr, sh);
    }
    Value[] arr = new Value[ia];
    int i = 0;
    for (int[] index : new Indexer(sh, off)) {
      arr[i] = w.at(index, 0);
      i++;
    }
    return Arr.create(arr, sh);
  }
  
  
  
  
  public Value underW(Obj o, Value a, Value w) {
    Value v = o instanceof Fun? ((Fun) o).call(call(a, w)) : (Value) o;
    return undo(a.asIntVec(), v, w);
  }
  public static Value undo(int[] e, Value w, Value origW) {
    Value[] r = new Value[origW.ia];
    int[] s = origW.shape;
    Indexer idx = new Indexer(s, 0);
    int[] tmp = new int[e.length];
    for (int[] i : idx) {
      Value c;
      boolean in = true;
      for (int j = 0; j < e.length; j++) {
        int ep = e[j];
        int ip = i[j];
        int lp = s[j];
        if (ep<0? ip <= lp+ep-1 : ip >= ep) {
          in = false;
          break;
        }
      }
      if (in) {
        for (int j = 0; j < e.length; j++) {
          tmp[j] = e[j]<0? i[j]-e[j]-s[j]: i[j];
        }
        c = w.simpleAt(tmp);
      } else {
        c = origW.simpleAt(i);
      }
      r[idx.pos()] = c;
      
    }
    
    return Arr.create(r, s);
  }
}