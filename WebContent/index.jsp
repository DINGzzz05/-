<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<% boolean demo = "true".equals(System.getenv("SCS_DEMO")); %>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>登录 · 高校选课管理系统</title>
    <link rel="stylesheet" href="assets/font-awesome/4.5.0/css/font-awesome.min.css">
    <link rel="stylesheet" href="assets/css/curriculum.css">
</head>
<body class="signin">
    <main class="signin-layout">
        <section class="signin-brand" aria-labelledby="system-title">
            <div class="brand-mark"><i class="fa fa-graduation-cap" aria-hidden="true"></i></div>
            <div class="brand-copy">
                <h1 id="system-title">高校选课<br>管理系统</h1>
                <p>选课、教学与成绩管理</p>
            </div>
            <div class="brand-grid" aria-hidden="true"></div>
        </section>
        <section class="signin-panel" aria-labelledby="login-title">
            <form action="Login" method="post" class="signin-form">
                <h2 id="login-title">登录系统</h2>
                <fieldset class="role-field">
                    <legend>登录身份</legend>
                    <div class="role-options">
                        <label><input type="radio" name="status" value="学生" checked><span><i class="fa fa-user" aria-hidden="true"></i>学生</span></label>
                        <label><input type="radio" name="status" value="教师"><span><i class="fa fa-book" aria-hidden="true"></i>教师</span></label>
                        <label><input type="radio" name="status" value="管理员"><span><i class="fa fa-cog" aria-hidden="true"></i>管理员</span></label>
                    </div>
                </fieldset>
                <label class="signin-label" for="num">账号</label>
                <input id="num" name="num" type="text" autocomplete="username" maxlength="12" placeholder="输入学号或编号" required>
                <label class="signin-label" for="password">密码</label>
                <input id="password" name="password" type="password" autocomplete="current-password" placeholder="输入密码" required>
                <% if (request.getAttribute("fail") != null) { %>
                    <p class="signin-error" role="alert"><%= request.getAttribute("fail") %></p>
                <% } %>
                <button class="signin-submit" type="submit">登录 <i class="fa fa-arrow-right" aria-hidden="true"></i></button>
                <% if (demo) { %>
                    <div class="demo-box">
                        <p>演示账号 <small>点击填充，初始密码统一为 123456</small></p>
                        <div class="demo-options">
                            <button type="button" data-role="学生" data-account="201706060101">学生</button>
                            <button type="button" data-role="教师" data-account="T0001">教师</button>
                            <button type="button" data-role="管理员" data-account="A001">管理员</button>
                        </div>
                    </div>
                <% } %>
            </form>
        </section>
    </main>
    <script src="assets/js/curriculum-login.js" defer></script>
</body>
</html>
