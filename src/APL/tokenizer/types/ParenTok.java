package APL.tokenizer.types;

import java.util.*;

public class ParenTok extends TokArr<LineTok> {
  public ParenTok(String line, int pos, List<LineTok> tokens) {
    super(line, pos, tokens);
  }
  
  @Override public String toRepr() {
    StringBuilder s = new StringBuilder("(");
    boolean tail = false;
    for (var v : tokens) {
      if (tail) s.append(" ⋄ ");
      s.append(v.toRepr());
      tail = true;
    }
    s.append(")");
    return s.toString();
  }
}
