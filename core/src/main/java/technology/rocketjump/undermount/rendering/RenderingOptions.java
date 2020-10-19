package technology.rocketjump.undermount.rendering;

import com.google.inject.Singleton;

@Singleton
public class RenderingOptions {

	private boolean floorOverlapRenderingEnabled = true;

	private DebugRenderingOptions debugRenderingOptions = new DebugRenderingOptions();

	public boolean isFloorOverlapRenderingEnabled() {
		return floorOverlapRenderingEnabled;
	}

	public void toggleFloorOverlapRenderingEnabled() {
		this.floorOverlapRenderingEnabled = !floorOverlapRenderingEnabled;
	}

	public DebugRenderingOptions debug() {
		return debugRenderingOptions;
	}
}
