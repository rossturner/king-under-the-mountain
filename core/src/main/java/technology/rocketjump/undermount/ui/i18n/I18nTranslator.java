package technology.rocketjump.undermount.ui.i18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.EntityStore;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.undermount.entities.behaviour.humanoids.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Sanity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.constructions.BridgeConstruction;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.rooms.constructions.FurnitureConstruction;
import technology.rocketjump.undermount.rooms.constructions.WallConstruction;
import technology.rocketjump.undermount.settlement.production.ProductionAssignment;
import technology.rocketjump.undermount.zones.Zone;
import technology.rocketjump.undermount.zones.ZoneClassification;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.ZONE;
import static technology.rocketjump.undermount.ui.i18n.I18nText.BLANK;

@Singleton
public class I18nTranslator implements I18nUpdatable {

	public static DecimalFormat oneDecimalFormat = new DecimalFormat("#.#");

	private final I18nRepo repo;
	private final ProfessionDictionary professionDictionary;
	private final EntityStore entityStore;
	private I18nLanguageDictionary dictionary;

	@Inject
	public I18nTranslator(I18nRepo repo, ProfessionDictionary professionDictionary, EntityStore entityStore) {
		this.repo = repo;
		this.professionDictionary = professionDictionary;
		this.dictionary = repo.getCurrentLanguage();
		this.entityStore = entityStore;
	}

	public I18nText getTranslatedString(String i18nKey) {
		return getTranslatedString(i18nKey, I18nWordClass.UNSPECIFIED);
	}

	public I18nText getTranslatedString(String i18nKey, I18nWordClass wordClass) {
		I18nWord word = dictionary.getWord(i18nKey);
		boolean highlightAsTooltip = word.hasTooltip() && !wordClass.equals(I18nWordClass.TOOLTIP);
		String translated = word.get(wordClass);
		if (translated.contains("{{")) {
			return replaceOtherI18nKeys(translated);
		} else {
			return new I18nText(translated, highlightAsTooltip ? word.getKey() : null);
		}
	}

	public LanguageType getCurrentLanguageType() {
		if (repo.getCurrentLanguageType() != null) {
			return repo.getCurrentLanguageType();
		} else {
			return new LanguageType();
		}
	}

	public I18nLanguageDictionary getDictionary() {
		return dictionary;
	}

