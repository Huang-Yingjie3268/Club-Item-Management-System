public class CmdCheckin extends RecordedCommand {
    private Item item;
    private ItemStatusBorrowed oldStatus;
    private Member notifiedHolder;

    @Override
    public void execute(String[] parts) throws ExClub {
        if (parts == null) {
            item.returnItem();
            return;
        }
        validateArgs(parts, 3);
        Member member = Club.getInstance().findMember(parts[1]);
        item = Club.getInstance().findItem(parts[2]);
        if (member == null) {
            throw new ExMemberNotFound();
        }
        if (item == null) {
            throw new ExItemNotFound();
        }
        if (!item.isBorrowed() || ((ItemStatusBorrowed) item.getStatus()).getBorrower() != member) {
            throw new ExNotBorrowedByMember();
        }
        oldStatus = (ItemStatusBorrowed) item.getStatus();
        notifiedHolder = item.getQueue().isEmpty() ? null : item.getQueue().get(0);
        item.returnItem();
        addUndoCommand();
        System.out.println("Done.");
    }

    @Override
    public void undoMe() {
        item.setStatus(oldStatus);
        oldStatus.getBorrower().increaseBorrowedCount();
        if (notifiedHolder != null) {
            item.getQueue().add(0, notifiedHolder);
            notifiedHolder.increaseRequestedCount();
            System.out.printf("Sorry. %s %s please ignore the pick up notice for %s %s.%n",
                    notifiedHolder.getId(), notifiedHolder.getName(), item.getId(), item.getName());
        }
    }
}
