package interview.guide.infrastructure.file;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("模块二 AI 生成：文件哈希补充测试")
class FileHashServiceAiGeneratedTest {

  private final FileHashService service = new FileHashService();

  @Test
  @DisplayName("AI-M2-HASH-001：空内容生成标准 SHA-256 哈希")
  void shouldCalculateKnownHashForEmptyContent() {
    assertThat(service.calculateHash(new byte[0]))
        .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  @DisplayName("AI-M2-HASH-002：字节数组和输入流计算结果一致")
  void shouldProduceSameHashForBytesAndStream() {
    byte[] data = "Java简历😀".getBytes(StandardCharsets.UTF_8);

    assertThat(service.calculateHash(new ByteArrayInputStream(data)))
        .isEqualTo(service.calculateHash(data));
  }

  @Test
  @DisplayName("AI-M2-HASH-003：MultipartFile 读取失败转换为业务异常")
  void shouldConvertMultipartReadFailureToBusinessException() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getBytes()).thenThrow(new IOException("disk error"));

    assertThatThrownBy(() -> service.calculateHash(file))
        .isInstanceOf(BusinessException.class)
        .hasMessage("计算文件哈希失败")
        .extracting("code").isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
  }

  @Test
  @DisplayName("AI-M2-HASH-004：输入流读取中断转换为业务异常")
  void shouldConvertStreamReadFailureToBusinessException() {
    InputStream brokenStream = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("stream interrupted");
      }

      @Override
      public int read(byte[] buffer, int offset, int length) throws IOException {
        throw new IOException("stream interrupted");
      }
    };

    assertThatThrownBy(() -> service.calculateHash(brokenStream))
        .isInstanceOf(BusinessException.class)
        .hasMessage("计算文件哈希失败")
        .extracting("code").isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
  }
}