	public I18nText getDescription(Entity entity) {
		if (entity == null) {
			return BLANK;
		}
		switch (entity.getType()) {
			case HUMANOID:
				return getDescription(entity, (HumanoidEntityAttributes) entity.getPhysicalEntityComponent().getAttributes());
			case ITEM:
				ItemEntityAttributes itemAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				return getItemDescription(itemAttributes.getQuantity(), itemAttributes.getMaterial(itemAttributes.getItemType().getPrimaryMaterialType()), itemAttributes.getItemType());
			case PLANT:
				return getDescription(entity, (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes());
			case FURNITURE:
				return getDescription((FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes());
			default:
				return new I18nText("Not yet implemented description for entity with type " + entity.getType());
		}
	}

	public I18nText getCurrentGoalDescription(Entity entity, GameContext gameContext) {
		if (entity.getType().equals(EntityType.HUMANOID)) {
			SettlerBehaviour behaviour = (SettlerBehaviour) entity.getBehaviourComponent();
			HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			AssignedGoal currentGoal = behaviour.getCurrentGoal();
			if (currentGoal == null || currentGoal.goal.i18nDescription == null) {
				return BLANK;
			} else {
				I18nWord description = dictionary.getWord(currentGoal.goal.i18nDescription);
				Action currentAction = currentGoal.getCurrentAction();
				if (currentAction != null && currentAction.getDescriptionOverrideI18nKey() != null) {
					description = dictionary.getWord(currentAction.getDescriptionOverrideI18nKey());
				}

				Map<String, I18nString> replacements = new HashMap<>();

				if (currentGoal.getFoodAllocation() != null && currentGoal.getFoodAllocation().getTargetEntity() != null) {
					if (currentGoal.getFoodAllocation().getTargetEntity().getType().equals(EntityType.ITEM)) {
						ItemEntityAttributes itemAttributes = (ItemEntityAttributes) currentGoal.getFoodAllocation().getTargetEntity().getPhysicalEntityComponent().getAttributes();
						GameMaterial material = itemAttributes.getMaterial(itemAttributes.getItemType().getPrimaryMaterialType());
						replacements.put("targetDescription", getItemDescription(1, material, itemAttributes.getItemType()));
					}
				} else if (currentGoal.getLiquidAllocation() != null) {
					replacements.put("targetDescription", currentGoal.getLiquidAllocation().getLiquidMaterial().getI18nValue());
				} else if (currentGoal.getAssignedHaulingAllocation() != null) {
					HaulingAllocation haulingAllocation = currentGoal.getAssignedHaulingAllocation();
					Entity hauledEntity = gameContext.getEntities().get(haulingAllocation.getHauledEntityId());
					I18nString targetDescription = getDescription(hauledEntity);
					if (hauledEntity != null && hauledEntity.getType().equals(EntityType.ITEM)) {
						// Override item description to use hauled quantity
						ItemEntityAttributes hauledEntityAttributes = (ItemEntityAttributes) hauledEntity.getPhysicalEntityComponent().getAttributes();
						targetDescription = getItemDescription(haulingAllocation.getItemAllocation().getAllocationAmount(), hauledEntityAttributes.getPrimaryMaterial(), hauledEntityAttributes.getItemType());
					}

					replacements.put("targetDescription", targetDescription);

					if (ZONE.equals(haulingAllocation.getTargetPositionType())) {
						MapTile targetTile = gameContext.getAreaMap().getTile(haulingAllocation.getTargetPosition());
						Optional<Zone> filteredZone = targetTile.getZones().stream().filter(zone -> zone.getClassification().getZoneType().equals(ZoneClassification.ZoneType.LIQUID_SOURCE)).findFirst();
						if (filteredZone.isPresent()) {
							replacements.put("targetZoneMaterial", filteredZone.get().getClassification().getTargetMaterial().getI18nValue());
						}
					}
				} else if (currentGoal.getAssignedJob() != null) {
					Job job = currentGoal.getAssignedJob();
					description = dictionary.getWord(job.getType().getOverrideI18nKey());
					Profession requiredProfession = job.getRequiredProfession();
					if (requiredProfession == null || NULL_PROFESSION.equals(requiredProfession)) {
						requiredProfession = professionDictionary.getDefault();
					}
					replacements.put("profession", dictionary.getWord(requiredProfession.getI18nKey()));

					if (job.getTargetId() != null) {
						Entity targetEntity = entityStore.getById(job.getTargetId());
						if (job.getCookingRecipe() != null && job.getCookingRecipe().getOutputDescriptionI18nKey() != null) {
							Map<String, I18nString> recipeReplacements = new HashMap<>();
							if (job.getCookingRecipe().getOutputMaterial() != null) {
								recipeReplacements.put("materialDescription", job.getCookingRecipe().getOutputMaterial().getI18nValue());
							} else {
								recipeReplacements.put("materialDescription", BLANK);
							}
							I18nWord descriptionWord = dictionary.getWord(job.getCookingRecipe().getOutputDescriptionI18nKey());

							if (job.getCookingRecipe().getVerbOverrideI18nKey() != null) {
								replacements.put("profession", dictionary.getWord(job.getCookingRecipe().getVerbOverrideI18nKey()));
							}

							I18nText targetDescription = applyReplacements(descriptionWord, recipeReplacements, Gender.ANY);
							replacements.put("targetDescription", targetDescription);
						} else if (targetEntity != null) {
							replacements.put("targetDescription", getDescription(targetEntity));
							if (targetEntity.getType().equals(EntityType.PLANT)) {
								replacements.put("targetPlant", getDescription(targetEntity));
							}
						} else if (job.getJobLocation() != null) {
							MapTile targetTile = gameContext.getAreaMap().getTile(job.getJobLocation());
							if (targetTile.hasConstruction()) {
								replacements.put("targetDescription", getConstructionTargetDescrption(targetTile.getConstruction()));
							}
						}

						if (targetEntity != null && targetEntity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
							CraftingStationBehaviour craftingStationBehaviour = (CraftingStationBehaviour) targetEntity.getBehaviourComponent();
							if (craftingStationBehaviour.getCurrentProductionAssignment() != null) {
								ProductionAssignment assignment = craftingStationBehaviour.getCurrentProductionAssignment();
								QuantifiedItemTypeWithMaterial output = assignment.targetRecipe.getOutput().get(0);
								I18nText targetDescription;
								// FIXME some duplication of the below
								if (output.isLiquid()) {
									targetDescription = getLiquidDescription(output.getMaterial(), output.getQuantity());
								} else {
									targetDescription = getItemDescription(output.getQuantity(),
											output.getMaterial(),
											output.getItemType());
								}
								replacements.put("targetDescription", targetDescription);

								if (assignment.targetRecipe.getVerbOverrideI18nKey() != null) {
									replacements.put("profession", dictionary.getWord(assignment.targetRecipe.getVerbOverrideI18nKey()));
								}
							}
						}

					} else if (job.getJobLocation() != null) {
						MapTile targetTile = gameContext.getAreaMap().getTile(job.getJobLocation());
						if (targetTile.hasConstruction()) {
							replacements.put("targetDescription", getConstructionTargetDescrption(targetTile.getConstruction()));
						} else if (targetTile.hasDoorway()) {
							replacements.put("targetDescription", getDescription(targetTile.getDoorway().getDoorEntity()));
						} else if (targetTile.getFloor().hasBridge()) {
							replacements.put("targetDescription", getDescription(targetTile.getFloor().getBridge()));
						} else {
							replacements.put("targetDescription", getDescription(targetTile));
						}

						for (Entity targetTileEntity : targetTile.getEntities()) {
							if (targetTileEntity.getType().equals(EntityType.PLANT)) {
								replacements.put("targetPlant", getDescription(targetTileEntity));
								break;
							}
						}
					}

					if (job.getRequiredItemType() != null) {
						InventoryComponent.InventoryEntry requiredItem;
						if (job.getRequiredItemMaterial() != null) {
							requiredItem = entity.getComponent(InventoryComponent.class).findByItemTypeAndMaterial(job.getRequiredItemType(), job.getRequiredItemMaterial(), gameContext.getGameClock());
						} else {
							requiredItem = entity.getComponent(InventoryComponent.class).findByItemType(job.getRequiredItemType(), gameContext.getGameClock());
						}

						if (requiredItem != null) {
							replacements.put("requiredItem", getDescription(requiredItem.entity));
						}
					}
				}

				if (entity.getLocationComponent().getContainerEntity() != null) {
					replacements.put("containerDescription", getDescription(entity.getLocationComponent().getContainerEntity()));
				} else if (entity.getLocationComponent().getWorldPosition() != null) {
					// Not in container entity
					MapTile currentTile = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldPosition());
					replacements.put("tileDescription", getDescription(currentTile));
				}

 				return applyReplacements(description, replacements, attributes.getGender());
			}
		} else {
			return BLANK; // Not supporting non-humanoid entities yet
		}
	}

	private I18nText getConstructionTargetDescrption(Construction construction) {
		// Construction might be wall, doorway or furniture
		I18nText targetDescription = BLANK;
		switch (construction.getConstructionType()) {
			case WALL_CONSTRUCTION:
				WallConstruction wallConstruction = (WallConstruction) construction;
				targetDescription = getWallDescription(wallConstruction.getPrimaryMaterial(), wallConstruction.getWallTypeToConstruct());
				break;
			case DOORWAY_CONSTRUCTION:
			case FURNITURE_CONSTRUCTION:
				FurnitureConstruction furnitureConstruction = (FurnitureConstruction) construction;
				targetDescription = getDescription(furnitureConstruction.getEntity());
				break;
			case BRIDGE_CONSTRUCTION:
				targetDescription = getDescription(((BridgeConstruction) construction).getBridge());
				break;
			default:
				Logger.error("Not yet implemented construction description for " + construction.getConstructionType());
		}
		return targetDescription;
	}

	public I18nText getDescription(Bridge bridge) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (bridge.getMaterial() != null && !NULL_MATERIAL.equals(bridge.getMaterial())) {
			replacements.put("materialType", bridge.getMaterial().getI18nValue());
		} else {
			replacements.put("materialType", I18nWord.BLANK);
		}
		replacements.put("furnitureType", dictionary.getWord(bridge.getBridgeType().getI18nKey()));
		return applyReplacements(dictionary.getWord("FURNITURE.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getWallDescription(MapTile targetTile) {
		if (targetTile == null || !targetTile.hasWall()) {
			return BLANK;
		} else {
			Wall wall = targetTile.getWall();
			if (wall.hasOre()) {
				return getWallDescription(wall.getOreMaterial(), wall.getOreType());
			} else {
				return getWallDescription(wall.getMaterial(), wall.getWallType());
			}
		}
	}

	private I18nText getWallDescription(GameMaterial gameMaterial, WallType wallType) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (gameMaterial != null && !NULL_MATERIAL.equals(gameMaterial)) {
			replacements.put("materialType", gameMaterial.getI18nValue());
		}
		if (wallType != null) {
			replacements.put("wallType", dictionary.getWord(wallType.getI18nKey()));
		}
		return applyReplacements(dictionary.getWord("WALL.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getDescription(MapTile tile) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (tile.hasWall()) {
			if (tile.getWall().hasOre()) {
				replacements.put("materialType", tile.getWall().getOreMaterial().getI18nValue());
				replacements.put("wallType", dictionary.getWord(tile.getWall().getOreType().getI18nKey()));
			} else {
				replacements.put("materialType", tile.getWall().getMaterial().getI18nValue());
				replacements.put("wallType", dictionary.getWord(tile.getWall().getWallType().getI18nKey()));
			}
			return applyReplacements(dictionary.getWord("WALL.DESCRIPTION"), replacements, Gender.ANY);
		} else {
			if (tile.isWaterSource()) {
				return getTranslatedString("FLOOR.RIVER");
			} else {
				replacements.put("floorType", dictionary.getWord(tile.getFloor().getFloorType().getI18nKey()));
				return applyReplacements(dictionary.getWord("FLOOR.DESCRIPTION"), replacements, Gender.ANY);
			}
		}
	}

	public I18nText getDescription(Construction construction) {

		if (construction instanceof FurnitureConstruction) {
			FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) construction.getEntity().getPhysicalEntityComponent().getAttributes();
			return getConstructionDescription(construction.getPrimaryMaterial(), furnitureEntityAttributes.getFurnitureType().getI18nKey());
		} else if (construction instanceof WallConstruction) {
			return getConstructionDescription(construction.getPrimaryMaterial(), ((WallConstruction) construction).getWallTypeToConstruct().getI18nKey());
		} else if (construction instanceof BridgeConstruction) {
			return getConstructionDescription(construction.getPrimaryMaterial(), ((BridgeConstruction)construction).getBridge().getBridgeType().getI18nKey());
		} else {
			Logger.error("Description of " + construction.getClass().getSimpleName() + " not yet implemented");
			return BLANK;
		}

	}

	public I18nText getDateTimeString(GameClock gameClock) {
		if (gameClock == null) {
			return new I18nText("");
		}
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("timeOfDay", new I18nWord(gameClock.getFormattedGameTime()));
		replacements.put("dayNumber", new I18nWord(String.valueOf(gameClock.getDayOfSeason())));
		replacements.put("season", dictionary.getWord(gameClock.getCurrentSeason().getI18nKey()));
		return applyReplacements(dictionary.getWord("GUI.DATE_TIME_LABEL"), replacements, Gender.ANY);
	}

	private I18nText getConstructionDescription(GameMaterial primaryMaterial, String furnitureTypeI18nKey) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (NULL_MATERIAL.equals(primaryMaterial)) {
			replacements.put("materialType", I18nWord.BLANK);
		} else {
			replacements.put("materialType", primaryMaterial.getI18nValue());
		}
		replacements.put("furnitureType", dictionary.getWord(furnitureTypeI18nKey));

		return applyReplacements(dictionary.getWord("CONSTRUCTION.DESCRIPTION"), replacements, Gender.ANY);
	}

