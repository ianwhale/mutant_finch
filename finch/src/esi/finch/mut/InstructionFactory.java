package esi.finch.mut;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.Opcodes;
import ec.util.MersenneTwisterFast;

public class InstructionFactory {

	/**
	 * The possible types for a new array.
	 */
	static final int[] NewArrayTypes = {
			Opcodes.T_BOOLEAN,
			Opcodes.T_CHAR,
			Opcodes.T_FLOAT,
			Opcodes.T_DOUBLE,
			Opcodes.T_BYTE,
			Opcodes.T_SHORT,
			Opcodes.T_INT,
			Opcodes.T_LONG
	};
	
	private InstructionFactory() {} // Do not construct me. 
	
	/**
	 * Make an instruction based on type and opcode. 
	 * 
	 * @param method
	 * @param random
	 * @param opcode
	 * @param type
	 * @return
	 */
	public static AbstractInsnNode makeInstruction(MethodNode method, MersenneTwisterFast random, int opcode, int type) {
		switch (type) {
		case AbstractInsnNode.INSN:
			return new InsnNode(opcode);
			
		case AbstractInsnNode.IINC_INSN:
			return makeIincInsn(method, random);
			
		case AbstractInsnNode.INT_INSN:
			return makeIntInsn(random, opcode);
			
		case AbstractInsnNode.JUMP_INSN:
			return makeJumpInsn(method, random, opcode);
			
		case AbstractInsnNode.LDC_INSN:
			return makeLdcInsn(random);
			
		case AbstractInsnNode.VAR_INSN:
			return makeVarInsn(method, random, opcode);
			
		default:
			throw new IllegalArgumentException(type + " is not a valid node type constant.");
		}
	}
	
	/**
	 * Iinc increments a local variable by a signed byte constant: [-128, 127]. 
	 * We must randomly choose a local variable in the method and return the instruction.
	 * 
	 * @param method
	 * @param random
	 * @return
	 */
	public static AbstractInsnNode makeIincInsn(MethodNode method, MersenneTwisterFast random) {
		byte roll = random.nextByte();
		int size = method.localVariables.size();
		int var = (size < 1) ? 0 : random.nextInt(size == 0 ? 1 : size);
		
		return new IincInsnNode(var, roll);
	}
	
	/**
	 * There are 3 IntInsn, we need to generate a different random value for each. 
	 * 
	 * @param random
	 * @param opcode
	 * @return
	 */
	public static AbstractInsnNode makeIntInsn(MersenneTwisterFast random, int opcode) {
		if (opcode == 0xbc) { // NEWARRAY, generate a primitive type (
			return new IntInsnNode(opcode, NewArrayTypes[random.nextInt(NewArrayTypes.length)]);
		}
		else if (opcode == 0x10) { // BIPUSH, generate a byte.
			return new IntInsnNode(opcode, random.nextByte());
		}
		else { // 0x11, SIPUSH, generate a short. 
			return new IntInsnNode(opcode, random.nextShort());
		}
	}
	
	/**
	 * Make a jump instruction that jumps to a random place in the method. 
	 * 
	 * @param method
	 * @param random
	 * @param opcode
	 * @return
	 */
	public static AbstractInsnNode makeJumpInsn(MethodNode method, MersenneTwisterFast random, int opcode) {
		LabelNode label = new LabelNode();
		
		// Pick a random place to add the new label. 
		int pos = random.nextInt(method.instructions.size());
		method.instructions.insertBefore(method.instructions.get(pos), label);
		
		return makeJumpInsn(opcode, label);
	}
	
	/**
	 * Make a jump instruction that jumps to the provided label.
	 * 
	 * @param method
	 * @param random
	 * @param opcode
	 * @param label
	 * @return
	 */
	public static AbstractInsnNode makeJumpInsn(int opcode, LabelNode label) {
		return new JumpInsnNode(opcode, label);
	}
	
	/**
	 * Generate a load constant instruction based on the opcode.
	 * 
	 * @param random
	 * @param opcode
	 * @return
	 */
	public static AbstractInsnNode makeLdcInsn(MersenneTwisterFast random) {
		if (random.nextDouble() < 0.5) { // Non-wide constant. 
			
			if (random.nextDouble() < 0.5) {
				return new LdcInsnNode(random.nextInt());
			}
			
			return new LdcInsnNode(random.nextFloat());
		}
		else { // Wide constant. 
			if (random.nextDouble() < 0.5) {
				return new LdcInsnNode(random.nextLong());
			}
			
			return new LdcInsnNode(random.nextDouble());
		}
	}

	public static AbstractInsnNode makeVarInsn(MethodNode method, MersenneTwisterFast random, int opcode) {
		int size = method.localVariables.size();
		int var = (size < 1) ? 0 : random.nextInt(size == 0 ? 1 : size);
		
		return new VarInsnNode(opcode, var);
	}
}
