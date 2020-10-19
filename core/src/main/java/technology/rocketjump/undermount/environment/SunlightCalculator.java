package technology.rocketjump.undermount.environment;

import com.badlogic.gdx.graphics.Color;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.environment.model.SunlightPhase;
import technology.rocketjump.undermount.guice.SunlightCalculatorProvider;

import java.util.List;

@Singleton
@ProvidedBy(SunlightCalculatorProvider.class)
public class SunlightCalculator {

	private final List<SunlightPhase> sunlightPhases;

	public SunlightCalculator(List<SunlightPhase> sunlightPhases) {
		this.sunlightPhases = sunlightPhases;
	}

	public Color getSunlightColor(double gameTimeInHours) {
		SunlightPhase previousPhase = null;
		SunlightPhase nextPhase = null;

		for (SunlightPhase sunlightPhase : sunlightPhases) {
			if (sunlightPhase.getTime() < gameTimeInHours) {
				previousPhase = sunlightPhase;
			} else if (sunlightPhase.getTime() > gameTimeInHours) {
				nextPhase = sunlightPhase;
				break;
			}
		}

		// Figure out ratio between two phases
		double phaseLength = nextPhase.getTime() - previousPhase.getTime();
		double timeSincePreviousPhase = gameTimeInHours - previousPhase.getTime();
		double timeToNextPhase = nextPhase.getTime() - gameTimeInHours;

		float previousWeighting = (float) (1.0f - timeSincePreviousPhase / phaseLength);
		float nextPhaseWeighting = (float) (1.0f - timeToNextPhase / phaseLength);

		// Pick weighted average of next and previous color
		return new Color(
				(previousPhase.getColor().r * previousWeighting) + (nextPhase.getColor().r * nextPhaseWeighting),
				(previousPhase.getColor().g * previousWeighting) + (nextPhase.getColor().g * nextPhaseWeighting),
				(previousPhase.getColor().b * previousWeighting) + (nextPhase.getColor().b * nextPhaseWeighting),
				1.0f);
	}

}
