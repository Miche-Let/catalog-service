package com.michelet.catalog.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    /**
     * Catalog 서비스가 구독 중인 모든 토픽의 DLT를 수신함 - 원본 토픽명 뒤에 ".DLT"가 붙은 토픽들을 배열로 등록
     */
    @KafkaListener(
        topics = {
            "${catalog.kafka.topic.product-created:product.created}.DLT",
            "${catalog.kafka.topic.product-updated:product.updated}.DLT",
            "${catalog.kafka.topic.status-changed:product.status-changed}.DLT",
            "${catalog.kafka.topic.stock-reserved:stock.reserved}.DLT",
            "${catalog.kafka.topic.stock-restored:stock.restored}.DLT",
            "${catalog.kafka.topic.daily-reset:stock.daily-reset}.DLT"
        },
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}-dlt",
        containerFactory = "dltListenerContainerFactory"
    )
    public void consumeDeadLetter(ConsumerRecord<String, String> record) {
        String originalTopic = extractHeaderAsString(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
        String exceptionMessage = extractHeaderAsString(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE);

        log.error("""
                ================================================================================
                [CRITICAL ALERT] 카탈로그 DLT 에러 메시지 격리 수신 완료!
                원본 토픽 : {}
                에러 원인 : {}
                원본 데이터 : {}
                ================================================================================""",
            originalTopic,
            exceptionMessage,
            record.value() != null ? record.value() : "데이터 없음"
        );
    }

    private String extractHeaderAsString(ConsumerRecord<String, String> record, String headerKey) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return "알 수 없음";
    }
}
