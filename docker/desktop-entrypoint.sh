#!/usr/bin/env bash
set -Eeuo pipefail

children=()
cleanup() {
    trap - EXIT INT TERM
    if ((${#children[@]})); then
        kill "${children[@]}" 2>/dev/null || true
        wait "${children[@]}" 2>/dev/null || true
    fi
}
trap cleanup EXIT
trap 'exit 0' INT TERM

Xvfb "${DISPLAY}" -screen 0 "${SCREEN_WIDTH}x${SCREEN_HEIGHT}x24" -nolisten tcp &
children+=("$!")
ready=false
for ((attempt = 0; attempt < 100; attempt++)); do
    if xdpyinfo -display "${DISPLAY}" >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 0.1
done
if [[ "$ready" != true ]]; then
    echo "Не удалось запустить виртуальный экран JavaFX" >&2
    exit 1
fi

openbox --sm-disable &
children+=("$!")
# VNC доступен только внутри контейнера; браузер подключается через websockify.
x11vnc -display "${DISPLAY}" -rfbport 5900 -localhost -forever -shared -nopw -noxdamage -quiet &
children+=("$!")
websockify --web /usr/share/novnc 6080 127.0.0.1:5900 &
children+=("$!")
java --enable-native-access=ALL-UNNAMED -Dprism.order=sw -jar /opt/loyalty/loyalty.jar &
children+=("$!")

# Закрытие приложения или выход любого обязательного процесса останавливает контейнер.
wait -n "${children[@]}"
