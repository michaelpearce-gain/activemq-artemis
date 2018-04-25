 # Artemis Bootstrapper
 
 The Artemis bootstrapper is a utility for controlling the lifecycle of 
 an Artemis broker via http. This can be particulary useful for testing
 and is used by the remote Arquillian Artemis container and the smoke tests.
 
 It is deployed as a war file and can be deployed in any web container however 
 for convenince this module also creates a Wildfy Swarm jar which can be run directly.
 
 To run the bootstrapper execute the following command:
 
    java -jar artemis-bootstrapper-swarm.jar -DARTEMIS_HOME=~/projects/activemq-artemis/artemis-distribution/target/apache-artemis-2.6.0-SNAPSHOT-bin/apache-artemis-2.6.0-SNAPSHOT/
    
 Where ARTEMIS_HOME points to a valid Artemis installation 
 
 
 you can then check the status of the bootstrapper by running:
 
    http://localhost:8080/artemis
    
 And create a broker by going to the web address:
 
    http://localhost:8080/artemis/create?artemisCreateCommand=--allow-anonymous%20--user%20admin%20--password%20password
    
 
 You can also pass a configuration parameter which can be broker.xml to be replaced, however this would need to be delimeted
 htpp. For instance if you wanted replace the addresses xml to add a queue element you use the configuration:
 
 
 ```xml
     <configuration xmlns="urn:activemq"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="urn:activemq /schema/artemis-configuration.xsd">
     
        <core xmlns="urn:activemq:core" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="urn:activemq:core ">
     
           <addresses>
              <address name="replicatedQueue">
                 <anycast>
                    <queue name="replicatedQueue"/>
                 </anycast>
              </address>
           </addresses>
        </core>
     
     </configuration>
 ```
 
 And delimit this as a single string like so:
 
    http://localhost:8080/artemis/create?artemisCreateCommand=--allow-anonymous+--user+admin+--password+password&configuration=%253Cconfiguration+xmlns%253D%2522urn%253Aactivemq%2522%250A+++++++++++++++xmlns%253Axsi%253D%2522http%253A%252F%252Fwww.w3.org%252F2001%252FXMLSchema-instance%2522%250A+++++++++++++++xsi%253AschemaLocation%253D%2522urn%253Aactivemq+%252Fschema%252Fartemis-configuration.xsd%2522%253E%250A%250A+++%253Ccore+xmlns%253D%2522urn%253Aactivemq%253Acore%2522+xmlns%253Axsi%253D%2522http%253A%252F%252Fwww.w3.org%252F2001%252FXMLSchema-instance%2522%250A+++++++++xsi%253AschemaLocation%253D%2522urn%253Aactivemq%253Acore+%2522%253E%250A%250A++++++%253Caddresses%253E%250A+++++++++%253Caddress+name%253D%2522replicatedQueue%2522%253E%250A++++++++++++%253Canycast%253E%250A+++++++++++++++%253Cqueue+name%253D%2522replicatedQueue%2522%252F%253E%250A++++++++++++%253C%252Fanycast%253E%250A+++++++++%253C%252Faddress%253E%250A++++++%253C%252Faddresses%253E%250A+++%253C%252Fcore%253E%250A%250A%253C%252Fconfiguration%253E%250A
    
-----
NOTE

It would make sense to use a tool for this or write your own REST client 

----- 

You can then start the broker using the address:

    http://localhost:8080/artemis/start
    
or stop the broker by running

    http://localhost:8080/artemis/stop
    
or hard kill the broker by running

    http://localhost:8080/artemis/kill
 
