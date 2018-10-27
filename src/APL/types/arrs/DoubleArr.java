package APL.types.arrs;

import APL.errors.*;
import APL.types.*;

import java.util.Arrays;

public class DoubleArr extends Arr {
  final public double[] arr;
  public DoubleArr(double[] arr, int[] sh) {
    super(sh);
    assert sh.length != 0;
    this.arr = arr;
  }
  public DoubleArr(double[] arr) { // 1D
    super(new int[]{arr.length});
    this.arr = arr;
  }
  public DoubleArr(Integer[] arr) { // 1D
    super(new int[]{arr.length});
    double[] a = new double[ia];
    for (int i = 0; i < ia; i++) {
      a[i] = arr[i];
    }
    this.arr = a;
  }
  
  @Override
  public int[] asIntArr() {
    if (rank >= 2) throw new RankError("trying to use a rank "+rank+" number array as vector", this);
    int[] r = new int[ia];
    for (int i = 0; i < ia; i++) {
      if (arr[i] != (int) arr[i]) throw new DomainError("using a fractional number as integer", this);
      r[i] = ((int) arr[i]);
    }
    return r;
  }
  
  @Override
  public int asInt() {
    throw new RankError("Using a number array as integer", this);
  }
  
  @Override
  public Value get(int i) {
    return new Num(arr[i]);
  }
  
  @Override
  public String asString() {
    throw new DomainError("using double array as string", this);
  }
  
  @Override
  public Value prototype() {
    return Num.ZERO;
  }
  
  @Override
  public Value ofShape(int[] sh) {
    return new DoubleArr(arr, sh);
  }
  
  @Override
  public double sum() { // TODO whether or not commented code
//    double r = 0;
//    for (double val : arr) r += val;
//    return r;
    return Arrays.stream(arr).sum();
  }
  
  @Override
  public double[] asDoubleArr() {
    return arr;
  }
  
  @Override
  public boolean quickDoubleArr() {
    return true;
  }
}
