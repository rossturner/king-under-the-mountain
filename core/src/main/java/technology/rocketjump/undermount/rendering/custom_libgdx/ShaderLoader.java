package technology.rocketjump.undermount.rendering.custom_libgdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderLoader {

	public static ShaderProgram createShader(FileHandle vertexShaderFile, FileHandle fragmentShaderFile) {
		ShaderProgram pointLightShader = new ShaderProgram(vertexShaderFile, fragmentShaderFile);
		pointLightShader.pedantic = false;
		if (pointLightShader.isCompiled() == false) {
			throw new IllegalArgumentException("couldn't compile shader: "
					+ pointLightShader.getLog());
		}
		return pointLightShader;
	}
}
