
CC = gcj
CFLAGS = -ffast-math -funroll-loops

WINSTONE_BASE = ./winstone/development/src
CRIMSON_BASE = ./crimson-src

WINSTONE_O = winstone.o winstone_res1.o
SERVLET_O = servlet.o servlet_res1.o servlet_res2.o
CRIMSON_O = crimson.o crimson_res1.o crimson_res2.o javax_xml.o xml_api.o

all:	$(WINSTONE_O) $(SERVLET_O) $(CRIMSON_O)
	$(CC) $(CFLAGS) --main=com.rickknowles.winstone.Listener -o winstone_bin $(WINSTONE_O) $(SERVLET_O) $(CRIMSON_O)

winstone.o: $(WINSTONE_BASE)/com/rickknowles/winstone/*.java
	$(CC) $(CFLAGS) -o winstone.o --classpath=$(WINSTONE_BASE):$(CRIMSON_BASE) -c $(WINSTONE_BASE)/com/rickknowles/winstone/*.java
winstone_res1.o: $(WINSTONE_BASE)/com/rickknowles/winstone/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_res1.o --resource=com/rickknowles/winstone/LocalStrings.properties -c $(WINSTONE_BASE)/com/rickknowles/winstone/LocalStrings.properties

servlet.o: $(WINSTONE_BASE)/javax/servlet/*.java $(WINSTONE_BASE)/javax/servlet/http/*.java
	$(CC) $(CFLAGS) -o servlet.o --classpath=$(WINSTONE_BASE) -c $(WINSTONE_BASE)/javax/servlet/*.java $(WINSTONE_BASE)/javax/servlet/http/*.java
servlet_res1.o: $(WINSTONE_BASE)/javax/servlet/resources/web-app_2_2.dtd
	$(CC) $(CFLAGS) -o servlet_res1.o --resource=javax/servlet/resources/web-app_2_2.dtd -c $(WINSTONE_BASE)/javax/servlet/resources/web-app_2_2.dtd
servlet_res2.o: $(WINSTONE_BASE)/javax/servlet/resources/web-app_2_3.dtd
	$(CC) $(CFLAGS) -o servlet_res2.o --resource=javax/servlet/resources/web-app_2_3.dtd -c $(WINSTONE_BASE)/javax/servlet/resources/web-app_2_3.dtd

javax_xml.o: $(CRIMSON_BASE)/javax/xml/*/*.java
	$(CC) $(CFLAGS) -o javax_xml.o -c $(CRIMSON_BASE)/javax/xml/*/*.java

xml_api.o: $(CRIMSON_BASE)/org/w3c/dom/*.java $(CRIMSON_BASE)/org/xml/sax/*.java $(CRIMSON_BASE)/org/xml/sax/ext/*.java $(CRIMSON_BASE)/org/xml/sax/helpers/*.java
	$(CC) $(CFLAGS) -o xml_api.o -c $(CRIMSON_BASE)/org/w3c/dom/*.java $(CRIMSON_BASE)/org/xml/sax/*.java $(CRIMSON_BASE)/org/xml/sax/ext/*.java $(CRIMSON_BASE)/org/xml/sax/helpers/*.java

crimson.o: $(CRIMSON_BASE)/org/apache/crimson/*/*.java
	$(CC) $(CFLAGS) -o crimson.o --classpath=$(CRIMSON_BASE) -c $(CRIMSON_BASE)/org/apache/crimson/*/*.java
crimson_res1.o: $(CRIMSON_BASE)/org/apache/crimson/parser/resources/Messages.properties
	$(CC) $(CFLAGS) -o crimson_res1.o --resource=org/apache/crimson/parser/resources/Messages.properties -c $(CRIMSON_BASE)/org/apache/crimson/parser/resources/Messages.properties
crimson_res2.o: $(CRIMSON_BASE)/org/apache/crimson/tree/resources/Messages.properties
	$(CC) $(CFLAGS) -o crimson_res2.o --resource=org/apache/crimson/tree/resources/Messages.properties -c $(CRIMSON_BASE)/org/apache/crimson/tree/resources/Messages.properties

clean:
	rm -f *.o
