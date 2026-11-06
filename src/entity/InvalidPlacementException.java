package entity;

// Thrown when a troop purchase or tile placement is rejected.

// Checked rather than unchecked on purpose: an illegal placement can be
// a player misclick rather than a programmer error, so the caller
// is expected to catch it and report it.
public class InvalidPlacementException extends Exception {
    /* ---- CONSTRUCTORS ---- */
    public InvalidPlacementException(String message) {
        super(message);
    }
}