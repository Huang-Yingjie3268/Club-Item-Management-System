public class ExInvalidDate extends ExClub {
    private static final long serialVersionUID = 1L;

    public ExInvalidDate(String date) {
        super("Invalid date: " + date + ". Expected a real date in DD-MMM-YYYY format (e.g. 03-Jan-2026).");
    }
}
