package beamline.examples.opensky;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction.Context;

import beamline.events.BEvent;
import beamline.miners.trivial.DirectlyFollowsDependencyDiscoveryMiner;
import beamline.miners.trivial.ProcessMap;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

public class OpenSky {

	public static void main(String[] args) throws Exception {
		System.out.println("starting...");
		
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		DataStreamSource<BEvent> stream = env.addSource(new OpenSkySource());
		stream
			.keyBy(BEvent::getProcessName)
			.flatMap(new DirectlyFollowsDependencyDiscoveryMiner()
					.setModelRefreshRate(10)
					.setMinDependency(0.5))
			.addSink(new SinkFunction<ProcessMap>(){
				private static final long serialVersionUID = 6818511702068908564L;

				public void invoke(ProcessMap value, Context context) throws Exception {
					System.out.println(value.getProcessedEvents());
					value.generateDot().exportToSvg(new File("src/main/resources/output/output.svg"));
				};
			});
		
//		// stream processing the activity names
//		stream.addSink(new RichSinkFunction<BEvent>() {
//			private static final long serialVersionUID = -3456643820922884608L;
//			private transient Connection connection;
//			
//			@Override
//			public void open(Configuration parameters) throws Exception {
//				super.open(parameters);
//				this.connection = DriverManager.getConnection("jdbc:mysql://localhost/grafana?user=grafana");
//			}
//			
//			public void invoke(BEvent value, Context context) throws Exception {
//				PreparedStatement stmt = connection.prepareStatement("insert into readings_opensky (`timestamp`, `metric_name`, `string_value`) values (CURRENT_TIMESTAMP, ?, ?)");
//				stmt.setString(1, "activity_names");
//				stmt.setString(2, value.getEventName());
//				stmt.execute();
//			}
//		});
//		
//		// stream processing the case ids
//		stream.addSink(new RichSinkFunction<BEvent>() {
//			private static final long serialVersionUID = -8410458024409476803L;
//			private transient Connection connection;
//			
//			@Override
//			public void open(Configuration parameters) throws Exception {
//				super.open(parameters);
//				this.connection = DriverManager.getConnection("jdbc:mysql://localhost/grafana?user=grafana");
//			}
//			
//			public void invoke(BEvent value, Context context) throws Exception {
//				PreparedStatement stmt = connection.prepareStatement("insert into readings_opensky (`timestamp`, `metric_name`, `string_value`) values (CURRENT_TIMESTAMP, ?, ?)");
//				stmt.setString(1, "case_ids");
//				stmt.setString(2, value.getTraceName());
//				stmt.execute();
//			}
//		});
//		
//		// stream processing the maps
//		stream
//			.keyBy(BEvent::getProcessName)
//			.flatMap(new DirectlyFollowsDependencyDiscoveryMiner()
//					.setModelRefreshRate(10)
//					.setMinDependency(0.5))
//			.addSink(new RichSinkFunction<ProcessMap>() {
//				private static final long serialVersionUID = 7655176342976001922L;
//				private transient Connection connection;
//				
//				@Override
//				public void open(Configuration parameters) throws Exception {
//					super.open(parameters);
//					this.connection = DriverManager.getConnection("jdbc:mysql://localhost/grafana?user=grafana");
//				}
//				
//				public void invoke(ProcessMap value, Context context) throws Exception {
//					System.out.println(value.getProcessedEvents());
//					value.generateDot().exportToSvg(new File("src/main/resources/output/output.svg"));
//					String svg = Graphviz.fromString(value.generateDot().toString()).render(Format.SVG).toString();
//					
//					PreparedStatement stmt = connection.prepareStatement("insert into readings_opensky (`timestamp`, `metric_name`, `string_value`) values (CURRENT_TIMESTAMP, ?, ?)");
//					stmt.setString(1, "process_maps");
//					stmt.setString(2, svg);
//					stmt.execute();
//				}
//			});
		
		env.execute();
	}

}
