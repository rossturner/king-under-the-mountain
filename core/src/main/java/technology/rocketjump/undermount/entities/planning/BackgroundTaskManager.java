package technology.rocketjump.undermount.entities.planning;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.messaging.ErrorType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.PathfindingRequestMessage;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

@Singleton
public class BackgroundTaskManager implements Telegraph, Updatable {

	private ExecutorService executorService;
	private final MessageDispatcher messageDispatcher;
	private final int numberOtherCores;
	private Queue<Future<ErrorType>> outstandingTasks = new ConcurrentLinkedQueue<>();

	private float timeSinceLastUpdate = 0f;
	private static final float UPDATE_CYCLE_TIME_SECONDS = 1.0472f;

	@Inject
	public BackgroundTaskManager(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
		numberOtherCores = Runtime.getRuntime().availableProcessors() - 1;
		// PERF Check how this runs on a single core machine, could run Java on multicore giving it only 1 core to run on
		int threads = Math.max(2, numberOtherCores * 2);
		executorService = Executors.newFixedThreadPool(threads);

		messageDispatcher.addListener(this, MessageType.PATHFINDING_REQUEST);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PATHFINDING_REQUEST: {
				PathfindingRequestMessage message = (PathfindingRequestMessage) msg.extraInfo;
				Future<ErrorType> task = executorService.submit(new PathfindingTask(message));
				outstandingTasks.add(task);
				return true;
			}
		}
		return false;
	}

	public Future<?> postUntrackedRunnable(Runnable runnable) {
		return executorService.submit(runnable);
	}

	public <T> Future<T> postUntrackedCallable(Callable<T> callable) {
		return executorService.submit(callable);
	}

	public Future<ErrorType> runTask(Callable<ErrorType> runnable) {
		Future<ErrorType> task = executorService.submit(runnable);
		outstandingTasks.add(task);
		return task;
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;

		if (timeSinceLastUpdate > UPDATE_CYCLE_TIME_SECONDS) {
			clearCompletedTasks();
			timeSinceLastUpdate = 0f;
		}
	}

	public void waitForOutstandingTasks() {
		clearCompletedTasks();
		for (Future<?> outstandingTask : outstandingTasks) {
			try {
				outstandingTask.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

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
		timeSinceLastUpdate = 0f;
		outstandingTasks = new LinkedList<>();
		try {
			executorService.shutdown();
			executorService.awaitTermination(2L, TimeUnit.SECONDS);
			executorService = Executors.newFixedThreadPool(Math.max(2, numberOtherCores * 2));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void clearCompletedTasks() {
		Queue<Future<ErrorType>> newOutstandingTasks = new ConcurrentLinkedQueue<>();
		for (Future<ErrorType> task : this.outstandingTasks) {
			if (!task.isDone()) {
				newOutstandingTasks.add(task);
			} else {
				try {
					ErrorType errorType = task.get();
					if (errorType != null) {
						messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, errorType);
					}
				} catch (InterruptedException| ExecutionException e) {
					Logger.error(e);
				}
			}
		}
		this.outstandingTasks = newOutstandingTasks;
	}
}
