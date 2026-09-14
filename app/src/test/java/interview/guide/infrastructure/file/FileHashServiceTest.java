package interview.guide.infrastructure.file;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("文件哈希服务单元测试")
class FileHashServiceTest {

  private FileHashService service;

  @BeforeEach
  void setUp() {
    service = new FileHashService();
  }

  @Test
  @DisplayName("TC-HASH-001：已知文本生成正确的 SHA-256")
  void shouldCalculateKnownSha256() {
    assertThat(service.calculateHash("abc".getBytes(StandardCharsets.UTF_8)))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  @DisplayName("TC-HASH-002：空内容生成标准 SHA-256")
  void shouldCalculateHashForEmptyContent() {
    assertThat(service.calculateHash(new byte[0]))
        .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  @DisplayName("TC-HASH-003：字节数组和输入流计算结果一致")
  void shouldProduceSameHashForBytesAndStream() {
    byte[] data = "同一份简历".getBytes(StandardCharsets.UTF_8);

    assertThat(service.calculateHash(new ByteArrayInputStream(data)))
        .isEqualTo(service.calculateHash(data));
  }

  @Test
  @DisplayName("TC-HASH-004：MultipartFile 与文件内容计算结果一致")
  void shouldCalculateHashForMultipartFile() {
    byte[] data = "resume-content".getBytes(StandardCharsets.UTF_8);
    var file = new MockMultipartFile("file", "resume.txt", "text/plain", data);

    assertThat(service.calculateHash(file)).isEqualTo(service.calculateHash(data));
  }

  @Test
  @DisplayName("TC-HASH-005：输入流读取失败时转换为业务异常")
  void shouldWrapStreamReadFailureAsBusinessException() {
    InputStream brokenStream = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("模拟读取失败");
      }

      @Override
      public int read(byte[] bytes, int offset, int length) throws IOException {
        throw new IOException("模拟读取失败");
      }
    };

    assertThatThrownBy(() -> service.calculateHash(brokenStream))
        .isInstanceOf(BusinessException.class)
        .hasMessage("计算文件哈希失败")
        .extracting("code").isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
  }
}
