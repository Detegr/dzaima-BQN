package APL.types.arrs;

import APL.errors.DomainError;
import APL.types.*;

public class Rank0Arr extends Arr {
  public final static int[] SHAPE = new int[0];
  public final Value item;
  
  public Rank0Arr(Value item) {
    super(SHAPE, 1, 0);
    this.item = item;
  }
  
  public int[] asIntArrClone() {
    return new int[]{item.asInt()};
  }
  
  public Value get(int i) {
    return item;
  }
  
  public String asString() {
    if (item instanceof Char) return String.valueOf(((Char) item).chr);
    throw new DomainError("Using array containing "+item.humanType(true)+" as string", this);
  }
  
  public Value prototype() {
    return item.prototype();
  }
  
  public Value safePrototype() {
    return item.safePrototype();
  }
  
  public Value ofShape(int[] sh) {
    assert ia == Arr.prod(sh);
    return new SingleItemArr(item, sh);
  }
  
  public Value[] valuesClone() {
    return new Value[]{item};
  }
}