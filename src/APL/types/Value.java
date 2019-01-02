package APL.types;

import APL.Type;
import APL.errors.*;
import APL.types.arrs.*;

import java.util.*;


public abstract class Value extends Obj implements Iterable<Value> {
  final public int[] shape;
  final public int rank;
  final public int ia; // item amount
  Value(int[] shape) {
    this.shape = shape;
    rank = shape.length;
    int tia = 1;
    for (int i = 0; i < rank; i++) tia*= shape[i];
    ia = tia;
  }
  Value(int[] shape, int ia, int rank) {
    this.shape = shape;
    this.ia = ia;
    this.rank = rank;
  }
  public abstract int[] asIntVec();
  public abstract int asInt();
  public boolean scalar() {
    return rank == 0;
  }
  public Value first() {
    return get(0);
  }
  public abstract Value get(int i); // WARNING: UNSAFE; doesn't need to throw for out-of-bounds
  
  
  
  public int compareTo(Value v) {
    if (this instanceof Num && v instanceof Num) return ((Num) this).compareTo((Num) v);
    if (this instanceof Char && v instanceof Char) return ((Char) this).compareTo((Char) v);
    if (this instanceof Num && (   v instanceof Char ||    v instanceof Arr)) return -1;
    if (   v instanceof Num && (this instanceof Char || this instanceof Arr)) return 1;
    if ((this instanceof Arr || this instanceof Char) && (v instanceof Arr || v instanceof Char)) {
      String s1 =   asString();
      String s2 = v.asString();
      return s1.compareTo(s2);
    }
    throw new DomainError("Can't compare "+v+" and "+this, this);
  }
  public abstract String asString();
  public Integer[] gradeUp() {
    if (rank != 1) throw new DomainError("grading rank ≠ 1", this);
    Integer[] na = new Integer[ia];
    
    for (int i = 0; i < na.length; i++) {
      na[i] = i;
    }
    Arrays.sort(na, (a, b) -> get(a).compareTo(get(b)));
    return na;
  }
  public Integer[] gradeDown() {
    if (rank != 1) throw new DomainError("grading rank ≠ 1", this);
    Integer[] na = new Integer[ia];
    
    for (int i = 0; i < na.length; i++) {
      na[i] = i;
    }
    Arrays.sort(na, (a, b) -> get(b).compareTo(get(a)));
    return na;
  }
  
  public int[] eraseDim(int place) {
    int[] res = new int[rank-1];
    System.arraycopy(shape, 0, res, 0, place);
    System.arraycopy(shape, place+1, res, place, rank-1-place);
    return res;
  }
  @Override
  public Type type() {
    return Type.array;
  }
  
  public abstract Value prototype(); // what to append to this array
  
  public String oneliner(int[] where) {
    return toString();
  }
  
  protected transient Value[] vs;
  
  public Value[] values() {
    if (vs == null) {
      Value[] vs = new Value[ia];
      for (int i = 0; i < ia; i++) vs[i] = get(i);
      this.vs = vs;
    }
    return vs;
  }
  
  @Override
  public Iterator<Value> iterator() {
    return new ValueIterator();
  }
  
  class ValueIterator implements Iterator<Value> {
    int c = 0;
    @Override
    public boolean hasNext() {
      return c < ia;
    }
    
    @Override
    public Value next() {
      return vs != null? vs[c++] : get(c++);
    }
  }
  
  public Value at(int[] pos, int IO) {
    if (pos.length != rank) throw new RankError("array rank was "+rank+", tried to get item at rank "+pos.length, this);
    int x = 0;
    for (int i = 0; i < rank; i++) {
      if (pos[i] < IO) throw new DomainError("Tried to access item at position "+pos[i], this);
      if (pos[i] >= shape[i]+IO) throw new DomainError("Tried to access item at position "+pos[i]+" while max was "+shape[i], this);
      x+= pos[i]-IO;
      if (i != rank-1) x*= shape[i+1];
    }
    return get(x);
  }
  public Value at(int[] pos, Value def) { // 0-indexed
    int x = 0;
    for (int i = 0; i < rank; i++) {
      if (pos[i] < 0 || pos[i] >= shape[i]) return def;
      x+= pos[i];
      if (i != rank-1) x*= shape[i+1];
    }
    return get(x);
  }
  public Value simpleAt(int[] pos) {
    int x = 0;
    for (int i = 0; i < rank; i++) {
      x+= pos[i];
      if (i != rank-1) x*= shape[i+1];
    }
    return get(x);
  }
  
  public abstract Value ofShape(int[] sh); // don't call with ×/sh ≠ ×/shape!
  public abstract Value with(Value what, int[] where);
  public double sum() {
    return Arrays.stream(values()).mapToDouble(Value::asInt).sum();
  }
  public double[] asDoubleArr() { // warning: also succeeds on a primitive number
    double[] res = new double[ia];
    int i = 0;
    for (Value c : values()) {
      res[i++] = c.asDouble();
    }
    return res;
  }
  public double[] asDoubleArrClone() {
    return asDoubleArr().clone();
  }
  public double asDouble() {
    throw new DomainError("Using "+this.humanType(true)+" as a number", this);
  }
  public boolean quickDoubleArr() { // if true, asDoubleArr must succeed; warning: also succeeds on a primitive number
    return false;
  }
  public Value squeeze() {
    if (ia == 0) return this;
    Value f = get(0);
    if (f instanceof Num) {
      double[] ds = new double[ia];
      for (int i = 0; i < ia; i++) {
        if (get(i) instanceof Num) ds[i] = get(i).asDouble();
        else {
          ds = null;
          break;
        }
      }
      if (ds != null) return new DoubleArr(ds, shape);
    }
    if (f instanceof Char) {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < ia; i++) {
        if (get(i) instanceof Char) s.append(((Char) get(i)).chr);
        else {
          s = null;
          break;
        }
      }
      if (s != null) return new ChrArr(s.toString(), shape);
    }
    boolean anyBetter = false;
    Value[] optimized = new Value[ia];
    Value[] values = values();
    for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
      Value v = values[i];
      Value vo = v.squeeze();
      if (vo != v) anyBetter = true;
      optimized[i] = vo;
    }
    if (anyBetter) return new HArr(optimized, shape);
    return this;
  }
}
