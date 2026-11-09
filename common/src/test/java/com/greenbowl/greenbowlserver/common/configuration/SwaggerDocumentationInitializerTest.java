package com.greenbowl.greenbowlserver.common.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import springfox.documentation.spring.web.plugins.DocumentationPluginsBootstrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SwaggerDocumentationInitializerTest {
    private static final int WEB_SERVER_START_PHASE = Integer.MAX_VALUE - 1;

    @Mock
    private DocumentationPluginsBootstrapper documentationPluginsBootstrapper;

    @Test
    @DisplayName("시작하면 Springfox 문서 스캔을 실행하고 실행 중 상태가 된다")
    void shouldStartDocumentationScanWhenStarted() {
        // given
        SwaggerDocumentationInitializer initializer =
                new SwaggerDocumentationInitializer(documentationPluginsBootstrapper);

        // when
        initializer.start();

        // then
        verify(documentationPluginsBootstrapper).start();
        verifyNoMoreInteractions(documentationPluginsBootstrapper);
        assertThat(initializer.isRunning()).isTrue();
    }

    @Test
    @DisplayName("중지하면 실행 중 상태가 해제된다")
    void shouldNotBeRunningWhenStopped() {
        // given
        SwaggerDocumentationInitializer initializer =
                new SwaggerDocumentationInitializer(documentationPluginsBootstrapper);
        initializer.start();

        // when
        initializer.stop();

        // then
        assertThat(initializer.isRunning()).isFalse();
    }

    @Test
    @DisplayName("문서 스캔 전에 ObjectMapper가 준비되도록 핸들러 어댑터는 지연 초기화에서 제외한다")
    void shouldExcludeHandlerAdapterFromLazyInitialization() {
        // given
        LazyInitializationExcludeFilter filter = SwaggerDocumentationInitializer.handlerAdapterEagerInitialization();
        RootBeanDefinition beanDefinition = new RootBeanDefinition();

        // when
        boolean adapterExcluded = filter.isExcluded("adapter", beanDefinition, RequestMappingHandlerAdapter.class);
        boolean otherExcluded = filter.isExcluded("other", beanDefinition, Object.class);

        // then
        assertThat(adapterExcluded).isTrue();
        assertThat(otherExcluded).isFalse();
    }

    @Test
    @DisplayName("웹 서버가 포트를 여는 단계보다 먼저 실행된다")
    void shouldRunBeforeWebServerStartPhase() {
        // given
        SwaggerDocumentationInitializer initializer =
                new SwaggerDocumentationInitializer(documentationPluginsBootstrapper);

        // when
        int phase = initializer.getPhase();

        // then
        assertThat(phase).isLessThan(WEB_SERVER_START_PHASE);
    }
}
