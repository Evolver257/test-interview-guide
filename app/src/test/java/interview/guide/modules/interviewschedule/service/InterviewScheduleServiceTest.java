package interview.guide.modules.interviewschedule.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.interviewschedule.model.CreateInterviewRequest;
import interview.guide.modules.interviewschedule.model.InterviewScheduleDTO;
import interview.guide.modules.interviewschedule.model.InterviewScheduleEntity;
import interview.guide.modules.interviewschedule.model.InterviewStatus;
import interview.guide.modules.interviewschedule.repository.InterviewScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("面试日程服务测试")
class InterviewScheduleServiceTest {

  private static final LocalDateTime INTERVIEW_TIME = LocalDateTime.of(2026, 9, 15, 10, 0);

  @Mock
  private InterviewScheduleRepository repository;

  @InjectMocks
  private InterviewScheduleService service;

  @Nested
  @DisplayName("创建日程")
  class CreateTests {

    @Test
    @DisplayName("TC-001 创建完整日程并默认设置待面试状态")
    void createShouldCopyFieldsAndSetPendingStatus() {
      CreateInterviewRequest request = request("Acme", "Java 后端");
      InterviewScheduleEntity saved = entity(1L, "Acme", "Java 后端", InterviewStatus.PENDING);
      when(repository.save(any(InterviewScheduleEntity.class))).thenReturn(saved);

      InterviewScheduleDTO result = service.create(request);

      ArgumentCaptor<InterviewScheduleEntity> captor =
              ArgumentCaptor.forClass(InterviewScheduleEntity.class);
      verify(repository).save(captor.capture());
      InterviewScheduleEntity input = captor.getValue();
      assertThat(input.getCompanyName()).isEqualTo("Acme");
      assertThat(input.getPosition()).isEqualTo("Java 后端");
      assertThat(input.getInterviewTime()).isEqualTo(INTERVIEW_TIME);
      assertThat(input.getStatus()).isEqualTo(InterviewStatus.PENDING);
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getStatus()).isEqualTo(InterviewStatus.PENDING);
    }

    @Test
    @DisplayName("TC-002 创建日程时复制可选字段")
    void createShouldCopyOptionalFields() {
      CreateInterviewRequest request = request("Acme", "测试开发");
      request.setInterviewType("VIDEO");
      request.setMeetingLink("https://example.test/meeting");
      request.setRoundNumber(2);
      request.setInterviewer("李老师");
      request.setNotes("准备项目经历");
      when(repository.save(any(InterviewScheduleEntity.class)))
              .thenAnswer(invocation -> invocation.getArgument(0));

      InterviewScheduleDTO result = service.create(request);

      assertThat(result.getInterviewType()).isEqualTo("VIDEO");
      assertThat(result.getMeetingLink()).isEqualTo("https://example.test/meeting");
      assertThat(result.getRoundNumber()).isEqualTo(2);
      assertThat(result.getInterviewer()).isEqualTo("李老师");
      assertThat(result.getNotes()).isEqualTo("准备项目经历");
    }

