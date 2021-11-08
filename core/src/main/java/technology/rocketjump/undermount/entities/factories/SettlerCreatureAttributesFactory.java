package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.misc.twitch.TwitchDataStore;
import technology.rocketjump.undermount.misc.twitch.model.TwitchViewer;
import technology.rocketjump.undermount.persistence.UserPreferences;

import java.util.Random;

@Singleton
public class SettlerCreatureAttributesFactory {

	private final DwarvenNameGenerator nameGenerator;
	private final UserPreferences userPreferences;
	private final TwitchDataStore twitchDataStore;
	private final Random random = new RandomXS128();
	private final Race dwarfRace;

	@Inject
	public SettlerCreatureAttributesFactory(DwarvenNameGenerator nameGenerator,
											UserPreferences userPreferences, TwitchDataStore twitchDataStore,
											GameMaterialDictionary gameMaterialDictionary, RaceDictionary raceDictionary) {
		this.nameGenerator = nameGenerator;
		this.userPreferences = userPreferences;
		this.twitchDataStore = twitchDataStore;

		this.dwarfRace = raceDictionary.getByName("Dwarf");
	}

	public CreatureEntityAttributes create(GameContext gameContext) {
		CreatureEntityAttributes attributes = new CreatureEntityAttributes(dwarfRace, random.nextLong());

		if (twitchSettlerNameReplacementsEnabled()) {
			for (TwitchViewer twitchViewer : twitchDataStore.getPrioritisedViewers()) {
				if (!gameContext.getSettlementState().usedTwitchViewers.contains(twitchViewer)) {
					attributes.setName(twitchViewer.toName());
					gameContext.getSettlementState().usedTwitchViewers.add(twitchViewer);
					break;
				}
			}
		}

		if (attributes.getName() == null) {
			attributes.setName(nameGenerator.create(attributes.getSeed(), attributes.getGender()));
		}

		return attributes;
	}

	private boolean twitchSettlerNameReplacementsEnabled() {
		return Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED, "false")) &&
				Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_VIEWERS_AS_SETTLER_NAMES, "false"));
	}

}
