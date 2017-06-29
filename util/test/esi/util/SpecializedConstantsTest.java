package esi.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

public class SpecializedConstantsTest {

	private static final String PROP_NAME = "class." + SpecializedConstantsTest.class.getSimpleName() + ".test";

	@After
	public void tearDown() {
		if (Config.esiProperties.getProperty(PROP_NAME) != null)
			assertNotNull(Config.esiProperties.remove(PROP_NAME));
	}

	@Test
	public void getString() {
		Config.esiProperties.setProperty(PROP_NAME, " \n  string \t");
		assertEquals("string", SpecializedConstants.get(SpecializedConstantsTest.class, "test"));
	}

	@Test
	public void getList() {
		Config.esiProperties.setProperty(PROP_NAME, "  abc,  d.ef  \n Geh,, ");

		String[] props = SpecializedConstants.getList(SpecializedConstantsTest.class, "test");

		assertEquals(3, props.length);
		assertEquals("abc",  props[0]);
		assertEquals("d.ef", props[1]);
		assertEquals("Geh",  props[2]);
	}

	@Test
	public void getInt() {
		Config.esiProperties.setProperty(PROP_NAME, "5");
		assertEquals(5, SpecializedConstants.getInt(SpecializedConstantsTest.class, "test"));
	}

	@Test
	public void getFloat() {
		Config.esiProperties.setProperty(PROP_NAME, "1.25");
		assertTrue(1.25f == SpecializedConstants.getFloat(SpecializedConstantsTest.class, "test"));
	}

	@Test
	public void getBoolean() {
		Config.esiProperties.setProperty(PROP_NAME, "yes");
		assertEquals(true, SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test"));
		Config.esiProperties.setProperty(PROP_NAME, "Yes");
		assertEquals(true, SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test"));
		Config.esiProperties.setProperty(PROP_NAME, "ON");
		assertEquals(true, SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test"));
		Config.esiProperties.setProperty(PROP_NAME, "truE");
		assertEquals(true, SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test"));
		Config.esiProperties.setProperty(PROP_NAME, "No");
		assertEquals(false, SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test"));
		Config.esiProperties.setProperty(PROP_NAME, "off");
		assertEquals(false, SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test"));
		Config.esiProperties.setProperty(PROP_NAME, "FALSE");
		assertEquals(false, SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test"));
	}

	@Test(expected = NullPointerException.class)
	public void getStringException() {
		SpecializedConstants.get(SpecializedConstantsTest.class, "test");
	}

	@Test(expected = NumberFormatException.class)
	public void getIntException() {
		Config.esiProperties.setProperty(PROP_NAME, "1.25");
		SpecializedConstants.getInt(SpecializedConstantsTest.class, "test");
	}

	@Test(expected = NumberFormatException.class)
	public void getFloatException() {
		Config.esiProperties.setProperty(PROP_NAME, "non-float");
		SpecializedConstants.getFloat(SpecializedConstantsTest.class, "test");
	}

	@Test(expected = NumberFormatException.class)
	public void getBooleanException() {
		Config.esiProperties.setProperty(PROP_NAME, "1");
		SpecializedConstants.getBoolean(SpecializedConstantsTest.class, "test");
	}

	@Test
    public void getListString() {
        String   value = "  abc,  d.ef  \n Geh,, ";
        String[] list  = SpecializedConstants.getList(value);

        assertEquals(3, list.length);
        assertEquals("abc",  list[0]);
        assertEquals("d.ef", list[1]);
        assertEquals("Geh",  list[2]);
    }

}
