package technology.rocketjump.undermount.entities.behaviour.furniture;

import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.util.List;

public interface SelectableDescription {

	List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext);

}
