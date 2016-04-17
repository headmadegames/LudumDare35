package headmade.ld35;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import net.dermetfan.gdx.graphics.g2d.Box2DSprite;
import net.dermetfan.gdx.utils.ArrayUtils;

public class Ld35 extends ApplicationAdapter {
	private static final String	TAG				= Ld35.class.getName();

	public final static int		VELOCITY_ITERS	= 3;
	public final static int		POSITION_ITERS	= 2;
	public final static int		MAX_FPS			= 60;
	public final static int		MIN_FPS			= 15;
	public final static float	MAX_STEPS		= 1f + MAX_FPS / MIN_FPS;
	public final static float	TIME_STEP		= 1f / MAX_FPS;
	public static float			UNIT_SCALE		= 10f / 400f;

	public final static int		STATE_LOGO		= 1;
	public final static int		STATE_INTRO		= 2;
	public final static int		STATE_GAME		= 3;
	public final static int		STATE_RESULT	= 4;
	public final static int		STATE_GAMEOVER	= 5;

	int							currentState	= STATE_LOGO;
	// int currentState = STATE_GAME;
	SpriteBatch					batch;
	PolygonSpriteBatch			polyBatch;
	ShapeRenderer				shapeRenderer;
	Box2DDebugRenderer			box2dRenderer;
	OrthographicCamera			cam;
	OrthographicCamera			camPix;
	World						world;
	BitmapFont					font;
	PhysicsFactory				phyFac;
	ShapeJson					mission;

	public boolean				debugEnabled	= false;
	public Rectangle			trunkRect;
	public Float				satisfaction	= null;
	public float				leafRadius		= 7f / 40f;
	public boolean				showDesc		= false;
	public boolean				gameOver		= false;

	private ParticleEffect		cutLeafEffect;
	private ParticleEffectPool	cutLeafEffectPool;
	private Array<PooledEffect>	effects			= new Array<PooledEffect>();

	private boolean				cutHedge		= false;
	private boolean				shearsOpen		= false;
	private Vector2				shearsPosition	= new Vector2();
	private Vector2				shearsTarget	= new Vector2();
	private Vector3				cutVec			= new Vector3();
	private Vector3				camTargetPos;
	private Vector2				bgPos			= new Vector2(0, 0);
	private Vector2				bgTargetPos		= new Vector2(0, 0);

	private TextureRegion		txShearsOpen;
	private TextureRegion		txShearsClosed;
	private TextureRegion		txBg;
	private Texture				txBluePrint;

	private int[][]				hedgeMatrix;
	private int					shapeIndex		= 0;
	private int					compareResult;

	@Override
	public void create() {
		Gdx.input.setInputProcessor(new Ld35InputProcessor(this));

		batch = new SpriteBatch();
		polyBatch = new PolygonSpriteBatch();
		box2dRenderer = new Box2DDebugRenderer();
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setAutoShapeType(true);

		Assets.instance.init();
		Assets.instance.loadAll();
		Assets.assetsManager.finishLoading();
		Assets.instance.onFinishLoading();

		final Music music = Assets.assetsManager.get(Assets.music, Music.class);
		music.setLooping(true);
		music.setVolume(0.1f);
		music.play();

		cutLeafEffect = new ParticleEffect();
		cutLeafEffect.load(Gdx.files.internal("particles/cutleafs.fx"), Assets.instance.atlas);
		cutLeafEffect.scaleEffect(UNIT_SCALE);
		cutLeafEffectPool = new ParticleEffectPool(cutLeafEffect, 5, 30);

		txShearsOpen = Assets.instance.skin.get(Assets.txShearsOpen, TextureRegion.class);
		txShearsClosed = Assets.instance.skin.get(Assets.txShearsClosed, TextureRegion.class);
		txBg = Assets.instance.skin.get(Assets.txBg, TextureRegion.class);
		font = Assets.instance.skin.getFont("default-font");

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

		cam = new OrthographicCamera(Gdx.graphics.getWidth() * UNIT_SCALE, Gdx.graphics.getHeight() * UNIT_SCALE);
		// cam.translate(0, Gdx.graphics.getHeight() * UNIT_SCALE * 0.6f);
		// cam.zoom = 0.5f;
		// cam.update();
		Gdx.app.log(TAG, "Gdx.graphics.getWidth() " + Gdx.graphics.getWidth());
		Gdx.app.log(TAG, "Gdx.graphics.getHeight() " + Gdx.graphics.getHeight());
		camPix = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		// camPix.translate(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
		// camPix.update();

		changeMission();
	}

