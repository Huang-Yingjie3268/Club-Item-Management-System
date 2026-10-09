import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Validated calendar date, retaining the course project's Day abstraction. */
public class Day implements Cloneable, Comparable<Day> {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter
            .ofPattern("dd-MMM-uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private LocalDate date;

    public Day(int year, int month, int day) {
        if (!valid(year, month, day)) {
            throw new IllegalArgumentException("Invalid calendar date.");
        }
        date = LocalDate.of(year, month, day);
    }

    protected Day(Day other) {
        date = other.date;
    }

    public Day(String text) {
        set(text);
    }

    /** Converts user input to the existing checked domain-exception hierarchy. */
    public static Day parse(String text) throws ExInvalidDate {
        try {
            return new Day(text);
        } catch (IllegalArgumentException e) {
            throw new ExInvalidDate(text);
        }
    }

    public final void set(String text) {
        if (text == null || !text.matches("\\d{2}-[A-Z][a-z]{2}-\\d{4}")) {
            throw new IllegalArgumentException("Expected DD-MMM-YYYY.");
        }
        try {
            LocalDate parsed = LocalDate.parse(text, FORMAT);
            if (parsed.getYear() < 1) {
                throw new IllegalArgumentException("Year must be positive.");
            }
            date = parsed; // Commit only after successful validation.
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid calendar date: " + text, e);
        }
    }

    public static boolean isLeapYear(int year) {
        return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0);
    }

    public static boolean valid(int year, int month, int day) {
        try {
            if (year < 1) {
                return false;
            }
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    public Day plusDays(int days) {
        LocalDate next = date.plusDays(days);
        return new Day(next.getYear(), next.getMonthValue(), next.getDayOfMonth());
    }

    protected final void copyFrom(Day other) {
        date = other.date;
    }

    public int daysUntil(Day other) {
        return Math.toIntExact(ChronoUnit.DAYS.between(date, other.date));
    }

    @Override
    public int compareTo(Day other) {
        return date.compareTo(other.date);
    }

    @Override
    public String toString() {
        return date.format(FORMAT);
    }

    @Override
    public Day clone() {
        return new Day(this);
    }
}
