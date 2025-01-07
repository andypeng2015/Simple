package com.seaofnodes.simple;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.fail;
import org.junit.Ignore;

public class Chapter18Test {

    @Test
    public void testJig() {
        CodeGen code = new CodeGen(
"""
return 0;
""");
        code.parse().opto().typeCheck().GCM();
        assertEquals("return 0;", code._stop.toString());
        assertEquals("0", Eval2.eval(code,  0));
    }

    @Test
    public void testJig2() {
        CodeGen code = new CodeGen(
"""
struct S { S? next; };
val f = { int dep, S s ->
    if (dep-- == 0) return s;
    S? v = f(dep, s);
    return v ? v.next : null;
};
return f(1, new S);
""");
        code.parse().opto().typeCheck().GCM();
        assertEquals("Stop[ return f( 1,S); return Phi(Region,Parm_s(f,*S {*S? !next; },S,s),Phi(Region,.next,null)); ]", code._stop.toString());
        assertEquals("null", Eval2.eval(code,  0));
        assertEquals("null", Eval2.eval(code,  1));
    }

    @Test
    public void testPhiParalleAssign() {
        CodeGen code = new CodeGen(
"""
int a = 1;
int b = 2;
while(arg--) {
  int t = a;
  a = b;
  b = t;
}
return a;
""");
        code.parse().opto().typeCheck().GCM();
        assertEquals("return Phi(Loop,1,Phi(Loop,2,Phi_a));", code._stop.toString());
        assertEquals("1", Eval2.eval(code,  0));
        assertEquals("2", Eval2.eval(code,  1));
        assertEquals("1", Eval2.eval(code,  2));
        assertEquals("2", Eval2.eval(code,  3));
    }


    // ---------------------------------------------------------------
    @Test
    public void testType0() {
        CodeGen code = new CodeGen(
"""
{int -> int}? x2 = null; // null function ptr
return x2;
""");
        code.parse().opto();
        assertEquals("return null;", code._stop.toString());
        assertEquals("null", Eval2.eval(code, 0 ) );
    }

    @Test
    public void testFcn0() {
        CodeGen code = new CodeGen(
"""
{int -> int}? sq = { int x ->
    x*x;
};
return sq;
""");
        code.parse().opto();
        assertEquals("Stop[ return { sq}; return (Parm_x(sq,int)*x); ]", code._stop.toString());
        assertEquals("{ sq}", Eval2.eval(code, 3));
    }

    @Test
    public void testFcn1() {
        CodeGen code = new CodeGen(
"""
var sq = { int x ->
    x*x;
};
return sq(arg)+sq(3);
""");
        code.parse().opto();
        assertEquals("Stop[ return (sq( 3)+sq( arg)); return (Parm_x(sq,int,3,arg)*x); ]", code._stop.toString());
        assertEquals("13", Eval2.eval(code, 2));
    }

    // Function scope test
    @Test
    public void testFcn2() {
        CodeGen code = new CodeGen(
"""
int cnt=1;
return { -> cnt; };
""");
        try { code.parse().opto(); fail(); }
        catch( Exception e ) { assertEquals("Variable 'cnt' is out of function scope and must be a final constant",e.getMessage()); }
    }

    // Function scope test
    @Test
    public void testFcn3() {
        CodeGen code = new CodeGen(
"""
val cnt=2;
return { -> cnt; }();
""");
        code.parse().opto();
        assertEquals("return 2;", code._stop.toString());
        assertEquals("2", Eval2.eval(code, 0));
    }

    // Function variables
    @Test
    public void testFcn4() {
        CodeGen code = new CodeGen(
"""
var fcn = arg ? { int x -> x*x; } : { int x -> x+x; };
return fcn(3);
""");
        code.parse().opto();
        assertEquals("Stop[ return Phi(Region,{ int -> int #1},{ int -> int #2})( 3); return (Parm_x($fun,int)*x); return (Parm_x($fun,int)*2); ]", code._stop.toString());
        assertEquals("6", Eval2.eval(code, 0));
        assertEquals("9", Eval2.eval(code, 1));
    }

