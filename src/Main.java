import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Scanner;

/** Reads a command file; its first command establishes the initial system date. */
public class Main {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Usage: java Main [command-file]");
            return;
        }
        String pathname;
        if (args.length == 1) {
            pathname = args[0];
        } else {
            // This standalone program owns System.in and exits after one command file.
            try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
                System.out.print("Please input the file pathname: ");
                if (!input.hasNextLine()) {
                    System.out.println("No file pathname supplied.");
                    return;
                }
                pathname = input.nextLine().trim();
            }
        }
        if (pathname.trim().isEmpty()) {
            System.out.println("No file pathname supplied.");
            return;
        }
        try (Scanner commands = new Scanner(Paths.get(pathname), StandardCharsets.UTF_8.name())) {
            runCommands(commands);
            if (commands.ioException() != null) {
                System.out.println("Unable to finish reading command file: "
                        + commands.ioException().getMessage());
            }
        } catch (IOException | InvalidPathException e) {
            System.out.println("Unable to read command file: " + e.getMessage());
        }
    }

    private static void runCommands(Scanner commands) {
        String firstLine = null;
        while (commands.hasNextLine()) {
            String line = commands.nextLine().trim();
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1).trim();
            }
            if (!line.isEmpty()) {
                firstLine = line;
                break;
            }
        }
        if (firstLine == null) {
            System.out.println("Command file is empty. First command must be startNewDay DD-MMM-YYYY.");
            return;
        }
        System.out.println("\n> " + firstLine);
        String[] firstParts = firstLine.trim().split("\\s+");
        if (firstParts.length != 2 || !firstParts[0].equals("startNewDay")) {
            System.out.println("First command must be startNewDay DD-MMM-YYYY.");
            return;
        }
        try {
            SystemDate.setDate(Day.parse(firstParts[1]));
        } catch (ExInvalidDate e) {
            System.out.println(e.getMessage());
            return;
        }
        while (commands.hasNextLine()) {
            String line = commands.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            System.out.println("\n> " + line);
            String[] parts = line.trim().split("\\s+");
            try {
                dispatch(parts);
            } catch (ExClub e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void dispatch(String[] parts) throws ExClub {
        switch (parts[0]) {
            case "register": new CmdRegister().execute(parts); break;
            case "arrive": new CmdArrive().execute(parts); break;
            case "checkout": new CmdCheckout().execute(parts); break;
            case "checkin": new CmdCheckin().execute(parts); break;
            case "request": new CmdRequest().execute(parts); break;
            case "cancelRequest": new CmdCancelRequest().execute(parts); break;
            case "startNewDay": new CmdStartNewDay().execute(parts); break;
            case "listItems":
                RecordedCommand.validateArgs(parts, 1);
                new CmdListItems().execute(parts);
                break;
            case "listMembers":
                RecordedCommand.validateArgs(parts, 1);
                new CmdListMembers().execute(parts);
                break;
            case "undo":
                RecordedCommand.validateArgs(parts, 1);
                RecordedCommand.undoOneCommand();
                break;
            case "redo":
                RecordedCommand.validateArgs(parts, 1);
                RecordedCommand.redoOneCommand();
                break;
            default: System.out.println("Unknown command - ignored.");
        }
    }
}
