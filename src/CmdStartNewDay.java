import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Date changes save both sides of every affected item and reservation queue. */
public class CmdStartNewDay extends RecordedCommand {
    private Day oldDate;
    private Day newDate;
    private Map<Item, ItemStatus> beforeStatuses;
    private Map<Item, List<Member>> beforeQueues;
    private Map<Item, ItemStatus> afterStatuses;
    private Map<Item, List<Member>> afterQueues;

    @Override
    public void execute(String[] parts) throws ExClub {
        if (parts == null) {
            restore(newDate, afterStatuses, afterQueues);
            return;
        }
        validateArgs(parts, 2);
        Day target = Day.parse(parts[1]);
        if (target.compareTo(SystemDate.getInstance()) <= 0) {
            throw new ExDateNotLater();
        }
        oldDate = SystemDate.getInstance().clone();
        newDate = target.clone();
        beforeStatuses = captureStatuses();
        beforeQueues = captureQueues();

        // Process each intervening day so a long jump can expire multiple holders.
        while (SystemDate.getInstance().compareTo(target) < 0) {
            SystemDate.setDate(SystemDate.getInstance().plusDays(1));
            Club.getInstance().handleExpiredOnHold();
        }
        afterStatuses = captureStatuses();
        afterQueues = captureQueues();
        addUndoCommand();
        System.out.println("Done.");
    }

    private Map<Item, ItemStatus> captureStatuses() {
        Map<Item, ItemStatus> statuses = new LinkedHashMap<>();
        for (Item item : Club.getInstance().getItems()) {
            statuses.put(item, item.getStatus());
        }
        return statuses;
    }

    private Map<Item, List<Member>> captureQueues() {
        Map<Item, List<Member>> queues = new LinkedHashMap<>();
        for (Item item : Club.getInstance().getItems()) {
            queues.put(item, new ArrayList<>(item.getQueue()));
        }
        return queues;
    }

    private void restore(Day date, Map<Item, ItemStatus> statuses,
                         Map<Item, List<Member>> queues) {
        SystemDate.setDate(date);
        for (Item item : statuses.keySet()) {
            for (Member member : item.getQueue()) {
                member.decreaseRequestedCount();
            }
            item.getQueue().clear();
            for (Member member : queues.get(item)) {
                item.addQueue(member);
            }
            item.setStatus(statuses.get(item));
        }
    }

    @Override
    public void undoMe() {
        restore(oldDate, beforeStatuses, beforeQueues);
    }
}
