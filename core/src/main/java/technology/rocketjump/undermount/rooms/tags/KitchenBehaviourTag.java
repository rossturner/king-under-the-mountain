package technology.rocketjump.undermount.rooms.tags;

import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.entities.tags.TagProcessingUtils;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.components.behaviour.KitchenBehaviour;

public class KitchenBehaviourTag extends Tag {
	@Override
	public String getTagName() {
		return "KITCHEN_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		KitchenBehaviour behaviourComponent = room.createComponent(KitchenBehaviour.class, tagProcessingUtils.messageDispatcher);
		behaviourComponent.setRequiredProfession(tagProcessingUtils.professionDictionary.getByName("CHEF")); // MODDING data-drive this
		behaviourComponent.setJobTypes( // MODDING data-drive this
				tagProcessingUtils.jobTypeDictionary.getByName("COOKING"),
				tagProcessingUtils.jobTypeDictionary.getByName("TRANSFER_LIQUID"),
				tagProcessingUtils.jobTypeDictionary.getByName("HAULING")
		);
		behaviourComponent.setCookingRecipes(tagProcessingUtils.cookingRecipeDictionary.getAll());
	}
}
