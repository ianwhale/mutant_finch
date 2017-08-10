package esi.finch.ecj.bc;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import ec.EvolutionState;
import ec.Individual;
import ec.simple.SimpleFitness;
import ec.util.MersenneTwisterFast;
import ec.util.Parameter;
import esi.bc.AnalyzedClassNode;
import esi.bc.AnalyzedMethodNode;
import esi.bc.manip.CodeInterrupter;
import esi.bc.manip.CodeMerger;
import esi.bc.manip.CodeModifier;
import esi.bc.manip.CodeProducer;
import esi.bc.manip.ConstantsMutator;
import esi.bc.manip.InstructionsMutator;
import esi.bc.xo.CompatibleCrossover;
import esi.bc.xo.TypeVerifier;
import esi.finch.ecj.immutable.ImmutableIndividual;
import esi.finch.ecj.immutable.ImmutableSpecies;
import esi.finch.mut.MutationVerifier;
import esi.finch.mut.MutatorFactory;
import esi.finch.xo.CrossoverFinder;
import esi.finch.xo.CrossoverFinder.Sections;
import esi.util.Config;
import esi.util.Loader;
import esi.util.SpecializedConstants;

/**
 * Bytecode individual.
 *
 * <p>
 * Default base: <tt>bytecode.ind</tt>. Parameters:
 * <ul>
 * <li><tt>class</tt> (name of initial class),
 * <li><tt>method-name</tt> (method name),
 * <li><tt>method-desc</tt> (optional method descriptor),
 * <li><tt>xo-class</tt> (crossover finder class)
 * <li><tt>mut-class</tt> (constants mutation class, assumes mut-prob > 0)
 * <li><tt>max-growth</tt> (optional maximal method growth factor).
 * </ul>
 *
 * <p>
 * Constants:
 * <ul>
 * <li><code>class.BytecodeIndividual.dump</code>: whether to save all
 * individuals in {@link Config#DIR_OUT_ECJ}
 * <li><code>class.BytecodeIndividual.steps-indexes</code>: number of supported
 * indexes for given evaluation thread step counters
 * </ul>
 *
 * @author Michael Orlov
 */
public class BytecodeIndividual extends Individual implements ImmutableIndividual<BytecodeIndividual> {

	private static final boolean DUMP = SpecializedConstants.getBoolean(BytecodeIndividual.class, "dump");
	private static final int STEPS_MULT = SpecializedConstants.getInt(BytecodeIndividual.class, "steps-indexes");
	private static int BYTE_COUNT_LIMIT;
	private static final Log log = Config.getLogger();

	private static final long serialVersionUID = 1L;
	private static final String P_BYTECODE_INDIVIDUAL = "ind";

	// Parameter names
	private static final String P_INIT_CLASS = "class";
	private static final String P_METHOD_NAME = "method-name";
	private static final String P_METHOD_DESC = "method-desc";
	private static final String P_XO_CLASS = "xo-class";
	private static final String P_MUT_CLASS = "mut-class";
	private static final String P_MAX_GROWTH = "max-growth";

	private boolean mutateInstructions;
	private boolean useDistribution;
	private Map<MutatorFactory.MutationType, Double> mutationDistribution;

	/**
	 * If steps limit for the given thread is set, decrement it and throw
	 * {@link InterruptedException} if zero is reached.
	 *
	 * If steps limit is not set, call {@link Thread#sleep(long)} to allow
	 * interruption of the thread.
	 *
	 * NOTE: the interrupt method is static in order to keep
	 * {@link CodeInterrupter} framework simple, which implies appropriate
	 * limitations.
	 */
	public static class StepsInterrupter {
		// Per-evaluation steps (initialized in setup)
		private static long[] steps;

		public static void interrupt(int id) throws InterruptedException {
			if (steps[id] != 0) {
				if (--steps[id] == 0)
					throw new InterruptedException("Steps limit exceeded");
			} else
				Thread.sleep(0);
		}
	}
	
