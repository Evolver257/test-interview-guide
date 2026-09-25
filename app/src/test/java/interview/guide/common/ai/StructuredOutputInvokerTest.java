package interview.guide.common.ai;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

@DisplayName("结构化输出调用器测试")
class StructuredOutputInvokerTest {

  private static final String SYSTEM_PROMPT = "按 JSON 格式输出";
  private static final String USER_PROMPT = "生成面试题";

  @Test
  @DisplayName("IG-M2-AI-009：首次返回合法 JSON 时成功转换")
  void validJsonSucceedsOnFirstAttempt() {
    ChatFixture fixture = chatWithResponses("{\"answer\":\"42\"}");
    StructuredOutputInvoker invoker = newInvoker(2, true, new SimpleMeterRegistry(), true, 200);

    Payload result = invoke(invoker, fixture.client(), new BeanOutputConverter<>(Payload.class));

    assertThat(result.answer()).isEqualTo("42");
    verify(fixture.requestSpec()).system(contains("安全边界"));
    verify(fixture.callResponse()).content();
  }

  @Test
  @DisplayName("IG-M2-AI-010：JSON 字符串内未转义引号可本地修复")
  void unescapedQuoteIsRepairedLocally() {
    ChatFixture fixture = chatWithResponses("{\"answer\":\"say \"hello\"\"}");
    StructuredOutputInvoker invoker = newInvoker(2, false, null, true, 200);

    Payload result = invoke(invoker, fixture.client(), new BeanOutputConverter<>(Payload.class));

    assertThat(result.answer()).isEqualTo("say \"hello\"");
    verify(fixture.callResponse(), times(1)).content();
  }

  @Test
  @DisplayName("IG-M2-AI-011：首次解析失败后重试成功")
  void retriesAfterInvalidJsonAndReturnsNextValidResult() {
    ChatFixture fixture = chatWithResponses("not-json", "{\"answer\":\"recovered\"}");
    StructuredOutputInvoker invoker = newInvoker(2, false, null, true, 200);

    Payload result = invoke(invoker, fixture.client(), new BeanOutputConverter<>(Payload.class));

    assertThat(result.answer()).isEqualTo("recovered");
    verify(fixture.callResponse(), times(2)).content();
  }

