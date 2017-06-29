package esi.bc.flow;

import static org.junit.Assert.*;

import org.junit.Test;
import org.objectweb.asm.commons.Method;

public class CodeSectionTest {

	@Test
	public void testCodeSection() {
		Method      method  = new Method("fact", "(I)I");
		CodeSection section = new CodeSection(method, 1, 5);

		assertSame(method, section.method);
		assertEquals(1, section.start);
		assertEquals(5, section.end);

		assertEquals(5, section.size());
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCodeSectionNegIndex() {
		new CodeSection(null, -1, 2);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCodeSectionWrongOrder() {
		new CodeSection(null, 3, 1);
	}

	@Test
	public void testCodeSectionEmpty() {
		new CodeSection(null, 2, 1);
	}

	@Test
	public void compareTo() {
		CodeSection s1 = new CodeSection(null, 1, 2);
		CodeSection s2 = new CodeSection(null, 2, 3);
		CodeSection s3 = new CodeSection(null, 3, 5);

		assertEquals(0, s1.compareTo(s1));
		assertEquals(0, s1.compareTo(s2));
		assertTrue(s1.compareTo(s3) < 0);
		assertTrue(s3.compareTo(s1) > 0);
	}

}