	/**
	 * Class to count all instructions executed by a method.
	 * Steps interrupter counts when a backward jump or invocation is made,
	 * for some improvement tasks we'd like to count how many bytecode instructions
	 * are executed by a method. 
	 */
	public static class ByteCodeCounter {
		private static long[] bytecodes;
		
		public static void count(int id) throws InterruptedException {
			if (bytecodes[id] < BYTE_COUNT_LIMIT) {
				if (++bytecodes[id] >= BYTE_COUNT_LIMIT) {
					throw new InterruptedException("Bytecode limit exceeded");
				}
			}
			else {
				Thread.sleep(0);
			}
		}
	}
	
	// Per-breed counters (initialized in setup)
	private int[] counters;
	// Initial values (filled in setup)
	private Class<?> initClass;
	private AnalyzedClassNode initClassNode;
	private Method methodDef;
	private Class<?>[] methodParams;
	private Class<? extends CrossoverFinder> xoFinderClass;
	private Class<? extends ConstantsMutator> mutConstantsClass;
	private int initSize;
	private float maxGrowth;

	// Genome
	private int size; // bytecode length (or initSize)
	private CodeProducer producer; // for accessing individuals' classes
	private Object info; // used in genome representation

	@Override
	public BytecodeIndividual crossover(BytecodeIndividual other, EvolutionState state, int thread) {
		// Individuals entering crossover pipeline are already evaluated
		assert evaluated && other.evaluated;

		ImmutableSpecies species = (ImmutableSpecies) this.species;
		MersenneTwisterFast random = state.random[thread];

		BytecodeIndividual res = this;

		if (random.nextFloat() < species.getXoProb()) {
			// Original name (from initClass) and new name (although same names
			// are ok)
			String origName = initClass.getName();
			String name = createClassName(state.generation, thread);

			// Alpha and beta class nodes
			AnalyzedClassNode alphaClassNode;
			AnalyzedClassNode betaClassNode;
			try {
				alphaClassNode = getClassNode();

			} catch (RuntimeException e) {
				// Invalid code structure, assign minimal fitness.
				SimpleFitness sfit = (SimpleFitness) res.fitness;
				sfit.setFitness(state, Integer.MIN_VALUE, false);
				res.fitness = sfit;
				return res;
			}

			try {
				betaClassNode = other.getClassNode();
			} catch (RuntimeException e) {
				// Other has invalid code structure.
				// Assign it minimal fitness, and quit crossover attempt.
				SimpleFitness sfit = (SimpleFitness) other.fitness;
				sfit.setFitness(state, Integer.MIN_VALUE, false);
				other.fitness = sfit;
				return res;
			}

			TypeVerifier verifier = new TypeVerifier(alphaClassNode, betaClassNode);

			// Alpha and beta method nodes
			AnalyzedMethodNode alphaMethod = alphaClassNode.findMethod(methodDef);
			AnalyzedMethodNode betaMethod = betaClassNode.findMethod(methodDef);

			// Pick crossover sections
			CompatibleCrossover xo;
			CrossoverFinder xoFinder;
			Sections xoSections;
			try {
				 xo = new CompatibleCrossover(alphaMethod, betaMethod, verifier);
				 xoFinder = Loader.loadClassInstance(xoFinderClass, alphaMethod, betaMethod, xo, random,
							Math.round(initSize * maxGrowth));
				 xoSections = xoFinder.getSuggestion();
			}
			catch (RuntimeException e) {
				// Something wrong with the stack data. 
				return res;
			}
			catch (AssertionError e) {
				// Something wrong with the frame layout.
				return res; 
			}
			
			// If crossover was found
			if (xoSections != null) {
				// Bytecode merger
				CodeMerger merger;
				try {
					 merger = new CodeMerger(name, origName, alphaClassNode, betaClassNode, xoSections.alpha,
							xoSections.beta);
				}
				catch (RuntimeException e) {
					return res;
				}

				// Create and fill new individual
				res = clone();
				res.fillGenome(merger);
			}
		}

		log.trace("Crossover " + ((res == this) ? "same" : "changed") + ": " + res);
		return res;
	}

