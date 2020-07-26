package APL.tools;

import APL.*;
import APL.errors.*;
import APL.tokenizer.types.DfnTok;
import APL.types.*;
import APL.types.functions.*;
import APL.types.functions.builtins.Quad;
import APL.types.functions.builtins.fns2.EvalBuiltin;
import APL.types.functions.trains.*;

import java.util.*;

import static APL.Comp.*;

public class JComp {
  public final JFn r;
  HashMap<Const, Integer> constants = new HashMap<>();
  static int ctr = 0;
  public JComp(Comp comp) {
    
    
    
    
    ArrayList<MA> methods = new ArrayList<>();
    ArrayList<Fld> fields = new ArrayList<>();
  
    {
      MA fn = new MA(0x1001, "<init>", "()V", 1);
      fn.aload(0);
      fn.invspec(JFn.class, "<init>", "()V");
      fn.vret();
      fn.mstack = 1;
      methods.add(fn);
    }
    
    {
      
      
      MA fn = new MA(0x0001, "get", met(Value.class, Scope.class, int.class), 3);
      // aload 0 - this (JFn); 1 - scope; 2 - offset; 3,4,5 - temp values
      int SC   = 1;
      int OFF  = 2;
      int TMP  = 3;
      int TMP2 = 4;
      int TMP3 = 5;
      // fields.add(new Fld(0x0009, "vals", name(Value[].class))); // ACC_PUBLIC ACC_STATIC ACC_FINAL
      
      
      MutIntArr offs = new MutIntArr(2); // todo store needed offsets in Comp
      
      offs.add(0);
      {
        int i = 0;
        while (i<comp.bc.length-1) {
          if (comp.bc[i]==RETN) offs.add(i+1);
          i = comp.next(i);
        }
      }
      MA.Lbl[] bodyBlocks = new MA.Lbl[offs.sz];
      for (int i = 0; i < bodyBlocks.length; i++) bodyBlocks[i] = fn.lbl();
      int cbody = 0;
      
      if (offs.sz!=1) {
        MA.Lbl def = fn.lbl();
        fn.iload(OFF);
        fn.lookupswitch(def, bodyBlocks.length);
        for (int i = 0; i < bodyBlocks.length; i++) fn.lookup(offs.is[i], bodyBlocks[i]);
        
        def.here();
        fn.new_(ImplementationError.class);
        fn.dup();
        fn.ldc("bad starting offset");
        fn.invspec(ImplementationError.class, "<init>", met(void.class, String.class));
        fn.athrow();
        
        bodyBlocks[cbody++].here();
      }
      
      
      byte[] bc = comp.bc;
      int i = 0;
      int cstack=0, mstack=0;
      while (i != bc.length) {
        int pi = i;
        i++;
        switch (bc[pi]) { default: throw new DomainError("Unsupported bytecode "+bc[pi]);
          case PUSH: {
            int n=0,h=0,b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            fn.aload(0);
            fn.getfield(JFn.class, "vals", Value[].class);
            fn.iconst(n);
            fn.aaload();
            mstack = Math.max(mstack, cstack+2);
            cstack++;
            break;
          }
          
          case VARO: {
            int n=0,h=0,b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            fn.aload(SC);
            fn.ldc(comp.strs[n]);
            fn.invvirt(Scope.class, "getC", met(Value.class, String.class));
            mstack = Math.max(mstack, cstack+4);
            cstack++;
            break;
          }
          case VARM: {
            int n=0,h=0,b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            fn.new_(Variable.class); fn.dup();
            fn.ldc(comp.strs[n]);
            fn.invspec(Variable.class, "<init>", met(void.class, String.class));
            mstack = Math.max(mstack, cstack+3);
            cstack++;
            break;
          }
  
  
          case LOCO: {
            int depth = bc[i++];
            int n=0,h=0,b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            fn.aload(SC);
            for (int j = 0; j < depth; j++) fn.getfield(Scope.class, "parent", Scope.class);
            fn.getfield(Scope.class, "vars", Value[].class);
            fn.iconst(n); fn.aaload();
            mstack = Math.max(mstack, cstack+2);
            cstack++;
            break;
          }
          case LOCM: {
            int depth = bc[i++];
            int n=0,h=0,b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            fn.new_(Local.class); fn.dup();
            fn.iconst(depth); fn.iconst(n);
            fn.invspec(Local.class, "<init>", met(void.class, int.class, int.class));
            mstack = Math.max(mstack, cstack+4);
            cstack++;
            break;
          }
          
          
          case ARRO: {
            int n=0,h=0,b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            fn.iconst(n); fn.anewarray(Value.class);
            for (int j = 0; j < n; j++) {
                                // .. v a
              fn.dup_x1();      // .. a v a
              fn.swap();        // .. a a v
              fn.iconst(n-j-1); // .. a a v n
              fn.swap();        // .. a a n v
              fn.aastore();     // .. a
            }
            fn.invstat(Arr.class, "create", met(Arr.class, Value[].class));
            
            mstack = Math.max(cstack+3, mstack);
            cstack-= n-1;
            break;
          }
          case ARRM: {
            int n=0,h=0,b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            
            fn.iconst(n); fn.anewarray(Settable.class);
            for (int j = 0; j < n; j++) {
              // .. v a
              fn.dup_x1();      // .. a v a
              fn.swap();        // .. a a v
              fn.iconst(n-j-1); // .. a a v n
              fn.swap();        // .. a a n v
              fn.aastore();     // .. a
            }
            fn.new_(SettableArr.class); //   a o
            fn.dup_x1();                // o a o
            fn.swap();                  // o o a
            fn.invspec(SettableArr.class, "<init>", met(void.class, Settable[].class));
  
            mstack = Math.max(cstack+3, mstack);
            cstack-= n-1;
            break;
          }
          case FN1C: {
            fn.invvirt(Value.class, "asFun", met(Fun.class));
            fn.swap();
            fn.invvirt(Fun.class, "call", met(Value.class, Value.class));
            cstack--;
            break;
          }
          case FN2C: {
            fn.astore(TMP);
            fn.invvirt(Value.class, "asFun", met(Fun.class));
            fn.swap();
            fn.aload(TMP);
            fn.swap();
            fn.invvirt(Fun.class, "call",  met(Value.class, Value.class, Value.class));
            cstack-= 2;
            break;
          }
          case FN1O: { MA.Lbl l1 = fn.lbl(), l2 = fn.lbl();
            
            fn.invvirt(Value.class, "asFun", met(Fun.class)); // note that this executes even when 𝕩 is ·; ¯\\_(ツ)_/¯
                       // x f
            fn.swap(); // f x
            fn.dup();  // f x x
            fn.is(Nothing.class); // f x B
            fn.ifeq0(l1); // if (x instanceof Nothing) {
              fn.swap(); // x f
              fn.pop();  // x   aka   ·
              fn.goto_(l2);
            l1.here();    // } else {
              fn.invvirt(Fun.class, "call", met(Value.class, Value.class));
            l2.here();    // }
            mstack = Math.max(mstack, cstack+1);
            cstack--;
            break;
          }
          case FN2O: { MA.Lbl l1=fn.lbl(), l2=fn.lbl(), l3=fn.lbl();
            // x f w
            fn.astore(TMP); // x f
            fn.invvirt(Value.class, "asFun", met(Fun.class));
            fn.swap(); // f x
            fn.dup();  // f x x
            fn.is(Nothing.class); // f x B
            fn.ifeq0(l1); // if (x instanceof Nothing) {
              fn.swap(); fn.pop();
              fn.goto_(l3);
            l1.here();    // } else {
              fn.aload(TMP);
              fn.is(Nothing.class);
              fn.ifeq0(l2); // if (w instanceof Nothing) {
                fn.invvirt(Fun.class, "call", met(Value.class, Value.class));
                fn.goto_(l3);
              l2.here();    // } else {
                fn.aload(TMP);
                fn.swap();
                fn.invvirt(Fun.class, "call", met(Value.class, Value.class, Value.class));
                            // }
            l3.here();    // }
            mstack = Math.max(mstack, cstack+1);
            cstack-= 2;
            break;
          }
          case OP1D: {
            fn.swap(); fn.cast(Mop.class); fn.swap();
            
            fn.invvirt(Mop.class, "derive", met(Fun.class, Value.class));
            cstack--;
            break;
          }
          case OP2D: {                //                ; g d f
            fn.astore(TMP);           // store f        ; g d
            fn.cast(Dop.class);       // cast d to dop  ; g D
            fn.swap();                // place d below f; D g
            fn.aload(TMP); fn.swap(); // place g below f; D f g
            fn.invvirt(Dop.class, "derive", met(Fun.class, Value.class, Value.class));
            cstack-= 2;
            break;
          }
          case OP2H: {
            // g d
            fn.cast(Dop.class);
            fn.swap();
            fn.invvirt(Dop.class, "derive", met(Mop.class, Value.class));

            cstack--;
            break;
          }
          case TR2D: {
            fn.invvirt(Value.class, "asFun", met(Fun.class)); fn.astore(TMP );
            fn.invvirt(Value.class, "asFun", met(Fun.class)); fn.astore(TMP2);
            
            fn.new_(Atop.class); fn.dup();
            fn.aload(TMP); fn.aload(TMP2);
            fn.invspec(Atop.class, "<init>", met(void.class, Fun.class, Fun.class));
            mstack = Math.max(mstack, cstack+2);
            cstack--;
            break;
          }
          case TR3D: {
                                                              fn.astore(TMP ); // f
            fn.invvirt(Value.class, "asFun", met(Fun.class)); fn.astore(TMP2); // g
            fn.invvirt(Value.class, "asFun", met(Fun.class)); fn.astore(TMP3); // h
  
            fn.new_(Fork.class); fn.dup();
            fn.aload(TMP); fn.aload(TMP2); fn.aload(TMP3);
            fn.invspec(Fork.class, "<init>", met(void.class, Value.class, Fun.class, Fun.class));
            mstack = Math.max(mstack, cstack+2);
            cstack-= 2;
            break;
          }
          case TR3O: { MA.Lbl l1 = fn.lbl(), l2 = fn.lbl();
                                                              fn.astore(TMP ); // f
            fn.invvirt(Value.class, "asFun", met(Fun.class)); fn.astore(TMP2); // g
            fn.invvirt(Value.class, "asFun", met(Fun.class)); fn.astore(TMP3); // h
            
            fn.aload(TMP);
            fn.is(Nothing.class);
            fn.ifeq0(l1); // if (f instanceof Nothing) {
              fn.new_(Atop.class); fn.dup();
              fn.aload(TMP2); fn.aload(TMP3); // g h
              fn.invspec(Atop.class, "<init>", met(void.class, Fun.class, Fun.class));
              fn.goto_(l2);
            l1.here();    // } else {
              fn.new_(Fork.class); fn.dup();
              fn.aload(TMP); fn.aload(TMP2); fn.aload(TMP3); // f g h
              fn.invspec(Fork.class, "<init>", met(void.class, Value.class, Fun.class, Fun.class));
            l2.here();    // }
            mstack = Math.max(mstack, cstack+2);
            cstack-= 2;
            break;
          }
          case SETU:
          case SETN: {
            fn.swap(); fn.dup_x1(); // v k → v k v
            fn.iconst(bc[pi]==SETU? 1 : 0); fn.aload(SC); fn.aconst_null();
            fn.invvirt(Settable.class, "set", met(void.class, Value.class, boolean.class, Scope.class, Callable.class));
            mstack = Math.max(mstack, cstack+4);
            cstack--;
            break;
          }
          case SETM: {
            // v f k     →
            // v k F K v
            
                            // v f k
            fn.swap();      // v k f
            fn.invvirt(Value.class, "asFun", met(Fun.class)); // v k F
            fn.astore(TMP); // v k
            fn.dup2();      // v k v k
            fn.aload(SC);   // v k v k sc
            fn.invvirt(Settable.class, "get", met(Value.class, Scope.class)); // v k v K
            fn.aload(TMP);  // v k v K F
            fn.dup_x2();    // v k F v K F
            fn.pop();       // v k F v K
            fn.swap();      // v k F K v
            fn.invvirt(Fun.class, "call",  met(Value.class, Value.class, Value.class)); // v k n
            fn.iconst(1); fn.aload(SC); fn.aconst_null(); // v k n true sc null
            fn.invvirt(Settable.class, "set", met(void.class, Value.class, boolean.class, Scope.class, Callable.class));
            mstack = Math.max(mstack, cstack+3);
            cstack-= 2;
            break;
          }
          case POPS: {
            fn.pop();
            cstack--;
            break;
          }
          case DFND: {
            int n=0,h=0; byte b; do { b = bc[i]; n|= (b&0x7f)<<h; h+=7; i++; } while (b<0);
            fn.aload(0);
            fn.getfield(JFn.class, "dfns", DfnTok[].class);
            fn.iconst(n);
            fn.aaload();
            fn.aload(SC);
            fn.invvirt(DfnTok.class, "eval", met(Value.class, Scope.class));
            mstack = Math.max(mstack, cstack+3);
            cstack++;
            break;
          }
          case CHKV: { MA.Lbl l = fn.lbl();
            fn.dup();
            fn.is(Nothing.class);
            fn.ifeq0(l);
              fn.astore(TMP);
              fn.new_(ValueError.class); fn.dup();
              fn.ldc("Didn't expect · here"); fn.aload(TMP);
              fn.invspec(ValueError.class, "<init>", met(void.class, String.class, Tokenable.class));
              fn.athrow();
            l.here();
            break;
          }
          case RETN: {
            if (cstack!=1) throw new ImplementationError("stack size at RETN was "+cstack); // not an absolute requirement, can be removed if need be
            cstack = 0;
            fn.aret();
            bodyBlocks[cbody++].here();
            break;
          }
          case SPEC: {
            switch(bc[i++]) {
              case EVAL:
                fn.new_(EvalBuiltin.class); fn.dup();
                fn.aload(SC);
                fn.invspec(EvalBuiltin.class, "<init>", met(void.class, Scope.class));
                break;
              case STDOUT:
                fn.new_(Quad.class); fn.dup();
                fn.aload(SC);
                fn.invspec(Quad.class, "<init>", met(void.class, Scope.class));
                break;
              case STDIN:
                fn.new_(Quad.class); fn.dup();
                fn.aload(SC);
                fn.invspec(Quad.class, "<init>", met(void.class, Scope.class));
                fn.invvirt(Quad.class, "get", met(Value.class));
                break;
              default:
                throw new InternalError("Unknown special "+bc[i-1]);
            }
            mstack = Math.max(mstack, cstack+3);
            cstack++;
            break;
          }
        }
      }
      if (cstack!=1) throw new ImplementationError("stack size at end was "+cstack); // see RETN
      fn.mstack = mstack;
      fn.aret();
      methods.add(fn);
    }
    
    
    String name = "gen" + ctr++;
    int this_class = CONSTANT_Class("BQN/"+name);
    int super_class = CONSTANT_Class(JFn.class);
    int code_att = CONSTANT_Utf8("Code");
    // int smt_att = CONSTANT_Utf8("StackMapTable");
    
    
    
    BA res = new BA();
    res.b(0xCA,0xFE,0xBA,0xBE); // magic
    res.u2(0x00); res.u2(49); // version
    res.u2(constants.size()+1); // constant count
    
    byte[][] map = new byte[constants.size()][];
    constants.forEach((k, v) -> map[v-1] = k.bs);
    for (byte[] c : map) res.b(c); // constants
    
    res.u2(0x0021); // access flags; ACC_PUBLIC ACC_SUPER
    res.u2(this_class);
    res.u2(super_class);
    res.u2(0); // interface count
    // no interfaces
    
    res.u2(fields.size()); // field count
    for (Fld f : fields) {
      res.u2(f.acc);
      res.u2(f.name);
      res.u2(f.type);
      res.u2(0); // attribute count
    }
    
    res.u2(methods.size()); // method count
    for (MA c : methods) {
      c.finish();
      res.u2(c.acc);
      res.u2(c.name);
      res.u2(c.type);
      res.u2(1); // attribute count
      res.u2(code_att);
      res.u4(12+c.len); // 20+c.len+c.smt.len with stackmapable attribute
      res.u2(c.mstack); // max_stack
      res.u2(c.localc); // max locals
      res.u4(c.len);
      res.b(c.get());
      res.u2(0); // exception table length
      // no exceptions
      res.u2(0);
      // res.u2(1); // attribute count
      // res.u2(smt_att);
      // res.u4(c.smt.len+2);
      // res.u2(c.smtc);
      // res.b(c.smt.get());
    }
    res.u2(0); // attribute count
    // no attributes
    // try {
    //   Files.write(Path.of("src","APL","tools","gen0.class"), res.get());
    // } catch (Throwable e) {
    //   e.printStackTrace();
    // }
    Class<?> def = l.def(null, res.get(), 0, res.len);
    try {
      JFn o = (JFn) def.getDeclaredConstructor().newInstance();
      o.vals = comp.objs;
      o.dfns = comp.dfns;
      // Method get = o.getClass().getDeclaredMethod("get", Scope.class);
      // System.out.println(get.invoke(o, new Object[]{null}));
      r = o;
    } catch (Throwable e) {
      throw new ImplementationError(e);
    }
  }
  
  
  
