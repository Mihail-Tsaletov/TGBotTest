package svaga.tgbottest.DTO;

import lombok.Data;

@Data
public class BroadcastResult {
    public final int sent;
    public final int failed;
    public final int total;

    public BroadcastResult(int sent, int failed, int total) {
        this.sent = sent;
        this.failed = failed;
        this.total = total;
    }
}
