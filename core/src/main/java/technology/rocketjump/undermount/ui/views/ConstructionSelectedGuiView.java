package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.ui.Selectable;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nTextWidget;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.Collection;
import java.util.List;

import static technology.rocketjump.undermount.ui.Selectable.SelectableType.CONSTRUCTION;

@Singleton
public class ConstructionSelectedGuiView implements GuiView, GameContextAware {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton cancelButton;
	private final MessageDispatcher messageDispatcher;
	private Table outerTable;
	private Table descriptionTable;
	private GameContext gameContext;

	@Inject
	public ConstructionSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
									   GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory) {
		uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;

		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		descriptionTable = new Table(uiSkin);
		descriptionTable.pad(10);

		cancelButton = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.get("#D4534C"), ButtonStyle.SMALL);
		cancelButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(CONSTRUCTION)) {
				messageDispatcher.dispatchMessage(MessageType.CANCEL_CONSTRUCTION, selectable.getConstruction());
			}
		});

		outerTable.add(descriptionTable).left();
		outerTable.add(cancelButton).center();
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();
		containerTable.add(outerTable);
	}

	@Override
	public void update() {
		descriptionTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(CONSTRUCTION)) {
			Construction construction = selectable.getConstruction();
			descriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(construction), uiSkin, messageDispatcher)).left();
			descriptionTable.row();
			descriptionTable.add(new I18nTextWidget(i18nTranslator.getConstructionStatusDescription(construction), uiSkin, messageDispatcher)).left();
			descriptionTable.row();
			List<HaulingAllocation> allocatedItems = construction.getIncomingHaulingAllocations();
			for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
				if (requirement.getMaterial() != null) {
					int numberAllocated = getAllocationAmount(requirement.getItemType(), allocatedItems, construction.getPlacedItemAllocations().values());
					descriptionTable.add(new I18nTextWidget(i18nTranslator.getItemAllocationDescription(numberAllocated, requirement), uiSkin, messageDispatcher)).left();
					descriptionTable.row();
				}
			}

//			descriptionTable.row();
		}
	}

	private int getAllocationAmount(ItemType itemType, List<HaulingAllocation> haulingAllocations, Collection<ItemAllocation> placedItems) {
		int allocated = 0;
		for (HaulingAllocation haulingAllocation : haulingAllocations) {
			if (haulingAllocation.getItemAllocation() != null) {
				Entity itemEntity = gameContext.getEntities().get(haulingAllocation.getItemAllocation().getTargetItemEntityId());
				if (itemEntity != null && ((ItemEntityAttributes)itemEntity.getPhysicalEntityComponent().getAttributes()).getItemType().equals(itemType)) {
					allocated += haulingAllocation.getItemAllocation().getAllocationAmount();
				}
			}
		}
		for (ItemAllocation itemAllocation : placedItems) {
			Entity itemEntity = gameContext.getEntities().get(itemAllocation.getTargetItemEntityId());
			if (itemEntity != null) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().equals(itemType)) {
					allocated += itemAllocation.getAllocationAmount();
				}
			}
		}

		return allocated;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.CONSTRUCTION_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
