# ===== 构建阶段：Node 构建 Vite 产物 =====
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --no-fund --no-audit
COPY . .
RUN npm run build

# ===== 运行阶段：Nginx 托管静态文件并反代后端 =====
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