	private I18nText getDescription(Entity entity, HumanoidEntityAttributes attributes) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("name", new I18nWord(attributes.getName().toString()));
		replacements.put("race", dictionary.getWord(attributes.getRace().i18nKey));


		if (attributes.getSanity().equals(Sanity.BROKEN)) {
			// TODO Other kinds of madness
			replacements.put("madness", dictionary.getWord("MADNESS.BROKEN"));
			return applyReplacements(dictionary.getWord("HUMANOID.BROKEN.DESCRIPTION"), replacements, attributes.getGender());
		} else {
			ProfessionsComponent professionsComponent = entity.getComponent(ProfessionsComponent.class);
			if (professionsComponent != null) {
				Profession primaryProfession = professionsComponent.getPrimaryProfession(professionDictionary.getDefault());
				replacements.put("profession", dictionary.getWord(primaryProfession.getI18nKey()));
			} else {
				replacements.put("profession", I18nWord.BLANK);
			}

			return applyReplacements(dictionary.getWord("HUMANOID.DESCRIPTION"), replacements, attributes.getGender());
		}
	}

	public I18nText getItemDescription(int quantity, GameMaterial material, ItemType itemType) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("quantity", new I18nWord(String.valueOf(quantity)));
		if (material == null) {
			replacements.put("materialType", I18nWord.BLANK);
		} else {
			replacements.put("materialType", material.getI18nValue());
		}
		if (itemType == null) {
			replacements.put("itemType", I18nWord.BLANK);
			return applyReplacements(dictionary.getWord("ITEM.DESCRIPTION"), replacements, Gender.ANY);
		} else {
			replacements.put("itemType", dictionary.getWord(itemType.getI18nKey()));
		}

		switch (itemType.getItemGroup()) {
			case INGREDIENT:
				return applyReplacements(dictionary.getWord("ITEM.INGREDIENT.DESCRIPTION"), replacements, Gender.ANY);
			default:
				return applyReplacements(dictionary.getWord("ITEM.DESCRIPTION"), replacements, Gender.ANY);
		}

	}

	public I18nText getLiquidDescription(GameMaterial material, float quantity) {
		return new I18nText(material.getI18nValue().toString() + " (" + oneDecimalFormat.format(quantity) + ")");
	}

	public I18nText getItemAllocationDescription(int numberAllocated, QuantifiedItemTypeWithMaterial requirement) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("quantity", new I18nWord(String.valueOf(numberAllocated)));
		replacements.put("total", new I18nWord(String.valueOf(requirement.getQuantity())));
		replacements.put("itemDescription", getItemDescription(requirement.getQuantity(), requirement.getMaterial(), requirement.getItemType()));

		return applyReplacements(dictionary.getWord("CONSTRUCTION.ITEM_ALLOCATION"), replacements, Gender.ANY);
	}

	private I18nText getDescription(Entity entity, PlantEntityAttributes attributes) {
		Map<String, I18nString> replacements = new HashMap<>();

		switch (attributes.getSpecies().getPlantType()) {
			case TREE:
			case MUSHROOM_TREE:
				replacements.put("materialType", attributes.getSpecies().getMaterial().getI18nValue());
				return applyReplacements(dictionary.getWord("TREE.DESCRIPTION"), replacements, Gender.ANY);
			case SHRUB:
				return applyReplacements(dictionary.getWord("SHRUB.DESCRIPTION"), replacements, Gender.ANY);
			case MUSHROOM:
				replacements.put("materialType", attributes.getSpecies().getMaterial().getI18nValue());
				return applyReplacements(dictionary.getWord("MUSHROOM.DESCRIPTION"), replacements, Gender.ANY);
			case CROP:
				if (attributes.getSpecies().getSeed() != null) {
					// TODO replace with actual plant material
					replacements.put("materialType", attributes.getSpecies().getSeed().getSeedMaterial().getI18nValue());
				}
				return applyReplacements(dictionary.getWord("CROP.DESCRIPTION"), replacements, Gender.ANY);
			default:
				return new I18nText("Not yet implemented description for " + attributes.getSpecies().getPlantType());
		}
	}

	private I18nText getDescription(FurnitureEntityAttributes attributes) {
		Map<String, I18nString> replacements = new HashMap<>();

		GameMaterial gameMaterial = attributes.getMaterials().get(attributes.getPrimaryMaterialType());
		if (gameMaterial != null && !NULL_MATERIAL.equals(gameMaterial)) {
			replacements.put("materialType", gameMaterial.getI18nValue());
		} else {
			replacements.put("materialType", I18nWord.BLANK);
		}
		if (attributes.getFurnitureType() != null && attributes.getFurnitureType().getI18nKey() != null) {
			replacements.put("furnitureType", dictionary.getWord(attributes.getFurnitureType().getI18nKey()));
		} else {
			replacements.put("furnitureType", I18nWord.BLANK);
		}

		return applyReplacements(dictionary.getWord("FURNITURE.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getTranslatedWordWithReplacements(String i18nKey, Map<String, I18nString> replacements) {
		I18nWord word = dictionary.getWord(i18nKey);
		return applyReplacements(word, replacements, Gender.ANY);
	}

	public I18nText applyReplacements(I18nWord originalWord, Map<String, I18nString> replacements, Gender gender) {
		String string = originalWord.get(I18nWordClass.UNSPECIFIED, gender);
		I18nText i18nText = new I18nText(string);

		String REGEX_START = Pattern.quote("{{");
		String REGEX_END = Pattern.quote("}}");
		Pattern pattern = Pattern.compile(REGEX_START + "([\\w\\\\.]+)" + REGEX_END);


		Matcher matcher = pattern.matcher(string);
		while (matcher.find()) {
			String token = matcher.group(0);
			token = token.substring(2, token.length() - 2);

			I18nString replacement;
			I18nWordClass replacementWordclass = I18nWordClass.UNSPECIFIED;

			if (token.equals("quantity_if_multiple")) {
				if (getQuantity(replacements) > 1) {
					replacement = new I18nWord(getQuantity(replacements) + " ");
				} else {
					replacement = I18nWord.BLANK;
				}
			} else if (token.toUpperCase().equals("BLANK")) {
				// Always replace {{BLANK}} with ""
				replacement = I18nWord.BLANK;

			} else if (token.contains(".")) {
				// Only expecting one . for now
				String[] split = token.split("\\.");
				replacement = replacements.getOrDefault(split[0], new I18nWord(split[0]));
				if (split[1].equals("noun_or_plural")) {
					if (getQuantity(replacements) > 1) {
						replacementWordclass = I18nWordClass.PLURAL;
					} else {
						replacementWordclass = I18nWordClass.NOUN;
					}
				} else {
					replacementWordclass = I18nWordClass.valueOf(split[1].toUpperCase());
				}
			} else {
				replacement = replacements.getOrDefault(token, I18nWord.BLANK);
			}

			if (replacement instanceof I18nWord) {
				I18nWord replacementWord = (I18nWord)replacement;

				String replacementText = replacementWord.get(replacementWordclass, gender);
				if (!token.equals("name")) {
					replacementText = replacementText.toLowerCase();
				}

				i18nText.replace(matcher.group(0), replacementText, replacementWord.hasTooltip() ? replacementWord.getKey() : null);
			} else if (replacement instanceof I18nText) {
				I18nText replacementText = (I18nText) replacement;
				i18nText.replace(matcher.group(0), replacementText);
			} else {
				if (replacement == null) {
					Logger.error("Replacement in applyReplacements is null, needs investigating");
					i18nText.replace(matcher.group(0), BLANK);
				} else {
					Logger.error("Not yet implemented: " + replacement.getClass().getSimpleName());
				}
			}

		}

		return i18nText.tidy();
	}


	public I18nText replaceOtherI18nKeys(String text) {
		I18nText i18nText = new I18nText(text);

		String REGEX_START = Pattern.quote("{{");
		String REGEX_END = Pattern.quote("}}");
		Pattern pattern = Pattern.compile(REGEX_START + "([\\w\\\\.]+)" + REGEX_END);

		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			String token = matcher.group(0);
			token = token.substring(2, token.length() - 2);

			I18nWord replacement = I18nWord.BLANK;
			I18nWordClass replacementWordclass = I18nWordClass.UNSPECIFIED;

			if (token.contains(".")) {
				String[] parts = token.split("\\.");
				String lastPart = parts[parts.length - 1].toUpperCase();
				I18nWordClass wordClass = EnumUtils.getEnum(I18nWordClass.class, lastPart);
				String[] partsWithoutWordClass = parts;
				if (wordClass != null) {
					partsWithoutWordClass = Arrays.copyOfRange(parts, 0, parts.length - 1);
					replacementWordclass = wordClass;
				}

				String rejoinedParts = StringUtils.join(partsWithoutWordClass, ".").toUpperCase();
				if (dictionary.containsKey(rejoinedParts)) {
					replacement = dictionary.getWord(rejoinedParts);
				}
			}

			String replacementText = replacement.get(replacementWordclass, Gender.ANY).toLowerCase();

			i18nText.replace(matcher.group(0), replacementText, replacement.hasTooltip() ? replacement.getKey() : null);
		}

		return i18nText.tidy();
	}

	private int getQuantity(Map<String, I18nString> replacements) {
		I18nString quantity = replacements.get("quantity");
		if (quantity == null) {
			return 0;
		} else if (quantity instanceof I18nWord) {
			I18nWord quantityWord = (I18nWord) quantity;
			return Integer.valueOf(quantityWord.get(I18nWordClass.UNSPECIFIED));
		} else {
			Logger.error("Not yet implemented: quantity from " + quantity.getClass().getSimpleName());
			return 0;
		}
	}

	public I18nText getAssignedToLabel(Entity assignedToEntity) {
		if (assignedToEntity.getPhysicalEntityComponent().getAttributes() instanceof HumanoidEntityAttributes) {
			Map<String, I18nString> replacements = new HashMap<>();
			HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) assignedToEntity.getPhysicalEntityComponent().getAttributes();
			replacements.put("name", new I18nWord(attributes.getName().toString())); // Names aren't translated
			return applyReplacements(dictionary.getWord("GUI.FURNITURE_ASSIGNED_TO"), replacements, Gender.ANY);
		} else {
			Logger.error("Furniture is assigned to a non-humanoid entity");
			return BLANK;
		}
	}

	public I18nText getConstructionStatusDescription(Construction construction) {
		Map<String, I18nString> replacements = new HashMap<>();

		ItemType missingItemType = null;
		for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
			if (requirement.getMaterial() == null) {
				missingItemType = requirement.getItemType();
				break;
			}
		}

		if (missingItemType != null) {
			replacements.put("materialType", dictionary.getWord(missingItemType.getPrimaryMaterialType().getI18nKey()));
			replacements.put("itemDescription", dictionary.getWord(missingItemType.getI18nKey()));
		}

		I18nWord word;
		switch (construction.getState()) {
			case CLEARING_WORK_SITE:
				word = dictionary.getWord("CONSTRUCTION.STATUS.CLEARING_WORK_SITE");
				break;
			case SELECTING_MATERIALS:
				word = dictionary.getWord("CONSTRUCTION.STATUS.SELECTING_MATERIALS");
				break;
			case WAITING_FOR_RESOURCES:
				word = dictionary.getWord("CONSTRUCTION.STATUS.WAITING_FOR_RESOURCES");
				break;
			case WAITING_FOR_COMPLETION:
				word = dictionary.getWord("CONSTRUCTION.STATUS.WAITING_FOR_COMPLETION");
				break;
			default:
				Logger.error("Not yet implemented: Construction state description for " + construction.getState());
				return BLANK;
		}

		return applyReplacements(word, replacements, Gender.ANY);
	}

	public I18nText getHarvestProgress(float progress) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("progress", new I18nWord("progress", oneDecimalFormat.format(progress)));
		return applyReplacements(dictionary.getWord("CROP.HARVEST_PROGRESS"), replacements, Gender.ANY);
	}

	public I18nText getDynamicMaterialDescription(GameMaterial gameMaterial) {
		I18nWord descriptionWord = dictionary.getWord(gameMaterial.getI18nKey());
		Map<String, I18nString> replacements = new HashMap<>();

		if (gameMaterial.getConstituentMaterials() != null && !gameMaterial.getConstituentMaterials().isEmpty()) {
			Iterator<GameMaterial> iterator = gameMaterial.getConstituentMaterials().iterator();
			if (gameMaterial.getConstituentMaterials().size() == 1) {
				replacements.put("materialDescription", iterator.next().getI18nValue());
			} else if (gameMaterial.getConstituentMaterials().size() == 2) {
				replacements.put("materialOne", iterator.next().getI18nValue());
				replacements.put("materialTwo", iterator.next().getI18nValue());

				replacements.put("materialDescription", applyReplacements(dictionary.getWord("COOKING.DUAL_INGREDIENT.DESCRIPTION"), replacements, Gender.ANY));
			} else {
				replacements.put("materialDescription", dictionary.getWord(iterator.next().getMaterialType().getI18nKey()));
			}
		}

		return applyReplacements(descriptionWord, replacements, Gender.ANY);
	}

	@Override
	public void onLanguageUpdated() {
		dictionary = repo.getCurrentLanguage();
	}
}
