#!/bin/zsh

echo "🚀 Iniciando IronFit..."

# 1. Verificar Docker
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker Desktop no está corriendo."
    echo "Abre Docker Desktop y vuelve a ejecutar este script."
    exit 1
fi

# 2. Levantar MongoDB si está detenido
if [ "$(docker ps -q -f name=ironfit-mongo)" = "" ]; then
    echo "📦 Iniciando MongoDB..."
    docker start ironfit-mongo
else
    echo "✅ MongoDB ya está corriendo."
fi

# 3. Levantar backend en una nueva Terminal
osascript -e '
tell application "Terminal"
    do script "cd \"'$PWD'/backend-mongo/backendmongo\" && export SPRING_DATA_MONGODB_URI=\"mongodb://localhost:27017/ironfit\" && ./mvnw spring-boot:run"
    activate
end tell
'

# 4. Levantar frontend en otra Terminal
osascript -e '
tell application "Terminal"
    do script "cd \"'$PWD'/frontend/ironfit\" && npm start"
    activate
end tell
'

echo "✅ Servicios iniciándose."
echo "Backend:  http://localhost:8080"
echo "Frontend: http://localhost:4200/login"

sleep 8
open http://localhost:4200/login