package com.sadamda.model.family;

import com.sadamda.model.order.OrderLine;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 가족에게 물어보기 한 건: "이 옷 어때?" 묻는 사람(requester)이 가족(responder)에게 보낸 상품들과 한마디,
 * 그리고 가족의 답(좋아요/다른 걸 추천해요 + 한 줄 의견)이나 대신 결제한 주문.
 */
public class AskRequest {
    public enum Status { WAITING, LIKED, DISLIKED, PAID }

    private final Long id;
    private final Long requesterId;
    private final Long responderId;
    private final List<OrderLine> lines;
    private final String message;
    private final LocalDateTime askedAt;
    private Status status = Status.WAITING;
    private String reply;
    private Long orderId;
    private LocalDateTime answeredAt;
    /** 묻는 사람이 답을 확인했는지 (상단 메뉴의 새 소식 숫자). */
    private boolean seenByRequester = true;

    public AskRequest(Long id, Long requesterId, Long responderId, List<OrderLine> lines, String message,
                      LocalDateTime askedAt) {
        this.id = id;
        this.requesterId = requesterId;
        this.responderId = responderId;
        this.lines = List.copyOf(lines);
        this.message = message;
        this.askedAt = askedAt;
    }

    public Long getId() { return id; }
    public Long getRequesterId() { return requesterId; }
    public Long getResponderId() { return responderId; }
    public List<OrderLine> getLines() { return lines; }
    public String getMessage() { return message; }
    public LocalDateTime getAskedAt() { return askedAt; }
    public Status getStatus() { return status; }
    public String getReply() { return reply; }
    public Long getOrderId() { return orderId; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public boolean isSeenByRequester() { return seenByRequester; }

    public boolean isAnswered() {
        return status != Status.WAITING;
    }

    void answer(Status newStatus, String newReply, Long paidOrderId) {
        this.status = newStatus;
        if (newReply != null) {
            this.reply = newReply;
        }
        if (paidOrderId != null) {
            this.orderId = paidOrderId;
        }
        this.answeredAt = LocalDateTime.now();
        this.seenByRequester = false;
    }

    void markSeen() {
        this.seenByRequester = true;
    }
}
