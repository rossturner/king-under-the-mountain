package technology.rocketjump.undermount.entities.components.humanoid;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.jobs.model.Profession;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ProfessionsComponentTest {

	@Mock
	private Profession mockProfA;
	@Mock
	private Profession mockProfB;
	@Mock
	private Profession mockProfC;

	@Test
	public void hasAnyActiveProfession() {
		ProfessionsComponent professionsComponent = new ProfessionsComponent();
		professionsComponent.add(mockProfA, 0.5f);
		professionsComponent.add(mockProfB, 0.3f);

		assertThat(professionsComponent.hasAnyActiveProfession(Sets.newHashSet(mockProfA))).isTrue();
		assertThat(professionsComponent.hasAnyActiveProfession(Sets.newHashSet(mockProfC))).isFalse();
	}
}