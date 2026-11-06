package view;

import entity.Team;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/* Owns one worker at a time; newer requests replace any waiting snapshot. */
public final class ArmyHealthMonitor {
    /* ---- RECORDS ---- */
    public record Summaries(ArmyHealthSummary teamA, ArmyHealthSummary teamB) { }

    private record Request(long revision, List<GroupHealthSnapshot> teamA, List<GroupHealthSnapshot> teamB) { }

    /* ---- PRIVATE VARIABLES ---- */
    private final Consumer<Summaries> onReady;
    private SwingWorker<Summaries, Void> worker;
    private Request pending;
    private long revision;

    /* ---- CONSTRUCTORS ---- */
    public ArmyHealthMonitor(Consumer<Summaries> onReady) {
        this.onReady = onReady;
    }

    /* ---- PUBLIC METHODS ---- */
    public void request(Team teamA, Team teamB) {
        requireEventThread();
        // Copy primitive fields, not references to mutable Troop objects.
        pending = new Request(++revision,
                teamA.getArmy().stream().map(GroupHealthSnapshot::from).toList(),
                teamB.getArmy().stream().map(GroupHealthSnapshot::from).toList());
        if (worker == null) startNext();
    }

    public void reset() {
        requireEventThread();
        revision++;
        pending = null;
        // An older worker may finish, but its revision can no longer update the UI.
    }

    /* ---- PRIVATE METHODS ---- */
    private void startNext() {
        Request request = pending;
        pending = null;
        worker = new SwingWorker<>() {
            @Override
            protected Summaries doInBackground() {
                return new Summaries(
                        request.teamA().parallelStream().collect(new ArmyHealthCollector()),
                        request.teamB().parallelStream().collect(new ArmyHealthCollector()));
            }

            @Override
            protected void done() {
                try {
                    Summaries result = get();
                    if (request.revision() == revision) onReady.accept(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Could not calculate army health.", e.getCause());
                } finally {
                    worker = null;
                    if (pending != null) startNext();
                }
            }
        };
        worker.execute();
    }

    private static void requireEventThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Capture health and update the display on the Swing event thread.");
        }
    }
}
