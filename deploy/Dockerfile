FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/personal
COPY backend/build/install/backend/ /opt/personal/
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/opt/personal/bin/backend"]