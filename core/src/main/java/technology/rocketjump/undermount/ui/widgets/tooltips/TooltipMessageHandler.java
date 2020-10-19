package technology.rocketjump.undermount.ui.widgets.tooltips;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.TooltipMessage;
import technology.rocketjump.undermount.ui.GuiContainer;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWordClass;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nTextWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static technology.rocketjump.undermount.ui.widgets.tooltips.Tooltip.TooltipState.DECAYING;
import static technology.rocketjump.undermount.ui.widgets.tooltips.Tooltip.TooltipState.DISPLAYED;

@Singleton
public class TooltipMessageHandler implements Updatable, Telegraph {

	private static final float TIME_BEFORE_SHOW_TOOLTIP = 1f;
	private static final float TIME_TO_DECAY = 0.5f;
	private static final int TOOLTIP_LINE_LENGTH = 60;
	private final Map<String, Tooltip> byI18nKey = new HashMap<>();
	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final GuiContainer guiContainer;

	@Inject
	public TooltipMessageHandler(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator, GuiContainer guiContainer) {
		this.skin = guiSkinRepository.getDefault();

		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.guiContainer = guiContainer;
		messageDispatcher.addListener(this, MessageType.TOOLTIP_AREA_ENTERED);
		messageDispatcher.addListener(this, MessageType.TOOLTIP_AREA_EXITED);
		messageDispatcher.addListener(this, MessageType.CLEAR_ALL_TOOLTIPS);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.TOOLTIP_AREA_ENTERED: {
				TooltipMessage message = (TooltipMessage) msg.extraInfo;
				tooltippableEntered(message.tooltipI18nKey, message.parent);
				return true;
			}
			case MessageType.TOOLTIP_AREA_EXITED: {
				TooltipMessage message = (TooltipMessage) msg.extraInfo;
				tooltippableExited(message.tooltipI18nKey, message.parent);
				return true;
			}
			case MessageType.CLEAR_ALL_TOOLTIPS: {
				for (Tooltip tooltip : new ArrayList<>(byI18nKey.values())) {
					this.remove(tooltip);
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void update(float deltaTime) {

		Vector2 cursorLocation = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		Vector2 stageCursorLocation = guiContainer.getPrimaryStage().getViewport().unproject(cursorLocation);

		for (String tooltipKey : new HashSet<>(byI18nKey.keySet())) {
			Tooltip tooltip = byI18nKey.get(tooltipKey);
			if (tooltip == null) {
				continue;
			}
			tooltip.incrementElapsedTime(deltaTime);

			switch (tooltip.getState()) {
				case PENDING:
					if (tooltip.getElapsedTime() >= TIME_BEFORE_SHOW_TOOLTIP) {
						display(tooltip);
					}
					break;
				case DISPLAYED:
					if (tooltip.getAlpha() < 1f) {
						float alphaDelta = (TIME_TO_DECAY / deltaTime);
						tooltip.setAlpha(tooltip.getAlpha() + alphaDelta);
					}
					break;
				case DECAYING:

					// Check if cursor over tooltip
					Vector2 tooltipLocal = tooltip.stageToLocalCoordinates(stageCursorLocation);
					if (tooltip.hit(tooltipLocal.x, tooltipLocal.y, false) != null) {
						// Reset state to reset decay time
						tooltip.setState(DECAYING);
						// Increase alpha if necessary
						if (tooltip.getAlpha() < 1f) {
							float alphaDelta = (TIME_TO_DECAY / deltaTime);
							tooltip.setAlpha(tooltip.getAlpha() + alphaDelta);
						}
					} else {

						float opacity = (TIME_TO_DECAY - tooltip.getElapsedTime()) / TIME_TO_DECAY;
						opacity = Math.max(opacity, 0f);
						if (opacity > 0f) {
							tooltip.setAlpha(opacity);
						} else {
							remove(tooltip);
						}
					}

					break;
			}
		}
	}

	public void tooltippableEntered(String tooltipI18nKey, Actor hoveredActor) {
		if (!byI18nKey.containsKey(tooltipI18nKey)) {
			I18nText text = i18nTranslator.getTranslatedString(tooltipI18nKey, I18nWordClass.TOOLTIP);
			text.breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength());
			I18nTextWidget label = new I18nTextWidget(text, skin, messageDispatcher);
			Tooltip newTooltip = new Tooltip(tooltipI18nKey, label, skin, hoveredActor);
			byI18nKey.put(newTooltip.getI18nKey(), newTooltip);
		}
	}

	public void tooltippableExited(String tooltipI18nKey, Actor hoveredActor) {
		Tooltip tooltip = byI18nKey.get(tooltipI18nKey);
		if (tooltip != null) {
			tooltip.setState(DECAYING);
		}
	}

	public Tooltip getTooltip(String tooltipI18nKey) {
		if (tooltipI18nKey == null) {
			return null;
		}
		return null;
	}

	private void display(Tooltip tooltip) {

		Vector2 cursorLocation = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		cursorLocation.add(5, -10);
		// TODO figure out if tooltip should be rearranged due to collision with edge of viewport
		Vector2 stageLocation = guiContainer.getPrimaryStage().getViewport().unproject(cursorLocation);
		tooltip.setPosition(stageLocation.x, stageLocation.y);

		guiContainer.showTooltip(tooltip);
		tooltip.setState(DISPLAYED);
	}

	private void remove(Tooltip tooltip) {
		tooltip.remove();
		byI18nKey.remove(tooltip.getI18nKey());
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {

	}

	@Override
	public void clearContextRelatedState() {
		for (Tooltip tooltip : new ArrayList<>(byI18nKey.values())) {
			remove(tooltip);
		}
	}

}
