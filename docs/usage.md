# Command and design notes

## Command format

The first nonblank command must be `startNewDay DD-MMM-YYYY`. It initializes the date and cannot be undone. Blank lines are ignored, and spaces or tabs separate arguments. Names remain single tokens; quoted names and names containing spaces are not parsed.

| Command | Operation |
| --- | --- |
| `register MEMBER_ID NAME` | Register a member |
| `arrive ITEM_ID NAME` | Add an item and record its arrival date |
| `checkout MEMBER_ID ITEM_ID` | Borrow an Available item or collect a hold assigned to this member |
| `checkin MEMBER_ID ITEM_ID` | Return an item as its actual borrower |
| `request MEMBER_ID ITEM_ID` | Join the queue for an item that cannot be borrowed immediately |
| `cancelRequest MEMBER_ID ITEM_ID` | Cancel a request still in the queue |
| `startNewDay DD-MMM-YYYY` | Advance to a later date |
| `listMembers` | Display members and counts |
| `listItems` | Display item states and request queues |
| `undo` | Undo the latest successful change |
| `redo` | Redo the latest undone change |

Command names, IDs and names are case-sensitive. Months use English abbreviations `Jan`–`Dec`; dates must be valid `DD-MMM-YYYY` values with input years 0001–9999. Extra arguments are rejected. File reading uses UTF-8 and closes resources with try-with-resources. Business exceptions are caught so subsequent commands can continue.

## Loan and reservation rules

- A member may borrow at most **six** items and have at most **three** queued requests.
- Available items can be borrowed directly without a reservation.
- A borrower cannot request their own borrowed item, and duplicate requests are rejected.
- A returned item becomes On Hold for the first queued member, or Available if no queue exists.
- The hold expiry date is the handover date plus **three days**. Collection remains possible on the expiry date; expiry occurs the next day. A 03-Jan handover expires after 06-Jan, transferring on 07-Jan.
- Expired holds transfer in FIFO order. When advancing across several dates, each transfer uses the actual expiry day to calculate the next hold's expiry date.
- `#Requested` counts only requests still queued. A member granted a hold no longer uses a queued-request slot. `cancelRequest` cannot cancel an already assigned hold.

## Undo and redo

Only successful changes enter the undo stack. A new successful change clears the redo stack. Listing, unknown commands and failed business operations leave history unchanged.

`execute(null)` retains the original command convention for redo. Registration and item-arrival redo reuse the original objects so references held by later commands remain valid. Date commands save before/after dates, item states and reservation queues; queue restoration also adjusts member request counts. Borrowing and expiry dates in state objects are independent of later system-date changes, allowing saved states to be restored. A checked exception during redo leaves the command on the redo stack.

Returns and first handovers print collection notices; undoing a return prints a withdrawal notice. Date-command redo restores its saved result without repeating notices already shown.

## Regression coverage

`tests/RegressionTests.java` covers complete history restoration, reservations across several items and dates, FIFO order, quota boundaries, argument validation, date boundaries, redo failure, and file/interactive entry points. Each of eight groups runs in an independent JVM to avoid singleton and static-history contamination. CLI subprocesses have timeouts.

The PowerShell verification script writes `build/verification.log` and compares the program's sample output with the committed reference. Build products and logs are excluded from the project ZIP.

## Earlier maintenance

The organized version retains the State, Command and Singleton designs and adds no framework, database or GUI. Earlier changes addressed:

- Empty files, invalid first commands, invalid dates, missing files and interactive EOF in `Main`.
- Argument-count checks before indexing command arguments.
- Rejection of identical or earlier dates, avoiding a nonterminating forward-date loop.
- Date undo/redo restoration of item states, queues and member counts, including multiple expired holds.
- Object identity on registration redo and validation of the actual borrower on return.
- Consistent naming and formatting, constants for quotas and hold duration, and shared handover logic.

Source from the earlier `Main.java` and `Other.zip` submission was organized into `src/`. The repository does not include original source ZIPs, assignment PDFs, compiled classes or personal IDE settings. This final documentation cleanup makes no further implementation changes.

The OOP examples also include generics (`List<Member>` and `Deque<RecordedCommand>`), member sorting through `Comparable<Member>`, date copies, a read-only item list and custom checked exceptions. No personal or team ownership is inferred solely from those code structures.
