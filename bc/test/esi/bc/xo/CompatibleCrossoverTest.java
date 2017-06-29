package esi.bc.xo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import esi.bc.AnalyzedClassNode;
import esi.bc.AnalyzedMethodNode;
import esi.bc.BytesClassLoader;
import esi.bc.flow.BranchAnalyzer;
import esi.bc.flow.CodeSection;
import esi.bc.flow.test.Flow;
import esi.bc.manip.CodeMerger;
import esi.bc.test.Fact;
import esi.bc.test.FactLong;
import esi.bc.xo.test.DecoherentVars;
import esi.util.Config;

public class CompatibleCrossoverTest {

	private static final String CONTROL_NAME = "esi.bc.test.Control";
	private static       Random random;

	@BeforeClass
	public static void setUpBeforeClass() {
		random = new Random();
	}

	// @Test /* Long */
	public void timeCrossovers() throws IOException {
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Fact.class, true);
		Method methodSpec = new Method("fact", "(I)I");
		timeCrossovers(Fact.class.getName(), classNode, methodSpec);

		classNode = AnalyzedClassNode.readClass(FactLong.class, true);
		methodSpec = new Method("fact", "(J)J");
		timeCrossovers(FactLong.class.getName(), classNode, methodSpec);

		classNode = AnalyzedClassNode.readClass(CONTROL_NAME, true, true);
		methodSpec = new Method("main", Type.VOID_TYPE, new Type[]{ Type.getObjectType("[" + Type.getType(String.class)) });
		timeCrossovers(CONTROL_NAME, classNode, methodSpec);
	}

	@Test
	public void allCrossoversFact() throws Exception {
		AnalyzedClassNode alphaClass = AnalyzedClassNode.readClass(Fact.class, true);
		AnalyzedClassNode betaClass  = AnalyzedClassNode.readClass(Fact.class, true);

		Method methodSpec = new Method("fact", "(I)I");

		int count = allCrossovers(Fact.class.getName(), alphaClass, betaClass, methodSpec, methodSpec);
		assertEquals(1828, count);
	}

	@Test
	public void legalFactCrossover() throws Exception {
		// Example from ESI paper
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Fact.class, true);
		TypeVerifier      verifier  = new TypeVerifier(classNode);

		Method             methodSpec = new Method("fact", "(I)I");
		AnalyzedMethodNode method     = classNode.findMethod(methodSpec);

		// alpha = [ fact(n-1) ], beta = [ ans ]
		CodeSection alphaSection = new CodeSection(methodSpec,  6, 10);
		CodeSection betaSection  = new CodeSection(methodSpec, 15, 15);

		CompatibleCrossover xo = new CompatibleCrossover(method, method, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		// It's ok to keep the same name, too
		CodeMerger merger = new CodeMerger(Fact.class.getName() + "LegalXO", Fact.class.getName(),
				classNode, classNode, alphaSection, betaSection);
		BytesClassLoader loader = merger.getClassLoader(Config.DIR_OUT_TESTS);
		Class<?> mergedClass = loader.loadClass(merger.getName());

		// Checks verification
		Object instance = mergedClass.newInstance();

		// Check result
		java.lang.reflect.Method m = mergedClass.getMethod(methodSpec.getName(), Integer.TYPE);
		int result = (Integer) m.invoke(instance, 5);
		assertEquals(5, result);

		// Now check illegality against the new example in the paper
		AnalyzedClassNode  classNodeX = merger.getClassNode();
		AnalyzedMethodNode methodX    = classNodeX.findMethod(methodSpec);
		verifier = new TypeVerifier(classNodeX, classNode);

		// alpha = [ IMUL ], beta = [ INVOKEVIRTUAL ]
		alphaSection = new CodeSection(methodSpec,  7,  7);
		betaSection  = new CodeSection(methodSpec, 10, 10);
		assertEquals(Opcodes.IMUL, methodX.instructions.get(7).getOpcode());

		xo = new CompatibleCrossover(methodX, method, verifier);
		assertFalse(xo.isCompatible(alphaSection, betaSection));
		assertFalse(xo.isStackCompatible(alphaSection, betaSection));
		assertTrue(xo.isLocalsCompatible(alphaSection, betaSection));
	}

	@Test
	public void illegalFactCrossover1() throws Exception {
		// Example from ESI paper
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Fact.class, true);
		TypeVerifier      verifier  = new TypeVerifier(classNode);

		Method             methodSpec = new Method("fact", "(I)I");
		AnalyzedMethodNode method     = classNode.findMethod(methodSpec);

		// alpha = [ fact(n-1) ], beta = [ IMUL ]
		CodeSection alphaSection = new CodeSection(methodSpec,  6, 10);
		CodeSection betaSection  = new CodeSection(methodSpec, 11, 11);

		CompatibleCrossover xo = new CompatibleCrossover(method, method, verifier);
		assertFalse(xo.isCompatible(alphaSection, betaSection));
		assertFalse(xo.isStackCompatible(alphaSection, betaSection));
		assertTrue(xo.isLocalsCompatible(alphaSection, betaSection));
	}

	@Test
	public void illegalFactCrossover2() throws Exception {
		// Example from ESI paper
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Fact.class, true);
		TypeVerifier      verifier  = new TypeVerifier(classNode);

		Method             methodSpec = new Method("fact", "(I)I");
		AnalyzedMethodNode method     = classNode.findMethod(methodSpec);

		// alpha = [ int ans=1 ], beta = [ ]
		CodeSection alphaSection = new CodeSection(methodSpec, 0,  2);
		CodeSection betaSection  = new CodeSection(methodSpec, 0, -1);

		CompatibleCrossover xo = new CompatibleCrossover(method, method, verifier);
		assertFalse(xo.isCompatible(alphaSection, betaSection));
		assertTrue(xo.isStackCompatible(alphaSection, betaSection));
		assertFalse(xo.isLocalsCompatible(alphaSection, betaSection));
	}

	@Test
	public void allCrossoversFactLong() throws Exception {
		AnalyzedClassNode alphaClass = AnalyzedClassNode.readClass(FactLong.class, true);
		AnalyzedClassNode betaClass  = AnalyzedClassNode.readClass(FactLong.class, true);

		Method methodSpec = new Method("fact", "(J)J");

		int count = allCrossovers(FactLong.class.getName(), alphaClass, betaClass, methodSpec, methodSpec);
		assertEquals(2172, count);
	}

	@Test
	public void allCrossoversControl() throws Exception {
		AnalyzedClassNode alphaClass = AnalyzedClassNode.readClass(CONTROL_NAME, true, true);
		AnalyzedClassNode betaClass  = AnalyzedClassNode.readClass(CONTROL_NAME, true, true);

		Method methodSpec = new Method("main", Type.VOID_TYPE, new Type[]{ Type.getObjectType("[" + Type.getType(String.class)) });

		int count = allCrossovers(CONTROL_NAME, alphaClass, betaClass, methodSpec, methodSpec);
		assertEquals(1716 - 98, count);
	}

	// @Test /* Long */
	public void allCrossoversFlow() throws Exception {
		AnalyzedClassNode alphaClass = AnalyzedClassNode.readClass(Flow.class, true);
		AnalyzedClassNode betaClass  = AnalyzedClassNode.readClass(Flow.class, true);

		Method methodSpec = new Method("foo", "(J)V");

		int count = allCrossovers(Flow.class.getName(), alphaClass, betaClass, methodSpec, methodSpec);
		assertEquals(21773, count);
	}

	@Test
	public void randomFactCrossovers() throws Exception {
		AnalyzedClassNode classNode1 = AnalyzedClassNode.readClass(Fact.class, true);
		AnalyzedClassNode classNode2 = AnalyzedClassNode.readClass(Fact.class, true);
		Method methodSpec = new Method("fact", "(I)I");

		randomCrossovers(Fact.class.getName(), 20, classNode1, classNode2, methodSpec);
	}

	@Test
	public void randomControlCrossovers() throws Exception {
		AnalyzedClassNode classNode1 = AnalyzedClassNode.readClass(CONTROL_NAME, true, true);
		AnalyzedClassNode classNode2 = AnalyzedClassNode.readClass(CONTROL_NAME, true, true);
		Method methodSpec = new Method("main", "([Ljava/lang/String;)V");

		randomCrossovers(CONTROL_NAME, 20, classNode1, classNode2, methodSpec);
	}

	@Test
	public void randomFlowCrossovers() throws Exception {
		AnalyzedClassNode classNode1 = AnalyzedClassNode.readClass(Flow.class, true);
		AnalyzedClassNode classNode2 = AnalyzedClassNode.readClass(Flow.class, true);
		Method methodSpec = new Method("foo", "(J)V");

		for (int i = 0;  i < 10;  ++i)
			randomCrossovers(Flow.class.getName(), 20, classNode1, classNode2, methodSpec);
	}

	@Test
	public void replaceFullNewWithNull() throws Exception {
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Flow.class, true);
		TypeVerifier      verifier  = new TypeVerifier(classNode);

		Method             methodSpec = new Method("foo", Type.VOID_TYPE, new Type[]{ Type.LONG_TYPE });
		AnalyzedMethodNode method     = classNode.findMethod(methodSpec);

		// alpha = [ new Integer(1) ], beta = [ ACONST_NULL ]
		CodeSection alphaSection = new CodeSection(methodSpec, 24, 27);
		CodeSection betaSection  = new CodeSection(methodSpec, 38, 38);

		CompatibleCrossover xo = new CompatibleCrossover(method, method, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		// It's ok to keep the same name, too
		CodeMerger merger = new CodeMerger(Flow.class.getName() + "FullNewWithNull", Flow.class.getName(),
				classNode, classNode, alphaSection, betaSection);
		Class<?> mergedClass = merger.getClassLoader().loadClass(merger.getName());

		// Checks verification
		mergedClass.newInstance();
	}

	@Test
	public void replaceFullNewWithNarrower() throws Exception {
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Flow.class, true);
		TypeVerifier      verifier  = new TypeVerifier(classNode);

		Method             methodSpec = new Method("foo", "(J)V");
		AnalyzedMethodNode method     = classNode.findMethod(methodSpec);

		// alpha = [ new Object() ], beta = [ new Integer(1) ]
		CodeSection alphaSection = new CodeSection(methodSpec, 42, 45);
		CodeSection betaSection  = new CodeSection(methodSpec, 24, 27);

		CompatibleCrossover xo = new CompatibleCrossover(method, method, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		CodeMerger merger = new CodeMerger(Flow.class.getName() + "FullNewWithNarrower", Flow.class.getName(),
				classNode, classNode, alphaSection, betaSection);
		Class<?> mergedClass = merger.getClassLoader().loadClass(merger.getName());

		// Checks verification
		mergedClass.newInstance();
	}

	@Test
	public void replaceNewWithNull() throws Exception {
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Flow.class, true);
		TypeVerifier      verifier  = new TypeVerifier(classNode);

		Method             methodSpec = new Method("foo", Type.VOID_TYPE, new Type[]{ Type.LONG_TYPE });
		AnalyzedMethodNode method     = classNode.findMethod(methodSpec);

		// alpha = [ NEW java/lang/Integer ], beta = [ ACONST_NULL ]
		CodeSection alphaSection = new CodeSection(methodSpec, 24, 24);
		CodeSection betaSection  = new CodeSection(methodSpec, 38, 38);

		CompatibleCrossover xo = new CompatibleCrossover(method, method, verifier);
		assertFalse(xo.isCompatible(alphaSection, betaSection));
		assertFalse(xo.isStackCompatible(alphaSection, betaSection));
		assertTrue(xo.isLocalsCompatible(alphaSection, betaSection));
	}

	@Test
	public void replaceAllWithGoto() throws Exception {
		// When copying parts with branches, class nodes should be different
		AnalyzedClassNode alphaClassNode = AnalyzedClassNode.readClass(Flow.class, true);
		AnalyzedClassNode betaClassNode  = AnalyzedClassNode.readClass(Flow.class, true);
		TypeVerifier      verifier       = new TypeVerifier(alphaClassNode);

		Method             methodSpec  = new Method("foo", "(J)V");
		AnalyzedMethodNode alphaMethod = alphaClassNode.findMethod(methodSpec);
		AnalyzedMethodNode betaMethod  = betaClassNode.findMethod(methodSpec);

		// alpha = [ all ], beta = [ GOTO x; ...; LABEL x ]
		// 8 instructions and 1 GOTO should be removed in DCE
		CodeSection alphaSection = new CodeSection(methodSpec,  0, alphaMethod.getIndexBeforeReturn());
		CodeSection betaSection  = new CodeSection(methodSpec, 10, 17);

		CompatibleCrossover xo = new CompatibleCrossover(alphaMethod, betaMethod, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		CodeMerger merger = new CodeMerger(Flow.class.getName() + "AllWithGoto", Flow.class.getName(),
				alphaClassNode, betaClassNode, alphaSection, betaSection);
		BytesClassLoader loader = merger.getClassLoader();
		Class<?> mergedClass = loader.loadClass(merger.getName());

		// Checks verification
		mergedClass.newInstance();

		// Check that locals shrinked
		AnalyzedClassNode  mergedClassNode  = merger.getClassNode();
		AnalyzedMethodNode mergedMethodNode = mergedClassNode.findMethod(methodSpec);

		assertEquals(0, mergedMethodNode.maxStack);
		assertEquals(3, mergedMethodNode.maxLocals);
	}

	@Test
	public void insertGoto() throws Exception {
		// When copying parts with branches, class nodes should be different
		AnalyzedClassNode alphaClassNode = AnalyzedClassNode.readClass(Flow.class, true);
		AnalyzedClassNode betaClassNode  = AnalyzedClassNode.readClass(Flow.class, true);
		TypeVerifier      verifier       = new TypeVerifier(alphaClassNode);

		Method             methodSpec  = new Method("foo", "(J)V");
		AnalyzedMethodNode alphaMethod = alphaClassNode.findMethod(methodSpec);
		AnalyzedMethodNode betaMethod  = betaClassNode.findMethod(methodSpec);

		// alpha = [ prior to second NEW ], beta = [ GOTO x; ...; LABEL x ]
		// 8 instructions and 1 GOTO should be removed in DCE
		CodeSection alphaSection = new CodeSection(methodSpec, 12, 11);
		CodeSection betaSection  = new CodeSection(methodSpec, 10, 17);

		CompatibleCrossover xo = new CompatibleCrossover(alphaMethod, betaMethod, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		CodeMerger merger = new CodeMerger(Flow.class.getName() + "InsertGoto", Flow.class.getName(),
				alphaClassNode, betaClassNode, alphaSection, betaSection);
		Class<?> mergedClass = merger.getClassLoader().loadClass(merger.getName());

		// Checks verification
		mergedClass.newInstance();
	}

	@Test
	public void replaceAfterFrameBeforeFrame() throws Exception {
		AnalyzedClassNode classNode = AnalyzedClassNode.readClass(Flow.class, true);
		TypeVerifier      verifier  = new TypeVerifier(classNode);

		Method             methodSpec  = new Method("foo", "(J)V");
		AnalyzedMethodNode method = classNode.findMethod(methodSpec);

		// alpha = [ ASTORE 3 (String), LABEL (String->Object copied from FRAME) ]
		// beta  = [ ASTORE 3, LABEL, FRAME (String->Object) ]
		CodeSection alphaSection = new CodeSection(methodSpec, 16, 17);
		CodeSection betaSection  = new CodeSection(methodSpec, 16, 18);

		CompatibleCrossover xo = new CompatibleCrossover(method, method, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		// alpha = [ ALOAD 3 (Object) ]
		// beta  = [ FRAME (Object), ALOAD 3 (Object) ]
		alphaSection = new CodeSection(methodSpec, 19, 19);
		betaSection  = new CodeSection(methodSpec, 18, 19);

		xo = new CompatibleCrossover(method, method, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		// alpha = [ ALOAD 3 (Object) ]
		// beta  = [ LABEL (Object) FRAME (Object), ALOAD 3 (Object) ]
		alphaSection = new CodeSection(methodSpec, 19, 19);
		betaSection  = new CodeSection(methodSpec, 17, 19);

		// NOTE: propagation of actions has been changed to propagation
		// of frame data, and this XO is now incompatible
		xo = new CompatibleCrossover(method, method, verifier);
		assertFalse(xo.isCompatible(alphaSection, betaSection));
	}

	@Test
	public void removeCheckCast() throws Exception {
		// When copying parts with branches, class nodes should be different
		AnalyzedClassNode alphaClassNode = AnalyzedClassNode.readClass(CONTROL_NAME, true, true);
		AnalyzedClassNode betaClassNode  = AnalyzedClassNode.readClass(CONTROL_NAME, true, true);
		TypeVerifier      verifier       = new TypeVerifier(alphaClassNode);

		Method methodSpec = new Method("main", Type.VOID_TYPE, new Type[]{ Type.getObjectType("[" + Type.getType(String.class)) });
		AnalyzedMethodNode alphaMethod = alphaClassNode.findMethod(methodSpec);
		AnalyzedMethodNode betaMethod  = betaClassNode.findMethod(methodSpec);

		// alpha = [ CHECKCAST ], beta = [ ]
		// 1 GOTO should be removed in DCE
		CodeSection alphaSection = new CodeSection(methodSpec, 11, 11);
		CodeSection betaSection  = new CodeSection(methodSpec, 10, 9);

		CompatibleCrossover xo = new CompatibleCrossover(alphaMethod, betaMethod, verifier);
		assertTrue(xo.isCompatible(alphaSection, betaSection));

		CodeMerger merger = new CodeMerger(CONTROL_NAME + "CheckCast", CONTROL_NAME,
				alphaClassNode, betaClassNode, alphaSection, betaSection);
		Class<?> mergedClass = merger.getClassLoader(Config.DIR_OUT_TESTS).loadClass(merger.getName());

		// Checks verification
		mergedClass.newInstance();
	}

	@Test
	public void decoherence() throws Exception {
		// When copying parts with branches, class nodes should be different
		AnalyzedClassNode alphaClassNode = AnalyzedClassNode.readClass(DecoherentVars.class, true);
		AnalyzedClassNode betaClassNode  = AnalyzedClassNode.readClass(DecoherentVars.class, true);
		TypeVerifier      verifier       = new TypeVerifier(alphaClassNode);

		Method             methodSpec  = new Method("foo", "()V");
		AnalyzedMethodNode alphaMethod = alphaClassNode.findMethod(methodSpec);
		AnalyzedMethodNode betaMethod  = betaClassNode.findMethod(methodSpec);

		// alpha = [ between J write and read ], beta = [ decoherent write ]
		CodeSection alphaSection = new CodeSection(methodSpec, 3, 2);
		CodeSection betaSection  = new CodeSection(methodSpec, 7, 15);

		CompatibleCrossover xo = new CompatibleCrossover(alphaMethod, betaMethod, verifier);
		assertFalse(xo.isCompatible(alphaSection, betaSection));
		assertTrue(xo.isStackCompatible(alphaSection, betaSection));
		assertFalse(xo.isLocalsCompatible(alphaSection, betaSection));
	}

	private int allCrossovers(String origName, AnalyzedClassNode alphaClass, AnalyzedClassNode betaClass, Method alphaMethodSpec, Method betaMethodSpec) throws Exception {
		TypeVerifier       verifier    = new TypeVerifier(alphaClass);
		AnalyzedMethodNode alphaMethod = alphaClass.findMethod(alphaMethodSpec);
		AnalyzedMethodNode betaMethod  = betaClass.findMethod(betaMethodSpec);

		BranchAnalyzer     alphaSections = new BranchAnalyzer(alphaMethod, true);
		BranchAnalyzer     betaSections  = new BranchAnalyzer(betaMethod,  false);

		Class<?>[] args = typeToClass(alphaMethodSpec.getArgumentTypes());
		Class<?>   ret  = typeToClass(alphaMethodSpec.getReturnType());

		CompatibleCrossover xo = new CompatibleCrossover(alphaMethod, betaMethod, verifier);
		int count = 0;

		for (CodeSection alphaSection: alphaSections)
			for (CodeSection betaSection: betaSections)
				if (xo.isCompatible(alphaSection, betaSection)) {
					String name = origName
						+ "_A" + alphaSection.start + "_" + alphaSection.end
						+ "_B" + betaSection.start  + "_" + betaSection.end;
					name = name.replace("-1", "m1");

					CodeMerger merger = new CodeMerger(name, origName, alphaClass, betaClass, alphaSection, betaSection);
					BytesClassLoader loader = merger.getClassLoader();

					Class<?> mergedClass = loader.loadClass(name);

					// Checks verification
					Object instance = mergedClass.newInstance();
					assertEquals(name, instance.getClass().getName());

					// Checks method is there
					java.lang.reflect.Method method = mergedClass.getMethod(alphaMethodSpec.getName(), args);
					assertSame(ret, method.getReturnType());

					// Check that new locals weren't added
					AnalyzedClassNode  mergedClassNode  = merger.getClassNode();
					AnalyzedMethodNode mergedMethodNode = mergedClassNode.findMethod(alphaMethodSpec);

					int maxStack  = alphaMethod.maxStack + betaMethod.maxStack;
					int maxLocals = Math.max(alphaMethod.maxLocals, betaMethod.maxLocals);

					assertTrue(mergedMethodNode.maxStack  <= maxStack);
					assertTrue(mergedMethodNode.maxLocals <= maxLocals);

					++count;
				}

		return count;
	}

	private Class<?> typeToClass(Type type) {
		assertEquals("[Ljava/lang/String;", Type.getObjectType("[" + Type.getType(String.class)).getDescriptor());

		if (type.equals(Type.VOID_TYPE))
			return Void.TYPE;
		else if (type.equals(Type.INT_TYPE))
			return Integer.TYPE;
		else if (type.equals(Type.LONG_TYPE))
			return Long.TYPE;
		else if (type.equals(Type.getObjectType("[" + Type.getType(String.class))))
			return String[].class;
		else
			throw new IllegalArgumentException("Unhandled type");
	}

	private Class<?>[] typeToClass(Type[] types) {
		Class<?>[] classes = new Class[types.length];

		for (int i = 0;  i < types.length;  ++i)
			classes[i] = typeToClass(types[i]);

		return classes;
	}

	private void randomCrossovers(String origName, int iterations, AnalyzedClassNode classNode1, AnalyzedClassNode classNode2, Method methodSpec) throws Exception {
		CodeMerger        merger     = null;

		for (int i = 0;  i < iterations;  ++i) {
			TypeVerifier       verifier   = new TypeVerifier(classNode1, classNode2);

			AnalyzedMethodNode method1    = classNode1.findMethod(methodSpec);
			AnalyzedMethodNode method2    = classNode2.findMethod(methodSpec);

			BranchAnalyzer alphaSections  = new BranchAnalyzer(method1, true);
			BranchAnalyzer betaSections   = new BranchAnalyzer(method2, false);

			Map<Integer, List<CodeSection>> sections1 = alphaSections.getSortedSections();
			Map<Integer, List<CodeSection>> sections2 = betaSections.getSortedSections();

			CompatibleCrossover xo = new CompatibleCrossover(method1, method2, verifier);
			CodeSection section1;
			CodeSection section2;

			while (true) {
				section1 = getRandomSection(sections1);
				section2 = getRandomSection(sections2);

				if (xo.isCompatible(section1, section2))
					break;
			}

			String name = origName + "_" + i;
			merger = new CodeMerger(name, origName, classNode1, classNode2, section1, section2);
			BytesClassLoader loader = merger.getClassLoader();

			Class<?>          mergedClass     = loader.loadClass(name);
			AnalyzedClassNode mergedClassNode = merger.getClassNode();

			if (random.nextBoolean())
				classNode1 = mergedClassNode;
			else
				classNode2 = mergedClassNode;

			// Check verification
			mergedClass.newInstance();
		}
	}

	private CodeSection getRandomSection(Map<Integer, List<CodeSection>> secs) {
		Integer[] keys = secs.keySet().toArray(new Integer[0]);
		int key = keys[random.nextInt(keys.length)];

		List<CodeSection> list = secs.get(key);
		CodeSection sec = list.get(random.nextInt(list.size()));

		return sec;
	}

	private void timeCrossovers(String origName, AnalyzedClassNode classNode, Method methodSpec) throws IOException {
		long before = System.nanoTime();
		int count = timeCrossovers(classNode, classNode, methodSpec, methodSpec);
		long diff = System.nanoTime() - before;

		System.out.printf("%s Compatible XO: %fs (count = %d)\n", origName, (diff / 1000000000.0), count);

		before = System.nanoTime();
		count = timeTrivialCrossovers(origName, classNode, classNode, methodSpec, methodSpec);
		diff = System.nanoTime() - before;

		System.out.printf("%s Trivial XO: %fs (count = %d)\n", origName, (diff / 1000000000.0), count);
	}

	private int timeCrossovers(AnalyzedClassNode alphaClass, AnalyzedClassNode betaClass, Method alphaMethodSpec, Method betaMethodSpec) {
		TypeVerifier       verifier    = new TypeVerifier(alphaClass);
		AnalyzedMethodNode alphaMethod = alphaClass.findMethod(alphaMethodSpec);
		AnalyzedMethodNode betaMethod  = betaClass.findMethod(betaMethodSpec);

		BranchAnalyzer     alphaSections = new BranchAnalyzer(alphaMethod, true);
		BranchAnalyzer     betaSections  = new BranchAnalyzer(betaMethod,  false);

		CompatibleCrossover xo = new CompatibleCrossover(alphaMethod, betaMethod, verifier);
		int count = 0;

		for (CodeSection alphaSection: alphaSections)
			for (CodeSection betaSection: betaSections)
				if (xo.isCompatible(alphaSection, betaSection))
					++count;

		return count;
	}

	private int timeTrivialCrossovers(String origName, AnalyzedClassNode alphaClass, AnalyzedClassNode betaClass, Method alphaMethodSpec, Method betaMethodSpec) {
		AnalyzedMethodNode alphaMethod = alphaClass.findMethod(alphaMethodSpec);
		AnalyzedMethodNode betaMethod  = betaClass.findMethod(betaMethodSpec);

		BranchAnalyzer     alphaSections = new BranchAnalyzer(alphaMethod, true);
		BranchAnalyzer     betaSections  = new BranchAnalyzer(betaMethod,  false);

		int count = 0;

		for (CodeSection alphaSection: alphaSections)
			for (CodeSection betaSection: betaSections) {

					String name = origName
						+ "_A" + alphaSection.start + "_" + alphaSection.end
						+ "_B" + betaSection.start  + "_" + betaSection.end;
					name = name.replace("-1", "m1");

					try {
						CodeMerger merger = new CodeMerger(name, origName, alphaClass, betaClass, alphaSection, betaSection);
						Class<?> mergedClass = merger.getClassLoader().loadClass(name);

						// Checks verification
						mergedClass.newInstance();

						++count;
					} catch (Exception e) {
						// Ignore ASM Exceptions
					} catch (VerifyError e) {
						// Ignore verification errors
					}
			}

		return count;
	}

}
