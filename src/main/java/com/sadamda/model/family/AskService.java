package com.sadamda.model.family;

import com.sadamda.common.AppException;
import com.sadamda.model.order.OrderLine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 가족에게 물어보기 저장소. 묻기·답하기·대신 결제 표시와, 화면에 보여 줄 새 소식 수를 맡는다.
 * 실패는 AppException(화면에 그대로 보여 줄 문장)으로 알린다.
 */
public class AskService {
    public static final int MAX_MESSAGE = 100;
    public static final int MAX_REPLY = 100;

    private static final Comparator<AskRequest> NEWEST_FIRST =
            Comparator.comparing(AskRequest::getAskedAt).thenComparing(AskRequest::getId).reversed();

    private final Map<Long, AskRequest> requests = new LinkedHashMap<>();
    /** 묻는 사람별로 마지막에 물어본 가족의 이메일 (다음에 물을 때 미리 채운다). */
    private final Map<Long, String> lastFamilyEmail = new HashMap<>();
    private long nextId = 1;

    /** 가족에게 묻는다. 상품이 없거나 자기 자신에게 물으면 AppException. */
    public synchronized AskRequest ask(Long requesterId, Long responderId, String responderEmail,
                                       List<OrderLine> lines, String message) {
        if (lines == null || lines.isEmpty()) {
            throw new AppException("물어볼 상품이 없어요.");
        }
        if (requesterId.equals(responderId)) {
            throw new AppException("나 자신에게는 물어볼 수 없어요. 가족의 이메일을 적어 주세요.");
        }
        String text = message == null ? "" : message.trim();
        if (text.length() > MAX_MESSAGE) {
            throw new AppException("하고 싶은 말은 " + MAX_MESSAGE + "자까지 쓸 수 있어요.");
        }
        AskRequest request = new AskRequest(nextId++, requesterId, responderId, lines, text, LocalDateTime.now());
        requests.put(request.getId(), request);
        lastFamilyEmail.put(requesterId, responderEmail);
        return request;
    }

    /** 내가 물어본 것, 최신순. */
    public synchronized List<AskRequest> askedBy(Long requesterId) {
        return requests.values().stream().filter(r -> r.getRequesterId().equals(requesterId))
                .sorted(NEWEST_FIRST).toList();
    }

    /** 가족이 나에게 물어본 것, 최신순. */
    public synchronized List<AskRequest> askedTo(Long responderId) {
        return requests.values().stream().filter(r -> r.getResponderId().equals(responderId))
                .sorted(NEWEST_FIRST).toList();
    }

    public synchronized AskRequest findById(Long id) {
        AskRequest request = id == null ? null : requests.get(id);
        if (request == null) {
            throw new AppException("요청을 찾을 수 없어요.");
        }
        return request;
    }

    /** 가족이 답한다: 좋아요(liked=true) 또는 다른 걸 추천해요. 이미 결제했으면 바꿀 수 없다. */
    public synchronized AskRequest respond(Long responderId, Long requestId, boolean liked, String reply) {
        AskRequest request = owned(responderId, requestId);
        if (request.getStatus() == AskRequest.Status.PAID) {
            throw new AppException("이미 대신 결제한 요청이에요.");
        }
        String text = reply == null ? "" : reply.trim();
        if (text.length() > MAX_REPLY) {
            throw new AppException("한마디는 " + MAX_REPLY + "자까지 쓸 수 있어요.");
        }
        request.answer(liked ? AskRequest.Status.LIKED : AskRequest.Status.DISLIKED, text, null);
        return request;
    }

    /** 대신 결제하기 전에 확인한다. 이미 결제했으면 AppException (두 번 결제 방지). */
    public synchronized AskRequest checkPayable(Long responderId, Long requestId) {
        AskRequest request = owned(responderId, requestId);
        if (request.getStatus() == AskRequest.Status.PAID) {
            throw new AppException("이미 대신 결제한 요청이에요.");
        }
        return request;
    }

    /** 대신 결제가 끝났음을 남긴다. */
    public synchronized AskRequest markPaid(Long responderId, Long requestId, Long orderId, String reply) {
        AskRequest request = checkPayable(responderId, requestId);
        request.answer(AskRequest.Status.PAID, reply, orderId);
        return request;
    }

    /** 묻는 사람이 답을 확인했다. */
    public synchronized void markSeen(Long requesterId) {
        for (AskRequest request : requests.values()) {
            if (request.getRequesterId().equals(requesterId) && request.isAnswered()) {
                request.markSeen();
            }
        }
    }

    /** 새 소식 수: 나에게 온 아직 답하지 않은 요청 + 내가 물어본 것 중 새로 답이 온 것. */
    public synchronized int newsCount(Long memberId) {
        int count = 0;
        for (AskRequest request : requests.values()) {
            if (request.getResponderId().equals(memberId) && !request.isAnswered()) {
                count++;
            }
            if (request.getRequesterId().equals(memberId) && !request.isSeenByRequester()) {
                count++;
            }
        }
        return count;
    }

    public synchronized String lastFamilyEmail(Long requesterId) {
        return lastFamilyEmail.get(requesterId);
    }

    private AskRequest owned(Long responderId, Long requestId) {
        AskRequest request = findById(requestId);
        if (!request.getResponderId().equals(responderId)) {
            throw new AppException("나에게 온 요청만 답할 수 있어요.");
        }
        return request;
    }

    /** 테스트·시연용: 모든 요청 (최신순). */
    public synchronized List<AskRequest> all() {
        List<AskRequest> list = new ArrayList<>(requests.values());
        list.sort(NEWEST_FIRST);
        return list;
    }
}