    // Recursive factorial test
    @Test
    public void testFcn5() {
        CodeGen code = new CodeGen("val fact = { int x -> x <= 1 ? 1 : x*fact(x-1); }; return fact(arg);");
        code.parse().opto();
        assertEquals("Stop[ return fact( arg); return Phi(Region,1,(Parm_x(fact,int,arg,(x-1))*fact( Sub))); ]", code._stop.toString());
        assertEquals( "1", Eval2.eval(code, 0));
        assertEquals( "1", Eval2.eval(code, 1));
        assertEquals( "2", Eval2.eval(code, 2));
        assertEquals( "6", Eval2.eval(code, 3));
        assertEquals("24", Eval2.eval(code, 4));
    }

    @Test
    public void testFcn6() {
        CodeGen code = new CodeGen(
"""
struct S { int i; };
val newS = { int x -> return new S { i=x; }; };
return newS(1).i;
""");
        code.parse().opto().typeCheck().GCM();
        assertEquals("return 1;", code._stop.toString());
        assertEquals("1", Eval2.eval(code,  0));
    }

    // Double forward reference
    @Test
    public void testFcn7() {
        CodeGen code = new CodeGen(
"""
if( arg ? f : g ) return 1;
val f = {->1;};
val g = {->2;};
return 2;
""");
        code.parse().opto().typeCheck().GCM();
        assertEquals("Stop[ return 1; return 1; return 2; ]", code._stop.toString());
        assertEquals("1", Eval2.eval(code,  0));
    }


    // Function break
    @Test
    public void testErr1() {
        CodeGen code = new CodeGen(
"""
for(;;) {
    val f = { ->
        break;
    };
    f();
    return 2;
}
return 1;
""");
        try { code.parse().opto(); fail(); }
        catch( Exception e ) { assertEquals("No active loop for a break or continue",e.getMessage()); }
    }

    // Calling and inlining a null function
    @Test
    public void testErr2() {
        CodeGen code = new CodeGen(
"""
{int -> int}? i2i = { int i -> return i; };
for(;;) {
    if (i2i(2) == arg) break;
    i2i = null;
}
""");
        try { code.parse().opto().typeCheck(); fail(); }
        catch( Exception e ) { assertEquals("Might be null calling { int -> int #1}?",e.getMessage()); }
    }


    @Test
    public void testErr3() {
        CodeGen code = new CodeGen(
"""
val f = { int i, int j -> return i+j; };
return f();
""");
        try { code.parse().opto().typeCheck(); fail(); }
        catch( Exception e ) { assertEquals("Expecting 2 arguments, but found 0",e.getMessage()); }
    }

    // Mutual recursion.  Fails without SCCP to lift the recursive return types.
    @Ignore @Test
    public void testFcnMutRec() {
        CodeGen code = new CodeGen(
"""
val is_even = { int x -> x ? is_odd (x-1) : true ; };
val is_odd  = { int x -> x ? is_even(x-1) : false; };
return is_even(arg);
""");
        code.parse().opto();
        assertEquals("Stop[ return is_even( arg); return Phi(Region,Phi(Region,is_even( ((Parm_x(is_even,int,arg,Sub)-1)-1)),0),1); ]", code._stop.toString());
        assertEquals("1", Eval2.eval(code, 0));
        assertEquals("0", Eval2.eval(code, 1));
        assertEquals("1", Eval2.eval(code, 2));
        assertEquals("0", Eval2.eval(code, 3));
    }

    // Forward ref to 'x' means that
    @Ignore @Test
    public void testForwardRef1() {
        CodeGen code = new CodeGen(
"""
struct S {
    { int } f = { -> return x(); };
};
val x = { -> return 1; };
S? s = null;
for(;;) {
    if (s) return s.f;
}
""");
        code.parse().opto().typeCheck().GCM();
        assertEquals("return 0;", code._stop.toString());
        assertEquals("0", Eval2.eval(code,  0));
    }

}
