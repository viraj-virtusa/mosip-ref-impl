# Stage 1 EDIT 13

FROM node as build-step
RUN mkdir -p /app
WORKDIR /app
COPY package.json /app
RUN npm install -g npm@7.7.4
COPY . /app
RUN sudo npm run ng build --prod --base-href

FROM nginx
COPY default.conf /etc/nginx/conf.d/
COPY --from=build-step dist/pre-registration/ /usr/share/nginx/html
