import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Item {
    private final String id;
    private final String name;
    private final Day arrivalDate;
    private ItemStatus status;
    private final List<Member> queue = new ArrayList<>();

    public Item(String id, String name) {
        this.id = id;
        this.name = name;
        arrivalDate = SystemDate.getInstance().clone();
        status = new ItemStatusAvailable();
    }

    public void setStatus(ItemStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    public void addQueue(Member member) {
        queue.add(member);
        member.increaseRequestedCount();
    }

    /** Shared transition used by returns and hold expiry. */
    public void offerToNextRequester() {
        if (queue.isEmpty()) {
            setStatus(new ItemStatusAvailable());
            return;
        }
        Member next = queue.remove(0);
        next.decreaseRequestedCount();
        ItemStatusOnhold hold = new ItemStatusOnhold(next);
        setStatus(hold);
        System.out.printf("Item [%s %s] is now ready for pick up by [%s %s].  On hold due on %s.%n",
                id, name, next.getId(), next.getName(), hold.getDueDate());
    }

    public boolean borrow(Member member) {
        return status.borrow(this, member);
    }

    public int getQueueNum() {
        return queue.size();
    }

    public boolean isAvailable() {
        return status instanceof ItemStatusAvailable;
    }

    public boolean isOnhold() {
        return status instanceof ItemStatusOnhold;
    }

    public boolean isBorrowed() {
        return status instanceof ItemStatusBorrowed;
    }

    public boolean hasRequest(Member member) {
        return queue.contains(member);
    }

    public void returnItem() {
        status.returnItem(this);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Day getArrivalDate() {
        return arrivalDate.clone();
    }

    public ItemStatus getStatus() {
        return status;
    }

    // Package access: only the model and command implementations mutate this queue.
    List<Member> getQueue() {
        return queue;
    }
}
