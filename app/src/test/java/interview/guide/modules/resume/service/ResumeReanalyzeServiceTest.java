package interview.guide.modules.resume.service;

import interview.guide.common.config.AppConfigProperties;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.infrastructure.file.FileValidationService;
import interview.guide.modules.resume.listener.AnalyzeStreamProducer;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("简历重新分析服务单元测试")
class ResumeReanalyzeServiceTest {

  @Mock
  private ResumeParseService parseService;
  @Mock
  private FileStorageService storageService;
  @Mock
  private ResumePersistenceService persistenceService;
  @Mock
  private FileValidationService fileValidationService;
  @Mock
  private AnalyzeStreamProducer analyzeStreamProducer;
  @Mock
  private ResumeRepository resumeRepository;

  private ResumeUploadService service;

  @BeforeEach
  void setUp() {
    AppConfigProperties config = new AppConfigProperties();
    config.setAllowedTypes(List.of("application/pdf", "text/plain"));
    service = new ResumeUploadService(
        parseService,
        storageService,
        persistenceService,
        config,
        fileValidationService,
        analyzeStreamProducer,
        resumeRepository
    );
  }

  @Test
  @DisplayName("TC-UPLOAD-006：有缓存文本时更新状态并重新发送任务")
  void shouldReanalyzeUsingCachedText() {
    ResumeEntity existing = resume(104L, "resume.pdf", "key-104", "http://file/104");
    existing.setResumeText("缓存的简历文本");
    existing.setAnalyzeStatus(AsyncTaskStatus.FAILED);
    existing.setAnalyzeError("上次失败");
    when(resumeRepository.findById(104L)).thenReturn(Optional.of(existing));

    service.reanalyze(104L);

    assertThat(existing.getAnalyzeStatus()).isEqualTo(AsyncTaskStatus.PENDING);
    assertThat(existing.getAnalyzeError()).isNull();
    verify(resumeRepository).save(existing);
    verify(analyzeStreamProducer).sendAnalyzeTask(104L, "缓存的简历文本");
    verify(parseService, never()).downloadAndParseContent(any(), any());
  }

  @Test
  @DisplayName("TC-UPLOAD-007：简历不存在时返回业务异常")
  void shouldRejectReanalysisForMissingResume() {
    when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reanalyze(999L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("简历不存在")
        .extracting("code").isEqualTo(ErrorCode.RESUME_NOT_FOUND.getCode());
    verify(analyzeStreamProducer, never()).sendAnalyzeTask(any(), any());
  }

  private ResumeEntity resume(Long id, String filename, String storageKey, String storageUrl) {
    ResumeEntity entity = new ResumeEntity();
    entity.setId(id);
    entity.setOriginalFilename(filename);
    entity.setStorageKey(storageKey);
    entity.setStorageUrl(storageUrl);
    entity.setAnalyzeStatus(AsyncTaskStatus.PENDING);
    return entity;
  }
}
