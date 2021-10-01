package technology.rocketjump.undermount.entities.model.physical.humanoid.body;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class Body implements ChildPersistable {

	private BodyStructure bodyStructure;
	private List<DamagedBodyPart> damagedBodyParts = new ArrayList<>();

	public Body() {

	}

	public Body(BodyStructure bodyStructure) {
		this.bodyStructure = bodyStructure;
	}

	public BodyPart randomlySelectPartBasedOnSize(Random random) {
		List<BodyPart> allBodyParts = new ArrayList<>();
		addBodyParts(bodyStructure.getRootPart(), null, allBodyParts);

		float totalSize = 0f;
		for (BodyPart bodyPart : allBodyParts) {
			totalSize += bodyPart.getPartDefinition().getSize();
		}

		float roll = random.nextFloat() * totalSize;
		BodyPart selected = null;
		for (BodyPart bodyPart : allBodyParts) {
			selected = bodyPart;
			roll -= bodyPart.getPartDefinition().getSize();
			if (roll <= 0) {
				break;
			}
		}
		return selected;
	}

	private void addBodyParts(BodyPartDefinition partDefinition, BodyPartDiscriminator discriminator, List<BodyPart> allBodyParts) {
		if (partDefinition == null) {
			Logger.error("Null body part definition");
			return;
		}
		Optional<DamagedBodyPart> damagedBodyPart = damagedBodyParts.stream()
				.filter(d -> d.getBodyPart().getPartDefinition().equals(partDefinition) && d.getBodyPart().getDiscriminator() == discriminator)
				.findAny();
		if (damagedBodyPart.isPresent() && damagedBodyPart.get().getDamage().equals(BodyPartDamage.Destroyed)) {
			// this part has been destroyed
			return;
		}


		allBodyParts.add(new BodyPart(partDefinition, discriminator));

		for (String childPartName : partDefinition.getChildParts()) {
			String[] split = childPartName.split("-");
			BodyPartDiscriminator childDiscriminator = null;
			if (split.length > 1) {
				childDiscriminator = EnumUtils.getEnum(BodyPartDiscriminator.class, split[0]);
				childPartName = split[1];
			}
			BodyPartDefinition childPartDefinition = bodyStructure.getPartDefinitionByName(childPartName).orElse(null);
			if (childDiscriminator == null) {
				childDiscriminator = discriminator;
			}
			addBodyParts(childPartDefinition, childDiscriminator, allBodyParts);
		}
	}

	public BodyStructure getBodyStructure() {
		return bodyStructure;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("bodyStructure", bodyStructure.getName());

		if (!damagedBodyParts.isEmpty()) {
			JSONArray damagedPartsJson = new JSONArray();
			for (DamagedBodyPart damagedBodyPart : damagedBodyParts) {
				JSONObject damagedPartJson = new JSONObject(true);
				throw new RuntimeException("Must self-persist child parts as this has the body structure information");
//				damagedPartsJson.add(damagedPartJson);
			}
			asJson.put("damagedParts", damagedPartsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.bodyStructure = relatedStores.bodyStructureDictionary.getByName(asJson.getString("bodyStructure"));
		if (this.bodyStructure == null) {
			throw new InvalidSaveException("Could not find body structure with name " + asJson.getString("bodyStructure"));
		}
	}
}
