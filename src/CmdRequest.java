public class CmdRequest extends RecordedCommand {
    private Member member;
    private Item item;

    @Override
    public void execute(String[] parts) throws ExClub {
        if (parts == null) {
            item.addQueue(member);
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
        if (member.getRequestedCount() >= Member.REQUEST_LIMIT) {
            throw new ExRequestQuotaExceeded();
        }
        if (item.isOnhold() && ((ItemStatusOnhold) item.getStatus()).getHolder() == member) {
            throw new ExItemAvailable();
        }
        if (item.isBorrowed() && ((ItemStatusBorrowed) item.getStatus()).getBorrower() == member) {
            throw new ExAlreadyBorrowed();
        }
        if (item.isAvailable()) {
            throw new ExItemAvailable();
        }
        if (item.hasRequest(member)) {
            throw new ExAlreadyRequested();
        }
        item.addQueue(member);
        addUndoCommand();
        System.out.println("Done. This request is no. " + item.getQueueNum() + " in the queue.");
    }

    @Override
    public void undoMe() {
        item.getQueue().remove(member);
        member.decreaseRequestedCount();
    }
}
