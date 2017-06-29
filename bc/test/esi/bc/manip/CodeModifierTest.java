package esi.bc.manip;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.commons.Method;

import com.thoughtworks.xstream.XStream;

import esi.bc.AnalyzedClassNode;
import esi.bc.BytesClassLoader;
import esi.bc.test.Fact;
import esi.util.Config;

public class CodeModifierTest {

	private static AnalyzedClassNode factNode;
	private static Method            factMethod;

	private static XStream           xstream;
	private static String            factNodeRep;

	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		factNode    = AnalyzedClassNode.readClass(Fact.class);
		factMethod  = new Method("fact", "(I)I");

		xstream     = new XStream();
		factNodeRep = xstream.toXML(factNode);
	}

	@Test
	public void testCodeModifier() throws Exception {
		ConstantsMutator mutator = new IdentityConstantsMutator() {
			private boolean countOk = false;
			private boolean mutated = false;

			@Override
			public int mutate(int x) {
				assertTrue(countOk);
				assertTrue(x == 1);

				if (! mutated)
					x = 2;

				mutated = true;
				return x;
			}

			@Override
			public void visitCounts(int ints, int floats, int longs, int doubles, int strings, int types) {
				assertFalse(countOk);
				assertEquals(2, ints);
				assertEquals(0, floats);
				assertEquals(0, longs);
				assertEquals(0, doubles);
				assertEquals(0, strings);
				assertEquals(0, types);
				countOk = true;
			}
		};

		String newName = Fact.class.getName() + "MTest";
		CodeModifier modifier = new CodeModifier(newName, factNode, factMethod, mutator, null, false);

		BytesClassLoader  loader          = modifier.getClassLoader(Config.DIR_OUT_TESTS);
		Class<?>          mergedClass     = loader.loadClass(newName);
		AnalyzedClassNode mergedClassNode = modifier.getClassNode();

		assertEquals(newName,                   mergedClass.getName());
		assertEquals(newName.replace('.', '/'), mergedClassNode.name);

		// Checks verification
		Object instance = mergedClass.newInstance();

		// Invoke FactMTest.f(5)
		java.lang.reflect.Method method = mergedClass.getMethod("fact", Integer.TYPE);
		assertSame(Integer.TYPE, method.getReturnType());
		assertEquals(120 * 2, method.invoke(instance, 5));

		// Check that ClassNode of factNode wasn't changed
		assertEquals(factNodeRep, xstream.toXML(factNode));
	}

}