	@Override
	public BytecodeIndividual mutate(EvolutionState state, int thread) {
		// It is possible for individuals entering mutation pipeline
		// to be unevaluated (if they come after crossover)

		ImmutableSpecies species = (ImmutableSpecies) this.species;
		BytecodeIndividual res = this;
		
		MersenneTwisterFast random = state.random[thread];

		if (species.getMutProb() > 0 &&
				random.nextFloat() < species.getMutProb()) {
			// New mutation method.
			// Only clone individual and fill the new genome if mutation produces a viable offspring.
			
			
			// New name (although same names are ok)
			String name = createClassName(state.generation, thread);

			AnalyzedClassNode originalClassNode = getClassNode();
			AnalyzedMethodNode originalMethod = originalClassNode.findMethod(methodDef);
			
			ConstantsMutator mutator = Loader.loadClassInstance(mutConstantsClass, species.getMutProb(), random);

			InstructionsMutator instructions_mutator;
			if (useDistribution) {
				instructions_mutator = MutatorFactory.makeMutator(random, mutationDistribution, species.getMutProb());
			} else {
				instructions_mutator = MutatorFactory.makeMutator(random, species.getMutProb());
			}
			
			AnalyzedClassNode mutantClassNode = null;
			CodeModifier modifier = null;
			AnalyzedMethodNode mutantMethod = null;
			TypeVerifier verifier = null;
			CompatibleCrossover xo = null;
			MutationVerifier mutVerifier = null;
			
			int i = 0; 
			// Attempt to mutate until we find a valid mutation.
			do {
				mutantClassNode = getClassNode(); // Not a reference, points to the same class but different object than original.
				
				try {
					modifier = new CodeModifier(name, mutantClassNode, methodDef, mutator, instructions_mutator,
							mutateInstructions);
				}
				catch (RuntimeException e) {
					return res;
				}
				
				mutantMethod = mutantClassNode.findMethod(methodDef);
				
				verifier = new TypeVerifier(originalClassNode, mutantClassNode);
				xo = new CompatibleCrossover(originalMethod, mutantMethod, verifier);
				
				mutVerifier = new MutationVerifier(originalMethod, mutantMethod, xo);
				
				if (i++ > 1000) {
					//System.out.println("Too many tries at mutation.");
					break;
				}
				
			} while(! mutVerifier.isValidMutation());
			
			//System.out.println("Made a good mutation!");
			
			// Create and fill new individual
			res = clone();
			res.fillGenome(modifier);
		}

		return res;
	}

/* OLD MUTATION METHOD. 
	@Override 
	public BytecodeIndividual mutate(EvolutionState state, int thread) {
		// It is possible for individuals entering mutation pipeline
		// to be unevaluated (if they come after crossover)

		ImmutableSpecies species = (ImmutableSpecies) this.species;
		BytecodeIndividual res = this;

		if (species.getMutProb() > 0) {
			// New name (although same names are ok)
			String name = createClassName(state.generation, thread);

			// Mutator and code modifier
			MersenneTwisterFast random = state.random[thread];
			ConstantsMutator mutator = Loader.loadClassInstance(mutConstantsClass, species.getMutProb(), random);

			InstructionsMutator instructions_mutator;
			if (useDistribution) {
				instructions_mutator = MutatorFactory.makeMutator(random, mutationDistribution, species.getMutProb());
			} else {
				instructions_mutator = MutatorFactory.makeMutator(random, species.getMutProb());
			}

			CodeModifier modifier;
			try {
				modifier = new CodeModifier(name, getClassNode(), methodDef, mutator, instructions_mutator,
						mutateInstructions);
			} catch (RuntimeException e) {
				// Invalid code structure, assign minimal fitness.
				SimpleFitness sfit = (SimpleFitness) res.fitness;
				sfit.setFitness(state, Integer.MIN_VALUE, false);
				return res;
			}

			// Create and fill new individual
			res = clone();
			res.fillGenome(modifier);
		}

		return res;
	}
*/
	
	
	/**
	 * Set up the instruction mutation parameters.
	 * 
	 * @param state
	 */
	private void setMutationDistribution(EvolutionState state) {
		mutateInstructions = state.parameters.getBoolean(new Parameter("insn.mut.allowed"),
				new Parameter("insn.mut.allowed"), true);
		useDistribution = state.parameters.getBoolean(new Parameter("insn.mut.custom-distrib"),
				new Parameter("insn.mut.custom-distrib"), false);

		mutationDistribution = new HashMap<MutatorFactory.MutationType, Double>();
		mutationDistribution.put(MutatorFactory.MutationType.DELETION,
				state.parameters.getDouble(new Parameter("insn.mut.deletion"), new Parameter("insn.mut.deletion")));
		mutationDistribution.put(MutatorFactory.MutationType.COPY,
				state.parameters.getDouble(new Parameter("insn.mut.copy"), new Parameter("insn.mut.copy")));
		mutationDistribution.put(MutatorFactory.MutationType.INSERT,
				state.parameters.getDouble(new Parameter("insn.mut.insert"), new Parameter("insn.mut.insert")));
		mutationDistribution.put(MutatorFactory.MutationType.MOVE,
				state.parameters.getDouble(new Parameter("insn.mut.move"), new Parameter("insn.mut.move")));
		mutationDistribution.put(MutatorFactory.MutationType.REPLACE,
				state.parameters.getDouble(new Parameter("insn.mut.replace"), new Parameter("insn.mut.replace")));
	}

