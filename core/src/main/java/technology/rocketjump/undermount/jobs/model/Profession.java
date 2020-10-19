package technology.rocketjump.undermount.jobs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.ui.widgets.ImageButton;

public class Profession {

	@Name
	private String name;
	private String i18nKey;
	private String icon;
	@JsonIgnore
	private ImageButton imageButton;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public ImageButton getImageButton() {
		return imageButton;
	}

	public void setImageButton(ImageButton imageButton) {
		this.imageButton = imageButton;
	}

	@Override
	public String toString() {
		return name;
	}
}
