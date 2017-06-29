.class public esi/bc/flow/test/BackFlow
.super java/lang/Object

.method public foo()V
	.limit stack  1
	.limit locals 2

label:
	iconst_0
	istore_1

	iload_1
	ifeq	label

	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
