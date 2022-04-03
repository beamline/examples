package beamline.examples.opensky;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.configuration.Configuration;
import org.opensky.api.OpenSkyApi;
import org.opensky.model.OpenSkyStates;
import org.opensky.model.StateVector;

import beamline.events.BEvent;
import beamline.exceptions.EventException;
import beamline.sources.BeamlineAbstractSource;

public class OpenSkySource extends BeamlineAbstractSource {

	private static final long serialVersionUID = -2323555154857716845L;
	private OpenSkyApi api;

	@Override
	public void open(Configuration parameters) throws Exception {
		Properties prop = new Properties();
		prop.load(new FileInputStream("./openskyCredentials.properties"));
		api = new OpenSkyApi(prop.getProperty("USERNAME"), prop.getProperty("PASSWORD"));
	}

	@Override
	public void run(SourceContext<BEvent> ctx) throws Exception {
		Queue<BEvent> buffer = new LinkedList<>();
		
		new Thread(() -> {
			while(isRunning()) {
				try {
					OpenSkyStates os = api.getStates(0, null, new OpenSkyApi.BoundingBox(35.0518857, 62.4097744, -5.8468354, 34.3186395));
					if (os != null) {
						for (StateVector sv : os.getStates()) {
							try {
								if (!sv.getCallsign().isBlank()) {
									buffer.add(BEvent.create("squawk", sv.getCallsign().trim(), squawkToString(sv.getSquawk())));
								}
							} catch (EventException e) {
								e.printStackTrace();
							}
						}
					} else {
						System.out.println("No new information...");
					}
					Thread.sleep(15000l);
				} catch (Exception e) {
					// nothing to see here
					e.printStackTrace();
				}
			}
		}).start();
		
		while(isRunning()) {
			while (isRunning() && buffer.isEmpty()) {
				Thread.sleep(100l);
			}
			if (isRunning()) {
				synchronized (ctx.getCheckpointLock()) {
					BEvent e = buffer.poll();
					ctx.collect(e);
				}
			}
		}
	}
	
	private static String squawkToString(String squawk) {
		Integer s = Integer.parseInt(squawk.trim());
		for (Entry<Pair<Integer, Integer>, String> e : squawks.entrySet()) {
			if (e.getKey().getLeft() <= s && e.getKey().getRight() >= s) {
				return e.getValue();
			}
		}
		return squawk;
	}
	
