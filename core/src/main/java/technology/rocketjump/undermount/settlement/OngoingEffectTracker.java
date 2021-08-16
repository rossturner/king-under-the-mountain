package technology.rocketjump.undermount.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.effects.BaseOngoingEffectBehaviour;
import technology.rocketjump.undermount.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.OnFireStatus;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.settlement.notifications.NotificationType;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

@Singleton
public class OngoingEffectTracker implements GameContextAware {

	private final Map<OngoingEffectType, Map<Long, Entity>> byEffectType = new HashMap<>();
	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	@Inject
	public OngoingEffectTracker(MessageDispatcher messageDispatcher) {

		this.messageDispatcher = messageDispatcher;
	}

	public void entityAdded(Entity entity) {
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) entity.getPhysicalEntityComponent().getAttributes();
		BaseOngoingEffectBehaviour behaviour = (BaseOngoingEffectBehaviour) entity.getBehaviourComponent();

		Map<Long, Entity> entitiesForType = byEffectType.computeIfAbsent(attributes.getType(), a -> new HashMap<>());
		if (gameContext != null && entitiesForType.isEmpty() && attributes.getType().getTriggersNotification() != null && behaviour.shouldNotificationApply(gameContext)) {
			NotificationType notificationType = EnumUtils.getEnum(NotificationType.class, attributes.getType().getTriggersNotification());
			if (notificationType != null) {
				messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, new Notification(notificationType,
						entity.getLocationComponent().getWorldOrParentPosition()));
			} else {
				Logger.error("Could not find " + NotificationType.class.getSimpleName() + " with name " + attributes.getType().getTriggersNotification());
			}
		}
		entitiesForType.put(entity.getId(), entity);
	}

	public void entityRemoved(Entity entity) {
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) entity.getPhysicalEntityComponent().getAttributes();
		byEffectType.getOrDefault(attributes.getType(), emptyMap()).remove(entity.getId());
		if (byEffectType.get(attributes.getType()) != null && byEffectType.get(attributes.getType()).isEmpty()) {
			byEffectType.remove(attributes.getType());
		}

		if (entity.getBehaviourComponent() instanceof FireEffectBehaviour) {
			if (entity.getLocationComponent().getContainerEntity() != null) {
				StatusComponent statusComponent = entity.getLocationComponent().getContainerEntity().getComponent(StatusComponent.class);
				if (statusComponent != null) {
					statusComponent.remove(OnFireStatus.class);
				}
			}
			messageDispatcher.dispatchMessage(MessageType.FIRE_REMOVED, toGridPoint(entity.getLocationComponent().getWorldPosition()));
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		byEffectType.clear();
		this.gameContext = null;
	}

}
