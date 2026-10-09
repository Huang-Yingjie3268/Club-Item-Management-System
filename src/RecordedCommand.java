import java.util.ArrayDeque;
import java.util.Deque;

/** Command history; execute(null) replays a previously successful command. */
public abstract class RecordedCommand {
    private static final Deque<RecordedCommand> undoStack = new ArrayDeque<>();
    private static final Deque<RecordedCommand> redoStack = new ArrayDeque<>();

    public abstract void execute(String[] parts) throws ExClub;
    public abstract void undoMe();

    public static void validateArgs(String[] parts, int expected) throws ExClub {
        if (parts == null || parts.length < expected) {
            throw new ExInsufficientArgs();
        }
        if (parts.length > expected) {
            throw new ExUnexpectedArgs();
        }
    }

    protected void addUndoCommand() {
        undoStack.push(this);
        redoStack.clear();
    }

    public static void undoOneCommand() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        RecordedCommand command = undoStack.peek();
        command.undoMe();
        undoStack.pop();
        redoStack.push(command);
    }

    public static void redoOneCommand() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo.");
            return;
        }
        RecordedCommand command = redoStack.peek();
        try {
            command.execute(null);
            redoStack.pop();
            undoStack.push(command);
        } catch (ExClub e) {
            System.out.println(e.getMessage());
        }
    }
}
