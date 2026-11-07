/* Standard-JDK test entry point. Swing timers must not keep a completed run alive. */
public class AllTests {
    public static void main(String[] args) {
        try {
            TroopBuyingTest.main(args);
            PlacementEditingTest.main(args);
            TeamIntegrationTest.main(args);
            GameOverTest.main(args);
            TileEffectVisitorTest.main(args);
            HillRangeTest.main(args);
            SpawnStrategyTest.main(args);
            TileEffectSummaryTest.main(args);
            ArmyHealthTest.main(args);
            HillDetourTest.main(args);
            System.out.println("All 10 test suites passed.");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