	private void changeMission() {
		if (shapeIndex < Assets.shapes.length) {
			reset();
			mission = Assets.instance.assetsManager.get(Assets.shapes[shapeIndex], ShapeJson.class);
			Gdx.app.log(TAG, "Mission: " + mission);

			final int width = Math.round(camPix.viewportWidth * 0.4f);
			final int height = Math.round(camPix.viewportHeight * 0.8f);
			final int blockWidth = height / mission.shape.length;
			// batch.setColor(Color.GRAY);
			final Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
			pixmap.setColor(0.75f, 0.75f, 0.75f, 0.75f);
			for (int y = mission.shape.length - 1; y >= 0; y--) {
				for (int x = 0; x < mission.shape[y].length; x++) {
					if (mission.shape[y][x] == 1) {
						pixmap.fillRectangle(x * blockWidth, y * blockWidth, blockWidth, blockWidth);
					} else {
						pixmap.drawRectangle(x * blockWidth, y * blockWidth, blockWidth, blockWidth);
					}
				}
			}
			if (txBluePrint != null) {
				txBluePrint.dispose();
			}
			txBluePrint = new Texture(pixmap);
			pixmap.dispose();
			shapeIndex++;
		} else {
			gameOver();
		}
	}

	private void gameOver() {
		gameOver = true;
		currentState = STATE_GAMEOVER;
	}

	public void reset() {
		cam.position.x = 0;
		cam.position.y = 0;
		cam.translate(Gdx.graphics.getHeight() * UNIT_SCALE * 0.35f, Gdx.graphics.getHeight() * UNIT_SCALE * 0.6f);
		cam.update();

		if (world != null) {
			world.dispose();
		}
		world = new World(new Vector2(0, -10f), true);

		if (phyFac != null) {
			phyFac.dispose();
		}
		phyFac = new PhysicsFactory(world, this);
		phyFac.createFloor();
		phyFac.createBush(0f, 0f, 3f, 4f);

		// Reset all effects:
		for (int i = effects.size - 1; i >= 0; i--) {
			effects.get(i).free(); // free all the effects back to the pool
		}
		effects.clear();
	}

