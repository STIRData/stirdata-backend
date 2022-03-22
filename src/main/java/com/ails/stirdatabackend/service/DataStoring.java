package com.ails.stirdatabackend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.repository.ActivitiesDBRepository;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.repository.PlacesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.util.VirtuosoSelectIterator;


@Service
//@Transactional // it is too slow: tens of thousands entries...
public class DataStoring  {

	private final static Logger logger = LoggerFactory.getLogger(DataStoring.class);
	
    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private StatisticsDBRepository statisticsDBRepository;

    @Autowired
    private PlacesDBRepository placesDBRepository;

    @Autowired
    private ActivitiesDBRepository activitiesDBRepository;

    @Autowired
    private CountriesDBRepository countriesDBRepository;

	@Autowired
	@Qualifier("endpoint-nace-eu")
	public String naceEndpointEU;

	@Autowired
	@Qualifier("endpoint-nuts-eu")
	public String nutsEndpointEU;

	@Autowired
	@Qualifier("endpoint-lau-eu")
	public String lauEndpointEU;
	
//    @Autowired
//    @Qualifier("namedgraph-nace-eu")
//    private String naceNamedgraphEU;
//    
//    @Autowired
//    @Qualifier("namedgraph-nuts-eu")
//    private String nutsNamedgraphEU;
//
//    @Autowired
//    @Qualifier("namedgraph-lau-eu")
//    private String lauNamedgraphEU;

	
	public void copyStatisticsFromMongoToRDBMS() {
		copyStatisticsFromMongoToRDBMS(statisticsRepository.findAll());
	}
	
	public void copyStatisticsFromMongoToRDBMS(String country, Dimension dimension) {
//		statisticsDBRepository.deleteAllByCountryAndDimension(country, dimension.toString());
		copyStatisticsFromMongoToRDBMS(statisticsRepository.findByCountryAndDimension(country, dimension));
	}

	public void copyStatisticsFromMongoToRDBMS(CountryDB cc) {
		for (Statistic s : statisticsRepository.groupCountDimensionsByCountry(cc.getCode()) ) {
			copyStatisticsFromMongoToRDBMS(cc, s);
		}
	}

	private void copyStatisticsFromMongoToRDBMS(CountryDB cc, Statistic mongoStatistic) {
		Dimension d = mongoStatistic.getDimension(); 

		Date ccReferenceDate = null;
		if (d.equals(Dimension.DATA)) {
			ccReferenceDate = cc.getStatsDataDate();
		} else if (d.equals(Dimension.NACE)) {
			ccReferenceDate = cc.getStatsNaceDate();
		} else if (d.equals(Dimension.NUTSLAU)) {
			ccReferenceDate = cc.getStatsNutsLauDate();
		} else if (d.equals(Dimension.FOUNDING)) {
			ccReferenceDate = cc.getStatsFoundingDate();
		} else if (d.equals(Dimension.DISSOLUTION)) {
			ccReferenceDate = cc.getStatsDissolutionDate();
		} else if (d.equals(Dimension.NUTSLAU_FOUNDING)) {
			ccReferenceDate = cc.getStatsNutsLauFoundingDate();
		} else if (d.equals(Dimension.NUTSLAU_DISSOLUTION)) {
			ccReferenceDate = cc.getStatsNutsLauDissolutionDate();
		} else if (d.equals(Dimension.NUTSLAU_NACE)) {
			ccReferenceDate = cc.getStatsNutsLauNaceDate();
		} else if (d.equals(Dimension.NACE_FOUNDING)) {
			ccReferenceDate = cc.getStatsNaceFoundingDate();
		} else if (d.equals(Dimension.NACE_DISSOLUTION)) {
			ccReferenceDate = cc.getStatsNaceDissolutionDate();
		}

		if (ccReferenceDate != null) {
			Calendar ccDate = Calendar.getInstance();
			ccDate.setTimeInMillis(ccReferenceDate.getTime());
	
			if (ccDate.getTime().equals(mongoStatistic.getReferenceDate())) {
				logger.info("DB statistics for " + cc.getCode() + " / " + d + " already up to date.");
				return;
			}
		}
		
		Date newReferenceDate = mongoStatistic.getReferenceDate();
		
		List<Statistic> stats = statisticsRepository.findByCountryAndDimensionAndReferenceDate(cc.getCode(), d, newReferenceDate);

		logger.info("Updating " + stats.size() + " DB statistics for " + cc.getCode() + " / " + d + ".");
		
		copyStatisticsFromMongoToRDBMS(stats);
		
		if (stats.size() > 0) {
			System.out.println(stats.size() + ".");
		}
		
		statisticsDBRepository.deleteAllByCountryAndReferenceDateNot(cc.getCode(), newReferenceDate);
		statisticsDBRepository.flush();
		
		if (d.equals(Dimension.DATA)) {
			cc.setStatsDataDate(newReferenceDate);
			
			List<Statistic> list = statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.DATA);
			if (!list.isEmpty()) {
				cc.setActiveLegalEntityCount(list.get(0).getCount());
			}
			
		} else if (d.equals(Dimension.NACE)) {
			cc.setStatsNaceDate(newReferenceDate);
		} else if (d.equals(Dimension.NUTSLAU)) {
			cc.setStatsNutsLauDate(newReferenceDate);
		} else if (d.equals(Dimension.FOUNDING)) {
			cc.setStatsFoundingDate(newReferenceDate);
		} else if (d.equals(Dimension.DISSOLUTION)) {
			cc.setStatsDissolutionDate(newReferenceDate);
		} else if (d.equals(Dimension.NUTSLAU_FOUNDING)) {
			cc.setStatsNutsLauFoundingDate(newReferenceDate);
		} else if (d.equals(Dimension.NUTSLAU_DISSOLUTION)) {
			cc.setStatsNutsLauDissolutionDate(newReferenceDate);
		} else if (d.equals(Dimension.NUTSLAU_NACE)) {
			cc.setStatsNutsLauNaceDate(newReferenceDate);
		} else if (d.equals(Dimension.NACE_FOUNDING)) {
			cc.setStatsNaceFoundingDate(newReferenceDate);
		} else if (d.equals(Dimension.NACE_DISSOLUTION)) {
			cc.setStatsNaceDissolutionDate(newReferenceDate);
		}
		
