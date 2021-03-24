# Stage 1 EDIT 345

FROM node as build-step
RUN mkdir -p /app
WORKDIR /app
COPY package.json /app
RUN npm install
COPY . /app
RUN npm run ng build --prod --base-href

FROM nginx
COPY default.conf /etc/nginx/conf.d/
COPY --from=build-step dist /usr/share/nginx/html
