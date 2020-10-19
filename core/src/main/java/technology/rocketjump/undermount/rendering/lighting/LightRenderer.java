package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.undermount.rendering.custom_libgdx.ShaderLoader;

public class LightRenderer implements Disposable {

	public final ShaderProgram lightShader;

	public LightRenderer() {
		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/point_light_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/point_light_fragment_shader.glsl");
		lightShader = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);
	}

	public void begin(TextureRegion bumpMapTextureRegion) {
		lightShader.begin();
		bumpMapTextureRegion.getTexture().bind(0);
		lightShader.setUniformi("u_texture", 0);
	}

	public void render(PointLight light, Camera camera) {
		light.render(camera, lightShader);
	}

	public void end() {
		lightShader.end();
	}

	@Override
	public void dispose() {
		lightShader.dispose();
	}
}
