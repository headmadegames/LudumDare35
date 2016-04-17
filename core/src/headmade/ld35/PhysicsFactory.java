package headmade.ld35;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;

import headmade.util.RandomUtil;
import net.dermetfan.gdx.graphics.g2d.Box2DSprite;
import net.dermetfan.gdx.physics.box2d.Chain.Connection;

public class PhysicsFactory {

	private static final String	TAG					= PhysicsFactory.class.getName();

	private static final short	LEVEL_CATEGORYBITS	= 0x0001;
	private static final short	LEAF_CATEGORYBITS	= 0x0010;
	private static final short	TRUNK_CATEGORYBITS	= 0x0020;
	private static final short	POT_CATEGORYBITS	= 0x0040;
	private static final short	LIGHT_CATEGORYBITS	= 0x0100;

	private static final short	LEVEL_MASKBITS		= -1;
	// private static final short LEAF_MASKBITS = LEVEL_CATEGORYBITS | POT_CATEGORYBITS | LIGHT_CATEGORYBITS;
	private static final short	LEAF_MASKBITS		= LEVEL_CATEGORYBITS | LEAF_CATEGORYBITS | POT_CATEGORYBITS | LIGHT_CATEGORYBITS;
	private static final short	TRUNK_MASKBITS		= LEVEL_CATEGORYBITS | LIGHT_CATEGORYBITS;
	private static final short	POT_MASKBITS		= LEVEL_CATEGORYBITS | LIGHT_CATEGORYBITS | LEAF_CATEGORYBITS;
	private static final short	LIGHT_MASKBITS		= LEVEL_CATEGORYBITS | LEAF_CATEGORYBITS | TRUNK_CATEGORYBITS | POT_CATEGORYBITS;

	private Ld35				game;
	private World				world;
	private Shape				circleShape;

	// private float leafRadius = Ld35.UNIT_SCALE * 7f;

	private TextureRegion		txPot;
	private Box2DSprite			potSprite;
	private Box2DSprite			trunkSprite;
	private Box2DSprite[]		leafsSprite			= new Box2DSprite[2];

	public PhysicsFactory(World world, Ld35 game) {
		this.world = world;
		this.game = game;
		circleShape = new CircleShape();

		txPot = Assets.instance.skin.get(Assets.txPot, TextureRegion.class);

		trunkSprite = new Box2DSprite(Assets.instance.skin.get(Assets.txTrunk, TextureRegion.class));
		trunkSprite.setZIndex(10);
		potSprite = new Box2DSprite(txPot);
		potSprite.setZIndex(11);
		leafsSprite[0] = new Box2DSprite(Assets.instance.skin.get(Assets.txLeafs[3], TextureRegion.class));
		leafsSprite[1] = new Box2DSprite(Assets.instance.skin.get(Assets.txLeafs[2], TextureRegion.class));
		int i = 1;
		for (final Box2DSprite box2dSprite : leafsSprite) {
			box2dSprite.setScale(2f);
			box2dSprite.setZIndex(11 + i);
			i++;
		}
	}

	public void dispose() {
		circleShape.dispose();
	}

	public Body createFloor() {
		final BodyDef def = new BodyDef();
		def.type = BodyType.StaticBody;
		final Body body = world.createBody(def);
		final FixtureDef fixDef = new FixtureDef();
		final PolygonShape groundShape = new PolygonShape();
		groundShape.setAsBox(5000f, 50f, new Vector2(0.0f, -50f), 0);
		fixDef.shape = groundShape;
		body.createFixture(fixDef);
		groundShape.dispose();
		return body;
	}