  @Test
  @DisplayName("IG-M2-AI-012：达到最大重试次数后抛出业务异常")
  void throwsBusinessExceptionAfterMaximumAttempts() {
    ChatFixture fixture = chatWithResponses("not-json", "still-not-json");
    StructuredOutputInvoker invoker = newInvoker(2, false, null, true, 200);

    assertThatThrownBy(() -> invoke(invoker, fixture.client(), new BeanOutputConverter<>(Payload.class)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("结构化输出失败");
    verify(fixture.callResponse(), times(2)).content();
  }

  @Test
  @DisplayName("IG-M2-AI-013：重试提示追加严格 JSON 约束")
  void retryPromptContainsStrictJsonInstruction() {
    ChatFixture fixture = chatWithResponses("not-json", "{\"answer\":\"ok\"}");
    StructuredOutputInvoker invoker = newInvoker(2, false, null, true, 200);

    invoke(invoker, fixture.client(), new BeanOutputConverter<>(Payload.class));

    var prompts = forClass(String.class);
    verify(fixture.requestSpec(), times(2)).system(prompts.capture());
    assertThat(prompts.getAllValues().get(1))
        .contains("仅返回可被 JSON 解析器直接解析的 JSON 对象")
        .contains("安全边界");
  }

  @Test
  @DisplayName("IG-M2-AI-014：重试提示将错误信息压成单行并截断")
  @SuppressWarnings("unchecked")
  void retryErrorIsSanitizedAndTruncated() {
    ChatFixture fixture = chatWithResponses("ignored", "ignored");
    StructuredOutputInvoker invoker = newInvoker(2, false, null, true, 20);
    BeanOutputConverter<Payload> converter = mock(BeanOutputConverter.class);
    when(converter.convert(anyString()))
        .thenThrow(new IllegalArgumentException("X".repeat(40) + "\nsecond line"))
        .thenReturn(new Payload("fixed"));

    invoke(invoker, fixture.client(), converter);

    var prompts = forClass(String.class);
    verify(fixture.requestSpec(), times(2)).system(prompts.capture());
    assertThat(prompts.getAllValues().get(1))
        .contains("上次失败原因：" + "X".repeat(20) + "...")
        .doesNotContain("second line");
  }

  @Test
  @DisplayName("IG-M2-AI-015：成功调用记录尝试次数、调用次数和耗时")
  void successfulInvocationRecordsMetrics() {
    ChatFixture fixture = chatWithResponses("{\"answer\":\"ok\"}");
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    StructuredOutputInvoker invoker = newInvoker(2, true, registry, true, 200);

    invoke(invoker, fixture.client(), new BeanOutputConverter<>(Payload.class));

    assertThat(registry.get("app.ai.structured_output.attempts")
        .tag("context", "module_test").tag("status", "success").counter().count()).isEqualTo(1);
    assertThat(registry.get("app.ai.structured_output.invocations")
        .tag("context", "module_test").tag("status", "success").counter().count()).isEqualTo(1);
    assertThat(registry.get("app.ai.structured_output.latency")
        .tag("context", "module_test").tag("status", "success").timer().count()).isEqualTo(1);
  }

  @Test
  @DisplayName("IG-M2-AI-016：未注入指标注册器时调用仍可完成")
  void missingMeterRegistryDoesNotBreakInvocation() {
    ChatFixture fixture = chatWithResponses("{\"answer\":\"ok\"}");
    StructuredOutputInvoker invoker = newInvoker(2, true, null, true, 200);

    assertThat(invoke(invoker, fixture.client(), new BeanOutputConverter<>(Payload.class)).answer())
        .isEqualTo("ok");
  }

  private Payload invoke(
      StructuredOutputInvoker invoker,
      ChatClient client,
      BeanOutputConverter<Payload> converter
  ) {
    return invoker.invoke(
        client,
        SYSTEM_PROMPT,
        USER_PROMPT,
        converter,
        ErrorCode.AI_SERVICE_ERROR,
        "结构化输出失败：",
        "Module Test",
        LoggerFactory.getLogger(StructuredOutputInvokerTest.class)
    );
  }

  private StructuredOutputInvoker newInvoker(
      int attempts,
      boolean metricsEnabled,
      SimpleMeterRegistry registry,
      boolean includeLastError,
      int errorMaxLength
  ) {
    StructuredOutputProperties properties = new StructuredOutputProperties();
    properties.setStructuredMaxAttempts(attempts);
    properties.setStructuredMetricsEnabled(metricsEnabled);
    properties.setStructuredIncludeLastError(includeLastError);
    properties.setStructuredErrorMessageMaxLength(errorMaxLength);
    properties.setStructuredRetryUseRepairPrompt(true);
    properties.setStructuredRetryAppendStrictJsonInstruction(true);
    return new StructuredOutputInvoker(properties, registry);
  }

  private ChatFixture chatWithResponses(String... responses) {
    ChatClient client = mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec callResponse = mock(ChatClient.CallResponseSpec.class);
    when(client.prompt()).thenReturn(requestSpec);
    when(requestSpec.system(anyString())).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponse);
    when(callResponse.content())
        .thenReturn(responses[0], Arrays.copyOfRange(responses, 1, responses.length));
    return new ChatFixture(client, requestSpec, callResponse);
  }

  private record ChatFixture(
      ChatClient client,
      ChatClient.ChatClientRequestSpec requestSpec,
      ChatClient.CallResponseSpec callResponse
  ) {}

  private record Payload(String answer) {}
}
