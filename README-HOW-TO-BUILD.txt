Because of some dependencies under HTTPS, we need to disable SSL security.

The cumbersome way to do this, is:
mvn clean install -U -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true

The easy way is to set the MAVEN_OPTS variable accordingly first, by runnig

source maven-opts.sha


Now, you can do
mvn clean install just as usual
