package APL.types.mut;

import APL.Scope;
import APL.errors.ValueError;
import APL.types.*;

public class Local extends Settable {
  
  public final int depth, index;
  
  public Local(int depth, int index) {
    this.depth = depth;
    this.index = index;
  }
  
  public Value get(Scope sc) {
    Value got = sc.getL(depth, index);
    if (got == null) throw new ValueError("Getting value of non-existing variable \""+name(sc)+"\"", this);
    return got;
  }
  
  public void set(Value v, boolean update, Scope sc, Callable blame) {
    sc = sc.owner(depth);
    if (update ^ sc.vars[index]!=null) {
      if (update) throw new ValueError("no variable \""+name(sc)+"\" to update", blame);
      else        throw redefine(name(sc), blame);
    }
    sc.vars[index] = v;
  }
  
  public static ValueError redefine(String name, Tokenable blame) {
    return new ValueError("←: cannot redefine \""+name+"\"", blame);
  }
  
  private String name(Scope sc) {
    return sc.owner(depth).varNames[index];
  }
  
  public String toString() {
    return "loc("+depth+","+index+")";
  }
}