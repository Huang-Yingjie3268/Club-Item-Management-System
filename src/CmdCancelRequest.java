public class CmdCancelRequest extends RecordedCommand {
    private Member member;
    private Item item;
    private int oldIndex;

    @Override
    public void execute(String[] parts) throws ExClub {
        if (parts == null) {
            item.getQueue().remove(member);
            member.decreaseRequestedCount();
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
        oldIndex = item.getQueue().indexOf(member);
        if (oldIndex < 0) {
            throw new ExRequestNotFound();
        }
        item.getQueue().remove(oldIndex);
        member.decreaseRequestedCount();
        addUndoCommand();
        System.out.println("Done.");
    }

    @Override
    public void undoMe() {
        item.getQueue().add(oldIndex, member);
        member.increaseRequestedCount();
    }
}
