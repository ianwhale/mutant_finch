.class public esi/bc/test/ComplexUnification
.super java/lang/Object

.method public static main([Ljava/lang/String;)V
    .limit stack  2
    .limit locals 2

    ;; compute args.length
    aload_0
    arraylength
    ifne args

    ;; flow A
    new esi/bc/test/UnificationTest$A
    dup
    invokespecial esi/bc/test/UnificationTest$A/<init>()V

    goto unify
args:
    ;; flow B
    new esi/bc/test/UnificationTest$B
    dup
    invokespecial esi/bc/test/UnificationTest$B/<init>()V

    ;checkcast esi/bc/test/UnificationTest$X

unify:
    ;; unification happens at this point
    astore_1

    aload_1
    invokeinterface esi/bc/test/UnificationTest$X/foo()V 1

    aload_1
    invokevirtual esi/bc/test/UnificationTest$S/goo()V

    return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
