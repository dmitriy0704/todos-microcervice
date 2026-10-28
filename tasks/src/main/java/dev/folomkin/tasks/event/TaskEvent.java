package dev.folomkin.tasks.event;

public class TaskEvent {
    private Long taskId;
    private String title;
    private String userId;
    private String status;


    public TaskEvent() {
    }

    public TaskEvent(
            Long taskId,
            String title,
            String userId,
            String status
    ) {
        this.taskId = taskId;
        this.title = title;
        this.userId = userId;
        this.status = status;
    }


    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
