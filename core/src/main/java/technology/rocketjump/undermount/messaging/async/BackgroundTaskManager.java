package technology.rocketjump.undermount.messaging.async;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.planning.PathfindingTask;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.PathfindingRequestMessage;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

@Singleton
public class BackgroundTaskManager implements Telegraph {

	private ExecutorService executorService;
	private final MessageDispatcher messageDispatcher;
	private final int numberOtherCores;
	private Queue<Future<BackgroundTaskResult>> outstandingTasks = new ConcurrentLinkedQueue<>();

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
				Future<BackgroundTaskResult> task = executorService.submit(new PathfindingTask(message));
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

	public Future<BackgroundTaskResult> runTask(Callable<BackgroundTaskResult> runnable) {
		Future<BackgroundTaskResult> task = executorService.submit(runnable);
		outstandingTasks.add(task);
		return task;
	}

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
		Queue<Future<BackgroundTaskResult>> newOutstandingTasks = new ConcurrentLinkedQueue<>();
		for (Future<BackgroundTaskResult> task : this.outstandingTasks) {
			if (!task.isDone()) {
				newOutstandingTasks.add(task);
			} else {
				try {
					BackgroundTaskResult result = task.get();
					if (result.isSuccessful()) {
						if (result.dispatchMessageOnSuccess) {
							messageDispatcher.dispatchMessage(result.successMessageType, result.successMessagePayload);
						}
					} else {
						messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, result.error);
					}
				} catch (InterruptedException| ExecutionException e) {
					Logger.error(e);
				}
			}
		}
		this.outstandingTasks = newOutstandingTasks;
	}
}
