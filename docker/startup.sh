APPNAME=gn-api
PORT=19093
docker build -t $APPNAME .
docker run -itd --name $APPNAME --net=host -p $PORT:$PORT --privileged=true -v /gn/logs/api:/gn/logs/api -v /gn/data/api:/gn/data/api $APPNAME
