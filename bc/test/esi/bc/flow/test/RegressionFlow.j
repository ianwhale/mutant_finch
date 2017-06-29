.class public esi/bc/flow/test/RegressionFlow
.super java/lang/Object

.method public foo()V
	.limit stack  2
	.limit locals 1

	iconst_0

	iconst_0
	ifeq    label_1

	goto    label_2
label_1:

	ineg

label_2:
	pop

	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
