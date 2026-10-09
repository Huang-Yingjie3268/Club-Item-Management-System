public class ItemStatusBorrowed implements ItemStatus {
    private final Member borrower;
    private final Day borrowDate;

    public ItemStatusBorrowed(Member member) {
        this(member, SystemDate.getInstance());
    }

    public ItemStatusBorrowed(Member member, Day date) {
        borrower = member;
        borrowDate = date.clone();
    }

    public Day getBorrowDate() {
        return borrowDate.clone();
    }

    public Member getBorrower() {
        return borrower;
    }

    @Override
    public boolean borrow(Item item, Member member) {
        if (member == borrower) {
            System.out.println("The item has been borrowed by the same member already.");
        } else {
            System.out.println("The item is not available.");
        }
        return false;
    }

    @Override
    public void returnItem(Item item) {
        borrower.decreaseBorrowedCount();
        item.offerToNextRequester();
    }
}
