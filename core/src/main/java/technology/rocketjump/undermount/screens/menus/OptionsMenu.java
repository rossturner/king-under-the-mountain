package technology.rocketjump.undermount.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.screens.menus.options.OptionsTab;
import technology.rocketjump.undermount.screens.menus.options.OptionsTabName;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Singleton
public class OptionsMenu implements Menu {

	private final IconButton backButton;
	private final Skin uiSkin;

	private final Table menuContainer;
	private final Table menuTable;
	private final Table tabsTable;
	private final Texture twitchLogo;

	private final Map<OptionsTabName, OptionsTab> tabs = new EnumMap<>(OptionsTabName.class);
	private OptionsTabName currentTab = OptionsTabName.GRAPHICS;

	@Inject
	public OptionsMenu(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
					   IconButtonFactory iconButtonFactory, I18nWidgetFactory i18NWidgetFactory) {
		this.uiSkin = guiSkinRepository.getDefault();

		twitchLogo = new Texture("assets/ui/TwitchGlitchPurple.png");

		tabsTable = new Table(uiSkin);
		OptionsMenu This = this;
		for (OptionsTabName tab : OptionsTabName.values()) {
			Table tabButton = new Table(uiSkin);
			tabButton.background("default-rect");
			tabButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					This.currentTab = tab;
					This.reset();
				}
			});
			if (tab.equals(OptionsTabName.TWITCH)) {
				Image twitchImage = new Image(twitchLogo);
				tabButton.add(twitchImage).pad(2);
			}
			tabButton.add(i18NWidgetFactory.createLabel("GUI.OPTIONS.TAB."+tab.name())).pad(5);
			tabsTable.add(tabButton).left().padRight(10);
		}

		backButton = iconButtonFactory.create("GUI.BACK_LABEL", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
		backButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
		});

		menuTable = new Table(uiSkin);
		menuTable.setFillParent(true);
		menuTable.left().top();

		menuContainer = new Table(uiSkin);
		menuContainer.background(uiSkin.getDrawable("default-rect"));
		menuContainer.add(menuTable).left().top().width(700).height(500).pad(3).row();

	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(tabsTable).left().row();
		containerTable.add(menuContainer).center().row();
		containerTable.add(backButton).left().pad(10).row();
	}

	@Override
	public void reset() {
		menuTable.clearChildren();

		OptionsTab currentTab = tabs.get(this.currentTab);
		if (currentTab == null) {
			Logger.error("No tab for name " + this.currentTab.name());
		} else {
			currentTab.populate(menuTable);
		}
	}

	@Override
	public void show() {

	}

	@Override
	public void hide() {

	}

	public void setTabImplementations(List<OptionsTab> optionsTabClasses) {
		for (OptionsTab optionsTabClass : optionsTabClasses) {
			this.tabs.put(optionsTabClass.getTabName(), optionsTabClass);
		}
	}
}
