/** State-specific operations used by Item through polymorphism. */
public interface ItemStatus {
    boolean borrow(Item item, Member member);
    void returnItem(Item item);
}
