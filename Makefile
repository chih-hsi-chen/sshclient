all: compile

compile:
	mvn clean install -DskipTests -f app/pom.xml
	mvn clean install -DskipTests -f web/pom.xml
	mvn clean install -DskipTests -f ./pom.xml
