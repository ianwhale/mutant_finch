package esi.finch.mut;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ec.util.MersenneTwisterFast;
import java.lang.IllegalArgumentException;

/**
 * Base class for mutators that introduce new genetic material into methods.
 */
public abstract class MaterialIntroducer {
	
	protected final MersenneTwisterFast random;
	protected final float mutProb;
	
	public MaterialIntroducer(MersenneTwisterFast random, float mutProb) {
		this.random = random;
		this.mutProb = mutProb;
	}
	
	/**
	 * Valid kinds of instruction node types we can produce. 
	 * 
	 * Invalid types are therefore:
	 * 		// Not "real" instructions
	 * 		{@link AbstractInsnNode.FRAME} 		 
	 * 		{@link AbstractInsnNode.LINE} 		
	 * 		{@link AbstractInsnNode.LABEL} 	
	 *		// "Real" but complex. 	
	 * 		{@link AbstractInsnNode.FIELD_INSN}
	 * 		{@link AbstractInsnNode.LOOKUPSWITCH_INSN}
	 * 		{@link AbstractInsnNode.MULTIANEWARRAY_INSN}
	 * 		{@link AbstractInsnNode.TABLESWITCH_INSN}
	 * 		{@link AbstractInsnNode.TYPE_INSN}
	 */
	public static final int[] ValidTypes = {
			AbstractInsnNode.INSN, 
			AbstractInsnNode.IINC_INSN,
			AbstractInsnNode.INT_INSN,
			AbstractInsnNode.JUMP_INSN,
			AbstractInsnNode.LDC_INSN,
			AbstractInsnNode.VAR_INSN
	};
	
	/**
	 * No argument instruction opcode ranges. 
	 */
	public static final int[][] InsnOpCodeRanges = {
			{0x0, 0xf},
			{0x1a, 0x35},
			{0x3b, 0x83},
			{0x85, 0x98},
			{0xac, 0xb1},
			{0xbe}
	};
	
	/**
	 * Iinc opcode range (there is only one).
	 */
	public static final int[][] IincInsnOpCodeRange = {
			{0x84}
	};
	
	/**
	 * IntInsn opcode ranges.
	 */
	public static final int[][] IntInsnOpCodeRanges = {
			{0x10, 0x11},
			{0xbc}
	};
	
	/**
	 * JumpInsn opcode ranges.
	 */
	public static final int[][] JumpInsnOpCodeRanges = {
			{0x99, 0xa7},
			{0xc6, 0xc7}
	};
	
	/**
	 * LdcInsn opcode ranges.
	 * There is more than one valid instruction here, however ASM will only ever build a node
	 * with the opcode 0x12 (LDC), and then replace it with 0x13 (LDC_W) or 0x14 (LDC2_W) when necessary. 
	 */
	public static final int[][] LdcInsnOpCodeRanges = {
			{0x12}
	};
	
	/**
	 * VarInsn opcode ranges.
	 */
	public static final int[][] VarInsnOpCodeRanges = {
			{0x15, 0x59},
			{0xac, 0xb1}
	};
	
	/**
	 * Get a random instruction type. 
	 * Keep in mind that we need to respect the distribution of instructions.
	 * So if we use 100 total instructions and a category has 20 instructions it should have a 1/5 chance of being chosen.
	 * Idea similar to {@link nextIntInRanges}.
	 * @return
	 */
	public int getRandomInsnType() {
		int total_insn = 0;
		for (int type : ValidTypes) {
			for (int[] range : getOpCodes(type)) {
				total_insn += (range.length > 1) ? range[1] - range[0] + 1 : 1;
			}
		}
		
		int roll = random.nextInt(total_insn);
		
		int insn_seen = 0;
		int insn_in_range = 0;
		int insn_type = -1;
		for (int type : ValidTypes) {
			for (int[] range : getOpCodes(type)) {
				insn_in_range = (range.length > 1) ? range[1] - range[0] + 1 : 1;
				
				if ((roll >= insn_seen) && (roll <= insn_seen + insn_in_range)) {
					insn_type = type;
					break;
				}
				
				insn_seen += insn_in_range;
			}
			
			if (insn_type > -1) {
				break;
			}
		}
		
		return insn_type;
	}
	
	/**
	 * Gets a random op code of a particular instruction type. 
	 * @param type from ValidTypes
	 * @return
	 */
	public int getRandomOpCode(int type) {
		return nextIntInRanges(getOpCodes(type));
	}
	
	/**
	 * Gets the opcode ranges of a certain type of instruction. 
	 * @param nodeType
	 * @return int[][]
	 */
	public final int[][] getOpCodes(final int nodeType) throws IllegalArgumentException {
		switch (nodeType) {
		case AbstractInsnNode.INSN:
			return InsnOpCodeRanges;
			
		case AbstractInsnNode.IINC_INSN:
			return IincInsnOpCodeRange;
			
		case AbstractInsnNode.INT_INSN:
			return IntInsnOpCodeRanges;
			
		case AbstractInsnNode.JUMP_INSN:
			return JumpInsnOpCodeRanges;
			
		case AbstractInsnNode.LDC_INSN:
			return LdcInsnOpCodeRanges;
			
		case AbstractInsnNode.VAR_INSN:
			return VarInsnOpCodeRanges;
			
		default:
			throw new IllegalArgumentException(nodeType + " is not a valid node type constant.");
		}
	}
	
	/**
	 * To get a random opcode of a certain type, we have to roll a random number across multiple random ranges.
	 * 		E.g., roll a random number in the range [0,30] U [90] U [51,60].
	 * 		In the example, there are 42 total numbers and 31 numbers in the range [0,30].
	 * 			Thus a number in the range [0,30] should have a 31/42 chance of being rolled. 
	 * 		All ranges are inclusive. 
	 * 		All ranges must be positive. 
	 * 
	 * @param numbers
	 * @return hit, a random number in the union of the desired ranges. 
	 */
	public int nextIntInRanges(int[][] ranges) {
		int total_ints = 0;
		
		for (int[] range : ranges) {
			if (range.length > 1 && range[0] > range[1]) {
				int tmp = range[0];
				range[0] = range[1];
				range[1] = tmp;
			}
			
			// Count the total number of integers in the given range. 
			total_ints += (range.length > 1) ? range[1] - range[0] + 1 : 1;
		}
		
		int roll = random.nextInt(total_ints) + 1; // Random number based on size of the union. 
		int numbers_seen = 0;
		int numbers_in_range = 0;
		int hit = -1;
		
		//
		// Essentially a roulette wheel selection of a number. 
		//
		for (int[] range : ranges) {
			numbers_in_range = (range.length > 1) ? range[1] - range[0] + 1 : 1;
			if ((roll >= numbers_seen) && (roll <= (numbers_seen + numbers_in_range))) {
				hit = range[0] + roll - numbers_seen - 1;
				break;
			}
			numbers_seen += numbers_in_range;
		}

		return hit;
	}
	
	/**
	 * Create random instruction node. 
	 * 
	 * @param method
	 * @return
	 */
	public AbstractInsnNode getRandomInsnNode(MethodNode method) {
		int type = getRandomInsnType();
		int opcode = getRandomOpCode(type);
		
		return InstructionFactory.makeInstruction(method, random, opcode, type);
	}
	
	/**
	 * Determine if a type is valid or not. 
	 * 
	 * @param validate
	 * @return boolean
	 */
	public boolean validType(int validate) {
		for (int type : ValidTypes) {
			if (type == validate) {
				return true;
			}
		}
		
		return false;
	}
}
