FROM openjdk:8
ADD gn-api-0.0.1-SNAPSHOT.jar gn-api.jar
EXPOSE 19093
#修改Docker时区-否则里面的时间会慢8小时 通过文件链接的方式修改 时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENTRYPOINT ["java","-jar","/gn-api.jar"]
