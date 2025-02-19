package com.seaofnodes.simple;

import com.seaofnodes.simple.evaluator.Evaluator;
import com.seaofnodes.simple.node.StopNode;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Chapter15Test {

    @Test
    public void testJig() {
        Parser parser = new Parser(
"""
return 3.14;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 3.14;", stop.toString());
        assertEquals(3.14, Evaluator.evaluate(stop,  0));
    }

    @Test
    public void testCyclic() {
        Parser parser = new Parser(
"""
struct C { C? l; };
C !c = new C;
c.l = c;
return c;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return C;", stop.toString());
        assertEquals("Obj<C>@1{l=obj@1}", Evaluator.evaluate(stop,  0).toString());
    }

    @Test
    public void testSafetyCheck() {
        Parser parser = new Parser(
"""
u8[] old = new u8[0];
u8[] !output = new u8[1];
int i = 0;
while (i < old#) {
    output[i] = old[i];
    i = i + 1;
}
output[i] = 1;
return output;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return [u8];", stop.toString());
        assertEquals("\u0001", Evaluator.evaluate(stop,  0).toString());
    }

    @Test
    public void testBasic1() {
        Parser parser = new Parser(
"""
int[] is = new int[2];
return is[1];
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 0;", stop.toString());
        assertEquals(0L, Evaluator.evaluate(stop,  0));
    }

    @Test
    public void testBasic2() {
        Parser parser = new Parser(
"""
int[] is = new int[2];
int[] is2 = new int[2];
return is[1];
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 0;", stop.toString());
        assertEquals(0L, Evaluator.evaluate(stop,  0));
    }

    @Test
    public void testBasic3() {
        Parser parser = new Parser(
"""
int[] !a = new int[2];
a[0] = 1;
a[1] = 2;
return a[0];
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 1;", stop.toString());
        assertEquals(1L, Evaluator.evaluate(stop,  0));
    }

    @Test
    public void testBasic4() {
        Parser parser = new Parser(
"""
struct A { int i; };
A?[] !a = new A?[2];
return a;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return [*A?];", stop.toString());
        assertEquals("Obj<[*A?]>{#=2,[]=[null,null]}", Evaluator.evaluate(stop, 0).toString());
    }

    @Test
    public void testBasic5() {
        Parser parser = new Parser(
"""
struct S { int x; flt y; };
// A new S
S !s = new S; s.x=99; s.y = 3.14;

// Double-d array of Ss.  Fill in one row.
S?[]?[] !iss = new S?[]?[2];
iss[0] = new S?[7];
iss[0][2] = s;

// Now pull out the filled-in value, with null checks
flt rez;
S?[]? is = iss[arg];
if( !is ) rez = 1.2;
else {
    S? i = is[2];
    if( !i ) rez = 2.3;
    else rez = i.y;
}
return rez;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return Phi(Region,1.2,Phi(Region,2.3,3.14));", stop.toString());
        assertEquals(3.14, Evaluator.evaluate(stop, 0));
        assertEquals(1.2 , Evaluator.evaluate(stop, 1));
    }

    @Test
    public void testBasic6() {
        Parser parser = new Parser(
"""
struct S { int x; flt y; };
// A new S
S !s = new S; s.x=99; s.y = 3.14;

// Double-d array of Ss.  Fill in one row.
S?[]?[] iss = new S?[]?[2];
iss[0] = new S?[7];
iss[0][2] = s;

// Now pull out the filled-in value, with null checks
flt rez = 1.2;
if( iss[arg] )
    if( iss[arg][2] )
        rez = iss[arg][2].y;
return rez;
""");
        try { parser.parse().iterate(); fail(); }
        catch( Exception e ) { assertEquals("Might be null accessing 'y'",e.getMessage()); }
    }

    @Test
    public void testTree() {
        Parser parser = new Parser(
"""
// Can we define a forward-reference array?
struct Tree { Tree?[]? _kids; };
Tree !root = new Tree;
root._kids = new Tree?[2]; // NO BANG SO ARRAY IS OF IMMUTABLE TREES????
root._kids[0] = new Tree;
return root;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return Tree;", stop.toString());
        assertEquals("Obj<Tree>{_kids=Obj<[*Tree?]>{#=2,[]=[Obj<Tree>{_kids=null},null]}}", Evaluator.evaluate(stop,  0).toString());
    }

    @Test
    public void testNestedStructAddMemProj() {
        Parser parser = new Parser(
"""
struct S { int a; int[] b; };
return 0;
""");
        StopNode stop = parser.parse();
        assertEquals("return 0;", stop.toString());
    }

    @Test
    public void testRollingSum() {
        Parser parser = new Parser(
"""
int[] !ary = new int[arg];
// Fill [0,1,2,3,4,...]
int i=0;
while( i < ary# ) {
    ary[i] = i;
    i = i+1;
}
// Fill [0,1,3,6,10,...]
i=0;
while( i < ary# - 1 ) {
    ary[i+1] = ary[i+1] + ary[i];
    i = i+1;
}
return ary[1] * 1000 + ary[3]; // 1 * 1000 + 6
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return (.[]+(.[]*1000));", stop.toString());
        assertEquals(1006L, Evaluator.evaluate(stop,  4));
    }

    @Test
    public void sieveOEratosthenes() {
        Parser parser = new Parser(
"""
int[] !ary = new int[arg], !primes = new int[arg];
int nprimes = 0, p=2;
// Find primes while p^2 < arg
while( p*p < arg ) {
    // skip marked non-primes
    while( ary[p]==1 ) p = p + 1;
    // p is now a prime
    primes[nprimes] = p;  nprimes = nprimes + 1;
    // Mark out the rest non-primes
    int i = p + p;
    while( i < ary# ) {
        ary[i] = 1;
        i = i + p;
    }
    p = p + 1;
}
// Now just collect the remaining primes, no more marking
while( p < arg ) {
    if( ary[p] == 0 ) {
        primes[nprimes] = p;  nprimes = nprimes + 1;
    }
    p = p + 1;
}
// Copy/shrink the result array
int[] !rez = new int[nprimes];
int j = 0;
while( j < nprimes ) {
    rez[j] = primes[j];
    j = j + 1;
}
return rez;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return [int];", stop.toString());
        Evaluator.Obj obj = (Evaluator.Obj)Evaluator.evaluate(stop, 20);
        assertEquals("[int] {int #; int ![]; }",obj.struct().toString());
        long nprimes = (Long)obj.fields()[0];
        long[] primes = new long[]{2,3,5,7,11,13,17,19};
        for( int i=0; i<nprimes; i++ )
            assertEquals(primes[i],(long)(Long)obj.fields()[i+1]);
    }

    @Test
    public void testNewNodeInit() {
        Parser parser = new Parser(
"""
struct S {int i; flt f;};
S !s1 = new S;
S !s2 = new S;
s2.i = 3;
s2.f = 2.0;
if (arg) s1 = new S;
return s1.i + s1.f;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return ((flt).i+.f);", stop.toString());
        assertEquals(0.0, Evaluator.evaluate(stop,  0));
    }


    @Test
    public void testBad0() {
        Parser parser = new Parser(
"""
return new flt;
""");
        try { parser.parse().iterate(); fail(); }
        catch( Exception e ) { assertEquals("Cannot allocate a flt",e.getMessage()); }
    }

    @Test
    public void testBad1() {
        Parser parser = new Parser(
"""
int is = new int[2];
""");
        try { parser.parse().iterate(); fail(); }
        catch( Exception e ) { assertEquals("Type *[int] is not of declared type int",e.getMessage()); }
    }

    @Test
    public void testBad2() {
        Parser parser = new Parser(
"""
int[] is = new int[3.14];
return is[1];
""");
        try { parser.parse().iterate(); fail(); }
        catch( Exception e ) { assertEquals("Cannot allocate an array with length 3.14",e.getMessage()); }
    }

    @Test
    public void testBad3() {
        Parser parser = new Parser(
"""
int[] is = new int[arg];
return is[1];
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 0;", stop.toString());
        assertEquals(0L, Evaluator.evaluate(stop,  4));
        try { Evaluator.evaluate(stop,0); }
        catch( ArrayIndexOutOfBoundsException e ) { assertEquals("Array index 1 out of bounds for array length 0",e.getMessage()); }
        try { Evaluator.evaluate(stop,-1); }
        catch( NegativeArraySizeException e ) { assertEquals("-1",e.getMessage()); }
    }

    @Test
    public void testProgress() {
        Parser parser = new Parser("""
i8 v1=0&0;
u8 v2=0;
byte v4=0;
if(0) {}
while(v2<0) {
    v4=0-v1;
    break;
}
int v5=0&0;
while(v5+(0&0)) {
    int v7=0&0;
    while(v7)
        v4=0>>>v5;
    while(v1)
        return 0;
}
return v1;
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 0;", stop.toString());
        assertEquals(0L, Evaluator.evaluate(stop,  0));
    }

    @Test
    public void testSharpNot() {
        Parser parser = new Parser(
"""
if(0>>0) {}
while(0) {}
u32 v7=0;
int v8=0;
while(0<--1>>>---(v7*0==v8)) {}
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 0;", stop.toString());
    }


    @Test
    public void testProgress2() {
        Parser parser = new Parser(
"""
if(1) {}
else {
        while(arg>>>0&0>>>0) {}
    byte v3=0>>>0;
                while(0) {}
        int v7=0>>>0;
        while(v7<0>>>0) {
                    while(0+v7<=0) if(1) arg=-12;
            if(arg) {
                v3=arg+v3+0;
                arg=0;
            }
        }
}
""");
        StopNode stop = parser.parse().iterate();
        assertEquals("return 0;", stop.toString());
    }
}
