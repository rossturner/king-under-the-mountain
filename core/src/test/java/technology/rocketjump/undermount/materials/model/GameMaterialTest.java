package technology.rocketjump.undermount.materials.model;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.undermount.materials.model.GameMaterialType.STONE;

@RunWith(MockitoJUnitRunner.class)
public class GameMaterialTest extends PersistenceTestHarness {

	@Test
	public void testPersistence() throws InvalidSaveException {
		when(dictionaries.gameMaterialDictionary.getById(NULL_MATERIAL.getMaterialId())).thenReturn(NULL_MATERIAL);

		GameMaterial testMaterial = new GameMaterial(
				"dynamicMaterialId", "materialName", 7L, STONE,
				Color.BLUE, true, false, true, true, false, Sets.newHashSet(NULL_MATERIAL));
		testMaterial.writeTo(stateHolder);

		JSONObject materialJson = stateHolder.dynamicMaterialsJson.getJSONObject(0);

		GameMaterial loaded = new GameMaterial();
		loaded.readFrom(materialJson, stateHolder, dictionaries);

		assertThat(loaded.getDynamicMaterialId()).isEqualTo("dynamicMaterialId");
		assertThat(loaded.getMaterialName()).isEqualTo("materialName");
		assertThat(loaded.getMaterialId()).isEqualTo(7L);
		assertThat(loaded.getMaterialType()).isEqualTo(STONE);
		assertThat(loaded.getColor()).isEqualTo(Color.BLUE);
		assertThat(loaded.isAlcoholic()).isTrue();
		assertThat(loaded.isPoisonous()).isTrue();
		assertThat(loaded.isEdible()).isTrue();
		assertThat(loaded.getConstituentMaterials().iterator().next()).isEqualTo(NULL_MATERIAL);
	}

}