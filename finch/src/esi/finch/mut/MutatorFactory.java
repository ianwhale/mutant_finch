package esi.finch.mut;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import esi.bc.manip.InstructionsMutator;
import esi.util.Config;
import ec.util.MersenneTwisterFast; 

/**
 * Class for dispatching mutators.
 * @author Ian Whalen
 *
 */
public class MutatorFactory {
	private static final Log log = Config.getLogger();
	
	public enum MutationType {
		DELETION,		// Delete an instruction.
		COPY, 			// Copy an instruction from a method and insert it at a random place in the method.
		INSERT,			// Insert an instruction of a random type at a random place.
		MOVE,			// Move an instruction to a different line of the method.
		REPLACE,	// Delete an instruction then insert a random new one. 
	}
	
	/**
	 * Pick from the mutator types with equal probability. 
	 * @param random MersenneTwisterFast, needs to be passed in for state consistency. 
	 * @return InstructionMutator
	 */
	public static InstructionsMutator makeMutator(MersenneTwisterFast random, float mutProb) {
		double equal_prob = 1 / (double)MutationType.values().length;
		double spin = random.nextDouble();
		
		for (int i = 0; i < MutationType.values().length; i++) {
			if (((i * equal_prob) <= spin) && (spin < ((i+1) * equal_prob))) {
				return generateMutator(random, MutationType.values()[i], mutProb);
			}
		}
		
		return null;
	}
	
	/**
	 * Pick from mutators with user provided probability distribution. 
	 * @param probDist a probability distribution containing all {@link MutationType} types mapping to doubles that all add to 1.
	 * @return InstructionsMutator
	 */
	public static InstructionsMutator makeMutator(MersenneTwisterFast random, Map<MutationType, Double> probDist, float mutProb) {
		if (MutatorFactory.validDistribution(probDist)) {
			double start_range = 0; 
			double spin = random.nextDouble();
			
			for (Map.Entry<MutationType, Double> entry : probDist.entrySet()) {
				if ((start_range <= spin) && (spin < (entry.getValue() + start_range))) {
					return generateMutator(random, entry.getKey(), mutProb);
				}
				start_range += entry.getValue();
			}
			
			return null;
		}
		else {
			log.debug("Probability distribution invalid, making mutators with equal probability.");
			return makeMutator(random, mutProb); // Probability distribution was invalid, so return with equal probability
		}
	}
	
	/**
	 * Actually generate the mutator based type passed in.
	 * @param type MutationType
	 * @return InstructionMutator
	 */
	public static InstructionsMutator generateMutator(MersenneTwisterFast random, final MutationType type, final float mutProb) {	
		switch(type) {
		case DELETION:
			return new DeletionMutator(random, mutProb);
		case COPY:
			return new CopyMutator(random, mutProb);
		case INSERT:
			return new InsertMutator(random, mutProb);
		case MOVE:
			return new MoveMutator(random, mutProb);
		case REPLACE:
			return new ReplaceMutator(random, mutProb);
		}
		return null;
	}
	
	/**
	 * Return true if:
	 * 	- All probabilities add to 1.
	 * 	- All probabilities are in [0,1].
	 * 	- All {@link MutationType} types are used. 
	 *  - No mutation type is used more that once. 
	 * Return false if any of these do not hold. 
	 * @param probDist
	 * @return boolean
	 */
	public static boolean validDistribution(Map<MutationType, Double> probDist) {
		Set<MutationType> types_used = new HashSet<MutationType>();
		double total_probability = 0.0;
		boolean valid_probabilities = true;
		for (Map.Entry<MutationType, Double> entry : probDist.entrySet()) {
			types_used.add(entry.getKey());
			total_probability += entry.getValue().doubleValue();
			valid_probabilities &= (0.0 <= entry.getValue().doubleValue() && entry.getValue().doubleValue() <= 1.0);
		}
		
		boolean contains_all = true;
		for (MutationType type : MutationType.values()) {
			contains_all &= types_used.contains(type);
		}
		
		return contains_all && 
			(total_probability == 1.0) &&
			valid_probabilities &&
			(probDist.size() == MutationType.values().length);
	}
}
