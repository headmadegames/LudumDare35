package headmade.ld35;

import java.io.File;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetErrorListener;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SkinLoader;
import com.badlogic.gdx.assets.loaders.SkinLoader.SkinParameter;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;

//import com.kotcrab.vis.ui.VisUI;

public class Assets implements Disposable, AssetErrorListener {

	private static final String			TAG				= Assets.class.getName();

	public static final String			HEADMADE_LOGO	= "headmade_large.png";
	public static final String			PACKS_BASE		= "packs/";											// +
																											// File.separator;
	public static final String			PACK			= "pack";
	public static final String			GAME_ATLAS		= PACKS_BASE + PACK + ".atlas";
	public static final String			RAW_ASSETS		= "assets-raw/images";

	public static final SkinParameter	skinParameter	= new SkinLoader.SkinParameter(Assets.GAME_ATLAS);
	public static final String			skinPath		= Assets.PACKS_BASE + Assets.PACK + ".json";
	public static final String			music			= "music/bg.ogg";
	public static final String			sndCash			= "audio/cash.wav";
	public static final String			sndOpen			= "audio/open.wav";
	public static final String			sndCut			= "audio/cut.wav";
	public static final String			txLeaf			= "leaf";
	public static final String[]		txLeafs			= { "leafs", "leafs2", "leafs3", "leafs4" };
	public static final String			txTrunk			= "trunk";
	public static final String			txPot			= "pot";
	public static final String			txLogo			= "logo";
	public static final String			txShearsClosed	= "shearsClosed";
	public static final String			txShearsOpen	= "shearsOpen";
	public static final String			txLeafsWhite	= "leafswhite";
	public static final String			txBg			= "bg";
	public static final String			txBlueprint		= "blueprint";
	// public static final String[] txLeafs = { "leaf01", "leaf02", "leaf03", "leaf04", "leaf05" };
	// public static final String[] txBranches = { "branch01", "branch02", "branch03", "branch04", "branch05" };
	public static final String			shapeDollar		= "shapes/dollar.json";

	public static final Assets			instance		= new Assets();										// Singleton

	public static AssetManager			assetsManager;

	public TextureAtlas					atlas;																// Don't make
																											// this
																											// static!!!
	public Skin							skin;

	// singleton: prevent instantiation from other classes
	private Assets() {
		Gdx.app.log(TAG, "File.seperator is " + File.separator);

		assetsManager = new AssetManager(new FileHandleResolver() {

			@Override
			public FileHandle resolve(String fileName) {
				if (fileName.contains("assets-raw")) {
					final String path = fileName.substring(fileName.lastIndexOf(File.separator), fileName.lastIndexOf('.'));
					return Gdx.files.internal(path);
				}
				return Gdx.files.internal(fileName);
			}
		});
		// set asset manager error handler
		assetsManager.setErrorListener(this);
		assetsManager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
	}

	@Override
	public void dispose() {
		Gdx.app.debug(TAG, "Disposing assets...");
		assetsManager.dispose();
	}

	@Override
	public void error(AssetDescriptor asset, Throwable throwable) {
		Gdx.app.error(TAG, "Error in Assets", throwable);
	}

	/**
	 * Loads minimal stuff
	 */
	public void init() {
		Gdx.app.debug(TAG, "Init minimal assets...");

		assetsManager.setLoader(ShapeJson.class, new ShapeJsonAssetLoader(new InternalFileHandleResolver()));
		assetsManager.load(HEADMADE_LOGO, Texture.class);

		assetsManager.finishLoading();
	}

	/**
	 * Load all assets using the {@link AssetManager#load}. It blocks until all loading is finished. This method must be called before
	 * accessing any asset.
	 */
	public void loadAll() {
		Gdx.app.debug(TAG, "Init assets...");

		// assetsManager.load(music, Music.class);
		assetsManager.load(sndOpen, Sound.class);
		assetsManager.load(sndCut, Sound.class);
		assetsManager.load(sndCash, Sound.class);

		assetsManager.load(GAME_ATLAS, TextureAtlas.class);
		assetsManager.load(skinPath, Skin.class, skinParameter);

		assetsManager.load(shapeDollar, ShapeJson.class);

	}

	public void onFinishLoading() {
		atlas = assetsManager.get(GAME_ATLAS, TextureAtlas.class);
		skin = assetsManager.get(skinPath, Skin.class);
		setTextureFilter(atlas, TextureFilter.Nearest);
	}

	public void playSound(String name) {
		playSound(name, 0.5f);
	}

	public void playSound(String name, float volume) {
		playSound(name, volume, 1f);
	}

	public void playSound(String name, float volume, float pitch) {
		Gdx.app.log(TAG, "Playing sound " + name + "with valume " + volume);
		final Sound sound = assetsManager.get(name, Sound.class);
		if (sound != null) {
			sound.play(volume * 0.5f, pitch, 0);
		} else {
			Gdx.app.error(TAG, "No Sound with name " + name);
		}
	}

	/**
	 * enable texture filtering for pixel smoothing
	 *
	 * @param atlas
	 * @param typeOfFilter
	 */
	private void setTextureFilter(TextureAtlas atlas, TextureFilter typeOfFilter) {
		Gdx.app.log(TAG, "setting filter for textures " + atlas.getTextures().size);
		for (final Texture t : atlas.getTextures()) {
			t.setFilter(typeOfFilter, typeOfFilter); // min=mag
		}

		final Skin skin = assetsManager.get(skinPath, Skin.class);
		final BitmapFont font = skin.getFont("default-font");
		for (int i = 0; i < font.getRegions().size; i++) {
			font.getRegion(i).getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		}

	}

}
