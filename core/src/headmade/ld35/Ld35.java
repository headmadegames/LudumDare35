package headmade.ld35;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;

import net.dermetfan.gdx.graphics.g2d.Box2DSprite;

public class Ld35 extends ApplicationAdapter {
	private static final String	TAG				= Ld35.class.getName();

	public final static int		VELOCITY_ITERS	= 8;
	public final static int		POSITION_ITERS	= 6;
	public final static int		MAX_FPS			= 60;
	public final static int		MIN_FPS			= 15;
	public final static float	MAX_STEPS		= 1f + MAX_FPS / MIN_FPS;
	public final static float	TIME_STEP		= 1f / MAX_FPS;
	public final static float	UNIT_SCALE		= 1f / 40f;

	SpriteBatch					batch;
	PolygonSpriteBatch			polyBatch;
	ShapeRenderer				shapeRenderer;
	Box2DDebugRenderer			box2dRenderer;
	OrthographicCamera			cam;
	OrthographicCamera			camPix;
	World						world;
	BitmapFont					font;
	PhysicsFactory				phyFac;

	public boolean				debugEnabled	= true;

	private ParticleEffect		cutLeafEffect;
	private boolean				cutHedge		= false;
	private boolean				shearsOpen		= false;
	private Vector2				shearsPosition	= new Vector2();
	private Vector2				shearsTarget	= new Vector2();

	private TextureRegion		txShearsOpen;
	private TextureRegion		txShearsClosed;

	@Override
	public void create() {
		Gdx.input.setInputProcessor(new Ld35InputProcessor(this));

		batch = new SpriteBatch();
		polyBatch = new PolygonSpriteBatch();
		box2dRenderer = new Box2DDebugRenderer();
		shapeRenderer = new ShapeRenderer();

		Assets.instance.init();
		Assets.instance.loadAll();
		Assets.assetsManager.finishLoading();
		Assets.instance.onFinishLoading();

		cutLeafEffect = new ParticleEffect();
		cutLeafEffect.load(Gdx.files.internal("particles/cutleafs.fx"), Assets.instance.atlas);
		cutLeafEffect.scaleEffect(UNIT_SCALE);

		txShearsOpen = Assets.instance.skin.get(Assets.txShearsOpen, TextureRegion.class);
		txShearsClosed = Assets.instance.skin.get(Assets.txShearsClosed, TextureRegion.class);

		// Box2DSprite.setZComparator(new Comparator<Box2DSprite>() {
		// @Override
		// public int compare(Box2DSprite o1, Box2DSprite o2) {
		// if (o1 != null) {
		// if (o2 != null) {
		// return o1.getZIndex() > o2.getZIndex() ? 1 : -1;
		// }
		// return 1;
		// }
		// return 0;
		// }
		// });
		// txPot = Assets.instance.skin.get(Assets.txPot, TextureRegion.class);
		// txLeafs = Assets.instance.skin.get(Assets.txLeafs, TextureRegion.class);
		// txTrunk = Assets.instance.skin.get(Assets.txTrunk, TextureRegion.class);

		reset();
	}

	private void reset() {
		cam = new OrthographicCamera(Gdx.graphics.getWidth() * UNIT_SCALE, Gdx.graphics.getHeight() * UNIT_SCALE);
		cam.translate(0, Gdx.graphics.getHeight() * UNIT_SCALE * 0.5f);
		// cam.zoom = 0.5f;
		cam.update();
		Gdx.app.log(TAG, "Gdx.graphics.getWidth() " + Gdx.graphics.getWidth());
		Gdx.app.log(TAG, "Gdx.graphics.getHeight() " + Gdx.graphics.getHeight());
		camPix = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camPix.translate(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
		camPix.update();

		if (world != null) {
			world.dispose();
		}
		world = new World(new Vector2(0, -10f), false);

		if (phyFac != null) {
			phyFac.dispose();
		}
		phyFac = new PhysicsFactory(world);
		phyFac.createFloor();
		phyFac.createBush(0f, 0f, UNIT_SCALE * 80f, UNIT_SCALE * 160f);
	}

	@Override
	public void render() {
		final float delta = Gdx.graphics.getDeltaTime();
		update();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		{
			batch.setProjectionMatrix(cam.combined);
			batch.begin();
			Box2DSprite.draw(batch, world);
			batch.end();
		}

		{ // camPix Stuff
			batch.setProjectionMatrix(camPix.combined);
			batch.begin();

			if (shearsOpen) {
				batch.draw(txShearsOpen, shearsPosition.x - 10, camPix.viewportHeight - shearsPosition.y - 10,
						txShearsOpen.getRegionWidth() / 2f, txShearsOpen.getRegionHeight() / 2f, txShearsOpen.getRegionWidth(),
						txShearsOpen.getRegionHeight(), 2f, 2f, -shearsTarget.cpy().sub(shearsPosition).angle() - 45);
				Gdx.app.log(TAG, "shearsPosition " + shearsPosition + " - shearsTarget " + shearsTarget + " - Shears angle "
						+ shearsTarget.cpy().sub(shearsPosition).angle());
			} else {

				batch.draw(txShearsClosed, shearsPosition.x - 10, camPix.viewportHeight - shearsPosition.y - 10,
						txShearsOpen.getRegionWidth() / 2f, txShearsOpen.getRegionHeight() / 2f, txShearsOpen.getRegionWidth(),
						txShearsOpen.getRegionHeight(), 2f, 2f, 0);
			}

			if (cutHedge) {
				Gdx.app.log(TAG, "Draw particle effect");
				cutHedge = false;
				cutLeafEffect.setPosition(shearsPosition.x, shearsPosition.y);
				cutLeafEffect.reset();
				cutLeafEffect.start();
			}
			cutLeafEffect.draw(batch, delta);

			batch.end();
		}

		if (debugEnabled) {
			box2dRenderer.render(world, cam.combined);
		}
	}

	private void update() {
		world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
	}

	@Override
	public void dispose() {
		super.dispose();
		Assets.instance.dispose();
		batch.dispose();
		box2dRenderer.dispose();
		shapeRenderer.dispose();
		polyBatch.dispose();
		phyFac.dispose();
		cutLeafEffect.dispose();
	}

	public boolean isCutHedge() {
		return cutHedge;
	}

	public void setCutHedge(boolean cutHedge) {
		this.cutHedge = cutHedge;
	}

	public boolean isShearsOpen() {
		return shearsOpen;
	}

	public void setShearsOpen(boolean shearsOpen) {
		if (this.shearsOpen && !shearsOpen) {
			Assets.instance.playSound(Assets.sndCut, 0.5f);
			setCutHedge(true);
		} else {
			Assets.instance.playSound(Assets.sndOpen, 0.5f);
		}
		this.shearsOpen = shearsOpen;
	}

	public Vector2 getShearsPosition() {
		return shearsPosition;
	}

	public void setShearsPosition(Vector2 shearsPosition) {
		this.shearsPosition = shearsPosition;
	}

	public Vector2 getShearsTarget() {
		return shearsTarget;
	}

	public void setShearsTarget(Vector2 shearsTarget) {
		this.shearsTarget = shearsTarget;
	}
}
