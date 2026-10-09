public class CmdArrive extends RecordedCommand {
    private Item item;

    @Override
    public void execute(String[] parts) throws ExClub {
        if (parts == null) {
            Club.getInstance().addItem(item);
            return;
        }
        validateArgs(parts, 3);
        Item existing = Club.getInstance().findItem(parts[1]);
        if (existing != null) {
            throw new ExItemIdInUse("Item ID already in use: " + existing.getId() + " " + existing.getName());
        }
        item = new Item(parts[1], parts[2]);
        Club.getInstance().addItem(item);
        addUndoCommand();
        System.out.println("Done.");
    }

    @Override
    public void undoMe() {
        Club.getInstance().removeItem(item);
    }
}