	@Override
	public void render() {
		final float delta = Gdx.graphics.getDeltaTime();
		update();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		{ // BG
			batch.setProjectionMatrix(camPix.combined);
			batch.setColor(Color.WHITE);
			batch.begin();
			batch.draw(txBg, bgPos.x - camPix.viewportWidth / 2, bgPos.y - camPix.viewportHeight / 2, camPix.viewportWidth * 3f,
					camPix.viewportHeight);
			if (mission != null && currentState == STATE_GAME && MathUtils.isEqual(bgPos.x, bgTargetPos.x)) {
				batch.setColor(Color.WHITE);
				if (showDesc) {
					font.draw(batch, mission.desc, camPix.viewportWidth * 0.05f, -30 + camPix.viewportHeight / 2f,
							camPix.viewportWidth * 0.4f, Align.center, true);
				} else {
					batch.draw(txBluePrint, camPix.viewportWidth * 0.075f, -camPix.viewportHeight * 0.4f);
					// font.draw(batch, mission.shapeText(), camPix.viewportWidth * 0.05f, -30 + camPix.viewportHeight / 2f,
					// camPix.viewportWidth * 0.4f, Align.center, true);
				}

			}

			batch.end();
		}

		final boolean drawBush = currentState == STATE_GAME && MathUtils.isEqual(bgPos.x, bgTargetPos.x);
		if (drawBush) { // draw bush

			batch.setProjectionMatrix(cam.combined);
			batch.begin();

			Box2DSprite.draw(batch, world);

			for (int i = effects.size - 1; i >= 0; i--) {
				final PooledEffect effect = effects.get(i);
				effect.draw(batch, delta);
				if (effect.isComplete()) {
					effect.free();
					effects.removeIndex(i);
				}
			}
			batch.end();
		}

		{ // camPix Stuff
			batch.setProjectionMatrix(camPix.combined);
			batch.begin();

			final int shearsOffsetX = -18;
			final int shearsOffsetY = -14;
			if (shearsOpen) {
				batch.draw(txShearsOpen, shearsPosition.x + shearsOffsetX - camPix.viewportWidth / 2f,
						camPix.viewportHeight / 2f - shearsPosition.y + shearsOffsetY, txShearsOpen.getRegionWidth() / 2f,
						txShearsOpen.getRegionHeight() / 2f, txShearsOpen.getRegionWidth(), txShearsOpen.getRegionHeight(), 2f, 2f,
						-shearsTarget.cpy().sub(shearsPosition).angle() - 45);
				// Gdx.app.log(TAG, "shearsPosition " + shearsPosition + " - shearsTarget " + shearsTarget + " - Shears angle "
				// + shearsTarget.cpy().sub(shearsPosition).angle());
			} else {

				batch.draw(txShearsClosed, shearsPosition.x + shearsOffsetX - camPix.viewportWidth / 2f,
						camPix.viewportHeight / 2f - shearsPosition.y + shearsOffsetY, txShearsOpen.getRegionWidth() / 2f,
						txShearsOpen.getRegionHeight() / 2f, txShearsOpen.getRegionWidth(), txShearsOpen.getRegionHeight(), 2f, 2f, 0);
			}

			batch.end();
		}

		{ // cutting line
			if (shearsOpen) {
				shapeRenderer.setProjectionMatrix(camPix.combined);
				shapeRenderer.setColor(Color.RED);
				shapeRenderer.begin();
				shapeRenderer.line(shearsPosition.x - camPix.viewportWidth / 2f, camPix.viewportHeight / 2f - shearsPosition.y, 0,
						Gdx.input.getX() - camPix.viewportWidth / 2f, camPix.viewportHeight / 2f - Gdx.input.getY(), 0);
				shapeRenderer.end();
			}

			// hint text
			batch.setProjectionMatrix(camPix.combined);
			batch.begin();
			if (drawBush) {
				font.draw(batch, "Hit Space when done", -camPix.viewportWidth * 0.45f, -camPix.viewportHeight * 0.4f,
						camPix.viewportWidth * 0.4f, Align.center, true);
			}
			if (currentState == STATE_GAMEOVER) {
				font.draw(batch, "Thanks for playing", -camPix.viewportWidth * 0.3f, 0, camPix.viewportWidth * 0.6f, Align.center, true);
			} else if (currentState == STATE_RESULT) {
				font.draw(batch, "Satisfaction: " + compareResult + "%", -camPix.viewportWidth * 0.3f, 0, camPix.viewportWidth * 0.6f,
						Align.center, true);
			}
			batch.end();
		}

		if (debugEnabled) {
			box2dRenderer.render(world, cam.combined);
		}
	}

	private void update() {
		world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);

		if (!MathUtils.isEqual(bgPos.x, bgTargetPos.x)) {
			if (bgPos.x > bgTargetPos.x) {
				bgPos.x -= 10;
			} else {
				bgPos.x += 10;
			}
		}
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
		txBluePrint.dispose();
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

	public void leafEffect(Vector2 worldCenter) {
		final PooledEffect effect = cutLeafEffectPool.obtain();
		effect.setPosition(worldCenter.x, worldCenter.y);
		effects.add(effect);
	}

	public void cutHedgeMatrix(Leaf leaf) {
		hedgeMatrix[leaf.y][leaf.x] = 0;
		Gdx.app.log(TAG, "HedgeMatrix updated " + leaf);
		// isConnectedToTrunk(leaf);
		for (int y = hedgeMatrix.length - 1; y >= 0; y--) {
			Gdx.app.log(TAG, ArrayUtils.toString(hedgeMatrix[y]));
		}
	}

	public void checkTrunkConnections() {
		for (int y = 0; y < 17; y++) {
			for (int x = 7; x < 11; x++) {
				if (hedgeMatrix[y][x] != 0) {
					hedgeMatrix[y][x] = 3;
				}
			}
		}
		for (int x = 6; x < 12; x++) {
			isConnectedToTrunk(x, 17);
		}
		for (int y = 0; y < 17; y++) {
			isConnectedToTrunk(6, y);
		}
		for (int y = 0; y < 17; y++) {
			isConnectedToTrunk(11, y);
		}
		setVisitedLeafs(3);

		for (int y = 0; y < hedgeMatrix.length; y++) {
			for (int x = 0; x < hedgeMatrix[y].length; x++) {
				if (hedgeMatrix[y][x] == 1) {
					hedgeMatrix[y][x] = 0;
				} else if (hedgeMatrix[y][x] > 1) {
					hedgeMatrix[y][x] = 1;
				}
			}
		}
		for (int y = hedgeMatrix.length - 1; y >= 0; y--) {
			Gdx.app.log(TAG, ArrayUtils.toString(hedgeMatrix[y]));
		}
	}