	public Bush createBush(float posX, float posY, float width, float height) {
		final Bush bush = new Bush();
		final BodyDef def = new BodyDef();
		def.type = BodyType.DynamicBody;

		final Body trunkBody;
		final Body potBody;
		final float trunkWidth = width / 2f;
		final float trunkHeight = height + width * 0.8f;
		game.trunkRect = new Rectangle(posX - trunkWidth / 2f, posY + width, trunkWidth, trunkHeight);
		Gdx.app.log(TAG, "TrunkRect " + game.trunkRect);
		{
			{
				// pot
				final PolygonShape potShape = new PolygonShape();
				final Vector2[] vertices = { new Vector2(-width * 0.8f, posY), new Vector2(width * 0.8f, posY),
						new Vector2(width * 0.9f, width), new Vector2(width * -0.9f, width) };
				potShape.set(vertices);
				final FixtureDef fixDef = new FixtureDef();
				fixDef.shape = potShape;
				fixDef.density = 1f;
				fixDef.filter.maskBits = POT_MASKBITS;
				fixDef.filter.categoryBits = POT_CATEGORYBITS;
				final BodyDef bd = new BodyDef();
				bd.type = BodyType.DynamicBody;
				bd.position.set(posX, posY);
				potBody = world.createBody(bd);
				final Fixture potFix = potBody.createFixture(fixDef);
				potFix.setDensity(1f);
				potFix.getFilterData().categoryBits = POT_CATEGORYBITS;
				potFix.getFilterData().maskBits = POT_MASKBITS;
				// potBody.setUserData(potSprite);
				potFix.setUserData(potSprite);
				potShape.dispose();
			}

			{
				// trunk
				final BodyDef bd = new BodyDef();
				bd.type = BodyType.DynamicBody;
				bd.position.set(game.trunkRect.x, game.trunkRect.y);
				final PolygonShape trunkShape = new PolygonShape();
				trunkShape.setAsBox(game.trunkRect.width / 2f, game.trunkRect.height / 2f);
				final FixtureDef fixDef = new FixtureDef();
				fixDef.shape = trunkShape;
				fixDef.density = 1f;
				fixDef.filter.maskBits = TRUNK_MASKBITS;
				fixDef.filter.categoryBits = TRUNK_CATEGORYBITS;
				trunkBody = world.createBody(bd);
				final Fixture trunkFix = trunkBody.createFixture(fixDef);
				trunkFix.setDensity(1f);
				trunkFix.getFilterData().categoryBits = TRUNK_CATEGORYBITS;
				trunkFix.getFilterData().maskBits = TRUNK_MASKBITS;
				// trunkBody.setUserData(trunkSprite);
				trunkFix.setUserData(trunkSprite);
				trunkShape.dispose();
			}

			final WeldJointDef jointDef = new WeldJointDef();
			jointDef.bodyA = potBody;
			jointDef.bodyB = trunkBody;
			jointDef.localAnchorA.set(0, 0);
			jointDef.localAnchorB.set(0, -game.trunkRect.height / 2f - width);

			final Joint j1 = world.createJoint(jointDef);
		}

		final Vector2 leafPos = new Vector2(posX, posY).add(-width / 2f, width * 1.25f);
		// create leaf bodies
		final Array<Array<Body>> bodies = new Array<Array<Body>>();
		final float leafRadius = game.leafRadius;// 6f / 40f;
		{
			final FixtureDef fixDef = new FixtureDef();
			circleShape.setRadius(leafRadius);
			fixDef.shape = circleShape;
			fixDef.density = 0.000001f;
			fixDef.friction = 0.2f;
			fixDef.restitution = 0.2f;
			fixDef.filter.maskBits = LEAF_MASKBITS;
			fixDef.filter.categoryBits = LEAF_CATEGORYBITS;

			for (int h = 0; leafRadius * h < height; h++) {
				bodies.add(new Array<Body>());
				for (int w = 0; leafRadius * w < width; w++) {
					final Vector2 pos = new Vector2(leafPos.x - width / 2f + leafRadius * 2 * w + leafRadius / 2f,
							leafPos.y + leafRadius * 2 * h);
					def.position.set(pos);
					final Body leafBody = world.createBody(def);
					final Fixture leafFix = leafBody.createFixture(fixDef);
					leafFix.setUserData(RandomUtil.random(leafsSprite));
					leafBody.setUserData(new Leaf(w, h));
					bodies.get(h).add(leafBody);
				}
			}
			Gdx.app.log(TAG, bodies.size + " bodies high; " + bodies.first().size + " bodies wide");
			game.setHedgeMatrix(bodies);
		}

		{ // create joints
			// final WeldJointDef jointDef = new WeldJointDef();
			final RevoluteJointDef jointDef = new RevoluteJointDef();
			jointDef.referenceAngle = 0f;
			for (int h = 0; leafRadius * h < height; h++) {
				for (int w = 0; leafRadius * w < width; w++) {
					final Body body = bodies.get(h).get(w);
					Joint j = null;
					final Connection connection = new Connection();
					if (bodies.get(h).size > w + 1) {
						// joint right
						// jointDef.initialize(body, bodies.get(h).get(w + 1), body.getWorldCenter().cpy().add(leafRadius, 0));
						// jointDef.frequencyHz = 0f;
						jointDef.collideConnected = true;
						jointDef.bodyA = body;
						jointDef.bodyB = bodies.get(h).get(w + 1);
						jointDef.localAnchorA.set(leafRadius, 0);
						jointDef.localAnchorB.set(-leafRadius, 0);
						// jointDef.enableLimit = true;
						j = world.createJoint(jointDef);
						connection.add(j);
					}
					if (bodies.size > h + 1) {
						// joint up
						// jointDef.initialize(body, bodies.get(h + 1).get(w), body.getWorldCenter().cpy().add(0, leafRadius));
						// jointDef.frequencyHz = 0f;
						jointDef.collideConnected = true;
						jointDef.bodyA = body;
						jointDef.bodyB = bodies.get(h + 1).get(w);
						jointDef.localAnchorA.set(0, leafRadius);
						jointDef.localAnchorB.set(0, -leafRadius);
						// jointDef.enableLimit = true;
						j = world.createJoint(jointDef);
						connection.add(j);
					}

					if (game.trunkRect.contains(body.getWorldCenter())) {
						// create joint with trunk
						// Gdx.app.log(TAG, "Welding to anchor " + body.getWorldCenter());
						final WeldJointDef jDef = new WeldJointDef();
						jDef.initialize(potBody, body, body.getWorldCenter());
						// jDef.bodyA = body;
						// jDef.bodyB = trunkBody;
						// jDef.localAnchorA.set(0, 0);
						// jDef.localAnchorB.set(0, 0);
						j = world.createJoint(jDef);
						connection.add(j);
					}
				}
			}
		}
		return bush;
	}
}
