package technology.rocketjump.undermount.entities.model.physical.plant;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PlantSpeciesDictionary {

	public static final String COLOR_SWATCH_ASSET_PATH = "assets/definitions/plantColorSwatches/";
	private final GameMaterialDictionary materialDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private Map<String, PlantSpecies> byName = new HashMap<>();
	private Map<PlantSpeciesType, List<PlantSpecies>> bySpeciesType = new HashMap<>();
	private Map<GameMaterial, PlantSpecies> bySeedMaterial = new HashMap<>();
	private List<PlantSpecies> allSpeciesList = new ArrayList<>();

	@Inject
	public PlantSpeciesDictionary(GameMaterialDictionary materialDictionary, ItemTypeDictionary itemTypeDictionary) throws IOException {
		this.materialDictionary = materialDictionary;
		this.itemTypeDictionary = itemTypeDictionary;

		ObjectMapper objectMapper = new ObjectMapper();
		File speciesJsonFile = new File("assets/definitions/types/plantSpecies.json");
		List<PlantSpecies> speciesList = objectMapper.readValue(FileUtils.readFileToString(speciesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, PlantSpecies.class));

		for (PlantSpeciesType plantSpeciesType : PlantSpeciesType.values()) {
			bySpeciesType.put(plantSpeciesType, new ArrayList<>());
		}

		for (PlantSpecies plantSpecies : speciesList) {
			initialiseTransientFields(plantSpecies);
			byName.put(plantSpecies.getSpeciesName(), plantSpecies);
			bySpeciesType.get(plantSpecies.getPlantType()).add(plantSpecies);
			allSpeciesList.add(plantSpecies);

			if (plantSpecies.getSeed() != null) {
				GameMaterial seedMaterial = plantSpecies.getSeed().getSeedMaterial();
				if (bySeedMaterial.containsKey(seedMaterial)) {
					Logger.warn("Duplicate plant species with same seed material : " + bySeedMaterial.get(seedMaterial).getSpeciesName() + " and " + plantSpecies.getSpeciesName());
				} else {
					bySeedMaterial.put(seedMaterial, plantSpecies);
				}
			}
		}
	}

	public PlantSpecies getByName(String speciesName) {
		return byName.get(speciesName);
	}

	public List<PlantSpecies> getAll() {
		return allSpeciesList;
	}

	public List<PlantSpecies> getBySpeciesType(PlantSpeciesType speciesType) {
		return bySpeciesType.get(speciesType);
	}

	public PlantSpecies getBySeedMaterial(GameMaterial seedMaterial) {
		return bySeedMaterial.get(seedMaterial);
	}

	private void initialiseTransientFields(PlantSpecies plantSpecies) {
		if (Gdx.graphics != null) { // This should only be null in headless tests
			for (PlantSpeciesColor plantSpeciesColor : plantSpecies.getAllColorObjects()) {
				plantSpeciesColor.setSpecificColor(HexColors.get(plantSpeciesColor.getColorCode()));

				if (plantSpeciesColor.getSwatch() != null) {
					Pixmap swatchPixmap = new Pixmap(new FileHandle(COLOR_SWATCH_ASSET_PATH + plantSpeciesColor.getSwatch()));
					for (int y = 0; y < swatchPixmap.getHeight(); y++) {
						for (int x = 0; x < swatchPixmap.getWidth(); x++) {
							plantSpeciesColor.getSwatchColors().add(new Color(swatchPixmap.getPixel(x, y)));
						}
					}
					swatchPixmap.dispose();
				}

				if (plantSpeciesColor.getTransitionSwatch() != null) {
					Pixmap transitionPixmap = new Pixmap(new FileHandle(COLOR_SWATCH_ASSET_PATH + plantSpeciesColor.getTransitionSwatch()));

					for (int row = 0; row < transitionPixmap.getHeight(); row++) {
						Array<Color> colorRow = new Array<>();
						for (int index = 0; index < transitionPixmap.getWidth(); index++) {
							colorRow.add(new Color(transitionPixmap.getPixel(index, row)));
						}
						plantSpeciesColor.getTransitionColors().add(colorRow);
					}
					transitionPixmap.dispose();
				}
			}
		}

		GameMaterial mainMaterial = materialDictionary.getByName(plantSpecies.getMaterialName());
		if (mainMaterial == null) {
			Logger.error("Could not find material " + plantSpecies.getMaterialName() + " defined in " + plantSpecies.getSpeciesName());
		} else {
			plantSpecies.setMaterial(mainMaterial);
			plantSpecies.setRepresentationColor(plantSpecies.getMaterial().getColor());
		}

		if (plantSpecies.getRepresentativeColor() != null) {
			plantSpecies.setRepresentationColor(HexColors.get(plantSpecies.getRepresentativeColor()));
		}

		for (PlantSpeciesGrowthStage growthStage : plantSpecies.getGrowthStages()) {
			for (PlantSpeciesItem harvestedItem : growthStage.getHarvestedItems()) {
				ItemType itemType = itemTypeDictionary.getByName(harvestedItem.getItemTypeName());
				if (itemType == null) {
					Logger.error("Could not find item type " + harvestedItem.getItemTypeName() + " defined in " + plantSpecies.getSpeciesName());
				} else {
					harvestedItem.setItemType(itemType);
				}

				GameMaterial material = materialDictionary.getByName(harvestedItem.getMaterialName());
				if (material == null) {
					Logger.error("Could not find material " + harvestedItem.getMaterialName() + " defined in " + plantSpecies.getSpeciesName());
				} else {
					harvestedItem.setMaterial(material);
				}
			}
		}

		if (plantSpecies.getSeed() != null) {
			ItemType seedItemType = itemTypeDictionary.getByName(plantSpecies.getSeed().getItemTypeName());
			if (seedItemType == null) {
				Logger.error("Could not find seed item type '" + plantSpecies.getSeed().getItemTypeName() + "' defined in " + plantSpecies.getSpeciesName());
			} else {
				GameMaterial seedMaterial = materialDictionary.getByName(plantSpecies.getSeed().getMaterialName());
				if (seedMaterial == null) {
					Logger.error("Could not find seed material '" + plantSpecies.getSeed().getMaterialName() + "' defined in " + plantSpecies.getSpeciesName());
				} else {
					plantSpecies.getSeed().setSeedMaterial(seedMaterial);
					plantSpecies.getSeed().setSeedItemType(seedItemType);
				}
			}
		}
	}

}
