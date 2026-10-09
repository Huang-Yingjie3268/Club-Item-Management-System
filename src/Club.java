import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory club registry, shared by all commands. */
public class Club {
    private final List<Member> allMembers = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private static final Club INSTANCE = new Club();

    private Club() {
    }

    public static Club getInstance() {
        return INSTANCE;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addMember(Member member) {
        allMembers.add(member);
        Collections.sort(allMembers);
    }

    public void removeMember(Member member) {
        allMembers.remove(member);
    }

    public void listClubMembers() {
        Member.list(allMembers);
    }

    public Member findMember(String id) {
        for (Member member : allMembers) {
            if (member.getId().equals(id)) {
                return member;
            }
        }
        return null;
    }

    public Item findItem(String id) {
        for (Item item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public boolean hasMember(String id) {
        return findMember(id) != null;
    }

    public void listItems() {
        System.out.printf("%-7s%-20s%-13s%s%n", "ID", "Name", "Arrival", "Status");
        for (Item item : items) {
            System.out.printf("%-7s%-20s%-13s%s%n", item.getId(), item.getName(),
                    item.getArrivalDate(), getItemStatusString(item));
        }
    }

    public String getItemStatusString(Item item) {
        StringBuilder result = new StringBuilder();
        if (item.isAvailable()) {
            result.append("Available");
        } else if (item.isBorrowed()) {
            ItemStatusBorrowed status = (ItemStatusBorrowed) item.getStatus();
            result.append("Borrowed by ").append(status.getBorrower().getId()).append(' ')
                    .append(status.getBorrower().getName()).append(" on ").append(status.getBorrowDate());
        } else if (item.isOnhold()) {
            ItemStatusOnhold status = (ItemStatusOnhold) item.getStatus();
            result.append("On holdshelf for ").append(status.getHolder().getId()).append(' ')
                    .append(status.getHolder().getName()).append(" until ").append(status.getDueDate());
        } else {
            result.append("Unknown");
        }
        if (!item.getQueue().isEmpty()) {
            result.append(" + ").append(item.getQueue().size()).append(" request(s):");
            for (Member member : item.getQueue()) {
                result.append(' ').append(member.getId());
            }
        }
        return result.toString();
    }

    public void handleExpiredOnHold() {
        for (Item item : items) {
            if (item.isOnhold() && ((ItemStatusOnhold) item.getStatus()).isExpired()) {
                System.out.printf("On hold period is over for %s %s.%n", item.getId(), item.getName());
                item.offerToNextRequester();
            }
        }
    }
}
