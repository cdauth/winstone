
CC = gcj
CFLAGS = -ffast-math -funroll-loops

WINSTONE_BASE = ./winstone/development/src
CRIMSON_BASE = ./crimson-src

WINSTONE_LITE_O = winstone_lite.o winstone_res1.o
WINSTONE_AJP_O = winstone_ajp.o winstone_ajp_res1.o
WINSTONE_AUTH_O = winstone_auth.o winstone_realm.o winstone_auth_res1.o winstone_realm_res1.o
WINSTONE_CLUSTER_O = winstone_cluster.o winstone_cluster_res1.o
WINSTONE_JNDI_O = winstone_jndi.o winstone_jndi_res1.o winstone_jndi_res2.o
SERVLET_O = servlet.o servlet_res1.o servlet_res2.o
CRIMSON_O = crimson.o crimson_res1.o crimson_res2.o javax_xml.o xml_api.o

all:	$(WINSTONE_LITE_O) $(WINSTONE_AJP_O) $(WINSTONE_AUTH_O) $(WINSTONE_CLUSTER_O) $(WINSTONE_JNDI_O) $(SERVLET_O) $(CRIMSON_O)
	$(CC) $(CFLAGS) --main=com.rickknowles.winstone.Launcher -o winstone_lite $(WINSTONE_LITE_O) $(SERVLET_O) $(CRIMSON_O)
	$(CC) $(CFLAGS) --main=com.rickknowles.winstone.Launcher -o winstone_full $(WINSTONE_LITE_O) $(SERVLET_O) $(CRIMSON_O) $(WINSTONE_AUTH_O) $(WINSTONE_AJP_O) $(WINSTONE_CLUSTER_O) $(WINSTONE_JNDI_O)

winstone_lite.o: $(WINSTONE_BASE)/com/rickknowles/winstone/*.java
	$(CC) $(CFLAGS) -o winstone_lite.o --classpath=$(WINSTONE_BASE):$(CRIMSON_BASE) -c $(WINSTONE_BASE)/com/rickknowles/winstone/*.java
winstone_res1.o: $(WINSTONE_BASE)/com/rickknowles/winstone/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_res1.o --resource=com/rickknowles/winstone/LocalStrings.properties -c $(WINSTONE_BASE)/com/rickknowles/winstone/LocalStrings.properties

winstone_ajp.o: $(WINSTONE_BASE)/com/rickknowles/winstone/ajp13/*.java
	$(CC) $(CFLAGS) -o winstone_ajp.o --classpath=$(WINSTONE_BASE):$(CRIMSON_BASE) -c $(WINSTONE_BASE)/com/rickknowles/winstone/ajp13/*.java
winstone_ajp_res1.o: $(WINSTONE_BASE)/com/rickknowles/winstone/ajp13/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_ajp_res1.o --resource=com/rickknowles/winstone/ajp13/LocalStrings.properties -c $(WINSTONE_BASE)/com/rickknowles/winstone/ajp13/LocalStrings.properties

winstone_auth.o: $(WINSTONE_BASE)/com/rickknowles/winstone/auth/*.java
	$(CC) $(CFLAGS) -o winstone_auth.o --classpath=$(WINSTONE_BASE):$(CRIMSON_BASE) -c $(WINSTONE_BASE)/com/rickknowles/winstone/auth/*.java
winstone_auth_res1.o: $(WINSTONE_BASE)/com/rickknowles/winstone/auth/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_auth_res1.o --resource=com/rickknowles/winstone/auth/LocalStrings.properties -c $(WINSTONE_BASE)/com/rickknowles/winstone/auth/LocalStrings.properties

winstone_realm.o: $(WINSTONE_BASE)/com/rickknowles/winstone/realm/*.java
	$(CC) $(CFLAGS) -o winstone_realm.o --classpath=$(WINSTONE_BASE):$(CRIMSON_BASE) -c $(WINSTONE_BASE)/com/rickknowles/winstone/realm/*.java
winstone_realm_res1.o: $(WINSTONE_BASE)/com/rickknowles/winstone/realm/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_realm_res1.o --resource=com/rickknowles/winstone/realm/LocalStrings.properties -c $(WINSTONE_BASE)/com/rickknowles/winstone/realm/LocalStrings.properties

winstone_cluster.o: $(WINSTONE_BASE)/com/rickknowles/winstone/cluster/*.java
	$(CC) $(CFLAGS) -o winstone_cluster.o --classpath=$(WINSTONE_BASE):$(CRIMSON_BASE) -c $(WINSTONE_BASE)/com/rickknowles/winstone/cluster/*.java
winstone_cluster_res1.o: $(WINSTONE_BASE)/com/rickknowles/winstone/cluster/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_cluster_res1.o --resource=com/rickknowles/winstone/cluster/LocalStrings.properties -c $(WINSTONE_BASE)/com/rickknowles/winstone/cluster/LocalStrings.properties

winstone_jndi.o: $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/*.java \
                 $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/java/*.java \
                 $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/resourceFactories/*.java
	$(CC) $(CFLAGS) -o winstone_jndi.o --classpath=$(WINSTONE_BASE):$(CRIMSON_BASE) -c \
                 $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/*.java \
                 $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/java/*.java \
                 $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/resourceFactories/*.java
winstone_jndi_res1.o: $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_jndi_res1.o --resource=com/rickknowles/winstone/jndi/LocalStrings.properties -c $(WINSTONE_BASE)/com/rickknowles/winstone/jndi/LocalStrings.properties
winstone_jndi_res2.o: $(WINSTONE_BASE)/jndi.properties
	$(CC) $(CFLAGS) -o winstone_jndi_res2.o --resource=jndi.properties -c $(WINSTONE_BASE)/jndi.properties

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
