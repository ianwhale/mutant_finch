package esi.finch.probs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class ArtificialAntMap {

	public final boolean[][]	foodMap;

	public final int			height;
	public final int			width;

	public final int			totalFood;

	public ArtificialAntMap(URL inFile) {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(inFile.openStream()));

			// Calculate dimensions
			String dims = reader.readLine();
			width  = Integer.valueOf(dims.substring(0, dims.indexOf(' ')));
			height = Integer.valueOf(dims.substring(dims.indexOf(' ') + 1));

			// All cells initialized to false
			foodMap = new boolean[height][width];

			// Load lines, '#' means 'food' (ignore '.', denoting trail)
			String line;
			int    row   = 0;
			int    count = 0;
			while ((line = reader.readLine()) != null) {
				assert row < height;
				assert line.length() < width;

				for (int col = 0;  col < line.length();  ++col) {
					foodMap[row][col] = (line.charAt(col) == '#');
					count += (foodMap[row][col] ? 1 : 0);
				}

				++row;
			}

			totalFood = count;
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load trail", e);
		}
	}

	public String toString(boolean[][] visitMap) {
		StringBuilder buf = new StringBuilder();
		assert foodMap.length == visitMap.length;

		for (int row = 0;  row < visitMap.length;  ++row) {
			assert foodMap[row].length == visitMap[row].length;

			for (int col = 0;  col < visitMap[row].length;  ++col) {
				char rep;
				if (visitMap[row][col])
					rep = foodMap[row][col] ? 'x' : '.';
				else
					rep = foodMap[row][col] ? '#' : ' ';

				buf.append(rep);
			}

			buf.append('\n');
		}

		return buf.toString();
	}

}
