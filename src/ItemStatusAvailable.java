public class ItemStatusAvailable implements ItemStatus {
    @Override
    public boolean borrow(Item item, Member member) {
        item.setStatus(new ItemStatusBorrowed(member));
        member.increaseBorrowedCount();
        return true;
    }

    @Override
    public void returnItem(Item item) {
        System.out.println("The item is currently available.");
    }
}
