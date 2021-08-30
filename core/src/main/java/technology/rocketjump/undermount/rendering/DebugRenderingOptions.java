package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.Gdx;

public class DebugRenderingOptions {

	private boolean showIndividualLightingBuffers = false;
	private boolean showJobStatus = false;
	private boolean showPathfindingNodes = false;
	private boolean showZones = false;
	private boolean showPathfindingSlowdown = false;
	private boolean showLiquidFlow = false;

	public boolean showIndividualLightingBuffers() {
		return showIndividualLightingBuffers;
	}

	public void setShowIndividualLightingBuffers(boolean showIndividualLightingBuffers) {
		this.showIndividualLightingBuffers = showIndividualLightingBuffers;
	}

	public int adjustScreenXForSplitView(int screenX) {
		if (screenX <= Gdx.graphics.getWidth() / 2) {
			return screenX * 2;
		} else {
			return (screenX - Gdx.graphics.getWidth() / 2) * 2;
		}
	}

	public int adjustScreenYForSplitView(int screenY) {
		if (screenY <= Gdx.graphics.getHeight() / 2) {
			return screenY * 2;
		} else {
			return (screenY - Gdx.graphics.getHeight() / 2) * 2;
		}
	}

	public boolean isShowZones() {
		return showZones;
	}

	public void setShowZones(boolean showZones) {
		this.showZones = showZones;
	}

	public boolean showPathfindingNodes() {
		return showPathfindingNodes;
	}

	public void setShowPathfindingNodes(boolean showPathfindingNodes) {
		this.showPathfindingNodes = showPathfindingNodes;
	}

	public boolean showJobStatus() {
		return showJobStatus;
	}

	public void setShowJobStatus(boolean showJobStatus) {
		this.showJobStatus = showJobStatus;
	}

	public boolean isShowPathfindingSlowdown() {
		return showPathfindingSlowdown;
	}

	public void setShowPathfindingSlowdown(boolean showPathfindingSlowdown) {
		this.showPathfindingSlowdown = showPathfindingSlowdown;
	}

	public boolean isShowLiquidFlow() {
		return showLiquidFlow;
	}

	public void setShowLiquidFlow(boolean showLiquidFlow) {
		this.showLiquidFlow = showLiquidFlow;
	}
}
