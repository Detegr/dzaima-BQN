package APL.types.functions.builtins.fns2;

import APL.errors.RankError;
import APL.tools.Pervasion;
import APL.types.*;
import APL.types.arrs.*;
import APL.types.functions.Builtin;
import APL.types.functions.builtins.mops.CellBuiltin;

import java.util.Arrays;

public class OrBuiltin extends Builtin {
  @Override public String repr() {
    return "∨";
  }
  
  
  
  public Value identity() {
    return Num.ZERO;
  }
  
  public Value call(Value x) { // TODO this isn't stable
    if (x.rank==0) throw new RankError("∨: argument cannot be scalar", this, x);
    if (x instanceof IntArr && x.rank==1) {
      int[] is = x.asIntArrClone();
      Arrays.sort(is); for (int i = 0; i < is.length>>1; i++) { int t = is[i]; is[i] = is[is.length-i-1]; is[is.length-i-1] = t; }
      return new IntArr(is, x.shape);
    }
    Value[] cells = x.rank==1? x.valuesClone() : CellBuiltin.cells(x);
    Arrays.sort(cells);
    return ReverseBuiltin.on(x.rank==1? Arr.create(cells, x.shape) : GTBuiltin.merge(cells, new int[]{x.shape[0]}, this));
  }
  
  public Pervasion.NN2N dyNum() { return DF; };
  public static final Pervasion.NN2N DF = new Pervasion.NN2NpB() {
    public Value on(BigValue w, BigValue x) {
      return new BigValue(w.i.gcd(x.i));
    }
    public double on(double w, double x) { return w+x - w*x; }
    public void on(double   w, double[] x, double[] res) { for (int i = 0; i < x.length; i++) { double wc=w   ,xc=x[i]; res[i] = wc+xc - wc*xc; } }
    public void on(double[] w, double   x, double[] res) { for (int i = 0; i < w.length; i++) { double wc=w[i],xc=x   ; res[i] = wc+xc - wc*xc; } }
    public void on(double[] w, double[] x, double[] res) { for (int i = 0; i < w.length; i++) { double wc=w[i],xc=x[i]; res[i] = wc+xc - wc*xc; } }
    
    public int[] on(int   w, int[] x) {int[]res=new int[x.length];for(int i=0;i<x.length;i++) {long wc=w   ,xc=x[i];long l=wc+xc - wc*xc;int n=(int)l;if (l!=n)return null;res[i]=n; } return res;} // todo i _think_ there's a probability that this still overflows
    public int[] on(int[] w, int   x) {int[]res=new int[w.length];for(int i=0;i<w.length;i++) {long wc=w[i],xc=x   ;long l=wc+xc - wc*xc;int n=(int)l;if (l!=n)return null;res[i]=n; } return res;}
    public int[] on(int[] w, int[] x) {int[]res=new int[x.length];for(int i=0;i<x.length;i++) {long wc=w[i],xc=x[i];long l=wc+xc - wc*xc;int n=(int)l;if (l!=n)return null;res[i]=n; } return res;}
    
    public Value on(boolean w, BitArr x) { return w? BitArr.fill(x, true) : x; }
    public Value on(BitArr w, boolean x) { return x? BitArr.fill(w, true) : w; }
    public Value on(BitArr w, BitArr x) {
      BitArr.BC bc = new BitArr.BC(w.shape);
      for (int i = 0; i < w.arr.length; i++) bc.arr[i] = w.arr[i] | x.arr[i];
      return bc.finish();
    }
  };
  public Value call(Value w, Value x) {
    return DF.call(w, x);
  }
  
  
  public static Num reduce(BitArr x) {
    x.setEnd(false);
    for (long l : x.arr) if (l != 0) return Num.ONE;
    return Num.ZERO;
  }
}