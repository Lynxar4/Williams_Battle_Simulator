package controller;

import entity.interactables.TileEffectEvent;
import entity.interactables.TileEffectListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// keeps the last few things that happened so the view can show them.
// reacts to the same events as budgetlistener without either one knowing the
// other exists.

public class EventLogListener implements TileEffectListener {
    /* ---- STATIC VARIABLES ---- */
    private static final int MAX_ENTRIES = 6;

    /* ---- PRIVATE VARIABLES ---- */
    private final Deque<String> entries = new ArrayDeque<>();

    /* ---- OVERRIDES ---- */
    @Override
    public void onTileEffectTriggered(TileEffectEvent event) {
        entries.addFirst(event.description());

        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    /* ---- GETTER METHODS ---- */
    // most recent first
    public List<String> getEntries() {
        return new ArrayList<>(entries);
    }

    public String getLatest() {
        return entries.isEmpty() ? "" : entries.getFirst();
    }
}
