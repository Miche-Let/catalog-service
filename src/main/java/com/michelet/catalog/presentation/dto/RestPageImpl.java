package com.michelet.catalog.presentation.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

// Redis 역직렬화 에러를 우회하기 위한 PageImpl 커스텀 래퍼
@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
public class RestPageImpl<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPageImpl(@JsonProperty("content") List<T> content,
                        @JsonProperty("number") int number,
                        @JsonProperty("size") int size,
                        @JsonProperty("totalElements") Long totalElements) {
        super(content, PageRequest.of(number, size == 0 ? 10 : size), totalElements == null ? 0 : totalElements);
    }
}
