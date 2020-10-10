package APL.tokenizer.types;

import APL.*;
import APL.errors.*;
import APL.tokenizer.Token;
import APL.tools.Body;
import APL.types.Value;
import APL.types.callable.blocks.*;

import java.util.*;

public class BlockTok extends TokArr {
  public Comp comp;
  public final boolean immediate;
  public final static boolean immBlock = true;
  public final Body[] bodiesM;
  public final Body[] bodiesD;
  
  public BlockTok(String line, int spos, int epos, ArrayList<Token> tokens) {
    super(line, spos, epos, tokens);
    type = 'f'; boolean canBeImmediate = funType(tokens, this);
    ArrayList<ArrayList<Token>> parts = new ArrayList<>();
    int li = 0;
    for (int i = 0; i < tokens.size(); i++) {
      if (((LineTok) tokens.get(i)).end == ';') {
        parts.add(new ArrayList<>(tokens.subList(li, i+1)));
        li = i+1;
      }
    }
    parts.add(new ArrayList<>(li==0? tokens : tokens.subList(li, tokens.size())));
    
    for (int i = 0; i < parts.size(); i++) {
      List<Token> part = parts.get(i);
      if (part.size() == 0) throw new SyntaxError("function contained empty body", this);
      for (int j = 1; j < part.size(); j++) {
        LineTok c = (LineTok) part.get(j);
        if (c.end == ':') throw new SyntaxError("function body contained header in the middle", c.tokens.get(c.tokens.size()-1));
      }
      if (i < parts.size()-2  &&  ((LineTok)part.get(0)).end != ':') throw new SyntaxError("only the last 2 bodies in a function can be header-less", part.get(0));
    }
    
    int tail; // amount of no-header bodies
    if (parts.size() > 1) {
      boolean p = ((LineTok)parts.get(parts.size()-2).get(0)).end != ':';
      boolean l = ((LineTok)parts.get(parts.size()-1).get(0)).end != ':';
      if (p && !l) throw new SyntaxError("header-less function bodies must be the last", parts.get(parts.size()-2).get(0));
      tail = p? 2 : l? 1 : 0;
    } else tail = ((LineTok)parts.get(0).get(0)).end != ':'? 1 : 0;
    
    ArrayList<Body> bodies = new ArrayList<>();
    for (int i = 0; i < parts.size(); i++) {
      ArrayList<Token> part = parts.get(i);
      Body body;
      if (((LineTok)part.get(0)).end == ':') {
        List<Token> src = part.subList(1, part.size());
        body = new Body(part.get(0), new ArrayList<>(src), funType(src, this));
      } else {
        assert tail != 0;
        int rid = parts.size()-i;
        body = new Body(part, tail==1? 'a' : rid==1? 'd' : 'm', funType(part, this));
      }
      bodies.add(body);
    }
    if (immBlock && canBeImmediate && type=='f' && bodies.size()==1) type = 'a';
    
    char htype = 0;
    for (Body b : bodies) {
      if (b.type != 0) {
        if (b.type=='a' && !canBeImmediate) throw new SyntaxError("Using function tokens in a value block", this);
        if (b.type=='a' && bodies.size()>1) throw new DomainError("Value blocks must contain only 1 body", this);
        if (htype==0) htype = b.type;
        else if (b.type != htype) throw new SyntaxError("Different type headers in one function", this);
      }
    }
    if (htype != 0) {
      if (type=='d' && htype!='d') throw new SyntaxError("Using combinator tokens with non-combinator header", this);
      if (type=='m' && htype=='f') throw new SyntaxError("Using modifier tokens with non-modifier header", this);
      type = htype;
    }
    
    if (type=='m' || type=='d') {
      if (bodies.size() == 1) {
        immediate = bodies.get(0).immediate;
      } else {
        immediate = false;
        for (Body b : bodies) if (b.immediate) throw new SyntaxError("Immediate operators cannot have multiple bodies", this);
      }
    } else {
      immediate = false;
    }
    Comp.Mut mut = new Comp.Mut(false); // TODO why is this still a thing
    comp = Comp.comp(mut, bodies, this);
    
    ArrayList<Body> mb = new ArrayList<>();
    ArrayList<Body> db = new ArrayList<>();
    for (Body c : bodies) {
      if (c.arity!='m') db.add(c);
      if (c.arity!='d') mb.add(c);
    }
    bodiesM = mb.toArray(new Body[0]);
    bodiesD = db.toArray(new Body[0]);
  }
  
