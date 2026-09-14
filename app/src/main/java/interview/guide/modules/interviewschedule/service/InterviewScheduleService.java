package interview.guide.modules.interviewschedule.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.interviewschedule.model.CreateInterviewRequest;
import interview.guide.modules.interviewschedule.model.InterviewScheduleDTO;
import interview.guide.modules.interviewschedule.model.InterviewScheduleEntity;
import interview.guide.modules.interviewschedule.model.InterviewStatus;
import interview.guide.modules.interviewschedule.repository.InterviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewScheduleService {

  private final InterviewScheduleRepository repository;

  @Transactional
  public InterviewScheduleDTO create(CreateInterviewRequest request) {
    InterviewScheduleEntity entity = new InterviewScheduleEntity();
    BeanUtils.copyProperties(request, entity);
    entity.setStatus(InterviewStatus.PENDING);

    return toDTO(repository.save(entity));
  }

  @Transactional
  public InterviewScheduleDTO update(Long id, CreateInterviewRequest request) {
    InterviewScheduleEntity entity = getByIdOrThrow(id);
    BeanUtils.copyProperties(request, entity, "id", "status");
    return toDTO(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    getByIdOrThrow(id);
    repository.deleteById(id);
  }

  @Transactional
  public InterviewScheduleDTO updateStatus(Long id, InterviewStatus status) {
    InterviewScheduleEntity entity = getByIdOrThrow(id);
    entity.setStatus(status);
    return toDTO(repository.save(entity));
  }

  public List<InterviewScheduleDTO> getAll(String status, LocalDateTime start, LocalDateTime end) {
    if ((start == null) != (end == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间和结束时间必须同时提供");
    }
    if (start != null && end.isBefore(start)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
    }

    List<InterviewScheduleEntity> entities;

    if (start != null && end != null) {
      entities = repository.findByInterviewTimeBetween(start, end);
    } else if (status != null && !status.isBlank()) {
      entities = repository.findByStatus(parseStatus(status));
    } else {
      entities = repository.findAll();
    }

    return entities.stream()
      .map(this::toDTO)
      .collect(Collectors.toList());
  }

  public InterviewScheduleDTO getById(Long id) {
    return toDTO(getByIdOrThrow(id));
  }

  private InterviewScheduleEntity getByIdOrThrow(Long id) {
    return repository.findById(id)
      .orElseThrow(() -> new BusinessException(
        ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND,
        "面试日程不存在: " + id));
  }

  private InterviewStatus parseStatus(String status) {
    try {
      return InterviewStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的面试状态: " + status);
    }
  }

  private InterviewScheduleDTO toDTO(InterviewScheduleEntity entity) {
    InterviewScheduleDTO dto = new InterviewScheduleDTO();
    BeanUtils.copyProperties(entity, dto);
    return dto;
  }
}
