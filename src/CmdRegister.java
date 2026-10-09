public class CmdRegister extends RecordedCommand {
    private Member member;

    @Override
    public void execute(String[] parts) throws ExClub {
        if (parts == null) {
            // Preserve identity: later recorded commands refer to this member.
            Club.getInstance().addMember(member);
            return;
        }
        validateArgs(parts, 3);
        Member existing = Club.getInstance().findMember(parts[1]);
        if (existing != null) {
            throw new ExMemberIdInUse("Member ID already in use: " + existing.getId() + " " + existing.getName());
        }
        member = new Member(parts[1], parts[2]);
        Club.getInstance().addMember(member);
        addUndoCommand();
        System.out.println("Done.");
    }

    @Override
    public void undoMe() {
        Club.getInstance().removeMember(member);
    }
}
