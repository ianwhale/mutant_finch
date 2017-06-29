package esi.bc.manip;

import static org.junit.Assert.*;

import org.junit.Test;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;

import esi.bc.BytesClassLoader;
import esi.bc.manip.test.Constants;

public class ConstantsAdapterTest {

	private static class CountsChecker extends IdentityConstantsMutator {
		boolean visited = false;

		@Override
		public void visitCounts(int ints, int floats, int longs, int doubles, int strings, int types) {
			assertEquals(3, ints);
			assertEquals(2, floats);
			assertEquals(2, longs);
			assertEquals(3, doubles);
			assertEquals(3, strings);
			assertEquals(1, types);

			visited = true;
		}
	}

	@Test
	public void mutateInt() throws Exception {
		Constants cs = new Constants();
		String[] funcs = { "getSimpleInt", "getSimpleInt1", "getByteInt", "getShortInt", "getBigInt" };
		int[]    offs  = { cs.getSimpleInt(), cs.getSimpleInt1(), cs.getByteInt(), cs.getShortInt(), cs.getBigInt() };

		int[] tests = { 123456,
				-32769, -32768, -32767, -129, -128, -127,
				-2, -1, 0, 1, 2, 3, 4, 5, 6,
				126, 127, 128, 32766, 32767, 32768 };

		for (int j = 0;  j < funcs.length;  ++j) {
			String    func = funcs[j];
			final int off  = offs[j];

			for (int i = 0;  i < tests.length;  ++i) {
				final int base = tests[i];

				ConstantsMutator mutator = new IdentityConstantsMutator() {
					@Override
					public int mutate(int x) {
						return x - off + base;
					}
				};

				int res = (Integer) callMutated(func, "()I", mutator);
				assertEquals(base, res);
			}
		}
	}

	@Test
	public void mutateFloat() throws Exception {
		Constants cs = new Constants();
		String[] funcs = { "getFloat0", "getFloat1", "getFloat2", "getFloat3" };
		float[]  offs  = { cs.getFloat0(), cs.getFloat1(), cs.getFloat2(), cs.getFloat3() };

		float[]  tests = { -2, -1, 0, 1, 2, 3, 4 };

		for (int j = 0;  j < funcs.length;  ++j) {
			String      func = funcs[j];
			final float off  = offs[j];

			for (int i = 0;  i < tests.length;  ++i) {
				final float base = tests[i];

				ConstantsMutator mutator = new IdentityConstantsMutator() {
					@Override
					public float mutate(float x) {
						return x - off + base;
					}
				};

				float res = (Float) callMutated(func, "()F", mutator);
				assertEquals(base, res, 0);
			}
		}
	}

	@Test
	public void mutateLong() throws Exception {
		Constants cs = new Constants();
		String[] funcs = { "getLong0", "getLong1", "getLong2" };
		long[]   offs  = { cs.getLong0(), cs.getLong1(), cs.getLong2() };

		long[]  tests = { -2, -1, 0, 1, 2, 3, 4 };

		for (int j = 0;  j < funcs.length;  ++j) {
			String     func = funcs[j];
			final long off  = offs[j];

			for (int i = 0;  i < tests.length;  ++i) {
				final long base = tests[i];

				ConstantsMutator mutator = new IdentityConstantsMutator() {
					@Override
					public long mutate(long x) {
						return x - off + base;
					}
				};

				long res = (Long) callMutated(func, "()J", mutator);
				assertEquals(base, res);
			}
		}
	}

	@Test
	public void mutateDouble() throws Exception {
		Constants cs = new Constants();
		String[] funcs = { "getDouble0", "getDouble1", "getDouble2" };
		double[] offs  = { cs.getDouble0(), cs.getDouble1(), cs.getDouble2() };

		double[]  tests = { -2, -1, 0, 1, 2, 3, 4 };

		for (int j = 0;  j < funcs.length;  ++j) {
			String       func = funcs[j];
			final double off  = offs[j];

			for (int i = 0;  i < tests.length;  ++i) {
				final double base = tests[i];

				ConstantsMutator mutator = new IdentityConstantsMutator() {
					@Override
					public double mutate(double x) {
						return x - off + base;
					}
				};

				double res = (Double) callMutated(func, "()D", mutator);
				assertEquals(base, res, 0);
			}
		}
	}

	@Test
	public void mutateString() throws Exception {
		ConstantsMutator mutator = new IdentityConstantsMutator() {
			@Override
			public String mutate(String x) {
				return x.toUpperCase();
			}

			@Override
			public int mutate(int x) {
				return x - 5;
			}
		};

		String res = (String) callMutated("getString", "()Ljava/lang/String;", mutator);
		assertEquals("ABC", res);
	}

	@Test
	public void mutateClass() throws Exception {
		ConstantsMutator mutator = new IdentityConstantsMutator() {
			@Override
			public Type mutate(Type x) {
				assertEquals(Constants.class.getName(), x.getClassName());
				return Type.getType(Constants[].class);
			}
		};

		Class<?> res = (Class<?>) callMutated("getKlass", "()Ljava/lang/Class;", mutator);
		assertEquals(Constants[].class.getName().replace("Constants", "Constants_Mut"), res.getName());
	}

	private Object callMutated(String methodName, String methodDesc, ConstantsMutator mutator) throws Exception {
		String oldName = Constants.class.getName();
		String newName = Constants.class.getName() + "_Mut";

		ClassReader cr = new ClassReader(oldName);
		ClassWriter cw = new ClassWriter(0);

		Method method  = new Method(methodName, methodDesc);

		ClassAdapter renamer = new RemappingClassAdapter(cw,
				new SimpleRemapper(oldName.replace('.', '/'), newName.replace('.', '/')));
		ClassVisitor cv      = new ConstantsClassAdapter(renamer, method, mutator);
		cr.accept(cv, 0);

		BytesClassLoader loader = new BytesClassLoader(newName, cw.toByteArray());
		Class<?>         cl     = loader.loadClass(newName);
		assertNotSame(Constants.class, cl);

		java.lang.reflect.Method met = cl.getDeclaredMethod(methodName);

		Object obj = cl.newInstance();
		Object res = met.invoke(obj);

		return res;
	}

	@Test
	public void countConstants() throws Exception {
		ClassReader cr = new ClassReader(Constants.class.getName());
		Method method  = new Method("foo", "(I)V");

		ConstantsCounter counter = new ConstantsCounter();
		ClassVisitor cv = new ConstantsClassAdapter(new EmptyVisitor(), method, counter);
		cr.accept(cv, 0);

		CountsChecker checker = new CountsChecker();
		counter.accept(checker);
		assertTrue(checker.visited);
	}

	@Test
	public void testOpcodes() {
		int iBase = Opcodes.ICONST_0;
		int fBase = Opcodes.FCONST_0;
		int lBase = Opcodes.LCONST_0;
		int dBase = Opcodes.DCONST_0;

		assertEquals(iBase - 1, Opcodes.ICONST_M1);
		assertEquals(iBase + 1, Opcodes.ICONST_1);
		assertEquals(iBase + 2, Opcodes.ICONST_2);
		assertEquals(iBase + 3, Opcodes.ICONST_3);
		assertEquals(iBase + 4, Opcodes.ICONST_4);
		assertEquals(iBase + 5, Opcodes.ICONST_5);

		assertEquals(fBase + 1, Opcodes.FCONST_1);
		assertEquals(fBase + 2, Opcodes.FCONST_2);

		assertEquals(lBase + 1, Opcodes.LCONST_1);

		assertEquals(dBase + 1, Opcodes.DCONST_1);
	}

}
