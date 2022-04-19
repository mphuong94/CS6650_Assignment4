package util;

public class LiftInfo {
    private Integer skierId;
    private Integer liftId;
    private Integer minute;
    private Integer waitTime;
    private Integer resortId;
    private Integer dayId;
    private Integer seasonId;

    public LiftInfo(Integer skierId, Integer liftId, Integer minute, Integer waitTime, Integer resortId, Integer dayId, Integer seasonId) {
        this.skierId = skierId;
        this.liftId = liftId;
        this.minute = minute;
        this.waitTime = waitTime;
        this.resortId = resortId;
        this.dayId = dayId;
        this.seasonId = seasonId;
    }

    public Integer getSkierId() {
        return skierId;
    }

    public void setSkierId(Integer skierId) {
        this.skierId = skierId;
    }

    public Integer getLiftId() {
        return liftId;
    }

    public void setLiftId(Integer liftId) {
        this.liftId = liftId;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }

    public Integer getResortId() {
        return resortId;
    }

    public void setResortId(Integer resortId) {
        this.resortId = resortId;
    }

    public Integer getDayId() {
        return dayId;
    }

    public void setDayId(Integer dayId) {
        this.dayId = dayId;
    }

    public Integer getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(Integer seasonId) {
        this.seasonId = seasonId;
    }

    @Override
    public String toString() {
        return "LiftInfo{" +
                "skierId=" + skierId +
                ", liftId=" + liftId +
                ", minute=" + minute +
                ", waitTime=" + waitTime +
                ", resortId=" + resortId +
                ", dayId=" + dayId +
                ", seasonId=" + seasonId +
                '}';
    }
}
