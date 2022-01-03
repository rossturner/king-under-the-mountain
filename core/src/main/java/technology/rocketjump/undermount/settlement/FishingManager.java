package technology.rocketjump.undermount.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.constants.SettlementConstants;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.settlement.notifications.NotificationType;
import technology.rocketjump.undermount.zones.Zone;
import technology.rocketjump.undermount.zones.ZoneClassification;
import technology.rocketjump.undermount.zones.ZoneTile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static technology.rocketjump.undermount.jobs.LiquidMessageHandler.pickTileInZone;

@Singleton
public class FishingManager implements Updatable, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final SettlerTracker settlerTracker;
	private final SettlementConstants settlementConstants;
	private final JobStore jobStore;
	private final JobType fishingJobType;
	private final Profession fishingProfession;
	private GameContext gameContext;

	private float timeSinceLastUpdate;

	@Inject
	public FishingManager(MessageDispatcher messageDispatcher, SettlerTracker settlerTracker, ConstantsRepo constantsRepo,
						  JobStore jobStore, JobTypeDictionary jobTypeDictionary, ProfessionDictionary professionDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.settlerTracker = settlerTracker;
		this.settlementConstants = constantsRepo.getSettlementConstants();
		this.jobStore = jobStore;
		this.fishingJobType = jobTypeDictionary.getByName(settlementConstants.getFishingJobType());
		this.fishingProfession = professionDictionary.getByName("FISHER");

		messageDispatcher.addListener(this, MessageType.YEAR_ELAPSED);
		messageDispatcher.addListener(this, MessageType.FISH_HARVESTED_FROM_RIVER);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.YEAR_ELAPSED: {
				cancelAllOutstandingFishingJobs();
				gameContext.getSettlementState().setFishRemainingInRiver(settlementConstants.getNumAnnualFish());
				return true;
			}
			case MessageType.FISH_HARVESTED_FROM_RIVER: {
				gameContext.getSettlementState().setFishRemainingInRiver(gameContext.getSettlementState().getFishRemainingInRiver() - 1);
				if (gameContext.getSettlementState().getFishRemainingInRiver() <= 0) {
					cancelAllOutstandingFishingJobs();
					messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, new Notification(NotificationType.FISH_EXHAUSTED, null));
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > 3.471f) {
			timeSinceLastUpdate = 0f;
			doUpdate();
		}
	}

	private void doUpdate() {
		if (gameContext.getSettlementState().getFishRemainingInRiver() <= 0) {
			return;
		}

		Entity fisherSettler = null;
		for (Entity entity : settlerTracker.getAll()) {
			ProfessionsComponent professionsComponent = entity.getComponent(ProfessionsComponent.class);
			if (professionsComponent != null) {
				if (professionsComponent.hasActiveProfession(fishingProfession)) {
					fisherSettler = entity;
					break;
				}
			}
		}

		if (fisherSettler != null) {
			MapTile location = gameContext.getAreaMap().getTile(fisherSettler.getLocationComponent().getWorldOrParentPosition());
			int regionId = location.getRegionId();

			List<Zone> zonesInRegion = new ArrayList<>(gameContext.getAreaMap().getZonesInRegion(regionId));
			Collections.shuffle(zonesInRegion, gameContext.getRandom());
			for (Zone zone : zonesInRegion) {
				if (zone.getClassification().getZoneType().equals(ZoneClassification.ZoneType.LIQUID_SOURCE) &&
					zone.isActive() && !zone.getClassification().isConstructed()) {
					// Is a natural active liquid source

					boolean zoneHasExistingFishingJob = false;
					Iterator<ZoneTile> iterator = zone.iterator();
					while (iterator.hasNext()) {
						ZoneTile tile = iterator.next();
						zoneHasExistingFishingJob = jobStore.getJobsAtLocation(tile.getTargetTile()).stream()
								.anyMatch(j-> j.getType().equals(fishingJobType));
						if (zoneHasExistingFishingJob) {
							break;
						}
					}

					if (!zoneHasExistingFishingJob) {
						ZoneTile zoneTile = pickTileInZone(zone, gameContext.getRandom(), gameContext.getAreaMap());
						if (zoneTile != null) {
							createFishingJob(zoneTile);
							break;
						}
					}

				}
			}
		}
	}

	private void createFishingJob(ZoneTile zoneTile) {
		Job fishingJob = new Job(fishingJobType);
		fishingJob.setJobLocation(zoneTile.getTargetTile());
		fishingJob.setSecondaryLocation(zoneTile.getAccessLocation());
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, fishingJob);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}


	private void cancelAllOutstandingFishingJobs() {
		List<Job> fishingJobs = new ArrayList<>(jobStore.getByType(fishingJobType));
		for (Job fishingJob : fishingJobs) {
			messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, fishingJob);
		}
	}
}
