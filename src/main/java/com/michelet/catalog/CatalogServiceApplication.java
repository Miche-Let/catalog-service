package com.michelet.catalog;

import com.michelet.common.config.CommonAutoConfiguration;
import com.michelet.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;

// 공통 모듈(common)에서 가져온 JPA 때문에 발생하는 불필요한 RDBMS 자동 설정 비활성화
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    CommonAutoConfiguration.class // 공통 모듈의 자동 설정(JPA Auditing 포함) 비활성화
})
@Import(GlobalExceptionHandler.class) // 공통 모듈에서 필요한 전역 예외 처리기만 수동으로 등록
public class CatalogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

}
