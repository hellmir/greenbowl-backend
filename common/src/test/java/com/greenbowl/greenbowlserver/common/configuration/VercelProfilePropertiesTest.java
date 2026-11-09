package com.greenbowl.greenbowlserver.common.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class VercelProfilePropertiesTest {
    private Properties properties;

    @BeforeEach
    void setUp() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-vercel.yml"));
        properties = factory.getObject();
    }

    @Test
    @DisplayName("콜드 스타트마다 Hibernate가 스키마를 만들거나 지우지 않는다")
    void shouldNotManageSchemaOnStartup() {
        // when
        String ddlAuto = properties.getProperty("spring.jpa.hibernate.ddl-auto");

        // then
        assertThat(ddlAuto).isEqualTo("none");
    }

    @Test
    @DisplayName("콜드 스타트마다 시드 SQL을 실행하거나 DB에 연결하지 않는다")
    void shouldNeverRunSqlInitializationOnStartup() {
        // when
        String mode = properties.getProperty("spring.sql.init.mode");

        // then
        assertThat(mode).isEqualTo("never");
    }

    @Test
    @DisplayName("기동 시간을 줄이기 위해 지연 초기화를 켠다")
    void shouldInitializeBeansLazily() {
        // when
        Object lazyInitialization = properties.get("spring.main.lazy-initialization");

        // then
        assertThat(lazyInitialization).isEqualTo(true);
    }

    @Test
    @DisplayName("플랫폼이 지정한 포트로 웹 서버를 연다")
    void shouldBindServerPortToPlatformPort() {
        // when
        String port = properties.getProperty("server.port");

        // then
        assertThat(port).isEqualTo("${PORT:8080}");
    }

    @Test
    @DisplayName("DB 연결은 Neon 통합이 주입하는 직결 호스트 변수만 쓴다")
    void shouldUseNeonUnpooledConnectionVariables() {
        // when
        String url = properties.getProperty("spring.datasource.url");
        String username = properties.getProperty("spring.datasource.username");
        String password = properties.getProperty("spring.datasource.password");

        // then
        assertThat(url).startsWith("jdbc:postgresql://${PGHOST_UNPOOLED}/${PGDATABASE}?");
        assertThat(username).isEqualTo("${PGUSER}");
        assertThat(password).isEqualTo("${PGPASSWORD}");
    }

    @Test
    @DisplayName("DB 서버 인증서와 호스트 이름을 검증한다")
    void shouldVerifyDatabaseServerCertificate() {
        // when
        String url = properties.getProperty("spring.datasource.url");

        // then
        assertThat(url).contains("sslmode=verify-full", "sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
    }

    @Test
    @DisplayName("서비스 접두사를 context-path가 맡으므로 Swagger 경로에 접두사를 다시 붙이지 않는다")
    void shouldNotPrefixSwaggerPathsAgain() {
        // when
        String pathMapping = properties.getProperty("greenbowl.swagger.path-mapping");

        // then
        assertThat(pathMapping).isEqualTo("/");
    }

    @Test
    @DisplayName("상태 확인 요청이 DB에 연결하지 않는다")
    void shouldNotCheckDatabaseInHealthEndpoint() {
        // when
        Object dbHealthEnabled = properties.get("management.health.db.enabled");

        // then
        assertThat(dbHealthEnabled).isEqualTo(false);
    }

    @Test
    @DisplayName("요청 로그에 SQL을 남기지 않는다")
    void shouldNotLogSql() {
        // when
        Object showSql = properties.get("spring.jpa.show-sql");

        // then
        assertThat(showSql).isEqualTo(false);
    }
}