	private static Map<Pair<Integer, Integer>, String> squawks = new HashMap<>();
	static {
		// codes from http://www.flightradars.eu/squawkcodes.html
		squawks.put(Pair.of(0000, 0000), "SSR data unreliable");
		squawks.put(Pair.of(0001, 0001), "Height Monitoring Unit");
		squawks.put(Pair.of(0002, 0002), "Ground Transponder Testing");
		squawks.put(Pair.of(0003, 0005), "Not allocated");
		squawks.put(Pair.of(0006, 0006), "British Transport Police ASU");
		squawks.put(Pair.of(0007, 0007), "Off-shore Safety Area (OSA) Conspicuity");
		squawks.put(Pair.of(0010, 0010), "Radar");
		squawks.put(Pair.of(0011, 0011), "Solent Monitoring Code");
		squawks.put(Pair.of(0012, 0012), "Radar");
		squawks.put(Pair.of(0013, 0013), "Radar");
		squawks.put(Pair.of(0014, 0014), "Kent Air Ambulance (HMD21)");
		squawks.put(Pair.of(0015, 0015), "Essex Air Ambulance (HMD07)");
		squawks.put(Pair.of(0016, 0016), "Thames Valley Air Ambulance(HMD24)");
		squawks.put(Pair.of(0017, 0017), "Virgin HEMS (HMD27)");
		squawks.put(Pair.of(0020, 0020), "Air Ambulance Helicopter Emergency Medivac");
		squawks.put(Pair.of(0021, 0021), "Fixed-wing aircraft (Receiving service from a ship)");
		squawks.put(Pair.of(0022, 0022), "Helicopter(s) (Receiving service from a ship)");
		squawks.put(Pair.of(0023, 0023), "Aircraft engaged in actual SAR Operations");
		squawks.put(Pair.of(0024, 0024), "Radar");
		squawks.put(Pair.of(0025, 0025), "Not allocated");
		squawks.put(Pair.of(0026, 0026), "Special Tasks (Mil) - activated under Special Flight Notification (SFN)");
		squawks.put(Pair.of(0027, 0027), "London AC (Swanwick) Ops Crossing/Joining CAS");
		squawks.put(Pair.of(0030, 0030), "FIR Lost");
		squawks.put(Pair.of(0031, 0031), "Radar");
		squawks.put(Pair.of(0032, 0032), "Aircraft engaged in police air support operations");
		squawks.put(Pair.of(0033, 0033), "Aircraft Paradropping");
		squawks.put(Pair.of(0034, 0034), "Antenna trailing/target towing");
		squawks.put(Pair.of(0035, 0035), "Selected Flights - Helicopters");
		squawks.put(Pair.of(0036, 0036), "Helicopter Pipeline/Powerline Inspection Flights");
		squawks.put(Pair.of(0037, 0037), "Royal Flights - Helicopters");
		squawks.put(Pair.of(0040, 0040), "Civil Helicopters North Sea");
		squawks.put(Pair.of(0041, 0061), "Police ASU");
		squawks.put(Pair.of(0062, 0077), "No 1 Air Control Centre");
		squawks.put(Pair.of(0100, 0100), "NATO");
		squawks.put(Pair.of(0101, 0177), "Transit");
		squawks.put(Pair.of(0200, 0200), "NATO");
		squawks.put(Pair.of(0201, 0213), "TC Stansted/TC Luton");
		squawks.put(Pair.of(0201, 0217), "RAF");
		squawks.put(Pair.of(0201, 0257), "Domestic");
		squawks.put(Pair.of(0201, 0257), "RNAS Yeovilton");
		squawks.put(Pair.of(0220, 0220), "RAF");
		squawks.put(Pair.of(0220, 0237), "RAF");
		squawks.put(Pair.of(0221, 0247), "RAF");
		squawks.put(Pair.of(0224, 0243), "Radar");
		squawks.put(Pair.of(0240, 0240), "RAF");
		squawks.put(Pair.of(0241, 0246), "RAF");
		squawks.put(Pair.of(0244, 0244), "North Denes Conspicuity");
		squawks.put(Pair.of(0245, 0267), "Radar");
		squawks.put(Pair.of(0247, 0247), "Cranfield Airport - IFR Conspicuity Purposes");
		squawks.put(Pair.of(0260, 0260), "Liverpool Airport Conspicuity");
		squawks.put(Pair.of(0260, 0261), "Coventry Airport Conspicuity");
		squawks.put(Pair.of(0260, 0261), "Oil Survey Helicopters - Faeroes/Iceland Gap");
		squawks.put(Pair.of(0260, 0267), "Westland Helicopters Yeovil");
		squawks.put(Pair.of(0260, 0267), "RAF");
		squawks.put(Pair.of(0261, 0267), "Liverpool Airport");
		squawks.put(Pair.of(0270, 0277), "Superdomestic");
		squawks.put(Pair.of(0300, 0300), "NATO");
		squawks.put(Pair.of(0301, 0377), "Transit");
		squawks.put(Pair.of(0400, 0400), "NATO");
		squawks.put(Pair.of(0401, 0401), "RAF");
		squawks.put(Pair.of(0401, 0401), "Shoreham Approach Procedural");
		squawks.put(Pair.of(0401, 0420), "Birmingham Approach");
		squawks.put(Pair.of(0401, 0430), "Exeter Approach");
		squawks.put(Pair.of(0401, 0437), "Domestic");
		squawks.put(Pair.of(0401, 0467), "RAF");
		squawks.put(Pair.of(0402, 0416), "RAF");
		squawks.put(Pair.of(0421, 0446), "Radar/LARS");
		squawks.put(Pair.of(0427, 0427), "RAF");
		squawks.put(Pair.of(0430, 0443), "Edinburgh Approach");
		squawks.put(Pair.of(0447, 0447), "Farnborough LARS - Blackbushe Departures");
		squawks.put(Pair.of(0450, 0456), "Blackpool Approach");
		squawks.put(Pair.of(0450, 0456), "Radar/LARS");
		squawks.put(Pair.of(0457, 0457), "Blackpool Approach (Liverpool Bay and Morecambe Bay Helicopters)");
		squawks.put(Pair.of(0457, 0457), "Farnborough LARS - Fairoaks Departures");
		squawks.put(Pair.of(0460, 0466), "Blackpool Approach");
		squawks.put(Pair.of(0460, 0467), "Radar/LARS");
		squawks.put(Pair.of(0467, 0467), "Blackpool Approach (Liverpool Bay and Morecambe Bay Helicopters)");
		squawks.put(Pair.of(0470, 0477), "Domestic");
		squawks.put(Pair.of(0500, 0500), "NATO");
		squawks.put(Pair.of(0501, 0577), "Transit");
		squawks.put(Pair.of(0600, 0600), "NATO");
		squawks.put(Pair.of(0601, 0637), "Transit");
		squawks.put(Pair.of(0640, 0677), "Transit");
		squawks.put(Pair.of(0700, 0700), "NATO");
		squawks.put(Pair.of(0701, 0777), "Transit");
		squawks.put(Pair.of(1000, 1000), "IFR GAT flights operating in designated Mode S Airspace");
		squawks.put(Pair.of(1001, 1077), "Transit");
		squawks.put(Pair.of(1100, 1100), "NATO");
		squawks.put(Pair.of(1101, 1137), "Transit");
		squawks.put(Pair.of(1140, 1177), "Transit");
		squawks.put(Pair.of(1177, 1177), "London AC (Swanwick) FIS");
		squawks.put(Pair.of(1200, 1200), "NATO");
		squawks.put(Pair.of(1201, 1277), "Domestic");
		squawks.put(Pair.of(1300, 1300), "NATO");
		squawks.put(Pair.of(1301, 1327), "NATO");
		squawks.put(Pair.of(1330, 1357), "Transit");
		squawks.put(Pair.of(1360, 1377), "Transit");
		squawks.put(Pair.of(1400, 1400), "NATO");
		squawks.put(Pair.of(1401, 1407), "Domestic");
		squawks.put(Pair.of(1410, 1437), "Superdomestic");
		squawks.put(Pair.of(1440, 1477), "Superdomestic");
		squawks.put(Pair.of(1500, 1577), "NATO");
		squawks.put(Pair.of(1601, 1677), "NATO");
		squawks.put(Pair.of(1701, 1727), "NATO");
		squawks.put(Pair.of(1730, 1746), "Newquay Approach");
		squawks.put(Pair.of(1730, 1756), "RAF");
		squawks.put(Pair.of(1730, 1767), "RAF");
		squawks.put(Pair.of(1747, 1747), "Newquay Conspicuity");
		squawks.put(Pair.of(1757, 1757), "RAF");
		squawks.put(Pair.of(1760, 1777), "RAF");
		squawks.put(Pair.of(1760, 1777), "RNAS Yeovilton Fighter Control");
		squawks.put(Pair.of(2000, 2000), "Aircraft from non SSR environment or on the aerodrome surface");
		squawks.put(Pair.of(2001, 2077), "Transit");
		squawks.put(Pair.of(2100, 2100), "NATO");
		squawks.put(Pair.of(2101, 2177), "Transit");
		squawks.put(Pair.of(2200, 2200), "NATO");
		squawks.put(Pair.of(2201, 2277), "Superdomestic");
		squawks.put(Pair.of(2300, 2300), "NATO");
		squawks.put(Pair.of(2301, 2337), "Transit");
		squawks.put(Pair.of(2340, 2377), "Transit");
		squawks.put(Pair.of(2400, 2477), "NATO");
		squawks.put(Pair.of(2500, 2500), "NATO");
		squawks.put(Pair.of(2501, 2577), "Transit");
		squawks.put(Pair.of(2600, 2600), "NATO");
		squawks.put(Pair.of(2601, 2620), "Aberdeen Approach");
		squawks.put(Pair.of(2601, 2637), "RAF");
		squawks.put(Pair.of(2601, 2645), "MoD Boscombe Down");
		squawks.put(Pair.of(2601, 2657), "Domestic Westbound departures and Eastbound arrivals");
		squawks.put(Pair.of(2621, 2630), "Aberdeen (Sumburgh Approach)");
		squawks.put(Pair.of(2631, 2637), "Aberdeen (Northern North Sea Off-shore)");
		squawks.put(Pair.of(2640, 2657), "Aberdeen (Northern North Sea Off-shore - Sumburgh Sector)");
		squawks.put(Pair.of(2641, 2642), "RAF");
		squawks.put(Pair.of(2646, 2647), "MoD Boscombe Down - High Risks Trial");
		squawks.put(Pair.of(2650, 2650), "MoD Boscombe Down Conspicuity");
		squawks.put(Pair.of(2650, 2653), "Leeds Bradford Approach");
		squawks.put(Pair.of(2651, 2657), "MoD Boscombe Down");
		squawks.put(Pair.of(2654, 2654), "Leeds Bradford Conspicuity");
		squawks.put(Pair.of(2655, 2677), "Leeds Bradford Approach");
		squawks.put(Pair.of(2660, 2675), "Middle Wallop");
		squawks.put(Pair.of(2660, 2677), "Aberdeen (Northern North Sea Off-shore)");
		squawks.put(Pair.of(2676, 2677), "Middle Wallop Conspicuity");
		squawks.put(Pair.of(2700, 2700), "NATO");
		squawks.put(Pair.of(2701, 2737), "Transit");
		squawks.put(Pair.of(2740, 2777), "Transit");
		squawks.put(Pair.of(3000, 3000), "NATO");
		squawks.put(Pair.of(3001, 3077), "Transit");
		squawks.put(Pair.of(3100, 3100), "NATO");
		squawks.put(Pair.of(3101, 3127), "Transit");
		squawks.put(Pair.of(3130, 3177), "Transit");
		squawks.put(Pair.of(3200, 3200), "NATO");
		squawks.put(Pair.of(3201, 3202), "Domestic (London TC (Swanwick) Special Sector Codes) ");
		squawks.put(Pair.of(3203, 3216), "Domestic (London AC (Swanwick) Special Sector Codes) ");
		squawks.put(Pair.of(3217, 3220), "Domestic");
		squawks.put(Pair.of(3221, 3257), "Superdomestic");
		squawks.put(Pair.of(3260, 3277), "Domestic");
		squawks.put(Pair.of(3300, 3300), "NATO");
		squawks.put(Pair.of(3301, 3304), "Swanwick (Military) Special Tasks");
		squawks.put(Pair.of(3305, 3307), "London D&D Cell");
		squawks.put(Pair.of(3310, 3367), "Swanwick (Military)");
		squawks.put(Pair.of(3370, 3377), "Domestic");
		squawks.put(Pair.of(3400, 3400), "NATO");
		squawks.put(Pair.of(3401, 3457), "Superdomestic");
		squawks.put(Pair.of(3460, 3477), "Transit");
		squawks.put(Pair.of(3500, 3500), "NATO");
		squawks.put(Pair.of(3501, 3507), "Transit");
		squawks.put(Pair.of(3510, 3537), "Transit");
		squawks.put(Pair.of(3540, 3577), "Transit");
		squawks.put(Pair.of(3600, 3600), "NATO");
		squawks.put(Pair.of(3601, 3623), "RAF");
		squawks.put(Pair.of(3601, 3632), "Scottish ATSOCA Purposes");
		squawks.put(Pair.of(3601, 3634), "RAF");
		squawks.put(Pair.of(3601, 3644), "Cardiff Approach");
		squawks.put(Pair.of(3601, 3647), "Jersey Approach");
		squawks.put(Pair.of(3624, 3624), "RAF");
		squawks.put(Pair.of(3640, 3645), "RAF");
		squawks.put(Pair.of(3640, 3665), "RAF");
		squawks.put(Pair.of(3640, 3677), "Aberdeen (Northern North Sea Off-shore)");
		squawks.put(Pair.of(3641, 3677), "BAe Warton");
		squawks.put(Pair.of(3645, 3645), "Cardiff Approach - St Athan Conspicuity");
		squawks.put(Pair.of(3646, 3646), "RAF");
		squawks.put(Pair.of(3646, 3657), "Cardiff Approach");
		squawks.put(Pair.of(3647, 3653), "RAF");
		squawks.put(Pair.of(3660, 3677), "Solent Approach (Southampton)");
		squawks.put(Pair.of(3666, 3666), "Radar");
		squawks.put(Pair.of(3666, 3666), "RAF");
		squawks.put(Pair.of(3667, 3667), "RAF");
		squawks.put(Pair.of(3667, 3677), "Solent Approach (Southampton)");
		squawks.put(Pair.of(3700, 3700), "NATO");
		squawks.put(Pair.of(3701, 3710), "Norwich Approach");
		squawks.put(Pair.of(3701, 3710), "BAe Woodford");
		squawks.put(Pair.of(3701, 3717), "Military aircraft under service from RN AEW aircraft in South West Approaches");
		squawks.put(Pair.of(3701, 3736), "RAF");
		squawks.put(Pair.of(3701, 3747), "Guernsey Approach");
		squawks.put(Pair.of(3701, 3747), "RAF");
		squawks.put(Pair.of(3711, 3711), "Woodford Entry/Exit Lane (Woodford Inbounds and Outbounds)");
		squawks.put(Pair.of(3712, 3712), "Woodford Entry/Exit Lane (Manchester Inbounds)");
		squawks.put(Pair.of(3713, 3713), "Manchester VFR/SVFR (Outbounds)");
		squawks.put(Pair.of(3720, 3720), "RAF");
		squawks.put(Pair.of(3720, 3727), "RAF");
		squawks.put(Pair.of(3720, 3766), "Newcastle Approach");
		squawks.put(Pair.of(3721, 3754), "RAF");
		squawks.put(Pair.of(3730, 3736), "RAF");
		squawks.put(Pair.of(3737, 3737), "RAF");
		squawks.put(Pair.of(3737, 3737), "RAF");
		squawks.put(Pair.of(3740, 3745), "RAF");
		squawks.put(Pair.of(3740, 3747), "RAF");
		squawks.put(Pair.of(3750, 3750), "RAF");
		squawks.put(Pair.of(3750, 3763), "Gatwick Approach (TC)");
		squawks.put(Pair.of(3751, 3751), "RAF");
		squawks.put(Pair.of(3752, 3752), "RAF");
		squawks.put(Pair.of(3753, 3753), "RAF");
		squawks.put(Pair.of(3754, 3754), "RAF");
		squawks.put(Pair.of(3755, 3755), "RAF");
		squawks.put(Pair.of(3755, 3762), "RAF");
		squawks.put(Pair.of(3756, 3765), "RAF");
		squawks.put(Pair.of(3764, 3767), "Gatwick Tower");
		squawks.put(Pair.of(3767, 3767), "Redhill Approach Conspicuity");
		squawks.put(Pair.of(3767, 3767), "Newcastle Approach Conspicuity");
		squawks.put(Pair.of(3770, 3777), "ATSOCAS");
		squawks.put(Pair.of(4000, 4000), "NATO");
		squawks.put(Pair.of(4001, 4077), "Transit");
		squawks.put(Pair.of(4100, 4100), "NATO");
		squawks.put(Pair.of(4101, 4127), "Transit");
		squawks.put(Pair.of(4130, 4177), "Transit");
		squawks.put(Pair.of(4200, 4200), "NATO");
		squawks.put(Pair.of(4201, 4214), "Domestic");
		squawks.put(Pair.of(4215, 4247), "Superdomestic");
		squawks.put(Pair.of(4250, 4250), "Manston Conspicuity");
		squawks.put(Pair.of(4250, 4257), "Belfast City Approach");
		squawks.put(Pair.of(4250, 4267), "Aberdeen Approach");
		squawks.put(Pair.of(4250, 4277), "BAe Bristol Filton");
		squawks.put(Pair.of(4250, 4277), "Humberside Approach");
		squawks.put(Pair.of(4251, 4267), "Manston Approach");
		squawks.put(Pair.of(4300, 4300), "NATO");
		squawks.put(Pair.of(4301, 4307), "Domestic");
		squawks.put(Pair.of(4310, 4323), "Domestic (Gatwick Special Sector Codes)");
		squawks.put(Pair.of(4324, 4337), "Domestic (Scottish Special Sector Codes)");
		squawks.put(Pair.of(4340, 4353), "Domestic (SCoACC Special Sector Codes)");
		squawks.put(Pair.of(4354, 4377), "Domestic");
		squawks.put(Pair.of(4400, 4400), "NATO");
		squawks.put(Pair.of(4401, 4427), "Superdomestic");
		squawks.put(Pair.of(4430, 4477), "Superdomestic");
		squawks.put(Pair.of(4500, 4500), "NATO");
		squawks.put(Pair.of(4501, 4501), "Wattisham Conspicuity");
		squawks.put(Pair.of(4501, 4515), "RAF");
		squawks.put(Pair.of(4501, 4520), "Prestwick Approach");
		squawks.put(Pair.of(4501, 4547), "RAF");
		squawks.put(Pair.of(4502, 4547), "Wattisham Approach");
		squawks.put(Pair.of(4516, 4517), "RAF");
		squawks.put(Pair.of(4520, 4524), "RAF");
		squawks.put(Pair.of(4530, 4542), "MoD Aberporth");
		squawks.put(Pair.of(4530, 4567), "Radar");
		squawks.put(Pair.of(4550, 4567), "Isle of Man");
		squawks.put(Pair.of(4550, 4572), "East Midlands Approach");
		squawks.put(Pair.of(4573, 4573), "East Midlands Approach Conspicuity");
		squawks.put(Pair.of(4574, 4574), "Not allocated");
		squawks.put(Pair.of(4575, 4575), "RAF");
		squawks.put(Pair.of(4575, 4575), "Southend Airport Conspicuity");
		squawks.put(Pair.of(4576, 4577), "RAF");
		squawks.put(Pair.of(4576, 4577), "Vale of York AIAA Conspicuity");
		squawks.put(Pair.of(4600, 4600), "NATO");
		squawks.put(Pair.of(4601, 4601), "Hawarden Conspicuity");
		squawks.put(Pair.of(4601, 4601), "RAF");
		squawks.put(Pair.of(4602, 4607), "Hawarden Approach");
		squawks.put(Pair.of(4610, 4667), "Radar");
		squawks.put(Pair.of(4670, 4676), "TC Stansted/TC Luton");
		squawks.put(Pair.of(4670, 4677), "RAF");
		squawks.put(Pair.of(4677, 4677), "Carlisle Airport Conspicuity");
		squawks.put(Pair.of(4677, 4677), "Luton Airport Tower Conspicuity");
		squawks.put(Pair.of(4700, 4700), "NATO");
		squawks.put(Pair.of(4701, 4777), "Special Events (activated by NOTAM)");
		squawks.put(Pair.of(5000, 5000), "NATO");
		squawks.put(Pair.of(5001, 5012), "TC Non-Standard Flights");
		squawks.put(Pair.of(5013, 5017), "Domestic");
		squawks.put(Pair.of(5020, 5046), "Farnborough LLARS");
		squawks.put(Pair.of(5047, 5047), "Farnborough LLARS Conspicuity");
		squawks.put(Pair.of(5050, 5067), "Bristol Approach");
		squawks.put(Pair.of(5070, 5070), "Bristol VFR Conspicuity");
		squawks.put(Pair.of(5071, 5077), "Bristol Approach");
		squawks.put(Pair.of(5100, 5100), "NATO");
		squawks.put(Pair.of(5101, 5177), "CRC Boulmer");
		squawks.put(Pair.of(5200, 5200), "NATO");
		squawks.put(Pair.of(5201, 5260), "Transit");
		squawks.put(Pair.of(5261, 5270), "Transit");
		squawks.put(Pair.of(5271, 5277), "Transit");
		squawks.put(Pair.of(5300, 5300), "NATO");
		squawks.put(Pair.of(5301, 5377), "Transit");
		squawks.put(Pair.of(5400, 5400), "NATO");
		squawks.put(Pair.of(5401, 5477), "Domestic");
		squawks.put(Pair.of(5500, 5500), "NATO");
		squawks.put(Pair.of(5501, 5577), "Transit");
		squawks.put(Pair.of(5600, 5600), "NATO");
		squawks.put(Pair.of(5601, 5647), "Transit");
		squawks.put(Pair.of(5650, 5657), "Transit");
		squawks.put(Pair.of(5660, 5677), "Transit");
		squawks.put(Pair.of(5700, 5700), "NATO");
		squawks.put(Pair.of(5701, 5777), "Transit");
		squawks.put(Pair.of(6000, 6000), "NATO");
		squawks.put(Pair.of(6001, 6007), "Domestic");
		squawks.put(Pair.of(6010, 6037), "Domestic");
		squawks.put(Pair.of(6040, 6077), "Radar");
		squawks.put(Pair.of(6100, 6100), "NATO");
		squawks.put(Pair.of(6101, 6107), "Radar");
		squawks.put(Pair.of(6110, 6137), "Radar");
		squawks.put(Pair.of(6140, 6147), "Radar");
		squawks.put(Pair.of(6151, 6157), "Radar");
		squawks.put(Pair.of(6160, 6160), "Doncaster Sheffield Conspicuity");
		squawks.put(Pair.of(6160, 6176), "Cambridge Approach");
		squawks.put(Pair.of(6160, 6176), "Inverness Approach");
		squawks.put(Pair.of(6160, 6177), "Radar");
		squawks.put(Pair.of(6161, 6167), "Doncaster Sheffield Approach");
		squawks.put(Pair.of(6170, 6170), "Radar");
		squawks.put(Pair.of(6171, 6177), "Doncaster Sheffield Approach");
		squawks.put(Pair.of(6177, 6177), "Cambridge Conspicuity");
		squawks.put(Pair.of(6177, 6177), "Inverness VFR Conspicuity");
		squawks.put(Pair.of(6200, 6200), "NATO");
		squawks.put(Pair.of(6201, 6227), "Superdomestic");
		squawks.put(Pair.of(6230, 6247), "Superdomestic");
		squawks.put(Pair.of(6250, 6257), "Superdomestic");
		squawks.put(Pair.of(6260, 6277), "Superdomestic");
		squawks.put(Pair.of(6300, 6300), "NATO");
		squawks.put(Pair.of(6301, 6377), "Superdomestic");
		squawks.put(Pair.of(6400, 6400), "NATO");
		squawks.put(Pair.of(6401, 6457), "Radar");
		squawks.put(Pair.of(6460, 6477), "Domestic");
		squawks.put(Pair.of(6500, 6500), "NATO");
		squawks.put(Pair.of(6501, 6577), "CRC Scampton");
		squawks.put(Pair.of(6600, 6600), "NATO");
		squawks.put(Pair.of(6601, 6677), "Transit");
		squawks.put(Pair.of(6700, 6700), "NATO");
		squawks.put(Pair.of(6701, 6747), "Transit");
		squawks.put(Pair.of(6750, 6777), "Transit");
		squawks.put(Pair.of(7000, 7000), "General Conspicuity code");
		squawks.put(Pair.of(7001, 7001), "Military Fixed-wing Low Level Conspicuity/Climbout");
		squawks.put(Pair.of(7002, 7002), "Danger Areas General");
		squawks.put(Pair.of(7003, 7003), "Red Arrows Transit");
		squawks.put(Pair.of(7004, 7004), "Conspicuity Aerobatics and Display");
		squawks.put(Pair.of(7005, 7005), "High-Energy Manoeuvres");
		squawks.put(Pair.of(7006, 7006), "Autonomous Operations within TRA and TRA (G)");
		squawks.put(Pair.of(7007, 7007), "Open Skies Observation Aircraft");
		squawks.put(Pair.of(7010, 7010), "Operating in Aerodrome Traffic Pattern");
		squawks.put(Pair.of(7011, 7013), "Not allocated");
		squawks.put(Pair.of(7014, 7027), "Domestic");
		squawks.put(Pair.of(7030, 7045), "RNAS Culdrose");
		squawks.put(Pair.of(7030, 7046), "TC Thames/TC Heathrow");
		squawks.put(Pair.of(7030, 7047), "Aldergrove Approach");
		squawks.put(Pair.of(7030, 7066), "Durham Tees Valley Airport");
		squawks.put(Pair.of(7030, 7077), "Aberdeen (Northern North Sea Off-shore)");
		squawks.put(Pair.of(7046, 7047), "RNAS Culdrose Conspicuity");
		squawks.put(Pair.of(7047, 7047), "TC Thames (Biggin Hill Airport Conspicuity)");
		squawks.put(Pair.of(7050, 7056), "TC Thames/TC Heathrow");
		squawks.put(Pair.of(7057, 7057), "TC Thames (London City Airport Conspicuity)");
		squawks.put(Pair.of(7050, 7077), "RNAS Culdrose");
		squawks.put(Pair.of(7067, 7067), "Durham Tees Valley Airport Conspicuity");
		squawks.put(Pair.of(7070, 7076), "TC Thames/TC Heathrow");
		squawks.put(Pair.of(7077, 7077), "TC Thames (London Heliport Conspicuity)");
		squawks.put(Pair.of(7100, 7100), "London Control (Swanwick) Saturation Code");
		squawks.put(Pair.of(7101, 7177), "Transit");
		squawks.put(Pair.of(7200, 7200), "RN Ships");
		squawks.put(Pair.of(7201, 7247), "Transit");
		squawks.put(Pair.of(7250, 7257), "UK Superdomestic");
		squawks.put(Pair.of(7260, 7267), "Superdomestic");
		squawks.put(Pair.of(7270, 7277), "Radar");
		squawks.put(Pair.of(7300, 7300), "Not allocated");
		squawks.put(Pair.of(7301, 7307), "Superdomestic");
		squawks.put(Pair.of(7310, 7327), "Superdomestic");
		squawks.put(Pair.of(7330, 7347), "Superdomestic");
		squawks.put(Pair.of(7350, 7350), "Norwich Approach Conspicuity");
		squawks.put(Pair.of(7350, 7361), "MoD Ops in EG D701 (Hebrides)");
		squawks.put(Pair.of(7350, 7365), "Manchester Approach");
		squawks.put(Pair.of(7350, 7367), "RNAS Culdrose");
		squawks.put(Pair.of(7350, 7376), "Bournemouth Approach/LARS");
		squawks.put(Pair.of(7351, 7377), "Norwich Approach");
		squawks.put(Pair.of(7362, 7362), "MoD Ops in EG D702 (Fort George)");
		squawks.put(Pair.of(7363, 7363), "MoD Ops in EG D703 (Tain)");
		squawks.put(Pair.of(7367, 7373), "Manchester Approach");
		squawks.put(Pair.of(7374, 7374), "Dundee Airport Conspicuity");
		squawks.put(Pair.of(7375, 7375), "Manchester TMA and Woodvale Local Area (Woodvale UAS Conspicuity)");
		squawks.put(Pair.of(7377, 7377), "Radar");
		squawks.put(Pair.of(7400, 7400), "MPA/DEFRA/Fishery Protection Conspicuity");
		squawks.put(Pair.of(7401, 7437), "Domestic");
		squawks.put(Pair.of(7440, 7477), "Superdomestic");
		squawks.put(Pair.of(7500, 7500), "Special Purpose Code - Hi-Jacking");
		squawks.put(Pair.of(7501, 7537), "Transit");
		squawks.put(Pair.of(7540, 7547), "Transit");
		squawks.put(Pair.of(7550, 7577), "Transit");
		squawks.put(Pair.of(7600, 7600), "Special Purpose Code - Radio Failure");
		squawks.put(Pair.of(7601, 7607), "Superdomestic");
		squawks.put(Pair.of(7610, 7617), "Superdomestic");
		squawks.put(Pair.of(7620, 7657), "Superdomestic");
		squawks.put(Pair.of(7660, 7677), "Superdomestic");
		squawks.put(Pair.of(7700, 7700), "Special Purpose Code - Emergency");
		squawks.put(Pair.of(7701, 7717), "Superdomestic");
		squawks.put(Pair.of(7720, 7727), "Transit");
		squawks.put(Pair.of(7730, 7757), "Superdomestic");
		squawks.put(Pair.of(7760, 7775), "Superdomestic");
		squawks.put(Pair.of(7776, 7777), "SSR Monitors");
	}
}
