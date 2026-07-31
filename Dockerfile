# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Tai dependency truoc de tan dung cache Docker layer
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Image nen mac dinh chay UTC, nhung toan bo nghiep vu (gio vao/ra, deadline dat cho, phu phi
# dem, quota theo khung gio...) dung LocalDateTime.now() gia dinh la gio Viet Nam. Neu khong ep
# timezone o day, moi thoi diem he thong ghi/tinh se lech 7 tieng so voi thuc te (VD: xe vao luc
# 09:19 gio VN bi luu thanh "03:19" - dung UTC nhung khong mang theo thong tin mui gio).
ENV TZ=Asia/Ho_Chi_Minh
# Render inject PORT; Spring doc PORT tu application.yml
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