  static final Ldr l = new Ldr();
  static class Ldr extends ClassLoader {
    public Class<?> def(String name, byte[] b, int off, int len) throws ClassFormatError {
      return defineClass(name, b, off, len);
    }
  }
  
  
  
  public int CONSTANT_Utf8(String s) {
    BA bs = new BA();
    bs.b(1);
    bs.u2(0); // placeholder
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i)&0xffff;
      if (c<=0x7f && c!=0) {
        bs.b(c);
      } else if (c<=0x07ff) {
        bs.b(0b1100_0000 | c>> 6);
        bs.b(0b1000_0000 | c     & 0b0011_1111);
      } else {
        bs.b(0b1110_0000 | c>>12);
        bs.b(0b1000_0000 | (c>>6)& 0b0011_1111);
        bs.b(0b1000_0000 | c     & 0b0011_1111);
      }
    }
    u2(bs.bs, 1, bs.len-3);
    return get(bs.get());
    // System.out.println(Arrays.toString(sb));
    // byte[] bs = new byte[sb.length+3]; bs[0] = 1; // tag
    // u2(bs, 1, sb.length);
    // ins(bs, 3, sb);
    // return get(bs);
  }
  public int CONSTANT_Class(Class<?> c) {
    return CONSTANT_Class(name(c));
  }
  public int CONSTANT_Class(String s) {
    byte[] bs = new byte[3]; bs[0] = 7; // tag
    u2(bs, 1, CONSTANT_Utf8(s));
    return get(bs);
  }
  public int CONSTANT_NameAndType(String name, String type) {
    byte[] bs = new byte[5]; bs[0] = 12; // tag
    u2(bs, 1, CONSTANT_Utf8(name));
    u2(bs, 3, CONSTANT_Utf8(type));
    return get(bs);
  }
  public int CONSTANT_Methodref(String cls, String name, String type) {
    byte[] bs = new byte[5]; bs[0] = 10; // tag
    u2(bs, 1, CONSTANT_Class(cls));
    u2(bs, 3, CONSTANT_NameAndType(name, type));
    return get(bs);
  }
  public int CONSTANT_Fieldref(String cls, String name, String type) {
    byte[] bs = new byte[5]; bs[0] = 9; // tag
    u2(bs, 1, CONSTANT_Class(cls));
    u2(bs, 3, CONSTANT_NameAndType(name, type));
    return get(bs);
  }
  
  public int get(byte[] bs) {
    Const o = new Const(bs);
    Integer r = constants.get(o);
    if (r!=null) return r;
    int n = constants.size()+1;
    if (n==32767) throw new DomainError("java constant pool limit exceeded");
    constants.put(o, n);
    // System.out.println(Arrays.toString(bs)+": "+n);
    return n;
  }
  
  
  static void u2(byte[] a, int pos, int v) {
    a[pos  ] = (byte) ((v>>8)&0xff);
    a[pos+1] = (byte) ( v    &0xff);
  }
  static void u4(byte[] a, int pos, int v) {
    a[pos  ] = (byte) ((v>>24)&0xff);
    a[pos+1] = (byte) ((v>>16)&0xff);
    a[pos+2] = (byte) ((v>> 8)&0xff);
    a[pos+3] = (byte) ( v     &0xff);
  }
  static void ins(byte[] a, int pos, byte[] v) {
    System.arraycopy(v, 0, a, pos, v.length);
  }
  
  
  static String name(Class<?> c) {
    return c.getName().replace(".", "/");
  }
  static String fname(Class<?> c) {
    if (c.isPrimitive()) {
        if (c==boolean.class) return "Z";
        if (c==byte   .class) return "B";
        if (c==char   .class) return "C";
        if (c==double .class) return "D";
        if (c==float  .class) return "F";
        if (c==int    .class) return "I";
        if (c==long   .class) return "J";
        if (c==short  .class) return "S";
        if (c==void   .class) return "V";
    }
    String n = c.getName().replace('.', '/');
    if (c.isArray()) return n;
    return "L"+n+";";
  }
  static String met(Class<?> ret, Class<?>... args) {
    StringBuilder res = new StringBuilder("(");
    for (Class<?> c : args) res.append(fname(c));
    res.append(')').append(fname(ret));
    return res.toString();
  }
  
  
  static class Const implements Comparable<Const> {
    byte[] bs;
  
    public Const(byte[] bs) { this.bs = bs; }
  
    public int compareTo(JComp.Const o) {
      int c;
      c = Integer.compare(bs.length, o.bs.length);
      if(c!=0) return c;
      for (int i = 0; i < bs.length; i++) {
        c = Byte.compare(bs[i], o.bs[i]);
        if (c!=0) return c;
      }
      return 0;
    }
    
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Const)) return false;
      Const that = (Const) o;
      
      return Arrays.equals(this.bs, that.bs);
    }
    
    public int hashCode() { return Arrays.hashCode(bs); }
  
    public String toString() {
      return Arrays.toString(bs);
    }
  }
  
  
  static class BA {
    byte[] bs = new byte[20];
    int len;
    void b(int v) {
      if (len>=bs.length) dbl();
      bs[len++] = (byte) v;
    }
    void b(byte... v) {
      while (len+v.length>=bs.length) dbl();
      System.arraycopy(v,0,bs,len,v.length);
      len+= v.length;
    }
    void b(int... v) {
      while (len+v.length>=bs.length) dbl();
      for (int i = 0; i < v.length; i++) bs[i+len] = (byte) v[i];
      len+= v.length;
    }
    void u2(int v) {
      b((v>>8)&0xff);
      b( v    &0xff);
    }
    void u4(int v) {
      b((v>>24)&0xff);
      b((v>>16)&0xff);
      b((v>> 8)&0xff);
      b( v     &0xff);
    }
    void dbl() {
      bs = Arrays.copyOf(bs, bs.length*2);
    }
    byte[] get() {
      return Arrays.copyOf(bs, len);
    }
  }
  
  class MA extends BA {
    public final int acc, name, type;
    public int mstack = 0; // max stack size
    public int localc; // local variable count
    
    // public BA smt = new BA(); // StackMapTable bytes
    // public int smtc; // StackMapTable entry count 
  
    public MA(int acc, String name, String type, int argc) { // argc should include `this` if applicable
      this.acc = acc;
      this.name = CONSTANT_Utf8(name);
      this.type = CONSTANT_Utf8(type);
      this.localc = argc; // in case the arguments aren't used
    }
  
    void aload(int i) { // get local variable
      localc = Math.max(i+1, localc);
      if (i<4) b(42+i);
      else if (i<256) b(25, i);
      else throw new NYIError("aload>255");
    }
    void astore(int i) { // store local variable
      localc = Math.max(i+1, localc);
      if (i<4) b(75+i);
      else if (i<256) b(58, i);
      else throw new NYIError("astore>255");
    }
    void iload(int i) { // get local var int
      localc = Math.max(i+1, localc);
      if (i<4) b(26+i);
      else if (i<256) b(21, i);
      else throw new NYIError("iload>255");
    }
    
    void aaload () { b(50); } // get array item
    void aastore() { b(83); } // set array item
    
    void aconst_null() { b(1); } // push null
    void iconst(int i) { // push integer constant
      if (i>=-1 && i<=5) b(3+i);
      else if (( byte)i == i) b(16, i);
      else if ((short)i == i) { b(17); u2(i); }
      // else if (constants.size()<254) {
      //   b(18); b()
      // }
      else throw new NYIError("iconst outside short range");
    }
    public void ldc(String str) {
      byte[] bs = new byte[3]; bs[0] = 8;
      JComp.u2(bs, 1, CONSTANT_Utf8(str));
      int v = JComp.this.get(bs);
      if (v < 256) b(18, v);
      else { b(19); u2(v); }
    }
  
    void invvirt (String cls, String name, String type) { b(182); u2(CONSTANT_Methodref(cls, name, type)); } // invoke virtual
    void invspec (String cls, String name, String type) { b(183); u2(CONSTANT_Methodref(cls, name, type)); } // invoke special
    void invstat (String cls, String name, String type) { b(184); u2(CONSTANT_Methodref(cls, name, type)); } // invoke static
    void getfield(String cls, String name, String type) { b(180); u2(CONSTANT_Fieldref (cls, name, type)); } // get field from pop
  
    void invvirt (Class<?> cls, String name, String   type) { invvirt (name(cls), name,      type ); }
    void invspec (Class<?> cls, String name, String   type) { invspec (name(cls), name,      type ); }
    void invstat (Class<?> cls, String name, String   type) { invstat (name(cls), name,      type ); }
    void getfield(Class<?> cls, String name, Class<?> type) { getfield(name(cls), name, name(type)); }
    
    public void new_     (String cls) { b(187); u2(CONSTANT_Class(cls)); }
    public void anewarray(String cls) { b(189); u2(CONSTANT_Class(cls)); }
    public void is       (String cls) { b(193); u2(CONSTANT_Class(cls)); }
    
    public void new_     (Class<?> cls) { new_     (name(cls)); }
    public void anewarray(Class<?> cls) { anewarray(name(cls)); }
    public void is       (Class<?> cls) { is       (name(cls)); }
    
    public void cast(String cls) {
      b(192);
      u2(CONSTANT_Class(cls));
    }
    public void cast(Class<?> c) { cast(name(c)); }
    
    void vret() { b(177); } // return void
    void aret() { b(176); } // return object
    void athrow(){b(191); } // throw ToS
    
    void swap  () { b( 95); } // ab → ba
    void dup   () { b( 89); } // a → aa
    void dup2  () { b( 92); } // ab → abab
    void dup_x1() { b( 90); } // ab → bab
    void dup_x2() { b( 91); } // abc → cabc
    void pop   () { b( 87); } // .. a → ..
    
    public void ifeq0   (Lbl l) { b(153); l.add2(); }
    public void ifne0   (Lbl l) { b(154); l.add2(); }
    public void iflt0   (Lbl l) { b(155); l.add2(); }
    public void ifge0   (Lbl l) { b(156); l.add2(); }
    public void ifgt0   (Lbl l) { b(157); l.add2(); }
    public void ifle0   (Lbl l) { b(158); l.add2(); }
    public void goto_   (Lbl l) { b(167); l.add2(); }
    public void ifnenull(Lbl l) { b(199); l.add2(); } // branch if pop!=null
    
    
    int lookupStart;
    public void lookupswitch(Lbl def, int count) {
      lookupStart = len;
      b(171);
      while (len%4!=0) b(0);
      def.add4(len, lookupStart);
      u4(count);
    }
    public void lookup(int match, Lbl to) {
      u4(match);
      to.add4(len, lookupStart);
    }
    
    ArrayList<Lbl> ls = new ArrayList<>();
    public Lbl lbl() {
      Lbl c = new Lbl();
      ls.add(c);
      return c;
    }
  
    public class Lbl {
      MutIntArr fr2 = new MutIntArr(1);
      MutIntArr fr4p= new MutIntArr(1);
      MutIntArr fr4r= new MutIntArr(1);
      int pos = -1;
      public void add2() {
        fr2.add(len);
        u2(-1);
      }
      public void add4(int pos, int rel) {
        fr4p.add(pos);
        fr4r.add(rel);
        u4(-1);
      }
  
      public void here() {
        assert pos==-1;
        pos = len;
      }
    }
    
    void finish() {
      // ArrayList<Integer> lend = new ArrayList<>();
      for (Lbl l : ls) {
        int to = l.pos;
        if (to==-1 && (l.fr2.sz!=0 || l.fr4p.sz!=0)) throw new ImplementationError("unset label");
        for (int i = 0; i < l.fr2.sz; i++) {
          int from = l.fr2.is[i];
          JComp.u2(bs, from, to-from+1); // TODO split fr2 into p/r
        }
        for (int i = 0; i < l.fr4p.sz; i++) {
          JComp.u4(bs, l.fr4p.is[i], to-l.fr4r.is[i]);
        }
      }
      // Collections.sort(lend);
      // int prev = 0;
      // for (Integer c : lend) {
      //   if (c==prev) continue;
      //   int off = c-prev-1;
      //   if(off>63) throw new NYIError("StackMapTable with delta>63");
      //   smt.b(off);
      //   smtc++;
      //   prev = c;
      // }
    }
  }
  
  class Fld {
    public final int acc, name, type;
  
    Fld(int acc, String name, String type) {
      this.acc = acc;
      this.name = CONSTANT_Utf8(name);
      this.type = CONSTANT_Utf8(type);
    }
  }
}
