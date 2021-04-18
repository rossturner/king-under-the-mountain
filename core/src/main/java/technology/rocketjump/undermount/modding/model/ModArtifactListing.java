package technology.rocketjump.undermount.modding.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.undermount.assets.entities.wallcap.model.WallCapAsset;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.OverlapType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.cooking.model.CookingRecipe;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.ai.goap.Goal;
import technology.rocketjump.undermount.entities.ai.goap.Schedule;
import technology.rocketjump.undermount.entities.factories.names.NameWord;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureCategory;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.modding.processing.*;
import technology.rocketjump.undermount.modding.validation.*;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.rooms.StockpileGroup;
import technology.rocketjump.undermount.settlement.production.ProductionQuota;
import technology.rocketjump.undermount.sprites.model.BridgeType;
import technology.rocketjump.undermount.ui.hints.model.Hint;
import technology.rocketjump.undermount.ui.i18n.LanguageType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.ArtifactCombinationType.ADDITIVE;
import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.ArtifactCombinationType.REPLACES_EXISTING;
import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.ModFileType.*;
import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.OutputType.*;

@Singleton
public class ModArtifactListing {

	private final Map<String, ModArtifactDefinition> byName = new HashMap<>();
	private final List<ModArtifactDefinition> allArtifacts;

	@Inject
	public ModArtifactListing() {
		allArtifacts = Arrays.asList(
				////////// AI //////////
				def("ai", "schedules", SINGLE_FILE, JSON_ARRAY, Schedule.class,
						"ai", "schedules.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("ai", "goals", SINGLE_FILE, JSON_ARRAY, Goal.class,
						"ai/goals", "*.json", JSON_OBJECT, ADDITIVE,
						/* TODO Check Goal JSON */ UntypedJsonProcessor.class),

				////////// definitions //////////
				def("definitions/plantColorSwatches", null, COPY_ORIGINAL_FILES, PNG, null,
						"entities/plant", "**/*-swatch.png", PNG, ADDITIVE, CopyFilesProcessor.class, UniqueFilenames.class),

				def("definitions/entityAssets", "entityAssetTypes", SINGLE_FILE, JSON_MAP, EntityAssetType.class,
						"entities", "entityAssetTypes", JSON_MAP, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("definitions/entityAssets", "renderLayers", SINGLE_FILE, JSON_MAP, null,
						"entities", "renderLayers", JSON_MAP, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("definitions/entityAssets", "furnitureEntityAssets", SINGLE_FILE, JSON_ARRAY, FurnitureEntityAsset.class,
						"entities/furniture", "**/descriptors", JSON_ARRAY, ADDITIVE,
						GenericClassTypeProcessor.class, ReferencedImagesExist.class, UniqueNames.class),
				def("definitions/entityAssets", "humanoidEntityAssets", SINGLE_FILE, JSON_ARRAY, HumanoidEntityAsset.class,
						"entities/humanoid", "**/descriptors", JSON_ARRAY, ADDITIVE,
						GenericClassTypeProcessor.class, ReferencedImagesExist.class, UniqueNames.class),
				def("definitions/entityAssets", "itemEntityAssets", SINGLE_FILE, JSON_ARRAY, ItemEntityAsset.class,
						"entities/item", "**/descriptors", JSON_ARRAY, ADDITIVE,
						GenericClassTypeProcessor.class, ReferencedImagesExist.class, UniqueNames.class),
				def("definitions/entityAssets", "plantEntityAssets", SINGLE_FILE, JSON_ARRAY, PlantEntityAsset.class,
						"entities/plant", "**/descriptors", JSON_ARRAY, ADDITIVE,
						GenericClassTypeProcessor.class, ReferencedImagesExist.class, UniqueNames.class),
				def("definitions/entityAssets", "wallCapAssets", SINGLE_FILE, JSON_ARRAY, WallCapAsset.class,
						"entities/wallCap", "**/descriptors", JSON_ARRAY, ADDITIVE,
						GenericClassTypeProcessor.class, ReferencedImagesExist.class, UniqueNames.class),

				def("definitions/crafting", "cookingRecipes", SINGLE_FILE, JSON_ARRAY, CookingRecipe.class,
						"definitions/crafting", "cookingRecipes", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/crafting", "craftingRecipes", SINGLE_FILE, JSON_ARRAY, CraftingRecipe.class,
						"definitions/crafting/recipes", "*.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/crafting", "craftingTypes", SINGLE_FILE, JSON_ARRAY, CraftingType.class,
						"definitions/crafting", "craftingTypes", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/crafting", "itemProductionDefaults", SINGLE_FILE, JSON_MAP, ProductionQuota.class,
						"definitions/crafting", "itemProductionDefaults.json", JSON_MAP, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/crafting", "liquidProductionDefaults", SINGLE_FILE, JSON_MAP, ProductionQuota.class,
						"definitions/crafting", "liquidProductionDefaults.json", JSON_MAP, ADDITIVE, GenericClassTypeProcessor.class),

				def("definitions/types", "wallTypes", SINGLE_FILE, JSON_ARRAY, WallType.class,
						"terrain/walls", "**/definition.json", JSON_OBJECT, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "bridgeTypes", SINGLE_FILE, JSON_ARRAY, BridgeType.class,
						"terrain/bridges", "**/bridge-tileset-definition.json", JSON_OBJECT, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "overlapTypes", SINGLE_FILE, JSON_ARRAY, OverlapType.class,
						"masks", "overlapTypes.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "floorTypes", SINGLE_FILE, JSON_ARRAY, FloorType.class,
						"terrain/floors", "**/*-tileset-definition.json", JSON_OBJECT, ADDITIVE, GenericClassTypeProcessor.class, OverlapTypeExists.class),
				def("definitions/types", "jobTypes", SINGLE_FILE, JSON_ARRAY, JobType.class,
						"definitions", "jobTypes.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "professions", SINGLE_FILE, JSON_ARRAY, Profession.class,
						"definitions", "professions.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "roomTypes", SINGLE_FILE, JSON_ARRAY, RoomType.class,
						"rooms", "roomTypes.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "stockpileGroups", SINGLE_FILE, JSON_ARRAY, StockpileGroup.class,
						"rooms", "stockpileGroups.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "furnitureCategories", SINGLE_FILE, JSON_ARRAY, FurnitureCategory.class,
						"entities/furniture", "furnitureCategories.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "furnitureCategories", SINGLE_FILE, JSON_ARRAY, FurnitureCategory.class,
						"entities/furniture", "furnitureCategories.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "furnitureLayouts", SINGLE_FILE, JSON_ARRAY, FurnitureLayout.class,
						"entities/furniture", "furnitureLayouts.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "furnitureTypes", SINGLE_FILE, JSON_ARRAY, FurnitureType.class,
						"entities/furniture", "**/furnitureTypes.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "itemTypes", SINGLE_FILE, JSON_ARRAY, ItemType.class,
						"entities/item", "**/itemTypes.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/types", "plantSpecies", SINGLE_FILE, JSON_ARRAY, PlantSpecies.class,
						"entities/plant", "**/plantSpecies.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),

				def("definitions", "materials", SINGLE_FILE, JSON_ARRAY, GameMaterial.class,
						"definitions/materials", "*-materials.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "designations", SINGLE_FILE, JSON_ARRAY, TileDesignation.class,
						"definitions", "designations.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "constants", SINGLE_FILE, JSON_OBJECT, null,
						"definitions/constants", "**.json", JSON_KEY_VALUES, ADDITIVE, UntypedJsonProcessor.class),

				////////// music //////////
				def("music", null, COPY_ORIGINAL_FILES, OGG, null,
						"music", "*.ogg", OGG, ADDITIVE, CopyFilesProcessor.class),

				////////// particles //////////
				def("definitions/particles", null, COPY_ORIGINAL_FILES, P_FILE, null,
						"particles/libgdx", "**/*.p", P_FILE, ADDITIVE, CopyFilesProcessor.class),
				def("definitions/types", "particleTypes", SINGLE_FILE, JSON_ARRAY, ParticleEffectType.class,
						"particles/types", "*.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),

				////////// settings //////////
				def("settings", "sunlight", SINGLE_FILE, JSON_ARRAY, null,
						"settings", "sunlight.json", JSON_OBJECT, REPLACES_EXISTING, SunlightProcessor.class),
				def("settings", "timeAndDaySettings", SINGLE_FILE, JSON_OBJECT, null,
						"settings", "timeAndDaySettings.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("settings", "immigrationSettings", SINGLE_FILE, JSON_OBJECT, null,
						"settings", "immigrationSettings.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),

				////////// sounds //////////
				def("sounds/data", null, COPY_ORIGINAL_FILES, WAV, null,
						"sounds", "**/*.wav", WAV, ADDITIVE, CopyFilesProcessor.class),
				def("sounds", "soundAssets", SINGLE_FILE, JSON_ARRAY, SoundAsset.class,
						"sounds", "**/sound_descriptors.json", JSON_ARRAY, ADDITIVE,
						GenericClassTypeProcessor.class, ReferencedSoundsExist.class, UniqueNames.class),

				////////// terrain //////////
				def("terrain", "doorwayEdges", SINGLE_FILE, JSON_OBJECT, null,
						"terrain", "doorwayEdges.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("terrain", "wallCapEdges", SINGLE_FILE, JSON_OBJECT, null,
						"terrain", "wallCapEdges.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("terrain", "wallEdges", SINGLE_FILE, JSON_OBJECT, null,
						"terrain", "wallEdges.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("terrain", "wallLayoutQuadrants", SINGLE_FILE, JSON_OBJECT, null,
						"terrain", "wallLayoutQuadrants.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),

				////////// text //////////
				// TODO MODDING this could do with improvement/being made more generic
				def("text/adjective_noun", "adjectives", SINGLE_FILE, CSV, NameWord.class,
						"text/names/adjective_noun", "adjectives.csv", CSV, ADDITIVE, GenericClassTypeProcessor.class, NameWordValidator.class),
				def("text/adjective_noun", "nouns", SINGLE_FILE, CSV, NameWord.class,
						"text/names/adjective_noun", "nouns.csv", CSV, ADDITIVE, GenericClassTypeProcessor.class, NameWordValidator.class),
				def("text/dwarven", "descriptor", SINGLE_FILE, JSON_OBJECT, null,
						"text/names/dwarven", "descriptor.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("text/old_norse", "descriptor", SINGLE_FILE, JSON_OBJECT, null,
						"text/names/old_norse", "descriptor.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("text/old_norse", "given_names", COPY_ORIGINAL_FILES, CSV, null,
						"text/names/old_norse", "given_names.csv", CSV, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("text/settlement", "descriptor", SINGLE_FILE, JSON_OBJECT, null,
						"text/names/settlement", "descriptor.json", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),

				////////// tilesets //////////
				def("tilesets", "entities", SPECIAL, PACKR_ATLAS_PLUS_NORMALS, null,
						"entities", "**/*![-swatch].png", PNG_PLUS_NORMALS, ADDITIVE,
						TexturePackerProcessor.class, UniqueFilenames.class, ReferencedByAssetDescriptor.class, WarnOnMissingNormals.class),
				def("tilesets", "terrain", SPECIAL, PACKR_ATLAS_PLUS_NORMALS, null,
						"terrain", "**/*.png", PNG_PLUS_NORMALS, ADDITIVE,
						TexturePackerProcessor.class, UniqueFilenames.class, ReferencedByTilesetDefinition.class, WarnOnMissingNormals.class),
				def("tilesets", "gui", SPECIAL, PACKR_ATLAS, null,
						"icons", "**/*.png", PNG, ADDITIVE, TexturePackerProcessor.class, UniqueFilenames.class),
				def("tilesets", "masks", SPECIAL, PACKR_ATLAS, null,
						"masks", "**/*.png", PNG, ADDITIVE, TexturePackerProcessor.class, UniqueFilenames.class),

				////////// translations //////////
				def("translations", "languages", SINGLE_FILE, JSON_ARRAY, LanguageType.class,
						"translations", "languages.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("translations", "collated", SPECIAL, CSV, null,
						"translations", "*.csv", CSV, ADDITIVE, LanguagesCsvProcessor.class),


				////////// ui //////////
				// TODO MODDING this also wants some work
				def("ui/cursors", null, COPY_ORIGINAL_FILES, PNG, null,
						"ui/cursors", "*.png", PNG, ADDITIVE, CopyFilesProcessor.class),
				def("ui/fonts", null, COPY_ORIGINAL_FILES, TTF, null,
						"ui/fonts", "*.[ot]tf", TTF, ADDITIVE, CopyFilesProcessor.class),
				// TODO MODDING add in skin
				def("ui/notifications", null, COPY_ORIGINAL_FILES, PNG, null,
						"ui/notifications", "*.png", PNG, ADDITIVE, CopyFilesProcessor.class),
				def("ui", "hints", SINGLE_FILE, JSON_ARRAY, Hint.class,
						"ui/hints", "*.json", JSON_ARRAY, ADDITIVE, GenericClassTypeProcessor.class),
				def("ui", "minimapSelection", SINGLE_FILE, PNG, null,
						"ui", "minimapSelection", PNG, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("ui", "uiSettings", SINGLE_FILE, JSON_OBJECT, null,
						"ui", "uiSettings", JSON_OBJECT, REPLACES_EXISTING, UntypedJsonProcessor.class),

				////////// water //////////
				def("water", "water", SINGLE_FILE, PNG, null,
						"water", "water.png", PNG, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("water", "water_flow_fragment_shader", SINGLE_FILE, GLSL, null,
						"water", "water_flow_fragment_shader.glsl", GLSL, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("water", "water_flow_vertex_shader", SINGLE_FILE, GLSL, null,
						"water", "water_flow_vertex_shader.glsl", GLSL, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("water", "water_NORMALS", SINGLE_FILE, PNG, null,
						"water", "water_NORMALS.png", PNG, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("water", "wave_mask", SINGLE_FILE, PNG, null,
						"water", "wave_mask.png", PNG, REPLACES_EXISTING, CopyFilesProcessor.class)

		);

		for (ModArtifactDefinition artifactDef : allArtifacts) {
			byName.put(artifactDef.getName(), artifactDef);
		}

		byName.get("tilesets/entities.atlas").setPackJsonPath("entities/pack.json");
		byName.get("tilesets/terrain.atlas").setPackJsonPath("terrain/pack.json");
		byName.get("tilesets/gui.atlas").setPackJsonPath("icons/pack.json");
		byName.get("tilesets/masks.atlas").setPackJsonPath("terrain/pack.json");
	}

	private ModArtifactDefinition def(String assetDir, String outputFileName, ModArtifactDefinition.OutputType outputType, ModArtifactDefinition.ModFileType modFileType,
									  Class<?> classType, String modDir, String inputFileNameMatcher, ModArtifactDefinition.ModFileType inputFileType,
									  ModArtifactDefinition.ArtifactCombinationType combinationType,
									  Class<? extends ModArtifactProcessor> processor, Class<? extends ModArtifactValidator>... validators) {
		return new ModArtifactDefinition(assetDir, outputFileName, outputType, modFileType, classType,
				modDir, inputFileNameMatcher, inputFileType, combinationType, processor, validators);
	}

	public List<ModArtifactDefinition> getAll() {
		return allArtifacts;
	}

	public ModArtifactDefinition getByName(String name) {
		return byName.get(name);
	}
}