	// Creates a name for a new class (counter increment side-effect)
	private String createClassName(int generation, int thread) {
		// The new individual is created for the next generation
		return initClass.getName() + "_G" + (generation + 1) + "_T" + thread + "_" + (++counters[thread]);
	}

	@Override
	public void reset(EvolutionState state, int thread) {
		assert !evaluated && isInitial();
		fillGenome(null);

		log.trace("Reset: " + this);
	}

	@Override
	public boolean isInitial() {
		return producer == null;
	}

	private AnalyzedClassNode getClassNode() {
		// NOTE: Expensive operation - production of AnalyzedClassNode
		return isInitial() ? initClassNode : producer.getClassNode();
	}

	// Fills the genome's fields
	private void fillGenome(CodeProducer producer) {
		// fill fields
		this.producer = producer;
		evaluated = false;
		info = null;

		// size (for parsimony pressure, if applicable)
		// class size, not the size used for max growth
		size = (producer == null) ? initSize : producer.size();

		// Save individual if dumping is enabled
		if (DUMP)
			try {
				saveClass(Config.DIR_OUT_ECJ);
			} catch (IOException e) {
				throw new RuntimeException("Unexpected I/O error", e);
			}
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		super.setup(state, base);

		BYTE_COUNT_LIMIT = 
				state.parameters.getInt(new Parameter("bytecode.count.limit"), 
						new Parameter("bytecode.count.limit"));
		
		System.out.println(BYTE_COUNT_LIMIT);
		
		// This should only happen once in a single experiment (but happens more
		// in tests)
		synchronized (getClass()) {
			if (StepsInterrupter.steps == null) {
				// Initialize per-eval-thread*multiplier step counters
				StepsInterrupter.steps = new long[state.evalthreads * STEPS_MULT];
			} else
				log.warn("ECJ attempted to initialize prototype individual more than once");
			
			if (ByteCodeCounter.bytecodes == null) {
				ByteCodeCounter.bytecodes = new long[state.evalthreads * BYTE_COUNT_LIMIT];
			}
		}

		// Initialize per-breeding-thread counters to 0
		counters = new int[state.breedthreads];

		Parameter def = defaultBase();

		// Load the class, and also check that it can be loaded
		initClass = state.parameters.getClassForParameter(base.push(P_INIT_CLASS), def.push(P_INIT_CLASS),
				Object.class);

		// Create initial class node (recompute frames)
		try {
			initClassNode = AnalyzedClassNode.readClass(initClass, true, true);
		} catch (IOException e) {
			throw new Error("Unexpected error, cannot re-read class", e);
		}

		// Load methodName
		String methodName = state.parameters.getString(base.push(P_METHOD_NAME), def.push(P_METHOD_NAME));
		if (methodName == null)
			state.output.error("No method name", base.push(P_METHOD_NAME), def.push(P_METHOD_NAME));

		// Load method descriptor (can be null)
		String methodDesc = state.parameters.getString(base.push(P_METHOD_DESC), def.push(P_METHOD_DESC));
		boolean methodDescProvided = (methodDesc != null);
		boolean methodFound = false;

		// Check that method is present, deduce descriptor if not supplied
		for (java.lang.reflect.Method method : initClass.getDeclaredMethods()) {
			if (method.getName().equals(methodName)) {
				String desc = Type.getMethodDescriptor(method);
				if (methodDesc == null || methodDesc.equals(desc)) {
					methodDesc = desc;
					methodFound = true;

					methodParams = method.getParameterTypes();
				} else if (!methodDescProvided && !methodDesc.equals(desc))
					state.output.error("Ambiguous method name, provide descriptor", base.push(P_METHOD_DESC),
							def.push(P_METHOD_DESC));
			}
		}

		if (!methodFound)
			state.output.error("Method not found", base.push(P_METHOD_NAME), def.push(P_METHOD_NAME));

		methodDef = new Method(methodName, methodDesc);
		initSize = initClassNode.findMethod(methodDef).instructions.size();

		// Load XO finder class (always) and constants mutator class (if given)
		xoFinderClass = ((Class<?>) state.parameters.getClassForParameter(base.push(P_XO_CLASS), def.push(P_XO_CLASS),
				CrossoverFinder.class)).asSubclass(CrossoverFinder.class);
		if (state.parameters.getString(base.push(P_MUT_CLASS), def.push(P_MUT_CLASS)) != null)
			mutConstantsClass = ((Class<?>) state.parameters.getClassForParameter(base.push(P_MUT_CLASS),
					def.push(P_MUT_CLASS), ConstantsMutator.class)).asSubclass(ConstantsMutator.class);

		// Load max growth factor (default is 0 - unused)
		maxGrowth = state.parameters.getFloat(base.push(P_MAX_GROWTH), def.push(P_MAX_GROWTH), 1);
		if (maxGrowth == 1 - 1)
			maxGrowth = 0;

		setMutationDistribution(state);

		log.info("Bytecode individual set up:" + "\n    class=" + initClass.getName() + "\n    method=" + methodDef
				+ "\n    size=" + initSize + "\n    xo-class=" + xoFinderClass.getName() + "\n    mut-class="
				+ (mutConstantsClass == null ? "none" : mutConstantsClass.getName()) + "\n    max-growth=" + maxGrowth
				+ "\n    mut-insn=" + mutateInstructions);
	}

