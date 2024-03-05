package script;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.List;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class LoadSimulation extends Simulation {

    private static final String AWS_HOST_NAME = System.getProperty("host",
            "ec2-35-183-5-49.ca-central-1.compute.amazonaws.com");

    // 1.http configuration
    private HttpProtocolBuilder protocolBuilder = http
            .baseUrl("http://" + AWS_HOST_NAME + ":8080/api/v1")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private ChainBuilder addAnItem() {
        return exec(http("Add an item")
                    .post("/addItem").body(StringBody("#{randomInt()}")));
    }

    private ChainBuilder checkForAnItem() {
        return exec(http("Check for an item")
                    .get("/hasItem/#{randomInt()}"));
    }

    private ChainBuilder removeAnItem() {
        return exec(http("Remove an item")
                    .delete("/removeItem").body(StringBody("#{randomInt()}")));
    }

    // 2.scenario definition
    private ScenarioBuilder scn = scenario("First Test")
            .exec(List.of(
                            exec(addAnItem()),
                            exec(checkForAnItem()),
                            exec(removeAnItem()))
                    // .pause(Duration.ofMillis(100))
            );

    // 3.load simulation
    {
        setUp(
                scn.injectOpen(
                        nothingFor(5),
                        atOnceUsers(10),
                        // rampUsers(10).during(5), // 3
                        constantUsersPerSec(20).during(30), // 4
//                        constantUsersPerSec(20).during(15).randomized(), // 5
                        rampUsersPerSec(10).to(20).during(10), // 6
//                        rampUsersPerSec(10).to(20).during(10).randomized(), // 7
                        stressPeakUsers(1000).during(20)
                )
        ).protocols(protocolBuilder);
    }
}
