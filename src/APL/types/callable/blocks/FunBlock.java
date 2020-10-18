package APL.types.callable.blocks;

import APL.*;
import APL.tokenizer.types.BlockTok;
import APL.types.*;


public class FunBlock extends Fun {
  public final BlockTok code;
  public final Scope sc;
  
  public FunBlock(BlockTok t, Scope sc) {
    this.sc = sc;
    code = t;
  }
  
  public Value call(Value x) { // 𝕊𝕩𝕨···
    Main.printdbg("FunBlock call", x);
    return code.exec(sc, null, new Value[]{this, x, Nothing.inst}, 0);
  }
  
  public Value call(Value w, Value x) { // 𝕊𝕩𝕨···
    Main.printdbg("FunBlock call", w, x);
    return code.exec(sc, w, new Value[]{this, x, w}, 0);
  }
  
  
  public Value callInv(Value x) { // 𝕊𝕩𝕨···
    Main.printdbg("FunBlock⁼ call", x);
    return code.exec(sc, null, new Value[]{this, x, Nothing.inst}, 1);
  }
  
  public Value callInvX(Value w, Value x) { // 𝕊𝕩𝕨···
    Main.printdbg("FunBlock⁼ call", w, x);
    return code.exec(sc, w, new Value[]{this, x, w}, 1);
  }
  public Value callInvW(Value w, Value x) { // 𝕊𝕩𝕨···
    Main.printdbg("FunBlock˜⁼ call", w, x);
    return code.exec(sc, w, new Value[]{this, w, x}, 2);
  }
  
  public String repr() {
    return code.toRepr();
  }
}