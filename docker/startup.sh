APPNAME=gn-api
PORT=19093
docker build -t $APPNAME .
docker run -itd --name $APPNAME -p $PORT:$PORT $APPNAME
