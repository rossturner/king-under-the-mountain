package technology.rocketjump.undermount.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.rooms.components.RoomComponent;

import java.util.*;

import static java.util.Collections.EMPTY_LIST;

@Singleton
public class RoomStore implements Updatable, GameContextAware {

	private final Map<RoomType, List<Room>> byType = new HashMap<>();
	private final Map<String, Room> byName = new HashMap<>();
	private final List<Room> withBehaviour = new ArrayList<>();
	private final Map<Class<? extends RoomComponent>, List<Room>> byComponent = new HashMap<>();

	private final MessageDispatcher messageDispatcher;

	private int updateCursor = 0;
	private GameContext gameContext;

	@Inject
	public RoomStore(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	void add(Room room) {
		gameContext.getRooms().put(room.getRoomId(), room);
		byName.put(room.getRoomName(), room);

		List<Room> roomsByType = getByType(room.getRoomType());
		roomsByType.add(room);
		byType.put(room.getRoomType(), roomsByType);

		for (RoomComponent roomComponent : room.getAllComponents()) {
			List<Room> roomsWithComponentClass = byComponent.getOrDefault(roomComponent.getClass(), new ArrayList<>());
			roomsWithComponentClass.add(room);
			byComponent.put(roomComponent.getClass(), roomsWithComponentClass);
		}

		if (room.getBehaviourComponent() != null) {
			withBehaviour.add(room);
		}
	}

	public void rename(Room room, String newName) throws RoomNameCollisionException {
		if (byName.containsKey(newName)) {
			throw new RoomNameCollisionException("Room with that name already exists");
		}
		byName.remove(room.getRoomName());
		room.setRoomName(newName);
		room.setNameChangedByPlayer(true);
		byName.put(newName, room);
	}

	public void remove(Room room) {
		if (!gameContext.getRooms().containsKey(room.getRoomId())) {
			Logger.error("Attempting to remove " + room + " from " + this.getClass().getSimpleName() + " which can not be found");
			return;
		}
		gameContext.getRooms().remove(room.getRoomId());
		byName.remove(room.getRoomName());

		List<Room> roomsWithType = byType.get(room.getRoomType());
		roomsWithType.remove(room);
		if (roomsWithType.isEmpty()) {
			byType.remove(room.getRoomType());
		}

		for (RoomComponent roomComponent : room.getAllComponents()) {
			List<Room> roomsWithComponent = byComponent.get(roomComponent.getClass());
			roomsWithComponent.remove(room);
			if (roomsWithComponent.isEmpty()) {
				byComponent.remove(roomComponent.getClass());
			}
		}

		if (room.getBehaviourComponent() != null) {
			withBehaviour.remove(room);
		}
	}

	public Collection<Room> getAll() {
		if (gameContext == null) {
			return EMPTY_LIST;
		} else {
			return gameContext.getRooms().values();
		}
	}

	public Room getById(long roomId) {
		return gameContext.getRooms().get(roomId);
	}

	public Room getByName(String roomName) {
		return byName.get(roomName);
	}

	public void nameChanged(Room room, String originalName) {
		byName.remove(originalName);
		byName.put(room.getRoomName(), room);
	}

	public List<Room> getByComponent(Class<? extends RoomComponent> componentClass) {
		return byComponent.getOrDefault(componentClass, Collections.emptyList());
	}

	public List<Room> getByType(RoomType roomType) {
		return byType.getOrDefault(roomType, new ArrayList<>());
	}

	@Override
	public void update(float deltaTime) {
		updateCursor++;
		if (updateCursor >= withBehaviour.size()) {
			updateCursor = 0;
		}

		if (!withBehaviour.isEmpty()) {
			withBehaviour.get(updateCursor).getBehaviourComponent().infrequentUpdate(gameContext, messageDispatcher);
		}
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;

		for (Room room : gameContext.getRooms().values()) {
			add(room);
		}
	}

	@Override
	public void clearContextRelatedState() {
		byType.clear();
		byName.clear();
		withBehaviour.clear();
		byComponent.clear();
		updateCursor = 0;
	}

	public static class RoomNameCollisionException extends Exception {

		public RoomNameCollisionException(String message) {
			super(message);
		}
	}
}
