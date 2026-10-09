/** Only the holder may check out the item, through the due date inclusive. */
public class ItemStatusOnhold implements ItemStatus {
    public static final int HOLD_DAYS = 3;
    private final Member holder;
    private final Day dueDate;

    public ItemStatusOnhold(Member member, int days) {
        if (days < 0) {
            throw new IllegalArgumentException("Hold duration must not be negative.");
        }
        holder = member;
        dueDate = SystemDate.getInstance().plusDays(days);
    }

    public ItemStatusOnhold(Member member) {
        this(member, HOLD_DAYS);
    }

    @Override
    public boolean borrow(Item item, Member member) {
        if (member == holder) {
            item.setStatus(new ItemStatusBorrowed(member));
            member.increaseBorrowedCount();
            return true;
        }
        System.out.println("The item is not available.");
        return false;
    }

    public Member getHolder() {
        return holder;
    }

    public int getRemainDays() {
        return SystemDate.getInstance().daysUntil(dueDate);
    }

    public String getDueDate() {
        return dueDate.toString();
    }

    @Override
    public void returnItem(Item item) {
        System.out.println("Item is on hold.");
    }

    public boolean isExpired() {
        return SystemDate.getInstance().compareTo(dueDate) > 0;
    }
}
