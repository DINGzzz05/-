package studentCurriculum;

public record CourseSchedule(String room, int weekday, int startPeriod, int endPeriod, int capacity) {
    public boolean isValid() {
        return room != null && !room.isBlank() && weekday >= 1 && weekday <= 7
                && startPeriod >= 1 && endPeriod >= startPeriod && endPeriod <= 12 && capacity > 0;
    }

    public String periodLabel() {
        String day = "一二三四五六日".substring(weekday - 1, weekday);
        String periods = startPeriod == endPeriod ? String.valueOf(startPeriod) : startPeriod + "–" + endPeriod;
        return "周" + day + " · 第 " + periods + " 节";
    }
}
