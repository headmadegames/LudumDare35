package headmade.ld35;

import java.util.Arrays;

public class ShapeJson {
	public String	desc;
	public int[][]	shape;
	public String	shapeText;

	@Override
	public String toString() {
		return "ShapeJson [desc=" + desc + ", shape=" + Arrays.toString(shape) + "]";
	}
	//
	// public CharSequence shapeText() {
	// if (shapeText == null) {
	// final StringBuilder sb = new StringBuilder();
	// for (int y = shape.length - 1; y >= 0; y--) {
	// for (int x = 0; x < shape[y].length; x++) {
	// sb.append(shape[y][x] == 1 ? 'X' : ' ');
	// }
	// sb.append('\n');
	// }
	// shapeText = sb.toString();
	// }
	// return shapeText;
	// }

}
