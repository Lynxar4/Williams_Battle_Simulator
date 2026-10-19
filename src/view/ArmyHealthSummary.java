package view;

public class ArmyHealthSummary {

    private int healthy;
    private int wounded;
    private int critical;
    private int dead;
    private int totalHealth;
    private int troopCount;

    public void addHealthy(int health) {
        healthy++;
        addHealth(health);
    }

    public void addWounded(int health) {
        wounded++;
        addHealth(health);
    }

    public void addCritical(int health) {
        critical++;
        addHealth(health);
    }

    public void addDead() {
        dead++;
        troopCount++;
    }

    private void addHealth(int health) {
        totalHealth += health;
        troopCount++;
    }

    public void combine(ArmyHealthSummary other) {
        healthy += other.healthy;
        wounded += other.wounded;
        critical += other.critical;
        dead += other.dead;
        totalHealth += other.totalHealth;
        troopCount += other.troopCount;
    }

    public int getHealthy() {
        return healthy;
    }

    public int getWounded() {
        return wounded;
    }

    public int getCritical() {
        return critical;
    }

    public int getDead() {
        return dead;
    }

    public double getAverageHealth() {
        if (troopCount == 0) {
            return 0;
        }

        return (double) totalHealth / troopCount;
    }
}