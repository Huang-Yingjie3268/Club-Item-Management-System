public class CmdListItems {
    public void execute(String[] parts) throws ExClub {
        RecordedCommand.validateArgs(parts, 1);
        Club.getInstance().listItems();
    }
}
