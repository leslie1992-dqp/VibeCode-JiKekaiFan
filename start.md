# 后端（Spring Boot，在 backend 目录）
cd d:\CursorDemo_Food\backend
mvn spring-boot:run

# 前端（Vue + Vite，在 frontend 目录，不要用 npm 跑 backend）
cd d:\CursorDemo_Food\frontend
npm run dev

# 在 Cursor 按 Ctrl+Shift+P（Mac：Cmd+Shift+P），输入并选择：
Simple Browser: Show
在地址栏里粘贴 http://localhost:5173 回车

# kafka 启动
zookeeper：bin\windows\zookeeper-server-start.bat config\zookeeper.properties

kafka：bin\windows\kafka-server-start.bat config\server.properties
