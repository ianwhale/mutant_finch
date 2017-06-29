package esi.bc.manip.test;

@RenameAnnotation(attr = Rename.class)
public class Rename {

	private static Rename staticField = null;
	private        Rename field       = null;

	@RenameAnnotation(attr = Rename.class)
	public Rename f(Rename x) throws RenameException {
		if (x == null) {
			x = new Rename();
			x = f(x);
			throw new RenameException();
		}

		// Frames unification, problematic when renaming
		// and computing frames
		Object q = new Rename();
		if (q == null)
			q = new Integer(1);

		x = this;
		field = x;
		staticField = field;
		field = staticField;

		Class<Rename> klass = Rename.class;
		klass.getClass();

		try {
			Rename[][][] ma = new Rename[1][1][];
			ma[0][0] = new Rename[1];
			ma[0][0][0] = new Rename();
			throw new RenameException();
		} catch (RenameException e) {
			long y = e.someField;
			y = y+1;
		}

		return x;
	}

}
