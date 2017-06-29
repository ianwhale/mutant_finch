package esi.finch.mut;

import java.util.Map;
import java.util.HashMap;
import static org.junit.Assert.*;
import org.junit.Test;

import esi.finch.mut.MutatorFactory;
import esi.finch.mut.MutatorFactory.MutationType;

public class MutatorFactoryTest {
	
	// A valid distribution. 
	private static final Map<MutationType, Double> valid;
	static {
		valid = new HashMap<MutationType, Double>();
		valid.put(MutationType.COPY, 0.4);
		valid.put(MutationType.DELETION, 0.1);
		valid.put(MutationType.MOVE, 0.1);
		valid.put(MutationType.INSERT, 0.2);
		valid.put(MutationType.REPLACE, 0.2);
	}
	
	// Does not add to 1. 
	private static final Map<MutationType, Double> tooSmall;
	static {
		tooSmall = new HashMap<MutationType, Double>();
		tooSmall.put(MutationType.COPY, 0.1);
		tooSmall.put(MutationType.DELETION, 0.01);
		tooSmall.put(MutationType.MOVE, 0.1);
		tooSmall.put(MutationType.INSERT, 0.1);
		tooSmall.put(MutationType.REPLACE, 0.2);
	}
	
	// Does not add to 1.  
	private static final Map<MutationType, Double> tooLarge;
	static {
		tooLarge = new HashMap<MutationType, Double>();
		tooLarge.put(MutationType.COPY, 0.6);
		tooLarge.put(MutationType.DELETION, 0.2);
		tooLarge.put(MutationType.MOVE, 0.22);
		tooLarge.put(MutationType.INSERT, 0.5);
		tooLarge.put(MutationType.REPLACE, 0.0);
	}
	
	// Adds to 1, but contains negative values. 
	private static final Map<MutationType, Double> negatives;
	static {
		negatives = new HashMap<MutationType, Double>();
		negatives.put(MutationType.COPY, -0.5);
		negatives.put(MutationType.DELETION, 0.1);
		negatives.put(MutationType.MOVE, 0.5);
		negatives.put(MutationType.INSERT, -0.1);
		negatives.put(MutationType.REPLACE, 0.5);
	}
	
	// Adds to 1 but does not contain all mutation types.  
	private static final Map<MutationType, Double> tooFew;
	static {
		tooFew = new HashMap<MutationType, Double>();
		tooFew.put(MutationType.COPY, 0.2);
		tooFew.put(MutationType.DELETION, 0.2);
		tooFew.put(MutationType.INSERT, 0.2);
		tooFew.put(MutationType.REPLACE, 0.2);
	}
	
	// Too many values not possible :)
	
	
	@Test
	public void testValidDistribution() {
		assertTrue(MutatorFactory.validDistribution(valid));
		assertFalse(MutatorFactory.validDistribution(tooSmall));
		assertFalse(MutatorFactory.validDistribution(tooLarge));
		assertFalse(MutatorFactory.validDistribution(negatives));
		assertFalse(MutatorFactory.validDistribution(tooFew));
	}
}
