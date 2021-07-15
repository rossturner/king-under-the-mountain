package technology.rocketjump.undermount.rendering.custom_libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderLoader {

	public static ShaderProgram defaultShaderInstance;
	static {
		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/default_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/default_fragment_shader.glsl");
		defaultShaderInstance = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);
	}

	public static ShaderProgram createShader(FileHandle vertexShaderFile, FileHandle fragmentShaderFile) {
		ShaderProgram shaderProgram = new ShaderProgram(vertexShaderFile, fragmentShaderFile);
		ShaderProgram.pedantic = false;
		if (!shaderProgram.isCompiled()) {
			throw new IllegalArgumentException("couldn't compile shader: "
					+ shaderProgram.getLog());
		}
		return shaderProgram;
	}
}
