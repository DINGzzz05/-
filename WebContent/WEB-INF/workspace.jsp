<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,studentCurriculum.CourseBean,studentCurriculum.Login,studentCurriculum.OfferingDao" %>
<%
    String role = (String) request.getAttribute("role");
    String accountName = Login.escapeHtml((String) session.getAttribute("accountName"));
    long[] counts = (long[]) request.getAttribute("counts");
    boolean student = "学生".equals(role);
    boolean teacher = "教师".equals(role);
    String[][] links = student ? new String[][]{{"StuChoose","选课"},{"StuCourse","个人课程"},{"StuGrade","个人成绩"},{"StuProfile","个人信息"},{"StuCPassword","修改密码"}}
            : teacher ? new String[][]{{"TeaCourse","任课查询"},{"TeaSetup","开课"},{"TeaIn","成绩录入"},{"TeaAve","平均成绩"},{"TeaGpa","均绩查询"},{"TeaSingle","单门成绩及排名"},{"TeaProfile","个人信息"},{"TeaCPassword","修改密码"}}
            : new String[][]{{"AdmTeacher","教师管理"},{"AdmStudent","学生管理"},{"AdmCourse","课程管理"},{"AdmCPassword","修改密码"}};
    String[] metrics = student ? new String[]{"开放的教学班","本学期已选","排队中"}
            : teacher ? new String[]{"本学期任课","待排课","候补申请"} : new String[]{"教师","学生","课程"};
%>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= role %>工作台 · 高校选课管理系统</title>
    <link rel="stylesheet" href="assets/font-awesome/4.5.0/css/font-awesome.min.css">
    <link rel="stylesheet" href="assets/css/workspace.css">
</head>
<body>
    <header class="work-header">
        <a href="Workspace" class="work-brand"><i class="fa fa-graduation-cap" aria-hidden="true"></i> 高校选课管理</a>
        <div class="work-account"><span><%= accountName %></span><a href="logout">退出登录</a></div>
    </header>
    <div class="work-shell">
        <nav aria-label="工作台导航" class="work-nav">
            <a href="Workspace" aria-current="page">工作台</a>
            <% for (String[] link : links) { %>
                <a href="<%= link[0] %>"><%= link[1] %></a>
            <% } %>
        </nav>
        <main class="work-main">
            <div class="work-title">
                <div>
                    <h1><%= role %>工作台</h1>
                    <% if (student || teacher) { %><p><%= request.getAttribute("year") %> 学年下学期</p><% } %>
                </div>
                <% if (student || teacher) { %>
                    <a class="work-primary" href="<%= student ? "StuChoose" : "TeaSetup" %>"><%= student ? "进入选课" : "开设课程" %></a>
                <% } %>
            </div>
            <dl class="work-metrics">
                <% for (int i = 0; i < metrics.length; i++) { %>
                    <div><dt><%= metrics[i] %></dt><dd><%= counts[i] %></dd></div>
                <% } %>
            </dl>
            <% if (student) {
                List<CourseBean> available = (List<CourseBean>) request.getAttribute("available");
                List<OfferingDao.WaitlistItem> waitlist = (List<OfferingDao.WaitlistItem>) request.getAttribute("waitlist"); %>
                <section class="work-section" aria-labelledby="open-courses">
                    <div class="work-section-head"><h2 id="open-courses">开放选课</h2><a href="StuChoose">查看全部</a></div>
                    <% if (available.isEmpty()) { %>
                        <p class="work-empty">当前没有可申请的已排课教学班。</p>
                    <% } else { %>
                        <div class="work-scroll"><table>
                            <thead><tr><th>课程</th><th>教师</th><th>上课安排</th><th>名额</th></tr></thead>
                            <tbody>
                                <% for (CourseBean course : available) { %>
                                    <tr><th scope="row"><%= Login.escapeHtml(course.getCoursename()) %></th>
                                    <td><%= Login.escapeHtml(course.getTeacher()) %></td>
                                    <td><%= Login.escapeHtml(course.getScheduleLabel()) %></td>
                                    <td><span class="<%= course.getRemainingSeats() == 0 ? "work-full" : "work-open" %>"><%= course.getRemainingSeats() == 0 ? "可候补" : "剩余 " + course.getRemainingSeats() %></span></td></tr>
                                <% } %>
                            </tbody>
                        </table></div>
                    <% } %>
                </section>
                <section class="work-section" aria-labelledby="my-waitlist">
                    <div class="work-section-head"><h2 id="my-waitlist">我的候补</h2><a href="StuCourse#waitlist">管理候补</a></div>
                    <% if (waitlist.isEmpty()) { %>
                        <p class="work-empty">暂无候补记录。</p>
                    <% } else { %>
                        <div class="work-scroll"><table>
                            <thead><tr><th>课程</th><th>教师</th><th>上课安排</th><th>状态</th></tr></thead>
                            <tbody>
                                <% for (OfferingDao.WaitlistItem item : waitlist) { %>
                                    <tr><th scope="row"><%= Login.escapeHtml(item.course()) %></th><td><%= Login.escapeHtml(item.teacher()) %></td>
                                    <td><%= Login.escapeHtml(item.schedule()) %></td><td><%= item.conflict() ? "递补时课表冲突" : "排队第 " + item.position() + " 位" %></td></tr>
                                <% } %>
                            </tbody>
                        </table></div>
                    <% } %>
                </section>
            <% } else if (teacher) {
                List<CourseBean> courses = (List<CourseBean>) request.getAttribute("courses"); %>
                <section class="work-section" aria-labelledby="my-teaching">
                    <div class="work-section-head"><h2 id="my-teaching">本学期教学班</h2><a href="TeaCourse">管理排课与容量</a></div>
                    <% if (courses.isEmpty()) { %>
                        <p class="work-empty">本学期暂无教学班。</p>
                    <% } else { %>
                        <div class="work-scroll"><table>
                            <thead><tr><th>课程</th><th>上课安排</th><th>已选 / 容量</th><th>候补</th></tr></thead>
                            <tbody>
                                <% for (CourseBean course : courses) { %>
                                    <tr><th scope="row"><%= Login.escapeHtml(course.getCoursename()) %></th><td><%= Login.escapeHtml(course.getScheduleLabel()) %></td>
                                    <td><%= course.getEnrolledCount() %> / <%= course.getSchedule() == null ? "待设置" : course.getSchedule().capacity() %></td>
                                    <td><%= course.getWaitingCount() %> 人</td></tr>
                                <% } %>
                            </tbody>
                        </table></div>
                    <% } %>
                </section>
            <% } else { %>
                <section class="work-section" aria-labelledby="maintenance">
                    <div class="work-section-head"><h2 id="maintenance">基础资料维护</h2></div>
                    <ul class="work-maintenance">
                        <li><span><strong>教师管理</strong><small>维护教师编号、姓名、职称和联系方式</small></span><a href="AdmTeacher">管理教师</a></li>
                        <li><span><strong>学生管理</strong><small>维护学生档案和班级归属</small></span><a href="AdmStudent">管理学生</a></li>
                        <li><span><strong>课程管理</strong><small>维护课程编号、学时、考核方式和学分</small></span><a href="AdmCourse">管理课程</a></li>
                    </ul>
                </section>
            <% } %>
        </main>
    </div>
</body>
</html>
