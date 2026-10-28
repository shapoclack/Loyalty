# syntax=docker/dockerfile:1
# JavaFX 25 requires Java 23+. Docker uses the available Java 25 LTS images;
# the local Maven build can continue using Java 27.
# JavaFX 25.0.1 has Linux x86_64 natives: build with --platform=linux/amd64
# (Compose already sets this platform for app and console).
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -Dmaven.compiler.release=25 verify

FROM eclipse-temurin:25-jre-noble AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       ca-certificates fontconfig fonts-dejavu-core libgtk-3-0t64 \
       libasound2t64 libgl1 libgl1-mesa-dri libxi6 libxrender1 libxtst6 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 loyalty \
    && useradd --uid 10001 --gid loyalty --create-home --shell /bin/bash loyalty \
    && mkdir -p /home/loyalty/exports /opt/loyalty \
    && chown -R loyalty:loyalty /home/loyalty
COPY --from=build /build/target/Loyalty-1.0-SNAPSHOT.jar /opt/loyalty/loyalty.jar
ENV LANG=C.UTF-8 \
    LOYALTY_EXPORT_DIR=/home/loyalty/exports
WORKDIR /home/loyalty
USER loyalty

FROM runtime AS console
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/opt/loyalty/loyalty.jar"]
CMD ["--console"]

FROM runtime AS desktop
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       xvfb x11-utils x11vnc openbox novnc websockify tini curl \
    && rm -rf /var/lib/apt/lists/*
COPY --chmod=755 docker/desktop-entrypoint.sh /usr/local/bin/loyalty-desktop
ENV DISPLAY=:99 \
    SCREEN_WIDTH=1280 \
    SCREEN_HEIGHT=800
EXPOSE 6080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD curl --fail --silent http://127.0.0.1:6080/vnc.html > /dev/null || exit 1
USER loyalty
ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/loyalty-desktop"]
