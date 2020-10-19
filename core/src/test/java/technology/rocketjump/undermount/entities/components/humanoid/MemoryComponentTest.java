package technology.rocketjump.undermount.entities.components.humanoid;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.environment.GameClock;

import java.util.Deque;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MemoryComponentTest {

	private MemoryComponent memoryComponent;
	@Mock
	private GameClock mockClock;

	@Before
	public void setUp() throws Exception {
		memoryComponent = new MemoryComponent();

		when(mockClock.getCurrentGameTime()).thenReturn(1.0);
	}

	@Test
	public void add_ignoresEqualMemories() throws Exception {
		Memory first = new Memory(MemoryType.LACKING_REQUIRED_ITEM, mockClock);
		Memory second = new Memory(MemoryType.LACKING_REQUIRED_ITEM, mockClock);

		memoryComponent.add(first, mockClock);
		memoryComponent.add(second, mockClock);

		Deque<Memory> shortTermMemories = memoryComponent.getShortTermMemories(mockClock);
		assertThat(shortTermMemories).hasSize(1);
		assertThat(shortTermMemories.getFirst()).isEqualTo(first);
	}

	@Test
	public void getShortTermMemories_removesExpiredMemories() {
		Memory first = new Memory(MemoryType.LACKING_REQUIRED_ITEM, mockClock);

		memoryComponent.add(first, mockClock);

		when(mockClock.getCurrentGameTime()).thenReturn(100.0);
		Memory second = new Memory(MemoryType.LACKING_REQUIRED_ITEM, mockClock);

		memoryComponent.add(second, mockClock);

		Deque<Memory> shortTermMemories = memoryComponent.getShortTermMemories(mockClock);
		assertThat(shortTermMemories).hasSize(1);
		assertThat(shortTermMemories.getFirst()).isEqualTo(second);

		when(mockClock.getCurrentGameTime()).thenReturn(200.0);
		shortTermMemories = memoryComponent.getShortTermMemories(mockClock);
		assertThat(shortTermMemories).isEmpty();
	}

}