  public BlockTok(String line, int spos, int epos, List<Token> tokens, boolean pointless) { // for pointless
    super(line, spos, epos, tokens);
    assert pointless;
    comp = null;
    immediate = false;
    bodiesM=bodiesD=null;
  }
  
  
  public BlockTok(char type, boolean imm, Body[] mb, Body[] db) {
    super(Token.COMP.raw, 0, 18, new ArrayList<>());
    this.type = type;
    immediate = imm;
    this.bodiesM = mb;
    this.bodiesD = db;
  }
  
  public static boolean funType(Token t, BlockTok dt) { // returns if can be immediate, mutates dt's type
    if (t instanceof TokArr) {
      if (!(t instanceof BlockTok)) {
        boolean imm = true;
        for (Token c : ((TokArr) t).tokens) imm&= funType(c, dt);
        return imm;
      }
      return true;
    } else if (t instanceof OpTok) {
      String op = ((OpTok) t).op;
      if (dt.type != 'd') {
        if (op.equals("𝕗") || op.equals("𝔽")) dt.type = 'm';
        else if (op.equals("𝕘") || op.equals("𝔾")) dt.type = 'd';
      }
      return !(op.equals("𝕨") || op.equals("𝕎") || op.equals("𝕩") || op.equals("𝕏") || op.equals("𝕤") || op.equals("𝕊"));
    } else if (t instanceof NameTok) {
      NameTok nt = (NameTok) t;
      if (nt.name.equals("𝕣")) {
        if (nt.rawName.equals("_𝕣_")) dt.type = 'd';
        else if (dt.type!='d') dt.type = 'm';
      }
      return true;
    } else if (t instanceof ParenTok) {
      return funType(((ParenTok) t).ln, dt);
    } else return true;
  }
  
  private boolean funType(List<Token> lns, BlockTok block) { // TODO split up into separate thing getting immediate and type
    boolean imm = true;
    for (Token ln : lns) imm&= funType(ln, block);
    return imm;
  }
  
  public String toRepr() {
    StringBuilder s = new StringBuilder("{");
    boolean tail = false;
    for (Token v : tokens) {
      if (tail) s.append(" ⋄ ");
      s.append(v.toRepr());
      tail = true;
    }
    s.append("}");
    return s.toString();
  }
  
  
  public static String[] varnames(char t, boolean imm) {
    assert "fmda".indexOf(t)!=-1;
    switch ((t=='d'? 2 : t=='m'? 1 : 0) + (imm? 3 : 0)) { default: throw new IllegalStateException();
      //    𝕊𝕩𝕨𝕣𝕗𝕘 | 012345
      case 0: return new String[]{"𝕤","𝕩","𝕨"            }; // f  012··· | 𝕊𝕩𝕨···
      case 1: return new String[]{"𝕤","𝕩","𝕨","𝕣","𝕗"    }; // m  01234· | 𝕊𝕩𝕨𝕣𝕗·
      case 2: return new String[]{"𝕤","𝕩","𝕨","𝕣","𝕗","𝕘"}; // d  012345 | 𝕊𝕩𝕨𝕣𝕗𝕘
      case 3: return new String[]{                       }; // fi ······ | ······
      case 4: return new String[]{            "𝕣","𝕗"    }; // mi ···01· | 𝕣𝕗····
      case 5: return new String[]{            "𝕣","𝕗","𝕘"}; // di ···012 | 𝕣𝕗𝕘···
    }
  }
  public String[] defNames() {
    return varnames(type, immediate || type=='a');
  }
  
  
  
  public Value eval(Scope sc) {
    switch (this.type) {
      case 'f': return new FunBlock(this, sc);
      case 'm': return new Md1Block(this, sc);
      case 'd': return new Md2Block(this, sc);
      case '?': if (Main.vind) return new FunBlock(this, sc);
        /* fallthrough */
      case 'a': {
        Body b = bodiesD[0];
        return comp.exec(new Scope(sc, b.vars), b);
      }
      default: throw new IllegalStateException(this.type+"");
    }
  }
  
  public Value exec(Scope psc, Value w, Value[] vb) {
    Scope sc = new Scope(psc);
    for (Body b : w!=null? bodiesD : bodiesM) {
      sc.varNames = b.vars; // no cloning is suuuuurely fiiine
      sc.vars = new Value[b.vars.length]; // TODO decide some nicer way than just creating a new array per header
      System.arraycopy(vb, 0, sc.vars, 0, vb.length);
      sc.varAm = b.vars.length;
      Value res = comp.exec(sc, b);
      if (res != null) return res;
    }
    throw new DomainError("No header matched", this);
  }
}