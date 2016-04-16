package headmade.ld35;

import com.badlogic.gdx.physics.box2d.Body;

public class Leaf {

	private static final String	TAG	= Leaf.class.getName();

	private Body				body;

	public Leaf(Body body) {
		super();
		this.body = body;
	}
}
