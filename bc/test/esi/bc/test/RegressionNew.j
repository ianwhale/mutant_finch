.class public esi/bc/test/RegressionNew
.super java/lang/Object

.method public static foo()V
	.limit stack  1
	.limit locals 0

	new java/lang/Object

	goto skip
skip:

	invokespecial java/lang/Object/<init>()V

	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