	@Override
	public Parameter defaultBase() {
		return BytecodeDefaults.base().push(P_BYTECODE_INDIVIDUAL);
	}

	/**
	 * Evolving method accessor, intended to be used by implementers of
	 * {@link BytecodeEvaluator}.
	 *
	 * @return Java method of the genome's class
	 */
	public java.lang.reflect.Method getMethod() {
		try {
			// NOTE: Expensive operation - production of Class
			Class<?> klass = isInitial() ? initClass : producer.getClassLoader().loadClass(producer.getName());
			assert isInitial() || producer.getName().equals(klass.getName());

			// Use getDeclaredMethod (and not getMethod), since the method can
			// only be located in the concrete class (never higher in the
			// hierarchy)
			return klass.getDeclaredMethod(methodDef.getName(), methodParams);
		} catch (ClassNotFoundException e) {
			throw new Error("Unexpected: class not found", e);
		} catch (NoSuchMethodException e) {
			throw new Error("Unexpected: method not found", e);
		}
	}

	/**
	 * Evolving method accessor, intended to be used by implementers of
	 * {@link BytecodeEvaluator} that need to get an interruptible class version
	 * of the genome class.
	 *
	 * Uses {@link CodeInterrupter}.
	 *
	 * @param maxSteps
	 *            maximum number of steps before throwing
	 *            {@link InterruptedException} (0 means no limit, in which case
	 *            {@link Thread#sleep(long)} is invoked every time)
	 * @param threadnum
	 *            number of evaluation thread
	 * @param index
	 *            index to disambiguate several method in same thread (must be
	 *            less than the "steps-indexes" specialized parameter)
	 *
	 * @return Java method of the genome's interruptible class
	 */
	public java.lang.reflect.Method getInterruptibleMethod(long maxSteps, int threadnum, int index) {
		assert threadnum < (StepsInterrupter.steps.length / STEPS_MULT) && index < STEPS_MULT;
		int id = index * (StepsInterrupter.steps.length / STEPS_MULT) + threadnum;

		StepsInterrupter.steps[id] = maxSteps;

		CodeProducer cp = isInitial()
				? new CodeInterrupter(initClass.getName() + "_Int", initClassNode, StepsInterrupter.class.getName(),
						"interrupt", id)
				: new CodeInterrupter(producer.getName() + "_Int", producer.getClassReader(),
						StepsInterrupter.class.getName(), "interrupt", id);

		try {
			Class<?> klass = cp.getClassLoader().loadClass(cp.getName());
			return klass.getDeclaredMethod(methodDef.getName(), methodParams);
		} catch (ClassNotFoundException e) {
			throw new Error("Unexpected: class not found", e);
		} catch (NoSuchMethodException e) {
			throw new Error("Unexpected: method not found", e);
		}
	}
	