    @Test
    @DisplayName("TC-003 创建日程时保留请求中的轮次默认值")
    void createShouldCopyDefaultRoundNumber() {
      CreateInterviewRequest request = request("Acme", "产品经理");
      when(repository.save(any(InterviewScheduleEntity.class)))
              .thenAnswer(invocation -> invocation.getArgument(0));

      InterviewScheduleDTO result = service.create(request);

      assertThat(result.getRoundNumber()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("更新日程")
  class UpdateTests {

    @Test
    @DisplayName("TC-004 更新已存在日程的业务字段")
    void updateShouldCopyRequestFields() {
      InterviewScheduleEntity existing = entity(7L, "旧公司", "旧岗位", InterviewStatus.COMPLETED);
      CreateInterviewRequest request = request("新公司", "新岗位");
      when(repository.findById(7L)).thenReturn(Optional.of(existing));
      when(repository.save(existing)).thenReturn(existing);

      InterviewScheduleDTO result = service.update(7L, request);

      assertThat(result.getId()).isEqualTo(7L);
      assertThat(result.getCompanyName()).isEqualTo("新公司");
      assertThat(result.getPosition()).isEqualTo("新岗位");
      verify(repository).save(existing);
    }

    @Test
    @DisplayName("TC-005 更新日程时不覆盖已有状态")
    void updateShouldPreserveStatus() {
      InterviewScheduleEntity existing = entity(8L, "Acme", "后端", InterviewStatus.CANCELLED);
      when(repository.findById(8L)).thenReturn(Optional.of(existing));
      when(repository.save(existing)).thenReturn(existing);

      service.update(8L, request("Acme", "后端二面"));

      assertThat(existing.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
    }

    @Test
    @DisplayName("TC-006 更新不存在日程时返回业务异常")
    void updateShouldRejectMissingSchedule() {
      when(repository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.update(99L, request("Acme", "后端")))
              .isInstanceOf(BusinessException.class)
              .hasMessageContaining("面试日程不存在");
      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("TC-007 更新日程时同步修改可选字段")
    void updateShouldCopyOptionalFields() {
      InterviewScheduleEntity existing = entity(9L, "Acme", "后端", InterviewStatus.PENDING);
      CreateInterviewRequest request = request("Acme", "后端");
      request.setInterviewType("PHONE");
      request.setInterviewer("王老师");
      when(repository.findById(9L)).thenReturn(Optional.of(existing));
      when(repository.save(existing)).thenReturn(existing);

      InterviewScheduleDTO result = service.update(9L, request);

      assertThat(result.getInterviewType()).isEqualTo("PHONE");
      assertThat(result.getInterviewer()).isEqualTo("王老师");
    }
  }

  @Nested
  @DisplayName("删除日程")
  class DeleteTests {

    @Test
    @DisplayName("TC-008 删除已存在日程")
    void deleteShouldRemoveExistingSchedule() {
      when(repository.findById(10L)).thenReturn(Optional.of(
              entity(10L, "Acme", "后端", InterviewStatus.PENDING)));

      service.delete(10L);

      verify(repository).deleteById(10L);
    }

    @Test
    @DisplayName("TC-009 删除不存在日程时返回业务异常")
    void deleteShouldRejectMissingSchedule() {
      when(repository.findById(11L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.delete(11L))
              .isInstanceOfSatisfying(BusinessException.class, exception -> {
                assertThat(exception.getMessage()).contains("面试日程不存在");
                assertThat(exception.getCode())
                        .isEqualTo(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND.getCode());
              });
      verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("TC-010 重复删除日程时第二次返回不存在业务异常")
    void deleteShouldRejectSecondDeletion() {
      InterviewScheduleEntity existing = entity(
              31L, "Acme", "后端", InterviewStatus.PENDING);
      when(repository.findById(31L))
              .thenReturn(Optional.of(existing))
              .thenReturn(Optional.empty());

      service.delete(31L);

      assertThatThrownBy(() -> service.delete(31L))
              .isInstanceOfSatisfying(BusinessException.class, exception -> {
                assertThat(exception.getMessage()).contains("面试日程不存在");
                assertThat(exception.getCode())
                        .isEqualTo(ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND.getCode());
              });
      verify(repository, times(1)).deleteById(31L);
    }
  }

  private static CreateInterviewRequest request(String companyName, String position) {
    CreateInterviewRequest request = new CreateInterviewRequest();
    request.setCompanyName(companyName);
    request.setPosition(position);
    request.setInterviewTime(INTERVIEW_TIME);
    return request;
  }

  private static InterviewScheduleEntity entity(
          long id, String companyName, String position, InterviewStatus status) {
    InterviewScheduleEntity entity = new InterviewScheduleEntity();
    entity.setId(id);
    entity.setCompanyName(companyName);
    entity.setPosition(position);
    entity.setInterviewTime(INTERVIEW_TIME);
    entity.setRoundNumber(1);
    entity.setStatus(status);
    return entity;
  }
}
