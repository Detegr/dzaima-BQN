package BQN.types.callable.builtins;

import BQN.errors.*;
import BQN.tools.*;
import BQN.types.*;

public class MathNS extends SimpleMap {
  public String ln(FmtInfo f) { return "•math"; }
  public static final Value INSTANCE = new MathNS();
  
  public static final MB sin = new MB("sin", new Fun.NumMV() { public Value call(BigValue x) { throw new DomainError("sin of biginteger"); }
    public Value call(Num x) { return new Num(Math.sin(x.num)); }
    public void call(double[] res, double[] x) { for (int i = 0; i < x.length; i++) res[i] = Math.sin(x[i]); }
  });
  public static final MB cos = new MB("cos", new Fun.NumMV() { public Value call(BigValue x) { throw new DomainError("cos of biginteger"); }
    public Value call(Num x) { return new Num(Math.cos(x.num)); }
    public void call(double[] res, double[] x) { for (int i = 0; i < x.length; i++) res[i] = Math.cos(x[i]); }
  });
  public static final MB tan = new MB("tan", new Fun.NumMV() { public Value call(BigValue x) { throw new DomainError("tan of biginteger"); }
    public Value call(Num x) { return new Num(Math.tan(x.num)); }
    public void call(double[] res, double[] x) { for (int i = 0; i < x.length; i++) res[i] = Math.tan(x[i]); }
  });
  public static final MB asin = new MB("asin", new Fun.NumMV() { public Value call(BigValue x) { throw new DomainError("asin of biginteger"); }
    public Value call(Num x) { return new Num(Math.asin(x.num)); }
    public void call(double[] res, double[] x) { for (int i = 0; i < x.length; i++) res[i] = Math.asin(x[i]); }
  });
  public static final MB acos = new MB("acos", new Fun.NumMV() { public Value call(BigValue x) { throw new DomainError("acos of biginteger"); }
    public Value call(Num x) { return new Num(Math.acos(x.num)); }
    public void call(double[] res, double[] x) { for (int i = 0; i < x.length; i++) res[i] = Math.acos(x[i]); }
  });
  public static final MB atan = new MB("atan", new Fun.NumMV() { public Value call(BigValue x) { throw new DomainError("atan of biginteger"); }
    public Value call(Num x) { return new Num(Math.atan(x.num)); }
    public void call(double[] res, double[] x) { for (int i = 0; i < x.length; i++) res[i] = Math.atan(x[i]); }
  });
  
  public Value getv(String s) {
    switch (s) {
      case "sin": return sin;
      case "cos": return cos;
      case "tan": return tan;
      case "asin": return asin;
      case "acos": return acos;
      case "atan": return atan;
    }
    throw new ValueError("No key "+s+" for •math");
  }
  
  public void setv(String s, Value v) {
    throw new DomainError("Assigning into •math");
  }
  
  private static class MB extends FnBuiltin {
    public final Fun.NumMV f;
    public final String name;
    public MB(String name, Fun.NumMV f) {
      this.f = f;
      this.name = "•math."+name;
    }
    public Value call(Value x) { return numM(f, x); }
    public String ln(FmtInfo f) { return name; }
  }
}
