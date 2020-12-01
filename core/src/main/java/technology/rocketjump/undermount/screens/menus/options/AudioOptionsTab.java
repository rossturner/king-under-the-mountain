package technology.rocketjump.undermount.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nLabel;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import static technology.rocketjump.undermount.audio.AmbientSoundManager.DEFAULT_AMBIENT_AUDIO_VOLUME_AS_STRING;
import static technology.rocketjump.undermount.audio.MusicJukebox.DEFAULT_VOLUME_AS_STRING;
import static technology.rocketjump.undermount.audio.SoundEffectManager.DEFAULT_SOUND_VOLUME_AS_STRING;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.*;

@Singleton
public class AudioOptionsTab implements OptionsTab {

	private final I18nLabel audioTitle;
	private final Label musicLabel;
	private final Slider musicSlider;
	private final Label soundEffectLabel;
	private final Slider soundEffectSlider;
	private final Label ambientEffectLabel;
	private final Slider ambientEffectSlider;

	@Inject
	public AudioOptionsTab(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						   IconButtonFactory iconButtonFactory, I18nWidgetFactory i18NWidgetFactory, SoundAssetDictionary soundAssetDictionary) {
		Skin uiSkin = guiSkinRepository.getDefault();
		audioTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.AUDIO.TITLE");

		final SoundAsset sliderSoundAsset = soundAssetDictionary.getByName("Slider");

		musicLabel = i18NWidgetFactory.createLabel("GUI.MUSIC_VOLUME");
		musicSlider = new Slider(0, 0.8f, 0.08f, false, uiSkin);
		String savedVolume = userPreferences.getPreference(MUSIC_VOLUME, DEFAULT_VOLUME_AS_STRING);
		musicSlider.setValue(Float.valueOf(savedVolume));
		musicSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float musicSliderValue = musicSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_MUSIC_VOLUME, musicSliderValue);
			}
			return true;
		});

		soundEffectLabel = i18NWidgetFactory.createLabel("GUI.SOUND_EFFECT_VOLUME");
		soundEffectSlider = new Slider(0, 1, 0.1f, false, uiSkin);
		String savedSoundEffectVolume = userPreferences.getPreference(SOUND_EFFECT_VOLUME, DEFAULT_SOUND_VOLUME_AS_STRING);
		soundEffectSlider.setValue(Float.valueOf(savedSoundEffectVolume));
		soundEffectSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float sliderValue = soundEffectSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME, sliderValue);
			}
			return true;
		});

		ambientEffectLabel = i18NWidgetFactory.createLabel("GUI.AMBIENT_EFFECT_VOLUME");
		ambientEffectSlider = new Slider(0, 1, 0.1f, false, uiSkin);
		String savedAmbientEffectVolume = userPreferences.getPreference(AMBIENT_EFFECT_VOLUME, DEFAULT_AMBIENT_AUDIO_VOLUME_AS_STRING);
		ambientEffectSlider.setValue(Float.valueOf(savedAmbientEffectVolume));
		ambientEffectSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float sliderValue = ambientEffectSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_AMBIENT_EFFECT_VOLUME, sliderValue);
			}
			return true;
		});


	}

	@Override
	public void populate(Table menuTable) {

		// AUDIO
		menuTable.add(audioTitle).width(250).left().pad(10);
		menuTable.add(new Container<>()).colspan(2).row();

		menuTable.add(musicLabel).pad(10).right();
		menuTable.add(musicSlider).pad(10);
		menuTable.add(new Container<>()).row(); // pad out 1 cell

		menuTable.add(soundEffectLabel).pad(10).right();
		menuTable.add(soundEffectSlider).pad(10);
		menuTable.add(new Container<>()).row(); // pad out 1 cell

		menuTable.add(ambientEffectLabel).pad(10).right();
		menuTable.add(ambientEffectSlider).pad(10);
		menuTable.add(new Container<>()).row(); // pad out 1 cell
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.AUDIO;
	}
}
