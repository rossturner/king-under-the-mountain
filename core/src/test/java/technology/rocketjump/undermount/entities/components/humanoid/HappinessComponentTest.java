package technology.rocketjump.undermount.entities.components.humanoid;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.SLEPT_IN_ENCLOSED_BEDROOM;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.SLEPT_IN_SHARED_BEDROOM;

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
		happinessComponent.add(SLEPT_IN_ENCLOSED_BEDROOM);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(10);
		assertThat(happinessComponent.currentModifiers()).contains(SLEPT_IN_ENCLOSED_BEDROOM);

		happinessComponent.add(SLEPT_IN_SHARED_BEDROOM);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(-10);
		assertThat(happinessComponent.currentModifiers()).hasSize(1);
	}

	@Test
	public void replaceBy_blocks_addition() {
		happinessComponent.add(SLEPT_IN_SHARED_BEDROOM);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(-10);

		happinessComponent.add(SLEPT_IN_ENCLOSED_BEDROOM);

		assertThat(happinessComponent.getNetModifier()).isEqualTo(-10);
		assertThat(happinessComponent.currentModifiers()).hasSize(1);
	}

}