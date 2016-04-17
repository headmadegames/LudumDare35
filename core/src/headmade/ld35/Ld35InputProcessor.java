package headmade.ld35;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.utils.Array;

import net.dermetfan.gdx.physics.box2d.Box2DUtils;

public class Ld35InputProcessor implements InputProcessor {
	private static final String	TAG				= Ld35InputProcessor.class.getName();

	public static boolean		mouseDown		= false;

	private Ld35				game;
	private Vector3				touchDownPoint;

	private float				cuttingLength	= 30f * Ld35.UNIT_SCALE;

	private Vector2				cutVec;

	public Ld35InputProcessor(Ld35 ld35) {
		this.game = ld35;
	}

	@Override
	public boolean keyDown(int keycode) {
		if (game.debugEnabled) {
			if (keycode == Keys.LEFT || keycode == Keys.A) {
				game.cam.translate(-1f, 0f);
				game.cam.update();
				return true;
			} else if (keycode == Keys.RIGHT || keycode == Keys.D) {
				game.cam.translate(1f, 0f);
				game.cam.update();
				return true;
			} else if (keycode == Keys.UP || keycode == Keys.W) {
				game.cam.translate(0f, 1f);
				game.cam.update();
				return true;
			} else if (keycode == Keys.DOWN || keycode == Keys.S) {
				game.cam.translate(0f, -1f);
				game.cam.update();
				return true;
			} else if (keycode == Keys.R) {
				if (game.currentState == game.STATE_GAME) {
					game.reset();
				}
				return true;
			}
		}
		if (keycode == Keys.F12) {
			game.debugEnabled = !game.debugEnabled;
			return true;
		} else if (keycode == Keys.SPACE) {
			game.checkTrunkConnections();
			game.finishHedge();
			return true;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (screenX > game.camPix.viewportWidth / 2f) {
			game.showDesc = !game.showDesc;
		}

		touchDownPoint = game.cam.unproject(new Vector3(screenX, screenY, 0));
		Gdx.app.log(TAG, "Mouse clicked at " + touchDownPoint);

		mouseDown = true;
		game.setShearsOpen(true);
		game.getShearsPosition().x = screenX;
		game.getShearsPosition().y = screenY;
		Gdx.app.log(TAG, "Shears Position " + game.getShearsPosition());

		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		final Vector3 touchUpPoint = game.cam.unproject(new Vector3(screenX, screenY, 0));
		mouseDown = false;

		game.setShearsOpen(false);
		final Vector2 touchDown = new Vector2(touchDownPoint.x, touchDownPoint.y);
		if (game.currentState == game.STATE_GAME && touchDownPoint != null) {
			if (touchUpPoint.dst2(touchDownPoint) > 0.1f) {
				game.getShearsTarget().x = screenX;
				game.getShearsTarget().y = screenY;

				final Vector2 touchUp = new Vector2(touchUpPoint.x, touchUpPoint.y);
				// cutVec = touchUp.cpy().sub(touchDown).nor().scl(cuttingLength);
				cutVec = touchUp.cpy().sub(touchDown);
				game.setCutVec(game.cam.project(new Vector3(cutVec.x, cutVec.y, 0f)));
			}
			if (cutVec != null) {

				final RayCastCallback callback = new RayCastCallback() {
					@Override
					public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
						final Body body = fixture.getBody();
						if (body.getUserData() != null && body.getUserData() instanceof Leaf) {
							final Array<JointEdge> joints = fixture.getBody().getJointList();
							for (final JointEdge jointEdge : joints) {
								game.world.destroyJoint(jointEdge.joint);
								jointEdge.other.applyForce(0, 0.00001f, touchUpPoint.x, touchUpPoint.y, true);
							}

							// destroy leafs
							game.cutHedgeMatrix(((Leaf) body.getUserData()));
							game.leafEffect(body.getWorldCenter());
							Box2DUtils.destroyFixtures(body);
							game.world.destroyBody(body);
						}
						return 1;
					}
				};
				// Gdx.app.log(TAG, "Cut Vec " + cutVec + " length " cutVec.len());
				game.world.rayCast(callback, touchDown, cutVec.cpy().add(touchDown));
			}
		}
		game.incState();

		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		game.getShearsTarget().x = screenX;
		game.getShearsTarget().y = screenY;
		// Gdx.app.log(TAG, "Shears Target " + game.getShearsTarget());
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		if (!mouseDown) {
			// final Vector3 mousePos = game.cam.unproject(new Vector3(screenX, screenY, 0));
			game.getShearsPosition().x = screenX;
			game.getShearsPosition().y = screenY;
		}
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		if (game.debugEnabled) {
			game.cam.zoom += amount * 0.5f;
			game.cam.zoom = MathUtils.clamp(game.cam.zoom, 0.5f, 50f);
			game.cam.update();
			Gdx.app.log(TAG, "new zoom " + game.cam.zoom);
		}
		return false;
	}

}
