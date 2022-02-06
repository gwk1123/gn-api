APPNAME=gn-api
PORT=19093
docker build -t $APPNAME .
docker run -itd --name $APPNAME --net=host -p $PORT:$PORT  -v /gn/api/logs:/gn/api/logs -v /gn/api/data:/gn/api/data $APPNAME
