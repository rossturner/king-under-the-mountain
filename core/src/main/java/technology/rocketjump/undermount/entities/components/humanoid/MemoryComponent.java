package technology.rocketjump.undermount.entities.components.humanoid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MemoryComponent implements EntityComponent {

	private static final int SHORT_TERM_MEMORY_LIMIT = 8;

	// FIFO queue for short term memory
	// May be no real purpose to separating short term and long term memories
	public Deque<Memory> shortTermMemory = new ArrayDeque<>();
	public List<Memory> longTermMemory = new ArrayList<>();

	public void addShortTerm(Memory memory, GameClock gameClock) {
		purgeExpiredMemories(gameClock);
		if (shortTermMemory.stream().noneMatch(m -> m.getType().equals(memory.getType()))) {
			shortTermMemory.addFirst(memory);
			while (shortTermMemory.size() > SHORT_TERM_MEMORY_LIMIT) {
				shortTermMemory.removeLast();
			}
		}
	}

	public void addLongTerm(Memory memory) {
		longTermMemory.add(memory);
	}

	public void removeByType(MemoryType memoryType) {
		shortTermMemory.removeIf(m -> m.getType().equals(memoryType));
		longTermMemory.removeIf(m -> m.getType().equals(memoryType));
	}

	public Deque<Memory> getShortTermMemories(GameClock gameClock) {
		purgeExpiredMemories(gameClock);
		return shortTermMemory;
	}

	public List<Memory> getLongTermMemories() {
		return longTermMemory;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		MemoryComponent cloned = new MemoryComponent();
		cloned.shortTermMemory = new ArrayDeque<>(this.shortTermMemory);
		cloned.longTermMemory = new ArrayList<>(this.longTermMemory);
		return cloned;
	}

	private void purgeExpiredMemories(GameClock gameClock) {
		final double currentGameTime = gameClock.getCurrentGameTime();
		shortTermMemory.removeIf(memory -> memory.getExpirationTime() != null && currentGameTime > memory.getExpirationTime());
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!shortTermMemory.isEmpty()) {
			JSONArray memoriesJson = new JSONArray();
			for (Memory memory : shortTermMemory) {
				JSONObject memoryJson = new JSONObject(true);
				memory.writeTo(memoryJson, savedGameStateHolder);
				memoriesJson.add(memoryJson);
			}
			asJson.put("shortTermMemories", memoriesJson);
		}

		if (!longTermMemory.isEmpty()) {
			JSONArray memoriesJson = new JSONArray();
			for (Memory memory : longTermMemory) {
				JSONObject memoryJson = new JSONObject(true);
				memory.writeTo(memoryJson, savedGameStateHolder);
				memoriesJson.add(memoryJson);
			}
			asJson.put("longTermMemories", memoriesJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray memoriesJson = asJson.getJSONArray("shortTermMemories");
		if (memoriesJson != null) {
			for (int cursor = 0; cursor < memoriesJson.size(); cursor++) {
				Memory memory = new Memory();
				memory.readFrom(memoriesJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
				this.shortTermMemory.add(memory);
			}
		}

		memoriesJson = asJson.getJSONArray("longTermMemories");
		if (memoriesJson != null) {
			for (int cursor = 0; cursor < memoriesJson.size(); cursor++) {
				Memory memory = new Memory();
				memory.readFrom(memoriesJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
				this.longTermMemory.add(memory);
			}
		}
	}
}