	/**
	 * Just like getInterruptibleMethod, however it counts the number of Bytecode instructions
	 * executed by a method. 
	 * 
	 * **Note: only can work on a singular method, won't count bytecodes executed by external method calls.
	 * **Note (2): {@link getInterruptibleMethod} won't interrupt external methods either.
	 * @param threadnum
	 * @param index
	 * @return
	 */
	public java.lang.reflect.Method getCountableMethod(int threadnum, int index) {
		// Ignore me!
		return null;
	}

	/**
	 * Saves the class file in the supplied directory.
	 *
	 * This is only possible when the class has been produced as a sequence of
	 * bytes, i.e., it has an associated bytes class loader.
	 *
	 * @param outputDir
	 *            where to save the class file
	 * @return whether file was saved (a merger was available)
	 * @throws IOException
	 *             in case of I/O error
	 * @throws RuntimeException
	 *             if the genome is an initial class
	 */
	public boolean saveClass(File outputDir) throws IOException {
		// No loader if it's an initial individual
		if (isInitial())
			return false;

		try {
			Class<?> savedClass = producer.getClassLoader(outputDir).loadClass(producer.getName());
			assert producer.getName().equals(savedClass.getName());
		} catch (ClassNotFoundException e) {
			throw new Error("Unexpected: class not found", e);
		}

		log.info("Saved " + this + " in directory " + outputDir.getAbsolutePath());
		return true;
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public boolean equals(Object ind) {
		assert ind instanceof BytecodeIndividual;
		return producer == ((BytecodeIndividual) ind).producer;
	}

	@Override
	public int hashCode() {
		return isInitial() ? 0 : producer.hashCode();
	}

	@Override
	public BytecodeIndividual clone() {
		// Default clone is OK
		return (BytecodeIndividual) super.clone();
	}

	@Override
	public String toString() {
		return "Bytecode = " + (isInitial() ? initClass.getName() : producer.getName()) + "." + methodDef + " ["
				+ size() + "]";
	}

	@Override
	public String genotypeToStringForHumans() {
		String rep = super.genotypeToStringForHumans();

		if (info != null)
			rep += "\n" + info;

		return rep;
	}

	public void setInfo(Object info) {
		this.info = info;
	}

}
