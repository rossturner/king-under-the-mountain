package technology.rocketjump.undermount.entities.components.humanoid;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.CARRIED_DEAD_BODY;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.SAW_DEAD_BODY;

@RunWith(MockitoJUnitRunner.class)
public class HappinessComponentTest {

	private HappinessComponent happinessComponent;
	private GameContext gameContext;
	@Mock
	private GameClock mockClock;

	@Before
	public void setup() {
		happinessComponent = new HappinessComponent();

		gameContext = new GameContext();
		gameContext.setGameClock(mockClock);
	}

	@Test
	public void add_modifier_replaces_other() {
		happinessComponent.add(SAW_DEAD_BODY);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(-50);
		assertThat(happinessComponent.currentModifiers()).contains(SAW_DEAD_BODY);

		happinessComponent.add(CARRIED_DEAD_BODY);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(-55);
		assertThat(happinessComponent.currentModifiers()).hasSize(1);
	}

	@Test
	public void replaceBy_blocks_addition() {
		happinessComponent.add(CARRIED_DEAD_BODY);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(-55);

		happinessComponent.add(SAW_DEAD_BODY);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(-55);
		assertThat(happinessComponent.currentModifiers()).hasSize(1);
	}

}