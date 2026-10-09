/** Singleton for the current simulation date. */
public class SystemDate extends Day {
    private static SystemDate instance;

    private SystemDate(Day date) {
        super(date);
    }

    public static SystemDate getInstance() {
        if (instance == null) {
            throw new IllegalStateException("System date has not been initialized.");
        }
        return instance;
    }

    public static void setDate(String text) {
        setDate(new Day(text));
    }

    public static void setDate(Day date) {
        if (instance == null) {
            instance = new SystemDate(date);
        } else {
            instance.copyFrom(date);
        }
    }
}
