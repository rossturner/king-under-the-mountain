package technology.rocketjump.undermount.settlement.notifications;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public enum NotificationType {

	IMMIGRANTS_ARRIVED("flying-flag", "#22dd72", "settlers.png", null),
	AREA_REVEALED("mining", "#f8f79f", "cavern_uncovered.png", null),
	MINING_COLLAPSE("rock", "#bfb9b4", "cavern_collapse.png", "MiningCollapse"),
	ROOFING_COLLAPSE("triple-gate", "#bfb9b4", "cavern_collapse.png", "MiningCollapse"),
	SETTLER_TANTRUM("nailed-head", "#bd9ec9", "tantrum.png", null),
	SETTLER_MENTAL_BREAK("nailed-head", "#d182d2", "clouds.png", null),
	DEATH("dead-head", "#9f533f", "death.png", "Body Drop"),
	FIRE_STARTED("flame", "#FF4400", "fire-landscape.png", null),
	OXIDISATION_DESTRUCTION("cracked-shield", "#a15f33", "rainfall.png", null),
	FISH_EXHAUSTED("fried-fish", "#38bfc8", "river.png", null),
	GAME_OVER("castle-ruins", "#cd0303", "settlement-game-over.png", null);


	private String iconName;
	private String imageFilename;
	private Color iconColor;
	private String overrideSoundAssetName;
	private SoundAsset overrideSoundAsset;

	NotificationType(String iconName, String iconColor, String imageFileName, String overrideSoundAssetName) {
		this.iconName = iconName;
		this.imageFilename = imageFileName;
		this.iconColor = HexColors.get(iconColor);
		this.overrideSoundAssetName = overrideSoundAssetName;
	}

	public String getI18nTitleKey() {
		return "NOTIFICATION."+name()+".TITLE";
	}

	public String getI18nDescriptionKey() {
		return "NOTIFICATION."+name()+".DESCRIPTION";
	}

	public String getIconName() {
		return iconName;
	}

	public String getImageFilename() {
		return imageFilename;
	}

	public Color getIconColor() {
		return iconColor;
	}

	public String getOverrideSoundAssetName() {
		return overrideSoundAssetName;
	}

	public SoundAsset getOverrideSoundAsset() {
		return overrideSoundAsset;
	}

	public void setOverrideSoundAsset(SoundAsset overrideSoundAsset) {
		this.overrideSoundAsset = overrideSoundAsset;
	}
}
