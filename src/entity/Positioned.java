package entity;

/*Specifies the positioning of an entity*/
public interface Positioned {
    boolean isAt(int row, int column);

    int getRow();

    int getColumn();
}
