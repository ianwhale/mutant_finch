.class public esi/bc/test/Control
.super java/lang/Object
.implements java/lang/Cloneable

.method public static main([Ljava/lang/String;)V
	.limit stack  4
	.limit locals 2

	;; put System.out on stack
	getstatic java/lang/System/out Ljava/io/PrintStream;

	;; compute args.length
	aload_0
	arraylength
	ifeq noargs

	;; if there are args, print a string
	dup
	ldc "Hello, world"
	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

	;; goto crashes AnalyzerAdapter with no frames
	goto call

noargs:
	;; if no args, convert to Flushable
	checkcast java/io/Flushable

call:
	;; unification happens at this point
	invokeinterface java/io/Flushable/flush()V 1

	;; just to test I2B
	iconst_1
	i2b
	istore_1

	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
