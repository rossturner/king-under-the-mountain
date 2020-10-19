package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.rooms.StockpileAllocation;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.ui.Selectable;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nTextWidget;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.undermount.ui.Selectable.SelectableType.TILE;

@Singleton
public class TileSelectedGuiView implements GuiView {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final MessageDispatcher messageDispatcher;
	private Table descriptionTable;

	@Inject
	public TileSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
							   GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory) {
		uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;

		descriptionTable = new Table(uiSkin);
		descriptionTable.background("default-rect");
		descriptionTable.pad(10);
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();

		containerTable.add(descriptionTable);
	}

	@Override
	public void update() {
		descriptionTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(TILE)) {
			MapTile tile = selectable.getTile();
			if (tile != null) {
				if (tile.getExploration().equals(EXPLORED)) {
					descriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(tile), uiSkin, messageDispatcher)).left();
				} else {
					descriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString("FLOOR.UNEXPLORED"), uiSkin, messageDispatcher)).left();
				}
				descriptionTable.row();
				if (GlobalSettings.DEV_MODE) {
					if (tile.getRoomTile() != null) {
						StockpileComponent stockpileComponent = tile.getRoomTile().getRoom().getComponent(StockpileComponent.class);
						if (stockpileComponent != null) {
							StockpileAllocation stockpileAllocation = stockpileComponent.getAllocationAt(tile.getTilePosition());
							if (stockpileAllocation == null) {
								descriptionTable.add(new Label("Stockpile allocations - null", uiSkin)).left().row();
							} else {
								descriptionTable.add(new Label("Stockpile allocations - Incoming: "+stockpileAllocation.getIncomingHaulingQuantity() +
										" In tile: " + stockpileAllocation.getQuantityInTile(), uiSkin)).left().row();

							}
						}
					}


					descriptionTable.add(new Label("Location: " + tile.getTilePosition(), uiSkin)).left().row();
					descriptionTable.add(new Label("Roof: " + tile.getRoof(), uiSkin)).left().row();
					descriptionTable.add(new Label("Region: " + tile.getRegionId(), uiSkin)).left().row();
					descriptionTable.add(new Label("Zones: " + StringUtils.join(tile.getZones(), ", "), uiSkin)).left().row();
				}
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.TILE_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

}
