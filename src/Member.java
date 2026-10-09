import java.util.List;

public class Member implements Comparable<Member> {
    public static final int LOAN_LIMIT = 6;
    public static final int REQUEST_LIMIT = 3;
    private final String id;
    private final String name;
    private final Day joinDate;
    private int borrowedCount;
    private int requestedCount;

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
        joinDate = SystemDate.getInstance().clone();
    }

    public static void list(List<Member> members) {
        System.out.printf("%-7s%-15s%-13s%11s%12s%n", "ID", "Name", "Join Date", "#Borrowed", "#Requested");
        for (Member member : members) {
            System.out.printf("%-7s%-15s%-13s%11d%12d%n", member.getId(), member.getName(),
                    member.getJoinDate(), member.getBorrowedCount(), member.getRequestedCount());
        }
    }

    public int getBorrowedCount() {
        return borrowedCount;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Day getJoinDate() {
        return joinDate.clone();
    }

    public void increaseBorrowedCount() {
        borrowedCount++;
    }

    public void decreaseBorrowedCount() {
        if (borrowedCount == 0) {
            throw new IllegalStateException("Loan count cannot be negative.");
        }
        borrowedCount--;
    }

    public void increaseRequestedCount() {
        requestedCount++;
    }

    public void decreaseRequestedCount() {
        if (requestedCount == 0) {
            throw new IllegalStateException("Request count cannot be negative.");
        }
        requestedCount--;
    }

    @Override
    public int compareTo(Member another) {
        return id.compareTo(another.id);
    }
}