		countriesDBRepository.save(cc);
		countriesDBRepository.flush();
		
		logger.info("Updating DB statistics for " + cc.getCode() + " / " + d + " completed.");
	}

	public void copyStatisticsFromMongoToRDBMS(Dimension dimension) {
//		statisticsDBRepository.deleteAllByDimension(dimension.toString());
		copyStatisticsFromMongoToRDBMS(statisticsRepository.findByDimension(dimension));
	}
	
//	private void copyStatisticsFromMongoToRDBMS(List<Statistic> stats) {
//		System.out.println(">>> " + stats.size());
//		for (Statistic s : stats) {
//			try {
//				StatisticDB sdb = new StatisticDB();
//				if (s.getActivity() != null) {
//					sdb.setActivity(new ActivityDB(new Code(s.getActivity())));
//	//				sdb.setActivity(new Code(s.getActivity()));
//				}
//				sdb.setCount(s.getCount());
//				sdb.setCountry(s.getCountry());
//				sdb.setDateInterval(s.getDateInterval());
//				sdb.setDimension(s.getDimension().toString());
//				if (s.getFromDate() != null) {
//					sdb.setFromDate(java.sql.Date.valueOf(s.getFromDate()));
//				}
//				if (s.getParentActivity() != null) {
//					sdb.setParentActivity(new ActivityDB(new Code(s.getParentActivity())));
//	//				sdb.setParentActivity(new Code(s.getParentActivity()));
//				}
//				if (s.getParentFromDate() != null) {
//					sdb.setParentFromDate(java.sql.Date.valueOf(s.getParentFromDate()));
//				}
//				if (s.getParentPlace() != null) {
//					sdb.setParentPlace(new PlaceDB(new Code(s.getParentPlace())));
//	//				sdb.setParentPlace(new Code(s.getParentPlace()));
//				}
//				if (s.getParentToDate() != null) {
//					sdb.setParentToDate(java.sql.Date.valueOf(s.getParentToDate()));
//				}
//				if (s.getPlace() != null) {
//					sdb.setPlace(new PlaceDB(new Code(s.getPlace())));
//	//				sdb.setPlace(new Code(s.getPlace()));
//				}
//				if (s.getToDate() != null) {
//					sdb.setToDate(java.sql.Date.valueOf(s.getToDate()));
//				}
//				sdb.setUpdated(s.getUpdated());
//				
//				statisticsDBRepository.save(sdb);
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//		
//		statisticsDBRepository.flush();
//	}
	
	private Set<Dimension> copyStatisticsFromMongoToRDBMS(List<Statistic> stats) {
		logger.info("Copying to DB " + stats.size());
		Set<Dimension> dimensions = new HashSet<>();
		
		int batchSize = 100;
		int counter = 0;
		
		List<StatisticDB> list = new ArrayList<>();
		
		for (int i = 0; i < stats.size(); i++) {
		
			if (i % batchSize == 0) {
				
				counter += list.size();
				if (counter % 5000 == 0) {
					System.out.print(counter + " ");
				}
				statisticsDBRepository.saveAll(list);

				
				list = new ArrayList<>();
			}
			
			Statistic s = stats.get(i);
			
			s.getReferenceDate();

			try {
				StatisticDB sdb;
				
				String country = s.getCountry();
				Dimension dimension = s.getDimension();
				List<StatisticDB> ex = null;
				
				PlaceDB placedb = null;
				ActivityDB activitydb = null;
				java.sql.Date foundingFromDate = null;
				java.sql.Date foundingToDate = null;
				java.sql.Date dissolutionFromDate = null;
				java.sql.Date dissolutionToDate = null;
				
				if (dimension == Dimension.DATA) {
					ex = statisticsDBRepository.findByCountryAndDimension(country, dimension.toString());
				} else if (dimension == Dimension.NUTSLAU) {
					placedb = new PlaceDB(new Code(s.getPlace()));
					ex = statisticsDBRepository.findByCountryAndDimensionAndPlace(country, dimension.toString(), placedb);
				} else if (dimension == Dimension.NACE) {
					activitydb = new ActivityDB(new Code(s.getActivity()));
					ex = statisticsDBRepository.findByCountryAndDimensionAndActivity(country, dimension.toString(), activitydb);
				} else if (dimension == Dimension.FOUNDING) {
					foundingFromDate = java.sql.Date.valueOf(s.getFromDate());
					foundingToDate = java.sql.Date.valueOf(s.getToDate());
					ex = statisticsDBRepository.findByCountryAndDimensionAndFoundingFromDateAndFoundingToDate(country, dimension.toString(), foundingFromDate, foundingToDate);
				} else if (dimension == Dimension.DISSOLUTION) {
					dissolutionFromDate = java.sql.Date.valueOf(s.getFromDate());
					dissolutionToDate = java.sql.Date.valueOf(s.getToDate());
					ex = statisticsDBRepository.findByCountryAndDimensionAndDissolutionFromDateAndDissolutionToDate(country, dimension.toString(), dissolutionFromDate, dissolutionToDate);
				} else if (dimension == Dimension.NUTSLAU_NACE) {
					placedb = new PlaceDB(new Code(s.getPlace()));
					activitydb = new ActivityDB(new Code(s.getActivity()));
					ex = statisticsDBRepository.findByCountryAndDimensionAndActivityAndPlace(country, dimension.toString(), activitydb, placedb);
				} else if (dimension == Dimension.NUTSLAU_FOUNDING) {
					placedb = new PlaceDB(new Code(s.getPlace()));
					foundingFromDate = java.sql.Date.valueOf(s.getFromDate());
					foundingToDate = java.sql.Date.valueOf(s.getToDate());
					ex = statisticsDBRepository.findByCountryAndDimensionAndPlaceAndFoundingFromDateAndFoundingToDate(country, dimension.toString(), placedb, foundingFromDate, foundingToDate);
				} else if (dimension == Dimension.NUTSLAU_DISSOLUTION) {
					placedb = new PlaceDB(new Code(s.getPlace()));
					dissolutionFromDate = java.sql.Date.valueOf(s.getFromDate());
					dissolutionToDate = java.sql.Date.valueOf(s.getToDate());
					ex = statisticsDBRepository.findByCountryAndDimensionAndPlaceAndDissolutionFromDateAndDissolutionToDate(country, dimension.toString(), placedb, dissolutionFromDate, dissolutionToDate);
				} else if (dimension == Dimension.NACE_FOUNDING) {
					activitydb = new ActivityDB(new Code(s.getActivity()));
					foundingFromDate = java.sql.Date.valueOf(s.getFromDate());
					foundingToDate = java.sql.Date.valueOf(s.getToDate());
					ex = statisticsDBRepository.findByCountryAndDimensionAndActivityAndFoundingFromDateAndFoundingToDate(country, dimension.toString(), activitydb, foundingFromDate, foundingToDate);
				} else if (dimension == Dimension.NACE_DISSOLUTION) {
					activitydb = new ActivityDB(new Code(s.getActivity()));
					dissolutionFromDate = java.sql.Date.valueOf(s.getFromDate());
					dissolutionToDate = java.sql.Date.valueOf(s.getToDate());
					ex = statisticsDBRepository.findByCountryAndDimensionAndActivityAndDissolutionFromDateAndDissolutionToDate(country, dimension.toString(), activitydb, dissolutionFromDate, dissolutionToDate);
				}				
					
				if (ex.size() == 0) {
					sdb = new StatisticDB();
				} else if (ex.size() == 1) {
					sdb = ex.get(0);
				} else {
					// this shouldn't happen
					sdb = ex.remove(0);

					//fix 
					statisticsDBRepository.deleteAll(ex);

					logger.error("Not unique " + dimension + " statistics for " + country + " [ " + placedb + " " + activitydb + " " + foundingFromDate + " " + foundingToDate + " " + dissolutionFromDate + " " + dissolutionToDate + " ].");
				}

				dimensions.add(s.getDimension());
				
				sdb.setCount(s.getCount());
				sdb.setCountry(s.getCountry());
				sdb.setDimension(s.getDimension().toString());

				if (s.getActivity() != null) {
					sdb.setActivity(activitydb);
	//				sdb.setActivity(new Code(s.getActivity()));
				}
				if (s.getParentActivity() != null) {
					sdb.setParentActivity(new ActivityDB(new Code(s.getParentActivity())));
	//				sdb.setParentActivity(new Code(s.getParentActivity()));
				}				
				if (s.getPlace() != null) {
					sdb.setPlace(new PlaceDB(new Code(s.getPlace())));
	//				sdb.setPlace(new Code(s.getPlace()));
				}
				if (s.getParentPlace() != null) {
					sdb.setParentPlace(new PlaceDB(new Code(s.getParentPlace())));
	//				sdb.setParentPlace(new Code(s.getParentPlace()));
				}

				if (foundingFromDate != null) {
					sdb.setFoundingFromDate(foundingFromDate);
					sdb.setFoundingToDate(foundingToDate);
					sdb.setFoundingDateInterval(s.getDateInterval());
					
					if (s.getParentFromDate() != null) {
						sdb.setParentFoundingFromDate(java.sql.Date.valueOf(s.getParentFromDate()));
					}
					if (s.getParentToDate() != null) {
						sdb.setParentFoundingToDate(java.sql.Date.valueOf(s.getParentToDate()));
					}
				}
				
				if (dissolutionFromDate != null) {
					sdb.setDissolutionFromDate(dissolutionFromDate);
					sdb.setDissolutionToDate(dissolutionToDate);
					sdb.setDissolutionDateInterval(s.getDateInterval());
					
					if (s.getParentFromDate() != null) {
						sdb.setParentDissolutionFromDate(java.sql.Date.valueOf(s.getParentFromDate()));
					}
					if (s.getParentToDate() != null) {
						sdb.setParentDissolutionToDate(java.sql.Date.valueOf(s.getParentToDate()));
					}
				}

				sdb.setUpdated(s.getUpdated());
				sdb.setReferenceDate(s.getReferenceDate());
				
				list.add(sdb);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		counter += list.size();
		if (counter % 5000 == 0) {
			System.out.print(counter + " ");
		}
		statisticsDBRepository.saveAll(list);
		
		statisticsDBRepository.flush();
		
		return dimensions;
	}	
	
	
	public void updateNUTSDB() {
//		List<StatisticDB> group = placesDBRepository.groupByParentPlace();
//		for (StatisticDB st : group) {
//			System.out.println(st.getPlace().getCode() + " " + st.getCount());
//			PlaceDB placedb = placesDBRepository.findByCode(st.getPlace().getCode());
//			placedb.setNumberOfChildren(st.getCount());
//			placesDBRepository.save(placedb);
//		}		

		List<PlaceDB> list = placesDBRepository.findAll();
		for (PlaceDB st : list) {
			System.out.println(st.getCode());
			st.setCountry(st.getCode().getCode().substring(0,2));
			placesDBRepository.save(st);
		}		

	}
	
	public void copyNUTSFromVirtuosoToRDBMS() throws IOException {

		for (int i = 0; i <= 3; i++) {
			String nutsSparql = "SELECT * " +
//		       (nutsNamedgraphEU != null ? "FROM <" + nutsNamedgraphEU + "> " : "")  + 
			   " WHERE {" +
			   "?nuts a <https://lod.stirdata.eu/nuts/ont/NUTSRegion> . " +
			   "?nuts <http://www.w3.org/2004/02/skos/core#inScheme> <https://lod.stirdata.eu/nuts/scheme/NUTS2021> ." +
			   "?nuts <http://www.w3.org/2004/02/skos/core#prefLabel> ?prefLabel . " +
			   "?nuts <https://lod.stirdata.eu/nuts/ont/level> " + i + " . " +
			   "OPTIONAL { ?nuts <http://www.w3.org/2004/02/skos/core#altLabel> ?altLabel } " +
			   "OPTIONAL { ?nuts <http://www.opengis.net/ont/geosparql#sfWithin> ?broader } " +
			   "OPTIONAL { ?nuts <http://www.opengis.net/ont/geosparql#hasGeometry> [ <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geo60M ; <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"1:60000000\" ] } " +
			   "OPTIONAL { ?nuts <http://www.opengis.net/ont/geosparql#hasGeometry> [ <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geo20M ; <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"1:20000000\" ] } " +
			   "OPTIONAL { ?nuts <http://www.opengis.net/ont/geosparql#hasGeometry> [ <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geo10M ; <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"1:10000000\" ] } " +
			   "OPTIONAL { ?nuts <http://www.opengis.net/ont/geosparql#hasGeometry> [ <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geo3M ; <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"1:3000000\" ] } " +
			   "OPTIONAL { ?nuts <http://www.opengis.net/ont/geosparql#hasGeometry> [ <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geo1M ; <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"1:1000000\" ] } " +
		       "}";
			
//			System.out.println(nutsSparql);
			
	    	try (VirtuosoSelectIterator qe = new VirtuosoSelectIterator(nutsEndpointEU, nutsSparql)) {
	            while (qe.hasNext()) {
	                QuerySolution sol = qe.next();

	                Code code = Code.fromNutsUri(sol.get("nuts").toString());
//	                if (code.isNutsZ()) {
//	                	continue;
//	                }
//	                
	                PlaceDB place = new PlaceDB();
	                
	                place.setCode(code);
	                if (sol.get("broader") != null) {
	                	place.setParent(new PlaceDB(Code.fromNutsUri(sol.get("broader").toString())));
	                }
	                place.setNationalName(sol.get("prefLabel").asLiteral().getLexicalForm());
	                if (sol.get("altLabel") != null) {
	                	place.setLatinName(sol.get("altLabel").asLiteral().getLexicalForm());
	                }
	                place.setType("NUTS");
	                place.setLevel(i);
	                place.setCountry(code.getCode().substring(0,2));
	                
	                if (sol.get("geo60M") != null) {
	                	place.setGeometry60M(sol.get("geo60M").asLiteral().getLexicalForm());
	                }
	
	                if (sol.get("geo20M") != null) {
	                	place.setGeometry20M(sol.get("geo20M").asLiteral().getLexicalForm());
	                }
	
	                if (sol.get("geo10M") != null) {
	                	place.setGeometry10M(sol.get("geo10M").asLiteral().getLexicalForm());
	                }
	
	                if (sol.get("geo3M") != null) {
	                	place.setGeometry3M(sol.get("geo3M").asLiteral().getLexicalForm());
	                }
	                
	                if (sol.get("geo1M") != null) {
	                	place.setGeometry1M(sol.get("geo1M").asLiteral().getLexicalForm());
	                }
	
//	                System.out.println(place.getCode() + " " + place.getNationalName() + " " + place.getLatinName() + " " + place.getParent() + " " + place.getType());
	                
	                placesDBRepository.save(place);
	                
	            }
	        }
		}
		
		placesDBRepository.flush();
		
	}
//	
	public void copyLAUFromVirtuosoToRDBMS() throws IOException {
		String lauSparql = "SELECT * " + 
//	               (lauNamedgraphEU != null ? "FROM <" + lauNamedgraphEU + "> " : "") + 
	               " WHERE {" +
				   "?lau a <https://lod.stirdata.eu/nuts/ont/LAURegion> . " +
				   "?lau <http://www.w3.org/2004/02/skos/core#inScheme> <https://lod.stirdata.eu/lau/scheme/LAU2021> ." +
				   "?lau <http://www.w3.org/2004/02/skos/core#prefLabel> ?prefLabel . " +
				   "OPTIONAL { ?lau <http://www.w3.org/2004/02/skos/core#altLabel> ?altLabel } " +
				   "OPTIONAL { ?lau <http://www.opengis.net/ont/geosparql#sfWithin> ?broader } " +
//				   "OPTIONAL { ?lau <http://www.opengis.net/ont/geosparql#hasGeometry> [ <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geo1M ; <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"1:1000000\" ] } " +
                   "BIND ( iri(concat(\"https://lod.stirdata.eu/lau/code/2020/\",substr(str(?lau),39))) as ?x ) . " +
                   "OPTIONAL { ?x <http://www.opengis.net/ont/geosparql#hasGeometry> [ <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geo1M ; <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"1:1000000\" ] } " +
			       "}";
		
//		System.out.println(lauSparql);
		
    	try (VirtuosoSelectIterator qe = new VirtuosoSelectIterator(lauEndpointEU, lauSparql)) {
            while (qe.hasNext()) {
                QuerySolution sol = qe.next();
                
                Code broaderCode = Code.fromNutsUri(sol.get("broader").toString());
                
//                if (broaderCode.isNutsZ()) {
//                	continue;
//                }

                Code code = Code.fromLauUri(sol.get("lau").toString(),"2021");
                
                PlaceDB place = new PlaceDB();
                
                place.setCode(code);
                if (sol.get("broader") != null) {
                	place.setParent(new PlaceDB(broaderCode));
                }
                place.setNationalName(sol.get("prefLabel").asLiteral().getLexicalForm());
                if (sol.get("altLabel") != null) {
                	place.setLatinName(sol.get("altLabel").asLiteral().getLexicalForm());
                }
                place.setType("LAU");
                place.setLevel(0);
                place.setCountry(code.getCode().substring(0,2));
                
                if (sol.get("geo1M") != null) {
                	place.setGeometry1M(sol.get("geo1M").asLiteral().getLexicalForm());
                }

//                System.out.println(place.getCode() + " " + place.getNationalName() + " " + place.getLatinName() + " " + place.getParent() + " " + place.getType());
                
                placesDBRepository.save(place);
            }
        }
    	
    	placesDBRepository.flush();

	}
	

	
	public void copyNACEFromVirtuosoToRDBMS() throws IOException {

		for (int i = 1; i <= 4; i++) {
			String naceSparql = "SELECT * " + 
//		       (naceNamedgraphEU != null ? "FROM <" + naceNamedgraphEU + "> " : "") + 
		       " WHERE {" +
			   "?nace a <https://lod.stirdata.eu/nace/ont/Activity> . " +
			   "?nace <http://www.w3.org/2004/02/skos/core#inScheme> <https://lod.stirdata.eu/nace/scheme/NACERev2> ." +
			   "?nace <https://lod.stirdata.eu/nace/ont/level> " + i + " . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?bgLabel . FILTER (lang(?bgLabel) = 'bg') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?csLabel . FILTER (lang(?csLabel) = 'cs') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?daLabel . FILTER (lang(?daLabel) = 'da') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?deLabel . FILTER (lang(?deLabel) = 'de') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?eeLabel . FILTER (lang(?eeLabel) = 'ee') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?elLabel . FILTER (lang(?elLabel) = 'el') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?enLabel . FILTER (lang(?enLabel) = 'en') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?esLabel . FILTER (lang(?esLabel) = 'es') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?fiLabel . FILTER (lang(?fiLabel) = 'fi') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?frLabel . FILTER (lang(?frLabel) = 'fr') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?hrLabel . FILTER (lang(?hrLabel) = 'hr') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?huLabel . FILTER (lang(?huLabel) = 'hu') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?itLabel . FILTER (lang(?itLabel) = 'it') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?ltLabel . FILTER (lang(?ltLabel) = 'lt') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?lvLabel . FILTER (lang(?lvLabel) = 'lv') } . " +			   
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?mtLabel . FILTER (lang(?mtLabel) = 'mt') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?nlLabel . FILTER (lang(?nlLabel) = 'nl') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?noLabel . FILTER (lang(?noLabel) = 'no') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?plLabel . FILTER (lang(?plLabel) = 'pl') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?ptLabel . FILTER (lang(?ptLabel) = 'pt') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?roLabel . FILTER (lang(?roLabel) = 'ro') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?ruLabel . FILTER (lang(?ruLabel) = 'ru') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?siLabel . FILTER (lang(?siLabel) = 'si') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?skLabel . FILTER (lang(?skLabel) = 'sk') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?svLabel . FILTER (lang(?svLabel) = 'sv') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?trLabel . FILTER (lang(?trLabel) = 'tr') } . " +			   
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#broader> ?broader } " +
		       "}";
			
//			System.out.println(naceSparql);
			
	    	try (VirtuosoSelectIterator qe = new VirtuosoSelectIterator(naceEndpointEU, naceSparql)) {
	            while (qe.hasNext()) {
	                QuerySolution sol = qe.next();
	                
	                ActivityDB activity = new ActivityDB();
	                
	                activity.setCode(Code.fromNaceRev2Uri(sol.get("nace").toString()));
	                if (sol.get("broader") != null) {
	                	activity.setParent(new ActivityDB(Code.fromNaceRev2Uri(sol.get("broader").toString())));
	                }
	                
	                if (sol.get("bgLabel") != null) {
	                	activity.setLabelBg(sol.get("bgLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("csLabel") != null) {
	                	activity.setLabelCz(sol.get("csLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("daLabel") != null) {
	                	activity.setLabelDa(sol.get("daLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("deLabel") != null) {
	                	activity.setLabelDe(sol.get("deLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("eeLabel") != null) {
	                	activity.setLabelEe(sol.get("eeLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("elLabel") != null) {
	                	activity.setLabelEl(sol.get("elLabel").asLiteral().getLexicalForm());	                
	                }
	                if (sol.get("enLabel") != null) {
	                	activity.setLabelEn(sol.get("enLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("esLabel") != null) {
	                	activity.setLabelEs(sol.get("esLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("fiLabel") != null) {
	                	activity.setLabelFi(sol.get("fiLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("frLabel") != null) {
	                	activity.setLabelFr(sol.get("frLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("hrLabel") != null) {
	                	activity.setLabelHr(sol.get("hrLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("huLabel") != null) {
	                	activity.setLabelHu(sol.get("huLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("itLabel") != null) {
	                	activity.setLabelIt(sol.get("itLabel").asLiteral().getLexicalForm());	                
	                }
	                if (sol.get("ltLabel") != null) {
	                	activity.setLabelLt(sol.get("ltLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("lvLabel") != null) {
	                	activity.setLabelLv(sol.get("lvLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("mtLabel") != null) {
	                	activity.setLabelMt(sol.get("mtLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("nlLabel") != null) {
	                	activity.setLabelNl(sol.get("nlLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("noLabel") != null) {
	                	activity.setLabelNo(sol.get("noLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("plLabel") != null) {
	                	activity.setLabelPl(sol.get("plLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("ptLabel") != null) {
	                	activity.setLabelPt(sol.get("ptLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("roLabel") != null) {
	                	activity.setLabelRo(sol.get("roLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("ruLabel") != null) {
	                	activity.setLabelRu(sol.get("ruLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("siLabel") != null) {
	                	activity.setLabelSi(sol.get("siLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("skLabel") != null) {
	                	activity.setLabelSk(sol.get("skLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("svLabel") != null) {
	                	activity.setLabelSv(sol.get("svLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("trLabel") != null) {
	                	activity.setLabelTr(sol.get("trLabel").asLiteral().getLexicalForm());
	                }
	                
	                activity.setScheme("nace-rev2");
	                activity.setLevel(i);
	                
//	                System.out.println(activity.getCode());
	                activitiesDBRepository.save(activity);
	                
	            }
	        }
		}
		
		activitiesDBRepository.flush();
	}

	@Transactional
	public void deleteNACEFromRDBMS(CountryDB cc) throws IOException {

		activitiesDBRepository.deleteAllByScheme(cc.getNaceNamespace());
	}
	
	
	public void copyNACEFromVirtuosoToRDBMS(CountryDB cc) throws IOException {

//		deleteNACEFromRDBMS(cc);

		for (int i = 1; i <= cc.getNaceLevels(); i++) {
			
			String naceSparql = "SELECT * " + 
//		       (naceNamedgraphEU != null ? "FROM <" + naceNamedgraphEU + "> " : "") + 
		       " WHERE {" +
			   "?nace a <https://lod.stirdata.eu/nace/ont/Activity> . " +
			   "?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" + cc.getNaceScheme() + "> ." +
			   "?nace <https://lod.stirdata.eu/nace/ont/level> " + i + " . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?bgLabel . FILTER (lang(?bgLabel) = 'bg') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?csLabel . FILTER (lang(?csLabel) = 'cs') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?daLabel . FILTER (lang(?daLabel) = 'da') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?deLabel . FILTER (lang(?deLabel) = 'de') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?eeLabel . FILTER (lang(?eeLabel) = 'ee') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?elLabel . FILTER (lang(?elLabel) = 'el') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?enLabel . FILTER (lang(?enLabel) = 'en') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?esLabel . FILTER (lang(?esLabel) = 'es') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?fiLabel . FILTER (lang(?fiLabel) = 'fi') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?frLabel . FILTER (lang(?frLabel) = 'fr') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?hrLabel . FILTER (lang(?hrLabel) = 'hr') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?huLabel . FILTER (lang(?huLabel) = 'hu') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?itLabel . FILTER (lang(?itLabel) = 'it') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?ltLabel . FILTER (lang(?ltLabel) = 'lt') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?lvLabel . FILTER (lang(?lvLabel) = 'lv') } . " +			   
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?mtLabel . FILTER (lang(?mtLabel) = 'mt') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?nlLabel . FILTER (lang(?nlLabel) = 'nl') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?noLabel . FILTER (lang(?noLabel) = 'no') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?plLabel . FILTER (lang(?plLabel) = 'pl') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?ptLabel . FILTER (lang(?ptLabel) = 'pt') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?roLabel . FILTER (lang(?roLabel) = 'ro') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?ruLabel . FILTER (lang(?ruLabel) = 'ru') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?siLabel . FILTER (lang(?siLabel) = 'si') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?skLabel . FILTER (lang(?skLabel) = 'sk') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?svLabel . FILTER (lang(?svLabel) = 'sv') } . " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?trLabel . FILTER (lang(?trLabel) = 'tr') } . " +			   
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#broader> ?broader } " +
			   "OPTIONAL { ?nace <http://www.w3.org/2004/02/skos/core#exactMatch> ?exactMatch } " +
		       "}";
			
//			System.out.println(naceSparql);
			
	    	try (VirtuosoSelectIterator qe = new VirtuosoSelectIterator(cc.getNaceEndpoint(), naceSparql)) {
	            while (qe.hasNext()) {
	                QuerySolution sol = qe.next();
	                
//	                System.out.println(sol);
	                
	                ActivityDB activity = new ActivityDB();
	                
	                activity.setCode(Code.fromNaceUri(sol.get("nace").toString(), cc));
	                
	                if (sol.get("broader") != null) {
	                	activity.setParent(new ActivityDB(Code.fromNaceUri(sol.get("broader").toString(), cc)));
	                }
	                
	                if (sol.get("exactMatch") != null) {
	                	activity.setExactMatch(new ActivityDB(Code.fromNaceRev2Uri(sol.get("exactMatch").toString())));
	                }
	                
	                if (sol.get("bgLabel") != null) {
	                	activity.setLabelBg(sol.get("bgLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("csLabel") != null) {
	                	activity.setLabelCz(sol.get("csLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("daLabel") != null) {
	                	activity.setLabelDa(sol.get("daLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("deLabel") != null) {
	                	activity.setLabelDe(sol.get("deLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("eeLabel") != null) {
	                	activity.setLabelEe(sol.get("eeLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("elLabel") != null) {
	                	activity.setLabelEl(sol.get("elLabel").asLiteral().getLexicalForm());	                
	                }
	                if (sol.get("enLabel") != null) {
	                	activity.setLabelEn(sol.get("enLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("esLabel") != null) {
	                	activity.setLabelEs(sol.get("esLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("fiLabel") != null) {
	                	activity.setLabelFi(sol.get("fiLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("frLabel") != null) {
	                	activity.setLabelFr(sol.get("frLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("hrLabel") != null) {
	                	activity.setLabelHr(sol.get("hrLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("huLabel") != null) {
	                	activity.setLabelHu(sol.get("huLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("itLabel") != null) {
	                	activity.setLabelIt(sol.get("itLabel").asLiteral().getLexicalForm());	                
	                }
	                if (sol.get("ltLabel") != null) {
	                	activity.setLabelLt(sol.get("ltLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("lvLabel") != null) {
	                	activity.setLabelLv(sol.get("lvLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("mtLabel") != null) {
	                	activity.setLabelMt(sol.get("mtLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("nlLabel") != null) {
	                	activity.setLabelNl(sol.get("nlLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("noLabel") != null) {
	                	activity.setLabelNo(sol.get("noLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("plLabel") != null) {
	                	activity.setLabelPl(sol.get("plLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("ptLabel") != null) {
	                	activity.setLabelPt(sol.get("ptLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("roLabel") != null) {
	                	activity.setLabelRo(sol.get("roLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("ruLabel") != null) {
	                	activity.setLabelRu(sol.get("ruLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("siLabel") != null) {
	                	activity.setLabelSi(sol.get("siLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("skLabel") != null) {
	                	activity.setLabelSk(sol.get("skLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("svLabel") != null) {
	                	activity.setLabelSv(sol.get("svLabel").asLiteral().getLexicalForm());
	                }
	                if (sol.get("trLabel") != null) {
	                	activity.setLabelTr(sol.get("trLabel").asLiteral().getLexicalForm());
	                }
	                
	                activity.setScheme(cc.getNaceNamespace());
	                activity.setLevel(i);
	                
//	                System.out.println(activity.getCode());
	                try {
	                	activitiesDBRepository.save(activity);
	                } catch (Exception e) {
	                	System.out.println(e.getMessage());
	                }
	                
	            }
	        }
		}

		String languageSparql = "SELECT DISTINCT(?language) " + 
			       "WHERE {" +
				   "?nace a <https://lod.stirdata.eu/nace/ont/Activity> . " +
				   "?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" + cc.getNaceScheme() + "> ." +
				   "?nace <http://www.w3.org/2004/02/skos/core#prefLabel> ?label . BIND (lang(?label) AS ?language) } "; 

        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), languageSparql)) {
        	String languages = "";
        	
       		ResultSet rs = qe.execSelect();
       		while (rs.hasNext()) {
       			QuerySolution qs = rs.next();
       			if (languages.length() > 0) {
       				languages += ",";
       			}
       			languages += qs.get("language").asLiteral();
            }
       		
       		cc.setNaceLanguages(languages);
        }
        
        countriesDBRepository.save(cc);
        countriesDBRepository.flush();
        
		activitiesDBRepository.flush();
	}	
}
