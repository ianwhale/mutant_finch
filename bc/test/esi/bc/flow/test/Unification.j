.class public esi/bc/flow/test/Unification
.super java/lang/Object

.method public static main([Ljava/lang/String;)V
	.limit stack  4
	.limit locals 2

	;; put System.out on stack
	getstatic java/lang/System/out Ljava/io/PrintStream;

	;; goto crashes AnalyzerAdapter with no frames
	iconst_0
	ifne call

noargs:
	;; if no args, convert to Flushable
	checkcast java/io/Flushable

call:
	;; unification happens at this point
	invokeinterface java/io/Flushable/flush()V 1

	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
