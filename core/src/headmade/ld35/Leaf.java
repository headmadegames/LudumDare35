package headmade.ld35;

public class Leaf {

	private static final String	TAG	= Leaf.class.getName();

	public int					x;
	public int					y;

	public Leaf(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "Leaf [x=" + x + ", y=" + y + "]";
	}

}
