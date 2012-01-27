QARQAS - Quick ARQuillian AS Tools
----------------------------------

Pronounced 'carcass'.

This project focuses on providing a local instance of JBoss AS7 that can be used in CI environments for testing during a project build even while other simultaneous builds (running their own ARQ + AS tests) are executed. The basic idea is to resolve the JBoss AS7 distribution zip from the Maven repository, unpack it somewhere in target/, and configure it appropriately to avoid collision with other instances that may be running at the time.

Currently, it consists of three parts:

- a reservation server, which manages families of ports that can be used together to configure the `standalone.xml` for one local instance of AS7
- a Maven plugin, which resolves the AS7 distribution, unpacks and configures it, and then cleans up once the tests have all run
- a port configuration model API, which is the shared model between the reservation server and the Maven plugin

It is possible to use the maven plugin as a standalone piece, by simply specifying a 'port shift' for the `standalone.xml` file. This port shift will adjust all ports by a given increment in an attempt to avoid other running AS7 instances. Using the standalone approach requires the user to manually coordinate the port shifts for all projects that may build concurrently on the same machine.

By contrast, if you can deploy the port registry WAR, you can make use of the 'reservation' configurator in the Maven plugin. This configurator will ask the running WAR for the next available port family, reserving it using some sort of client key (by default this is the artifactId of the project being built, so it may need to be configured so the same configuration can be used across a multimodule build). Once the build's tests are complete (in the post-integration-test lifecycle phase), the Maven plugin will then release the reservation for reuse by the next build. This allows your build to be oblivious to which family of ports it uses, which *should* make it more portable. 

The WAR is a drop-in that works in AS7 without any other configuration. The Maven plugin can be configured to use the 'reservation' configurator with the following POM snippet:

    <plugin>
      <groupId>org.commonjava.maven.plugins</groupId>
      <artifactId>qarqas-maven-plugin</artifactId>
      <version>0.1-SNAPSHOT</version>
      <executions>
        <execution>
          <id>infra</id>
          <goals>
            <goal>setup</goal>
            <goal>teardown</goal>
          </goals>
          <configuration>
            <clientKey>aprox</clientKey>
            <configurators>reservation</configurators>
            <useDefaultConfigurators>false</useDefaultConfigurators>
            <configProperties>
              <reservationBaseUrl>http://localhost:9080/qarqas/api/1.0/reservation/</reservationBaseUrl>
            </configProperties>
          </configuration>
        </execution>
      </executions>
    </plugin>

If you want to see the WAR at work, try this:

    curl -i http://localhost:8080/qarqas/api/1.0/reservation/my-client-key
    
You should see something like the following:

    HTTP/1.1 200 OK
    Server: Apache-Coyote/1.1
    Content-Type: application/json
    Content-Length: 414
    Date: Fri, 27 Jan 2012 02:59:16 GMT
    
    {
      "key": 0,
      "ports": {
        "jacorb": 3528,
        "https": 8443,
        "remoting": 4447,
        "management-native": 9999,
        "txn-status-manager": 4713,
        "messaging-throughput": 5455,
        "jmx-connector-server": 1091,
        "jmx-connector-registry": 1090,
        "management-http": 9990,
        "http": 8080,
        "jacorb-ssl": 3529,
        "txn-recovery-environment": 4712,
        "messaging": 5445,
        "osgi-http": 8090
      }
    }

If you re-run with the same URL, you'll see the same result (the reservation is renewed every time the same client-key requests a reservation, IF it requests it before the reservation expires):

    HTTP/1.1 200 OK
    Server: Apache-Coyote/1.1
    Content-Type: application/json
    Content-Length: 414
    Date: Fri, 27 Jan 2012 02:59:16 GMT
    
    {
      "key": 0,
      "ports": {
        "jacorb": 3528,
        "https": 8443,
        "remoting": 4447,
        "management-native": 9999,
        "txn-status-manager": 4713,
        "messaging-throughput": 5455,
        "jmx-connector-server": 1091,
        "jmx-connector-registry": 1090,
        "management-http": 9990,
        "http": 8080,
        "jacorb-ssl": 3529,
        "txn-recovery-environment": 4712,
        "messaging": 5445,
        "osgi-http": 8090
      }
    }

On the other hand, if you re-run with a different client-key:

    curl -i http://localhost:8080/qarqas/api/1.0/reservation/other-client-key

...you'll get a different port family:

    HTTP/1.1 200 OK
    Server: Apache-Coyote/1.1
    Content-Type: application/json
    Content-Length: 416
    Date: Fri, 27 Jan 2012 03:00:55 GMT
    
    {
      "key": 1,
      "ports": {
        "jacorb": 4528,
        "https": 9443,
        "remoting": 5447,
        "management-native": 10999,
        "txn-status-manager": 5713,
        "messaging-throughput": 6455,
        "jmx-connector-server": 2091,
        "jmx-connector-registry": 2090,
        "management-http": 10990,
        "http": 9080,
        "jacorb-ssl": 4529,
        "txn-recovery-environment": 5712,
        "messaging": 6445,
        "osgi-http": 9090
      }
    }

If you want more control over how long the reservation can be used without expiring, you can specify that in the URL as well:

    curl -i http://localhost:8080/qarqas/api/1.0/reservation/client-key?expires=5000

This reservation will expire in 5 seconds. Use it fast, or you may wind up with a port collision between your build and the next!

