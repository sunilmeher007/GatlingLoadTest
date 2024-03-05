package script;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.List;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class LoadSimulationV2 extends Simulation {

    private static final String AWS_HOST_NAME = System.getProperty("host",
            "ec2-35-183-5-49.ca-central-1.compute.amazonaws.com");

    // 1.http configuration
	private HttpProtocolBuilder protocolBuilder = http
			.baseUrl("http://" + AWS_HOST_NAME + ":8080/api")
			.acceptHeader("application/json")
			.contentTypeHeader("application/json");

	private ChainBuilder authenticate() {
		return exec(http("Authenticate")
				.post("/authenticate")
				.body(StringBody("{\r\n" + "  \"userName\": \"admin\",\r\n" + "  \"password\": \"admin\"\r\n" + "}"))
				.check(bodyString().saveAs("jwtToken")));
	}

	private ChainBuilder addAnItem() {
		return exec(http("Add an item")
				.post("/v2/addItem")
				.header("Authentication", "Bearer #{jwtToken}")
				.body(StringBody("{\r\n" + "  \"userName\": \"admin\",\r\n" + "  \"key\": #{randomInt()}\r\n" + "}")));
	}

	private ChainBuilder checkForAnItem() {
		return exec(http("Check for an item")
				.post("/v2/hasItem")
				.header("Authentication", "Bearer #{jwtToken}")
				.body(StringBody("{\r\n" + "  \"userName\": \"admin\",\r\n" + "  \"key\": #{randomInt()}\r\n" + "}")));
	}

	private ChainBuilder removeAnItem() {
		return exec(http("Remove an item")
				.delete("/v2/removeItem")
				.header("Authentication", "Bearer #{jwtToken}")
				.body(StringBody("{\r\n" + "  \"userName\": \"admin\",\r\n" + "  \"key\": #{randomInt()}\r\n" + "}")));
    }

    // 2.scenario definition
    private ScenarioBuilder scn = scenario("First Test")
    		.exec(exec(authenticate()))
//    		.exec(session -> {
//	    			System.out.println(">>>>> " + session.getString("jwtToken"));
//	    			return session;
//	    		}
//    		)
    		.pause(Duration.ofMillis(2000))
    		.exec(checkForAnItem());
			//.exec(List.of(exec(addAnItem()), exec(checkForAnItem()), exec(removeAnItem())));

    // 3.load simulation
    {
        setUp(
                scn.injectOpen(
                        nothingFor(5),
                        atOnceUsers(1)
                        // rampUsers(10).during(5), // 3
//                        constantUsersPerSec(20).during(30), // 4
//                        constantUsersPerSec(20).during(15).randomized(), // 5
//                        rampUsersPerSec(10).to(20).during(10), // 6
//                        rampUsersPerSec(10).to(20).during(10).randomized(), // 7
//                        stressPeakUsers(1000).during(20)
                )
        ).protocols(protocolBuilder);
    }
}
