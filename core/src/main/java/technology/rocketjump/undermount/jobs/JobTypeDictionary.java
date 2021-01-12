package technology.rocketjump.undermount.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.CONTEXT_DEPENDENT_PROFESSION_REQUIRED;
import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

@Singleton
public class JobTypeDictionary {

	private final ItemTypeDictionary itemTypeDictionary;
	private final ProfessionDictionary professionDictionary;
	private final SoundAssetDictionary soundAssetDictionary;
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;

	private final Map<String, JobType> byName = new HashMap<>();

	@Inject
	public JobTypeDictionary(ItemTypeDictionary itemTypeDictionary, ProfessionDictionary professionDictionary,
							 SoundAssetDictionary soundAssetDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.professionDictionary = professionDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;

		File assetDefinitionsFile = new File("assets/definitions/types/jobTypes.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<JobType> jobTypeList = objectMapper.readValue(FileUtils.readFileToString(assetDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, JobType.class));

			for (JobType jobType : jobTypeList) {
				init(jobType);
				byName.put(jobType.getName(), jobType);
			}
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}

		for (PlantSpeciesType plantSpeciesType : PlantSpeciesType.values()) {
			plantSpeciesType.setRemovalJobType(byName.get(plantSpeciesType.removalJobTypeName));
			if (plantSpeciesType.getRemovalJobType() == null) {
				Logger.error("Could not find required removalJobType " + plantSpeciesType.removalJobTypeName + " for " + plantSpeciesType.name());
			}
		}

	}

	public JobType getByName(String jobTypeName) {
		return byName.get(jobTypeName);
	}

	public Collection<JobType> getAll() {
		return byName.values();
	}

	private void init(JobType jobType) {
		if (jobType.getRequiredProfessionName() != null) {
			if (jobType.getRequiredProfessionName().equals("CONTEXT_DEPENDENT_PROFESSION_REQUIRED")) {
				jobType.setRequiredProfession(CONTEXT_DEPENDENT_PROFESSION_REQUIRED);
			} else {
				jobType.setRequiredProfession(professionDictionary.getByName(jobType.getRequiredProfessionName()));
				if (jobType.getRequiredProfession() == null) {
					Logger.error("Could not profession with name " + jobType.getRequiredProfessionName() + " for job type " + jobType.getName());
				}
			}
		} else {
			jobType.setRequiredProfession(NULL_PROFESSION);
		}

		if (jobType.getRequiredItemTypeName() != null) {
			jobType.setRequiredItemType(itemTypeDictionary.getByName(jobType.getRequiredItemTypeName()));
			if (jobType.getRequiredItemType() == null) {
				Logger.error("Could not find item type with name " + jobType.getRequiredItemTypeName() + " for job type " + jobType.getName());
			}
		}
		if (jobType.getActiveSoundAssetName() != null) {
			jobType.setActiveSoundAsset(soundAssetDictionary.getByName(jobType.getActiveSoundAssetName()));
			if (jobType.getActiveSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + jobType.getActiveSoundAssetName() + " for job type " + jobType.getName());
			}
		}
		if (jobType.getOnCompletionSoundAssetName() != null) {
			jobType.setOnCompletionSoundAsset(soundAssetDictionary.getByName(jobType.getOnCompletionSoundAssetName()));
			if (jobType.getOnCompletionSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + jobType.getOnCompletionSoundAssetName() + " for job type " + jobType.getName());
			}
		}
		if (jobType.getWorkOnJobParticleEffectName() != null) {
			jobType.setWorkOnJobParticleEffectType(particleEffectTypeDictionary.getByName(jobType.getWorkOnJobParticleEffectName()));
			if (jobType.getWorkOnJobParticleEffectType() == null) {
				Logger.error("Could not find particle effect with name " + jobType.getWorkOnJobParticleEffectName() + " for job type " + jobType.getName());
			}
		}
	}
}
