/***
 * ASM Guide
 * Copyright (c) 2007 Eric Bruneton
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch6.sec1;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import java.util.Iterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * ASM Guide example class.
 * 
 * @author Eric Bruneton
 */
public class RemoveGetFieldPutFieldTransformer extends
    MethodTransformer {

  public RemoveGetFieldPutFieldTransformer(MethodTransformer mt) {
    super(mt);
  }

  public void transform(MethodNode mn) {
    InsnList insns = mn.instructions;
    Iterator i = insns.iterator();
    while (i.hasNext()) {
      AbstractInsnNode i1 = (AbstractInsnNode) i.next();
      if (isALOAD0(i1)) {
        AbstractInsnNode i2 = getNext(i1);
        if (i2 != null && isALOAD0(i2)) {
          AbstractInsnNode i3 = getNext(i2);
          if (i3 != null && i3.getOpcode() == GETFIELD) {
            AbstractInsnNode i4 = getNext(i3);
            if (i4 != null && i4.getOpcode() == PUTFIELD) {
              if (sameField(i3, i4)) {
                while (i.next() != i4) {
                }
                insns.remove(i1);
                insns.remove(i2);
                insns.remove(i3);
                insns.remove(i4);
              }
            }
          }
        }
      }
    }
    super.transform(mn);
  }

  private static AbstractInsnNode getNext(AbstractInsnNode insn) {
    do {
      insn = insn.getNext();
      if (insn != null && !(insn instanceof LineNumberNode)) {
        break;
      }
    } while (insn != null);
    return insn;
  }

  private static boolean isALOAD0(AbstractInsnNode i) {
    return i.getOpcode() == ALOAD && ((VarInsnNode) i).var == 0;
  }

  private static boolean sameField(AbstractInsnNode i,
      AbstractInsnNode j) {
    return ((FieldInsnNode) i).name.equals(((FieldInsnNode) j).name);
  }
}
