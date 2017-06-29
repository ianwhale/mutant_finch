package esi.bc.manip;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import com.thoughtworks.xstream.XStream;

import esi.bc.AnalyzedClassNode;
import esi.bc.manip.test.Loops;
import esi.util.Config;

public class CodeInterrupterTest {

	private static final String  COMPLEX_METHOD = "complex";

	public static class SleepInterrupter {
		public static void interrupt(int id) throws InterruptedException {
			if (id == 10  ||  id == 200)
				Thread.sleep(0);
		}
	}

	private static XStream xstream;

	@BeforeClass
	public static void setUpBeforeClass() {
		xstream = new XStream();
	}

	@Test
	public void consistency() throws IOException {
		AnalyzedClassNode cn = AnalyzedClassNode.readClass(Loops.class);
		String rep = xstream.toXML(cn);

		new CodeInterrupter(Loops.class.getName() + "Int", cn, SleepInterrupter.class.getName(), "interrupt", 0);
		String repAfter = xstream.toXML(cn);

		assertEquals(rep, repAfter);
	}

	@Test
	public void classNodeLoop() throws Exception {
		String            name = Loops.class.getName() + "IntTree";
		AnalyzedClassNode cn   = AnalyzedClassNode.readClass(Loops.class, true);
		CodeProducer      cp   = new CodeInterrupter(name, cn, SleepInterrupter.class.getName(), "interrupt", 10);
		assertEquals(name, cp.getName());

		testLoop(cp);
	}

	@Test
	public void classReaderLoop() throws Exception {
		String       name = Loops.class.getName() + "IntPipe";
		ClassReader  cr   = new ClassReader(Loops.class.getName());
		CodeProducer cp   = new CodeInterrupter(name, cr, SleepInterrupter.class.getName(), "interrupt", 200);
		assertEquals(name, cp.getName());

		testLoop(cp);
	}

	private void testLoop(CodeProducer cp) throws Exception {
		Class<?>        cl  = cp.getClassLoader(Config.DIR_OUT_TESTS).loadClass(cp.getName());
		final Method    met = cl.getDeclaredMethod(COMPLEX_METHOD);
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
		assertFalse(thread.isAlive());
		assertTrue(ok[0]);
	}

}
