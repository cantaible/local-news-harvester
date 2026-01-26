package com.example.springboot3newsreader.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot3newsreader.models.ThumbnailTask;

public interface ThumbnailTaskRepository extends JpaRepository<ThumbnailTask, Long> {
  // 查询可执行任务：
  // 1) 状态在 WAITING/FAILED 等可重试集合中
  // 2) nextRetryAt 为空或已经到达（允许再次执行）
  // 3) 按 ID 升序，保证处理顺序稳定
  @Query("select t from ThumbnailTask t "
    + "where t.status in :statuses and (t.nextRetryAt is null or t.nextRetryAt <= :now) "
    + "order by t.id asc")
  List<ThumbnailTask> findReadyTasks(
    @Param("statuses") List<String> statuses,
    @Param("now") LocalDateTime now,
    Pageable pageable);
}
