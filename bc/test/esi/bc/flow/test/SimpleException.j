.class public esi/bc/flow/test/SimpleException
.super java/lang/Object

.method public static main([Ljava/lang/String;)V
	.catch any from L0 to L1 using Handler

	goto start

Handler:
	athrow

start:

L0:
	return
L1:
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
