package headmade.ld35;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

public class ShapeJsonAssetLoader extends SynchronousAssetLoader<ShapeJson, AssetLoaderParameters<ShapeJson>> {

	private static final String TAG = ShapeJsonAssetLoader.class.getName();

	public ShapeJsonAssetLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, AssetLoaderParameters<ShapeJson> parameter) {
		return null;
	}

	@Override
	public ShapeJson load(AssetManager assetManager, String fileName, FileHandle file, AssetLoaderParameters<ShapeJson> parameter) {
		try {
			final Json json = new Json();
			return json.fromJson(ShapeJson.class, file);
		} catch (final Exception e) {
			Gdx.app.error(TAG, "Unable to parse Json for shape " + file.file().getAbsolutePath(), e);
		}
		return null;
	}

}
