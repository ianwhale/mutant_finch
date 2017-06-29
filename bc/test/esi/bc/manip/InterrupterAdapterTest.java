package esi.bc.manip;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;

import esi.bc.BytesClassLoader;
import esi.bc.manip.test.Loops;

public class InterrupterAdapterTest {

	private static final String  SIMPLE_METHOD  = "simple";
	private static final String  COMPLEX_METHOD = "complex";

	public static class SleepInterrupter {
		public static void interrupt(int id) throws InterruptedException {
			if (id == 3)
				// Non-interruptible!
				Thread.sleep(0);
		}
	}

	@Test
	public void simple() throws Exception {
		testLoop(SIMPLE_METHOD, false);
		testLoop(SIMPLE_METHOD, true);
	}

	@Test
	public void complex() throws Exception {
		testLoop(COMPLEX_METHOD, true);
	}

	@SuppressWarnings("deprecation")
	private void testLoop(String methodName, boolean transform) throws Exception {
		Class<?> cl;

		if (transform) {
			String oldName = Loops.class.getName();
			String newName = Loops.class.getName() + "_Int";

			ClassReader cr = new ClassReader(oldName);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			ClassAdapter renamer = new RemappingClassAdapter(cw,
					new SimpleRemapper(oldName.replace('.', '/'), newName.replace('.', '/')));
			ClassVisitor cv      = new InterrupterClassAdapter(renamer, SleepInterrupter.class.getName(), "interrupt", 3);
			cr.accept(cv, ClassReader.SKIP_DEBUG);

			BytesClassLoader loader = new BytesClassLoader(newName, cw.toByteArray());
			cl = loader.loadClass(newName);
			assertNotSame(Loops.class, cl);
		}
		else
			cl = Loops.class;

		final Method    met = cl.getDeclaredMethod(methodName);
		final Object    obj = cl.newInstance();
		final boolean[] ok  = { false };

		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					met.invoke(obj);
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
					ok[0] = (e.getCause() instanceof InterruptedException);
				}
			}
		};
		thread.start();
		thread.join(5);

		assertTrue(thread.isAlive());
		thread.interrupt();

		Thread.sleep(50);
		assertEquals(!transform, thread.isAlive());
		assertEquals(transform,  ok[0]);

		// Stop the thread, so that other tests (if any) have CPU time
		// NOTE: This only seems to work for the SIMPLE case on 64-bit dual-core CPU
		if (thread.isAlive())
			thread.stop();
	}

}
