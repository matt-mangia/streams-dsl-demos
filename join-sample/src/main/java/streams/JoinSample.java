package streams;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Joined;

public class JoinSample {
    final static String APPLICATION_ID = "live-demo-v0.1.0";
    final static String APPLICATION_NAME = "Join Sample";

    public static void main(String[] args) {
        System.out.printf("*** Starting %s Application ***%n", APPLICATION_NAME);

        Properties config = getConfig();
        Topology topology = getTopology();
        KafkaStreams streams =  startApp(config, topology);

        setupShutdownHook(streams);
    }

    private static Topology getTopology(){
        StreamsBuilder builder = new StreamsBuilder();

        final Serde<String> stringSerde = Serdes.String();

        // TODO: here we construct the Kafka Streams topology
        KStream<String, String> leftStream = builder.stream("ioc-topic", 
            Consumed.with(stringSerde, stringSerde)).selectKey((key, value) -> value.split(",")[0]);

        KStream<String, String> rightStream = builder.stream("right-topic",
            Consumed.with(stringSerde, stringSerde)).selectKey((key, value) -> value.split(",")[0]);

	KStream<String, String> anotherStream = builder.stream("third-topic",
            Consumed.with(stringSerde, stringSerde)).selectKey((key, value) -> value.split(",")[0]);

//        leftStream
//            .leftJoin(rightStream,
//                (leftValue, rightValue) -> "[" + leftValue + ", " + rightValue + "]",
//                JoinWindows.of(Duration.ofMinutes(5)),
//                Joined.with(stringSerde, stringSerde, stringSerde)
//            )
//            .to("joined-topic", Produced.with(stringSerde, stringSerde));
//
	leftStream
            .join(rightStream,
                (leftValue, rightValue) -> "[" + leftValue + ", " + rightValue + "]",
                JoinWindows.of(Duration.ofMinutes(5)),
                Joined.with(stringSerde, stringSerde, stringSerde)
            )
	    .join(anotherStream,
                (leftValue, rightValue) -> "[" + leftValue + ", " + rightValue + "]",
                JoinWindows.of(Duration.ofMinutes(5)),
                Joined.with(stringSerde, stringSerde, stringSerde)
        )
	    .to("triple-join-topic",Produced.with(stringSerde, stringSerde));

        Topology topology = builder.build();
        return topology;
    }

    private static Properties getConfig(){
        Properties settings = new Properties();
        settings.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        return settings;        
    }

    private static KafkaStreams startApp(Properties config, Topology topology){
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();
        return streams;
    }

    private static void setupShutdownHook(KafkaStreams streams){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.printf("### Stopping %s Application ###%n", APPLICATION_NAME);
            streams.close();
        }));
    }
}
