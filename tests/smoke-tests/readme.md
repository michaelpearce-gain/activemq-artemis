# Smoke tests

## Running Smoke tests

The Smoke tests use the Artemis Arquillian container to allow tests to be executed against real Brokers. These Brokers 
can either be local or remote. There are multiple profiles to tun the different types of tests.

### Running the Tests against Local brokers

There are multiple profiles to run each one dedicated to a particular topology, Each profile has an arquillian configuration
 file that is used by Arquillian to configure the containers. currently the available profiles are:


profile | topology | Arquillian Config 
--- | --- | ---
single-node | `A single broker` | arquillian-single-node.xml 
replicated-2node | `A single Replicated Pair` | arquillian2nodereplicated.xml
replicated-6node | `3 Replicated Pairs` | arquillian6nodereplicated.xml

The tests will use the Artemis Arquillian Local Container which will create a broker instance and bootstrap brokers when 
needed. By default it will use the distribution built by the top level project so you will need to make sure this has built first.
Then simply run

    mvn clean test -Psingle-node
    
Or whichever profile you want to run.

You can point to a different Artemis installation by setting the artemis.home variable like so:

    mvn clean test -Dartemis.home=/my/broker/dir -Psingle-node 
    
### Running the tests Remote brokers

The same set of smoke tests can also be run against remote brokers on other machines or even containers. This uses the 
Artemis Arquillian Remote container in conjunction with the Artemis Bootstrapper utility. The current profiles:

profile | topology | Arquillian Config 
--- | --- | ---
single-node-remote | `A single broker` | arquillian-single-node-remote.xml  
replicated-2node-remote | `A single Replicated Pair` | arquillian2nodereplicated-remote.xml
replicated-6node-remote | `3 Replicated Pairs` | arquillian6nodereplicated-remote.xml

Before running any tests you will need to install Artemis on the remote hosts you ant along with the `artemis-bootstrapper-swarm.jar`
from the arquillian bootstrapper module. Follow the bootstrapper README.md to start the bootstrapper on each remote machine. 
One for each container/broker the test suite needs. You can then run the tests like so:

    mvn clean test -Dartemis.live.bootstrapHost=hostname -Dartemis.live.host -Psingle-node-remote 
    
Note that the hostnames and ports of the remote brokers need to be set via system properties and vary between the different 
profiles. Look at each profiles Arquillian configuration files to see which properties can be set. 


## Developing Tests

The tests are split into Junit categories where each category represents a specific topology. Each category has 2 profiles,
a local profile that uses the Arquillian local container and a remote profile that uses the Arquillian remote container.
Each profile has its own Arquillian configuration file that is used to configure each Brokers container.

For each test category there will be a base class to extend. The base class will take care of th elifecycle of the containers 
as well as other utility methods. Simple extend the base class for the profile you want to add to, for instance SingleNodeTestBase.

Each Test class must have the following annotations at the class level

```java 
@RunWith(Arquillian.class)
@Category(SingleNode.class)
```

And each test needs to have:

```java
@RunAsClient
```

Currently the framework only supports running the test locally however it might be possible to improve this in future versions

 


 