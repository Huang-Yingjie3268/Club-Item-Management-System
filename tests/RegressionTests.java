import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Dependency-free regression suite. Each scenario runs in a fresh JVM. */
public class RegressionTests {
    private static final String[] SCENARIOS = {
        "history", "holds", "queues", "quotas", "validation", "dates", "redoFailure", "cli"
    };
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            for (String scenario : SCENARIOS) {
                Process process = javaProcess("RegressionTests", scenario).inheritIO().start();
                require(process.waitFor(30, TimeUnit.SECONDS), "Scenario timed out: " + scenario);
                require(process.exitValue() == 0, "Scenario failed: " + scenario);
            }
            System.out.println("ALL PASSED: " + SCENARIOS.length + " regression scenarios.");
            return;
        }
        SystemDate.setDate("01-Jan-2026");
        switch (args[0]) {
            case "history": history(); break;
            case "holds": holds(); break;
            case "queues": queues(); break;
            case "quotas": quotas(); break;
            case "validation": validation(); break;
            case "dates": dates(); break;
            case "redoFailure": redoFailure(); break;
            case "cli": cli(); break;
            default: throw new AssertionError("Unknown scenario");
        }
        System.out.println("PASSED " + args[0] + ": " + checks + " checks");
    }

    private static ProcessBuilder javaProcess(String main, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(main);
        command.addAll(Arrays.asList(arguments));
        return new ProcessBuilder(command).redirectErrorStream(true);
    }

    private static void execute(String command) throws ExClub {
        String[] parts = command.trim().split("\\s+");
        RecordedCommand action;
        switch (parts[0]) {
            case "register": action = new CmdRegister(); break;
            case "arrive": action = new CmdArrive(); break;
            case "checkout": action = new CmdCheckout(); break;
            case "checkin": action = new CmdCheckin(); break;
            case "request": action = new CmdRequest(); break;
            case "cancelRequest": action = new CmdCancelRequest(); break;
            case "startNewDay": action = new CmdStartNewDay(); break;
            default: throw new AssertionError("Unsupported test command");
        }
        action.execute(parts);
        invariants();
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends ExClub> type, String command) throws ExClub {
        String before = snapshot();
        try {
            execute(command);
            throw new AssertionError("Expected " + type.getSimpleName() + ": " + command);
        } catch (ExClub error) {
            require(type.isInstance(error), "Wrong exception: " + error);
        }
        require(before.equals(snapshot()), "Rejected command changed state: " + command);
    }

    private static String snapshot() {
        StringBuilder result = new StringBuilder(SystemDate.getInstance().toString());
        for (int index = 1; index <= 4; index++) {
            Member member = Club.getInstance().findMember("M" + index);
            if (member != null) {
                result.append('|').append(member.getId()).append(':').append(member.getJoinDate())
                        .append(':').append(member.getBorrowedCount()).append(':').append(member.getRequestedCount());
            }
        }
        for (Item item : Club.getInstance().getItems()) {
            result.append('|').append(item.getId()).append(':').append(item.getArrivalDate())
                    .append(':').append(Club.getInstance().getItemStatusString(item));
        }
        return result.toString();
    }

    /** Counts must equal actual loans and queued requests, including after history replay. */
    private static void invariants() {
        for (int index = 1; index <= 4; index++) {
            Member member = Club.getInstance().findMember("M" + index);
            if (member == null) {
                continue;
            }
            int borrowed = 0;
            int requested = 0;
            for (Item item : Club.getInstance().getItems()) {
                if (item.isBorrowed() && ((ItemStatusBorrowed) item.getStatus()).getBorrower() == member) {
                    borrowed++;
                }
                if (item.getQueue().contains(member)) {
                    requested++;
                }
            }
            require(member.getBorrowedCount() == borrowed, "Loan count disagrees for " + member.getId());
            require(member.getRequestedCount() == requested, "Request count disagrees for " + member.getId());
        }
    }

    private static void undo() {
        RecordedCommand.undoOneCommand();
        invariants();
    }

    private static void redo() {
        RecordedCommand.redoOneCommand();
        invariants();
    }

    private static void history() throws ExClub {
        String[] commands = {
            "register M1 Alice", "register M2 Bob", "register M3 Carol",
            "arrive I1 Book", "arrive I2 Laptop", "checkout M1 I1",
            "request M2 I1", "request M3 I1", "cancelRequest M3 I1",
            "request M3 I1", "checkin M1 I1", "startNewDay 04-Jan-2026",
            "checkout M2 I1", "checkin M2 I1", "startNewDay 12-Jan-2026"
        };
        List<String> states = new ArrayList<>();
        states.add(snapshot());
        for (String command : commands) {
            execute(command);
            states.add(snapshot());
        }
        Member originalMember = Club.getInstance().findMember("M1");
        Item originalItem = Club.getInstance().findItem("I1");
        for (int index = commands.length - 1; index >= 0; index--) {
            undo();
            require(states.get(index).equals(snapshot()), "Undo mismatch at " + commands[index]);
        }
        undo(); // Empty history is safe; initial date is not undoable.
        require(states.get(0).equals(snapshot()), "Empty undo changed state");
        for (int index = 0; index < commands.length; index++) {
            redo();
            require(states.get(index + 1).equals(snapshot()), "Redo mismatch at " + commands[index]);
        }
        require(Club.getInstance().findMember("M1") == originalMember, "Registration redo replaced member");
        require(Club.getInstance().findItem("I1") == originalItem, "Arrival redo replaced item");
        String end = snapshot();
        redo();
        require(end.equals(snapshot()), "Empty redo changed state");
    }

    private static void holds() throws ExClub {
        for (int index = 1; index <= 4; index++) {
            execute("register M" + index + " Name" + index);
        }
        execute("arrive I1 Book");
        execute("arrive I2 Laptop");
        execute("checkout M1 I1");
        execute("checkout M1 I2");
        for (int index = 2; index <= 4; index++) {
            execute("request M" + index + " I1");
        }
        execute("request M3 I2");
        execute("request M4 I2");
        execute("checkin M1 I1");
        execute("checkin M1 I2");
        Item item = Club.getInstance().findItem("I1");
        execute("startNewDay 04-Jan-2026");
        require(((ItemStatusOnhold) item.getStatus()).getHolder().getId().equals("M2"), "Expired on due date");
        require(((ItemStatusOnhold) item.getStatus()).getRemainDays() == 0, "Wrong remaining days");
        execute("checkout M2 I1");
        undo();
        execute("startNewDay 05-Jan-2026");
        require(((ItemStatusOnhold) item.getStatus()).getHolder().getId().equals("M3"), "FIFO handoff failed");
        require(((ItemStatusOnhold) item.getStatus()).getDueDate().equals("08-Jan-2026"), "Wrong next due date");
        String before = snapshot();
        execute("startNewDay 13-Jan-2026");
        require(item.isAvailable(), "Long jump failed to expire all holders");
        require(Club.getInstance().findItem("I2").isAvailable(), "Second item's hold did not expire");
        String after = snapshot();
        undo();
        require(before.equals(snapshot()), "Hold undo lost queue/status/counts");
        redo();
        require(after.equals(snapshot()), "Hold redo lost queue/status/counts");
    }

    private static void queues() throws ExClub {
        for (int index = 1; index <= 4; index++) {
            execute("register M" + index + " Name" + index);
        }
        execute("arrive I1 Book");
        execute("checkout M1 I1");
        execute("request M2 I1");
        execute("request M3 I1");
        execute("request M4 I1");
        Item item = Club.getInstance().findItem("I1");
        String before = snapshot();
        execute("cancelRequest M3 I1");
        undo();
        require(before.equals(snapshot()), "Cancellation undo changed queue order");
        redo();
        require(item.getQueue().get(1).getId().equals("M4"), "Cancellation redo removed wrong member");
        undo();
        execute("checkin M1 I1");
        undo();
        require(before.equals(snapshot()), "Checkin undo failed to restore first requester");
        redo();
        expect(ExNotBorrowedByMember.class, "checkin M4 I1");
        expect(ExRequestNotFound.class, "cancelRequest M2 I1");
        expect(ExItemAvailable.class, "request M2 I1");
        expect(ExAlreadyRequested.class, "request M3 I1");
        String held = snapshot();
        execute("checkout M4 I1"); // A state rejects this action without recording it.
        require(held.equals(snapshot()), "Non-holder borrowed held item");
        undo(); // Must undo the preceding successful checkin.
        require(before.equals(snapshot()), "Rejected checkout polluted history");
    }

    private static void quotas() throws ExClub {
        execute("register M1 Alice");
        execute("register M2 Bob");
        for (int index = 1; index <= 7; index++) {
            execute("arrive I" + index + " Book" + index);
        }
        for (int index = 1; index <= 6; index++) {
            execute("checkout M1 I" + index);
        }
        expect(ExQuotaExceeded.class, "checkout M1 I7");
        for (int index = 1; index <= 3; index++) {
            execute("request M2 I" + index);
        }
        expect(ExRequestQuotaExceeded.class, "request M2 I4");
        undo();
        execute("request M2 I4");
        require(Club.getInstance().findMember("M2").getRequestedCount() == 3, "Request quota not released");
        execute("checkin M1 I6");
        execute("checkout M1 I7");
        require(Club.getInstance().findMember("M1").getBorrowedCount() == 6, "Loan quota not released");
    }

    private static void validation() throws ExClub {
        for (String command : new String[] {"arrive", "arrive I1", "register", "register M1",
                "checkout", "checkin", "request", "cancelRequest", "startNewDay"}) {
            expect(ExInsufficientArgs.class, command);
        }
        expect(ExUnexpectedArgs.class, "arrive I1 Book Extra");
        execute("register M1 Alice");
        execute("register M2 Bob");
        execute("arrive I1 Book");
        expect(ExMemberIdInUse.class, "register M1 Other");
        expect(ExItemIdInUse.class, "arrive I1 Other");
        expect(ExMemberNotFound.class, "checkout M9 I1");
        expect(ExItemNotFound.class, "request M1 I9");
        expect(ExItemAvailable.class, "request M1 I1");
        expect(ExNotBorrowedByMember.class, "checkin M1 I1");
        expect(ExRequestNotFound.class, "cancelRequest M1 I1");
        execute("checkout M1 I1");
        expect(ExNotBorrowedByMember.class, "checkin M2 I1");
        expect(ExAlreadyBorrowed.class, "request M1 I1");
        String borrowed = snapshot();
        execute("checkout M2 I1");
        require(borrowed.equals(snapshot()), "Unavailable checkout changed state");
        undo();
        expect(ExMemberNotFound.class, "checkout M9 I1");
        redo(); // An invalid action must not clear the redo stack.
        require(borrowed.equals(snapshot()), "Invalid command cleared redo history");
        undo();
        execute("arrive I2 Laptop"); // A new successful action clears the redo stack.
        String branch = snapshot();
        redo();
        require(branch.equals(snapshot()), "Successful new command did not clear redo");
    }

    private static void dates() throws ExClub {
        require(new Day("28-Feb-2024").plusDays(1).toString().equals("29-Feb-2024"), "Leap day failed");
        require(new Day("28-Feb-2026").plusDays(1).toString().equals("01-Mar-2026"), "Month rollover failed");
        require(new Day("31-Dec-2026").plusDays(1).toString().equals("01-Jan-2027"), "Year rollover failed");
        require(Day.isLeapYear(2000) && !Day.isLeapYear(1900), "Century leap rule failed");
        for (String date : new String[] {"29-Feb-2026", "31-Apr-2026", "00-Jan-2026",
                "01-Xyz-2026", "1-Jan-2026", "01-Jan-0000", "garbage"}) {
            expect(ExInvalidDate.class, "startNewDay " + date);
        }
        expect(ExDateNotLater.class, "startNewDay 01-Jan-2026");
        expect(ExDateNotLater.class, "startNewDay 31-Dec-2025");
        SystemDate original = SystemDate.getInstance();
        execute("startNewDay 02-Jan-2026");
        require(original == SystemDate.getInstance(), "SystemDate singleton identity changed");
        execute("register M1 Alice");
        Day exposed = Club.getInstance().findMember("M1").getJoinDate();
        exposed.set("01-Jan-2020");
        require(Club.getInstance().findMember("M1").getJoinDate().toString().equals("02-Jan-2026"),
                "Join date escaped encapsulation");
    }

    private static class ReplayFailure extends RecordedCommand {
        private boolean fail = true;

        @Override
        public void execute(String[] parts) throws ExClub {
            if (parts == null && fail) {
                throw new ExClub("Simulated replay failure");
            }
            if (parts != null) {
                addUndoCommand();
            }
        }

        @Override
        public void undoMe() {
        }
    }

    private static void redoFailure() throws ExClub {
        execute("register M1 Alice");
        ReplayFailure command = new ReplayFailure();
        command.execute(new String[] {"test"});
        undo();
        redo(); // Must stay in redo and never migrate into undo after failure.
        undo();
        require(Club.getInstance().findMember("M1") == null, "Failed redo polluted undo stack");
        redo();
        command.fail = false;
        redo();
        undo();
        require(Club.getInstance().findMember("M1") != null, "Failed redo lost replay command");
    }

    private static void cli() throws Exception {
        Path fixtures = Files.createTempDirectory(Paths.get("build"), "cli-");
        String[][] cases = {
            {"", "Command file is empty"},
            {"\n  \n", "Command file is empty"},
            {"startNewDay\n", "First command must be"},
            {"register M1 Alice\n", "First command must be"},
            {"startNewDay 01-Jan-2026 extra\n", "First command must be"},
            {"startNewDay 31-Feb-2026\n", "Invalid date"},
            {"\uFEFF  startNewDay\t01-Jan-2026\nregister   M1\tAlice\narrive\narrive I1\n"
                    + "arrive I1 Book\nstartNewDay\nstartNewDay bad\nstartNewDay 31-Dec-2025\n"
                    + "unknown\nundo extra\nlistItems extra\nlistMembers\n", "M1"}
        };
        for (int index = 0; index < cases.length; index++) {
            Path file = fixtures.resolve("case" + index + ".txt");
            Files.write(file, cases[index][0].getBytes(StandardCharsets.UTF_8));
            String output = runMain(null, file.toString());
            require(output.contains(cases[index][1]), "CLI missing expected message: " + output);
            if (index == cases.length - 1) {
                require(output.contains("Insufficient command arguments."), "Missing argument error");
                require(output.contains("New date must be later"), "Date reversal not rejected");
                require(output.contains("Unknown command"), "Unknown command not handled");
                require(output.contains("#Borrowed"), "Processing stopped after invalid command");
            }
        }
        require(runMain(null, fixtures.resolve("missing.txt").toString()).contains("Unable to read"),
                "Missing file not handled");
        require(runMain("").contains("No file pathname supplied"), "Console EOF not handled");
        require(runMain("\n").contains("No file pathname supplied"), "Blank console path not handled");
        Path valid = fixtures.resolve("case6.txt");
        require(runMain(valid + "\n").contains("#Borrowed"), "Interactive file prompt failed");
        require(runMain(null, "a", "b").contains("Usage:"), "CLI usage not checked");
        String sample = runMain(null, "data/sample_commands.txt");
        require(sample.contains("On holdshelf for M003 Carol"), "Sample hold workflow missing");
        require(sample.contains("#Requested"), "Sample not completed");
    }

    private static String runMain(String stdin, String... arguments) throws Exception {
        Path log = Files.createTempFile(Paths.get("build"), "cli-output-", ".txt");
        Process process = javaProcess("Main", arguments).redirectOutput(log.toFile()).start();
        if (stdin != null) {
            process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
        }
        process.getOutputStream().close();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Main timed out");
        }
        String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        require(process.exitValue() == 0, "Main terminated abnormally: " + output);
        require(!output.contains("Exception in thread"), "Uncaught CLI exception: " + output);
        return output;
    }
}
