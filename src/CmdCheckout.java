public class CmdCheckout extends RecordedCommand {
    private Member member;
    private Item item;
    private ItemStatus oldStatus;

    @Override
    public void execute(String[] parts) throws ExClub {
        if (parts == null) {
            item.borrow(member);
            return;
        }
        validateArgs(parts, 3);
        member = Club.getInstance().findMember(parts[1]);
        item = Club.getInstance().findItem(parts[2]);
        if (member == null) {
            throw new ExMemberNotFound();
        }
        if (item == null) {
            throw new ExItemNotFound();
        }
        if (member.getBorrowedCount() >= Member.LOAN_LIMIT) {
            throw new ExQuotaExceeded();
        }
        oldStatus = item.getStatus();
        if (item.borrow(member)) {
            addUndoCommand();
            System.out.println("Done.");
        }
    }

    @Override
    public void undoMe() {
        item.setStatus(oldStatus);
        member.decreaseBorrowedCount();
    }
}
