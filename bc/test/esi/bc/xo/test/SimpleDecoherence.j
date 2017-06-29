.class public esi/bc/xo/test/SimpleDecoherence
.super java/lang/Object

.method public static foo(J)V
	.limit stack  1
	.limit locals 2
	
	iconst_0
	ifne skip

	iconst_0
	istore_1
	
skip:

	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
