package esi.finch.mut;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import ec.util.MersenneTwisterFast;
import esi.bc.AnalyzedClassNode;
import esi.bc.test.Fact;

import org.junit.BeforeClass;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MaterialIntroducerTest {

	private static MersenneTwisterFast random;
	private static MaterialIntroducer mi;
	private static MethodNode fact;
	
	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		random = new MersenneTwisterFast(1234);
		mi = new InsertMutator(random, 1);
		
		AnalyzedClassNode cn  = AnalyzedClassNode.readClass(Fact.class);
		fact = cn.findMethod(new Method("fact", "(I)I"));
	}
	
	@Test
	public void testGetRandomOpCode() {
		int type = AbstractInsnNode.INSN;
		
		// Keep a map of the op codes that occur.
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		
		// Initialize counts
		for (int[] range : mi.getOpCodes(type)) {
			if (range.length > 1) {
				for (int i = range[0]; i < range[1] + 1; i++) {
					counts.put(i, 0);
				}
			}
			else {
				counts.put(range[0], 0);
			}
		}
	
		int opcode;
		for (int i = 0; i < 1000; i++) {
			opcode = mi.getRandomOpCode(type);
			counts.put(opcode, counts.get(opcode) + 1);
		}
		
		for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
			assertTrue(entry.getValue() > 0);
		}
	}
	
	@Test
	public void testGetRandomInsnType() {
		// Keep a map of the types that occur. 
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		
		// Initialize.
		for (int type : MaterialIntroducer.ValidTypes) {
			counts.put(type, 0);
		}
		
		// Count occurrences of types. 
		int type; 
		for (int i = 0; i < 2000; i++) {
			type = mi.getRandomInsnType();
			counts.put(type, counts.get(type) + 1);
		}
		
		// Make sure everyone is here. 
		for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
			assertTrue(entry.getValue() > 0);
		}
	}
	
	@Test
	public void testNextIntInRanges() {
		// We want to generate random numbers in the range [0,30] U [51, 60] U [90]
		int[][] ranges = {
				{0, 30},
				{90},
				{51,60}, 
		};
		
		// Keep a map of numbers that occur. 
		// Each number should come up at least once in 1000 rolls. 
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		
		// Initialize Counts
		for (int[] range : ranges) {
			if (range.length > 1) {
				for (int i = range[0]; i < range[1] + 1; i++) {
					counts.put(i, 0);
				}
			}
			else {
				counts.put(range[0], 0);
			}
		}
		
		for (int i = 0; i < 1000; i++) {
			Integer generated = new Integer(mi.nextIntInRanges(ranges));
			counts.put(generated, counts.get(generated) + 1);
		}
		
		for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
			assertTrue(entry.getValue() > 0);
		}
	}
	
	@Test
	public void testGetRandomInsnNode() {
		// Keep track of occurrences of opcodes. 
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		
		for (int type : MaterialIntroducer.ValidTypes) {
			for (int[] range : mi.getOpCodes(type)) {
				if (range.length > 1) {
					for (int i = range[0]; i < range[1] + 1; i++) {
						counts.put(i, 0);
					}
				}
				else {
					counts.put(range[0], 0);
				}
			}
		}
		
		AbstractInsnNode insn = null;
		for (int i = 0; i < 2000; i++) {
			insn = mi.getRandomInsnNode(fact);
			
			counts.put(insn.getOpcode(), counts.get(insn.getOpcode()) + 1);
		}
		
		// Make sure everyone is here. 
		for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
			assertTrue(entry.getValue() > 0);
		}
	}
}
