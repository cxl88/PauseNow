# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

This is the **server** component of the PauseNow repo: a RuoYi-Vue v3.9.2 scaffold — a Spring Boot 4 + Vue 2前后端分离的 Java 快速开发框架 (user/role/dept/menu/dict management, scheduled tasks, code generator, system monitor). It is a self-contained admin backend, not part of the Android app's Gradle build.

The directory was renamed from `RuoYi-Vue-master/` to `server/` (rename is **uncommitted** — git currently shows the old `RuoYi-Vue-master/` paths as deleted and `server/` as untracked). The parent `../CLAUDE.md` documents the Android app and still describes this directory as "parked/unrelated" by its old name; treat that note as stale when working here. There is no build coupling between `server/` and `../android/`.

Stack: Spring Boot 4.0.6, JDK 17, MyBatis (+ PageHelper), Druid, Redis, Spring Security + JWT, Kaptcha, SpringDoc OpenAPI. Frontend: Vue 2.6 + Element UI 2.15 + Vuex + Vue Router 3 (Vue CLI 4).

## Commands

### Backend (Maven, run from `server/`)

There is **no Maven wrapper** (`mvnw`) — `mvn` must be on PATH (JDK 17 required).

```bash
mvn clean package -Dmaven.test.skip=true     # build all modules -> ruoyi-admin/target/ruoyi-admin.jar  (same as bin/package.bat)
mvn -pl ruoyi-admin -am clean package         # build just the runnable module + its deps
mvn -pl ruoyi-system test                     # run tests in a single module
java -jar ruoyi-admin/target/ruoyi-admin.jar  # run the built jar  (bin/run.bat / ry.sh start do this)
```

`ry.sh {start|stop|restart|status}` and `ry.bat` manage the packaged `ruoyi-admin.jar` as a background process with JVM opts. `bin/clean.bat` clears `target/`.

### Frontend (run from `server/ruoyi-ui/`)

```bash
npm install
npm run dev          # vue-cli-service serve, dev server on port 80, proxies /dev-api -> http://localhost:8080
npm run build:prod   # production build -> dist/  (VUE_APP_BASE_API=/prod-api)
npm run build:stage  # staging build
```

### Runtime prerequisites

- **MySQL** — load schema `sql/ry_20260417.sql` (tables + seed data) and `sql/quartz.sql` (scheduler tables) into the `ruoyi` database. Datasource is configured in `ruoyi-admin/src/main/resources/application-druid.yml`.
- **Redis** on `localhost:6379` (sessions/cache) — see `application.yml`.
- Backend listens on `:8080`, context-path `/`. Frontend dev server on `:80` proxies `/dev-api` to it.

## Architecture

### Multi-module Maven layout (parent `pom.xml`, `packaging=pom`)

| Module | Role |
|---|---|
| `ruoyi-admin` | Web entry. `RuoYiApplication` (`@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)` — RuoYi uses its own dynamic Druid datasource). All controllers live in `com.ruoyi.web.controller.*`. `RuoYiServletInitializer` enables WAR deployment. Builds to `ruoyi-admin.jar` (`finalName = ruoyi-admin`). |
| `ruoyi-framework` | Technical layer: Spring Security + JWT (`security/`), dynamic datasource master/slave switching (`datasource/`), AOP aspects (`aspectj/`), interceptors, web/config. |
| `ruoyi-system` | System business: `domain` / `mapper` / `service` for user, role, dept, menu, dict, config, notice, oper/login logs. |
| `ruoyi-quartz` | Scheduled-task management (Quartz). |
| `ruoyi-generator` | Velocity-template code generator (CRUD java/xml/sql/html). |
| `ruoyi-common` | Shared utils, constants, enums, exceptions, annotations, XSS filter. |

Dependency direction: `admin → framework → system → common`; `quartz` and `generator` depend on `framework`.

### Request flow

```
HTTP request
  └─ Security filter chain (JwtAuthenticationTokenFilter reads token.header=Authorization)
  └─ Controller under com.ruoyi.web.controller.*  → returns AjaxJson-style {code,msg,data}
  └─ Service (ruoyi-system) → MyBatis Mapper interface
  └─ mapper/**/*Mapper.xml (typeAliasesPackage=com.ruoyi.**.domain)
  └─ Druid datasource (master, optional slave) → MySQL
```

PageHelper handles pagination (`helperDialect: mysql`); pass `pageNum`/`pageSize` on list endpoints.

### Controller packages (`ruoyi-admin/.../web/controller/`)

- `common/` — `CaptchaController` (Kaptcha image, `captchaType: math`), `CommonController` (file upload/download to `ruoyi.profile`).
- `monitor/` — cache, server (OS/JVM stats via oshi), login & oper logs, online users.
- `system/` — SysUser, SysRole, SysDept, SysMenu, SysDictType/Data, SysConfig, SysNotice, SysProfile, SysRegister, SysLogin (JWT issue/refresh).
- `tool/` — `TestController`, the SpringDoc demo group (`springdoc.group-configs` scans `com.ruoyi.web.controller.tool`).

### Frontend (`ruoyi-ui/`)

Vue CLI 4 project. `src/` uses Vuex (`store/`), Vue Router 3 (`router/`), and an axios instance (`src/utils/request.js`) with base `VUE_APP_BASE_API` (`/dev-api` in dev, `/prod-api` in prod). `vue.config.js` proxies that prefix to `http://localhost:8080` and also proxies `/v3/api-docs/*` so the frontend can reach SpringDoc. API base URLs and titles come from `.env.development` / `.env.staging` / `.env.production`.

## Key configuration (`ruoyi-admin/src/main/resources/`)

- `application.yml` — server port 8080, Redis, MyBatis mapper scan, PageHelper, SpringDoc paths, XSS filter (`/system/*,/monitor/*,/tool/*`, excludes `/system/notice`), JWT (header `Authorization`, 30 min), file upload `ruoyi.profile` (default `D:/ruoyi/uploadPath` on Windows).
- `application-druid.yml` — Druid datasource (master URL/creds, slave disabled), pool sizing, Druid monitor servlet at `/druid/*`.
- `logback.xml`, `i18n/messages.properties`, `mybatis/mybatis-config.xml`.

Useful URLs once running: Swagger UI at `/swagger-ui.html`, API docs at `/v3/api-docs`, Druid monitor at `/druid/*`.

## Gotchas

- **Hardcoded credentials are committed** in `application.yml` / `application-druid.yml`: the MySQL root password, Druid monitor login (`ruoyi/123456`), and the JWT signing secret. Externalize these (env vars / profile-specific config) before any non-local deployment, and never reuse them. The seeded admin login is `admin / admin123` (from `sql/ry_20260417.sql`, also the RuoYi demo credential).
- The datasource in `application-druid.yml` points at a **remote** MySQL host — confirm reachability before starting the backend, or repoint it to a local instance.
- `mvn` is not on PATH in the default shell here and there is no wrapper — install Maven or run builds from IntelliJ (the `.idea/` is present).
- The directory rename `RuoYi-Vue-master/` → `server/` is uncommitted. If you `git add`/commit, do it as a rename (`git add -A` from the repo root) so history follows.
- This scaffold is upstream RuoYi largely unmodified — when fixing behavior, check the upstream RuoYi v3.9.2 docs (`http://doc.ruoyi.vip`) before assuming project-specific customization.
