package entity;

// Thrown when a tile effect cannot legally be placed: off the grid, or on a
// cell that already holds something.

// Checked rather than unchecked on purpose: once PlaceState exists an illegal
// placement will be a player misclick, not a programmer error, so the caller
// is expected to catch it and report it.
public class InvalidPlacementException extends Exception
{
    public InvalidPlacementException(String message)
    {
        super(message);
    }
}