	private boolean isConnectedToTrunk(Leaf leaf) {
		boolean connected = false;
		if (!isConnectedToTrunk(leaf.x, leaf.y - 1)) {
			setVisitedLeafs(0);
		} else {
			setVisitedLeafs(3);
			connected = true;
		}
		if (!isConnectedToTrunk(leaf.x + 1, leaf.y)) {
			setVisitedLeafs(0);
		} else {
			setVisitedLeafs(3);
			connected = true;
		}
		if (!isConnectedToTrunk(leaf.x - 1, leaf.y)) {
			setVisitedLeafs(0);
		} else {
			setVisitedLeafs(3);
			connected = true;
		}
		if (!isConnectedToTrunk(leaf.x, leaf.y + 1)) {
			setVisitedLeafs(0);
		} else {
			setVisitedLeafs(3);
			connected = true;
		}

		for (int y = 0; y < hedgeMatrix.length; y++) {
			for (int x = 0; x < hedgeMatrix[y].length; x++) {
				if (connected) {
					if (hedgeMatrix[y][x] > 0) {
						hedgeMatrix[y][x] = 1;
					}
				} else {
					hedgeMatrix[y][x] = 0;
				}
			}
		}
		return connected;
	}

	private void setVisitedLeafs(int value) {
		for (int y = 0; y < hedgeMatrix.length; y++) {
			for (int x = 0; x < hedgeMatrix[y].length; x++) {
				if (hedgeMatrix[y][x] == 2) {
					hedgeMatrix[y][x] = value;
				}
			}
		}
	}

	private boolean isConnectedToTrunk(final int x, final int y) {
		// Gdx.app.log(TAG, "Checking if connected to trunk (" + x + ", " + y + ")");
		if (x < 0 || x >= hedgeMatrix[0].length || y < 0 || y >= hedgeMatrix.length) {
			// Gdx.app.log(TAG, "Out of bounds (" + x + ", " + y + ")");
			return false;
		} else if (hedgeMatrix[y][x] == 0) {
			// Gdx.app.log(TAG, "Not connected to trunk (" + x + ", " + y + ")");
			return false;
		} else if (hedgeMatrix[y][x] == 3) {
			// Gdx.app.log(TAG, "Connected via connected (" + x + ", " + y + ")");
			return true;
		} else if (hedgeMatrix[y][x] == 2) {
			// Gdx.app.log(TAG, "Already checked (" + x + ", " + y + ")");
			return false;
		} else {
			hedgeMatrix[y][x] = 2;
			if (isTrunk(x, y) && hedgeMatrix[y][x] > 0) {
				// Gdx.app.log(TAG, "Is intact trunk (" + x + ", " + y + ")");
				return true;
			}
			boolean connected = false;
			connected = isConnectedToTrunk(x, y - 1) || connected;
			connected = isConnectedToTrunk(x - 1, y) || connected;
			connected = isConnectedToTrunk(x + 1, y) || connected;
			connected = isConnectedToTrunk(x, y + 1) || connected;
			if (connected) {
				hedgeMatrix[y][x] = 3;
				// Gdx.app.log(TAG, "Connected to trunk (" + x + ", " + y + ")");
				return true;
			}
			// Gdx.app.log(TAG, "Not connected to trunk (" + x + ", " + y + ")");
			// hedgeMatrix[y][x] = 0;
			return false;
		}
	}

