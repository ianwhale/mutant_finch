package esi.finch.probs;

/**
 * Artificial Ant (Santa Fe trail).
 * Koza I, Chapter 7.2.
 *
 * @author Michael Orlov
 */
public class ArtificialAnt {

	private static final String SANTA_FE_FILE = "santafe.trl";

	// Package-level access for testing purposes
	// Immutable!
	static final ArtificialAntMap antMap =
		new ArtificialAntMap(ArtificialAntMap.class.getResource(SANTA_FE_FILE));

	public static class OperationsLimit extends RuntimeException {
		private static final long serialVersionUID = 1;
		public final int ops;

		public OperationsLimit(int ops) {
			super("Operations limit of " + ops + " reached");
			this.ops = ops;
		}
	}

	private final int	maxOps;
	private int			opsCount;

	// Package-level access for testing purposes
	final boolean[][]	visitMap;
	private int			eaten;
	private int			x,  y;		// col, row
	private int			dx, dy;		// { -1, 0, +1 }

	public ArtificialAnt(int maxOps) {
		this.maxOps = maxOps;
		opsCount    = 0;

		boolean[][] model = antMap.foodMap;
		visitMap = new boolean[model.length][];

		for (int row = 0;  row < visitMap.length;  ++row)
			// Initialized to 'false'
			visitMap[row] = new boolean[model[row].length];

		eaten = 0;
		x  = 0;	 y = 0;
		dx = 1; dy = 0;

		visit();
		assert opsCount == 0;
	}

	// Perform as many steps as possible
	public void go() {
		// Each step is deterministic, and either moves or doesn't move.
		// Therefore we can perform at most maxOps+food steps, avoiding timeouts.
		int maxSteps = maxOps + antMap.totalFood;
		while (!ateAll()  &&  --maxSteps >= 0)
			step();
	}

	// Avoider (Koza I, p.151)
	public void step() {
		if (foodAhead())
			right();
		else if (foodAhead())
			right();
		else {
			move();
			left();
		}
	}

	// Best-of-run (Koza I, p.154)
	// Needs 545 steps!
	public void stepSolution() {
		if (foodAhead())
			move();
		else {
			left();

			if (foodAhead())
				move();
			else
				right();

			right();
			left();
			right();

			if (foodAhead())
				move();
			else
				left();

			move();
		}
	}

	// Visits current cell
	private void visit() {
		if (! visitMap[y][x]) {
			visitMap[y][x] = true;

			if (antMap.foodMap[y][x]) {
				++eaten;

				// Eating makes a move not count
				--opsCount;
			}
		}
	}

	// Package-level access for testing purposes
	// Moves to next cell in current direction
	void move() {
		x = (x + dx + antMap.width)  % antMap.width;
		y = (y + dy + antMap.height) % antMap.height;
		visit();
		operation();
	}

	// Package-level access for testing purposes
	// Turns counter-clockwise
	void left() {
		if (dy == 0) {
			dy = -dx;
			dx = 0;
		}
		else {
			dx = dy;
			dy = 0;
		}

		assert dx == 0  ||  dy == 0;
		assert Math.abs(dx) == 1  ||  Math.abs(dy) == 1;

		// operation();
	}

	// Package-level access for testing purposes
	// Turns clockwise
	void right() {
		if (dy == 0) {
			dy = dx;
			dx = 0;
		}
		else {
			dx = -dy;
			dy = 0;
		}

		assert dx == 0  ||  dy == 0;
		assert Math.abs(dx) == 1  ||  Math.abs(dy) == 1;

		// operation();
	}

	private void operation() {
		if (++opsCount >= maxOps)
			throw new OperationsLimit(opsCount);
	}

	// Package-level access for testing purposes
	// Checks whether a food pellet is at next cell
	boolean foodAhead() {
		int xx = (x + dx + antMap.width)  % antMap.width;
		int yy = (y + dy + antMap.height) % antMap.height;

		return antMap.foodMap[yy][xx]  &&  !visitMap[yy][xx];
	}

	// Returns number of eaten food pellets
	public int getEatenCount() {
		return eaten;
	}

	// Returns true if all food pellets were eaten
	public boolean ateAll() {
		assert eaten <= antMap.totalFood;
		return eaten == antMap.totalFood;
	}

	@Override
	public String toString() {
		return antMap.toString(visitMap);
	}

}
