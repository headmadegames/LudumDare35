package headmade.ld35;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;

public class Ld35 extends ApplicationAdapter {
	private static final String	TAG				= Ld35.class.getName();

	public final static int		VELOCITY_ITERS	= 6;
	public final static int		POSITION_ITERS	= 4;
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

		reset();
	}

	private void reset() {
		cam = new OrthographicCamera(Gdx.graphics.getWidth() * UNIT_SCALE, Gdx.graphics.getHeight() * UNIT_SCALE);
		cam.translate(0, Gdx.graphics.getHeight() * UNIT_SCALE * 0.4f);
		// cam.zoom = 0.5f;
		cam.update();
		camPix = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		if (world != null) {
			world.dispose();
		}
		world = new World(new Vector2(0, -10f), false);

		if (phyFac != null) {
			phyFac.dispose();
		}
		phyFac = new PhysicsFactory(world);
		phyFac.createFloor();
		phyFac.createBush(0f, 0f, 1.25f, 2.25f);
	}

	@Override
	public void render() {
		update();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		{
			batch.begin();
			batch.end();
		}

		box2dRenderer.render(world, cam.combined);
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
	}
}