	// private boolean isConnectedToTrunk(final int x, final int y) {
	// Gdx.app.log(TAG, "Checking if connected to trunk (" + x + ", " + y + ")");
	// if (x < 0 || x >= hedgeMatrix[0].length || y < 0 || y >= hedgeMatrix.length) {
	// Gdx.app.log(TAG, "Out of bounds (" + x + ", " + y + ")");
	// return false;
	// } else if (hedgeMatrix[y][x] == 0) {
	// Gdx.app.log(TAG, "Not connected to trunk (" + x + ", " + y + ")");
	// return false;
	// } else if (hedgeMatrix[y][x] == 3) {
	// Gdx.app.log(TAG, "Is connected via connected (" + x + ", " + y + ")");
	// return true;
	// } else if (hedgeMatrix[y][x] == 2) {
	// Gdx.app.log(TAG, "Already checked (" + x + ", " + y + ")");
	// return false;
	// } else {
	// hedgeMatrix[y][x] = 2;
	// if (isTrunk(x, y) && hedgeMatrix[y][x] > 0) {
	// Gdx.app.log(TAG, "Is intact trunk (" + x + ", " + y + ")");
	// return true;
	// }
	// if (isConnectedToTrunk(x, y - 1) || isConnectedToTrunk(x - 1, y) || isConnectedToTrunk(x + 1, y)
	// || isConnectedToTrunk(x, y + 1)) {
	// hedgeMatrix[y][x] = 3;
	// Gdx.app.log(TAG, "Connected to trunk (" + x + ", " + y + ")");
	// return true;
	// }
	// Gdx.app.log(TAG, "Not connected to trunk (" + x + ", " + y + ")");
	// hedgeMatrix[y][x] = 0;
	// return false;
	// }
	// }

	private boolean isTrunk(int x, int y) {
		if ((x >= 7 && x <= 10) && (y <= 16)) {
			return true;
		} else {
			return false;
		}
	}

	public void setHedgeMatrix(Array<Array<Body>> bodies) {
		hedgeMatrix = new int[bodies.size][bodies.first().size];
		for (int y = 0; y < bodies.size; y++) {
			final Array<Body> row = bodies.get(y);
			for (int x = 0; x < row.size; x++) {
				hedgeMatrix[y][x] = 1;
			}
		}
	}

	public void setCutVec(Vector3 cutVec) {
		this.cutVec = cutVec;
	}

	public void incState() {
		if (currentState < STATE_GAME) {
			currentState++;
			bgTargetPos.x -= 800;
			// camTargetPos =
		} else {
			if (currentState != STATE_GAME) {
				currentState = STATE_GAME;
				changeMission();
			}
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);

		UNIT_SCALE = 10f / (new Float(width) / 2f);
		cam.viewportWidth = width * UNIT_SCALE;
		cam.viewportHeight = height * UNIT_SCALE;
		cam.update();

		camPix.viewportWidth = width;
		camPix.viewportHeight = height;
		camPix.update();
	}

	public void finishHedge() {
		compareResult = compare(mission.shape, hedgeMatrix);
		currentState = STATE_RESULT;
	}

	private int compare(int[][] shape, int[][] hedgeMatrix2) {
		final float totalCount = (hedgeMatrix2.length - 2) * (hedgeMatrix2[0].length - 2);
		float maxHitCount = 0;
		for (int yShift = -1; yShift < 2; yShift++) {
			for (int xShift = -1; xShift < 2; xShift++) {
				float hitCount = 0;
				for (int y = 1; y < hedgeMatrix2.length - 1; y++) {
					for (int x = 1; x < hedgeMatrix2[y].length - 1; x++) {
						if (hedgeMatrix2[y][x] == shape[y][x]) {
							hitCount++;
						}
					}
				}
				maxHitCount = Math.max(hitCount, maxHitCount);
			}
		}

		int hedgeZeroCount = 0;
		int shapeZeroCount = 0;
		int hedgeOneCount = 0;
		int shapeOneCount = 0;
		for (int y = 0; y < hedgeMatrix2.length; y++) {
			for (int x = 0; x < hedgeMatrix2[y].length; x++) {
				if (shape[y][x] == 0) {
					shapeZeroCount++;
				} else {
					shapeOneCount++;
				}
				if (hedgeMatrix2[y][x] == 0) {
					hedgeZeroCount++;
				} else {
					hedgeOneCount++;
				}
			}
		}

		for (int y = shape.length - 1; y >= 0; y--) {
			Gdx.app.log(TAG, ArrayUtils.toString(shape[y]));
		}
		Gdx.app.log(TAG, "hedgeZeroCount: " + hedgeZeroCount + " shapeZeroCount: " + shapeZeroCount);
		Gdx.app.log(TAG, "hedgeOneCount: " + hedgeOneCount + " shapeOneCount: " + shapeOneCount);
		Gdx.app.log(TAG, "Result: " + maxHitCount + "/" + totalCount + "=" + maxHitCount / totalCount);
		final float result = 20f / Math.abs(hedgeOneCount - shapeOneCount);
		Gdx.app.log(TAG, "Result: " + result);
		return Math.round(MathUtils.clamp(result, 0f, 1f) * 100f);
	}
}
