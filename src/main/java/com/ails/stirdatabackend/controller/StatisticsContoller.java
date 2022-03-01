package com.ails.stirdatabackend.controller;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.model.StatisticResult;
import com.ails.stirdatabackend.payload.GenericResponse;
import com.ails.stirdatabackend.payload.CodeLabel;
import com.ails.stirdatabackend.payload.ComplexResponse;
import com.ails.stirdatabackend.repository.ActivitiesDBRepository;
import com.ails.stirdatabackend.repository.PlacesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.service.StatisticsService;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics API")
public class StatisticsContoller {

	@Autowired 
	StatisticsDBRepository statisticsRepository;

	@Autowired 
	ActivitiesDBRepository activityRepository;

	@Autowired 
	PlacesDBRepository placeRepository;

    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    @Qualifier("default-from-date")
    private Date defaultFromDate;
    
    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;
    
	@Operation(
			summary = "Get statistics for selected place, activity, time",
			description = 	"The place, activity, founding, dissolution variables narrow down the results of your search.\n" +
							"The \"dimension\" variable filters out the response data \n"
		)
		@ApiResponses(value = {
			@ApiResponse(
				responseCode = "200",
				description = "OK",
				content = {@Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ComplexResponse.class)
				)}
			),
	        @ApiResponse(
	            responseCode = "500",
	            description = "Internal Server Error",
	            content = {@Content(
	                mediaType = "application/json",
	                schema = @Schema(hidden = true)
	            )}
	        )
		})
		@GetMapping(produces = "application/json")
	    public ResponseEntity<?> getStatistics(@RequestParam(required = false) 
												@Parameter(
													description = "Place definition in the format [prefix]:[code]" +
																			"where prefix= nuts, lau",
													schema = @Schema(implementation = String.class),
													example = "nuts:NO"
												)
													Code place,
	                                           @RequestParam(required = false) 
											   	@Parameter(
													description = "Activity definition in the format [prefix]:[code]" +
																							"where prefix = nace-rev2",
													schema = @Schema(implementation = String.class),
													example = "nace-rev2:F"
												) 
													Code activity,
	                                           @RequestParam(required = false)  
											   	@Parameter(
													schema = @Schema(implementation = String.class)
												) 
													Code founding,
	                                           @RequestParam(required = false)  
												@Parameter(
														schema = @Schema(implementation = String.class)
												) 
													Code dissolution,
	                                           @RequestParam(defaultValue = "selection")  
											   	@Parameter(
													description = 	"Select the return types of the API call." +
													  			  	"To select multiple, append it in a string separated by comma. "+
																	"Available values: any combination of selection, place, activity, foundingDate, dissolutionDate",
													example = "place,activity"
												) 
													String dimension,
	                                           @RequestParam(defaultValue = "en") 
											   	@Parameter(
													description = "Select the language of the labels on response"
												) 
													String language,
	                                           @RequestParam(required = false) 
											   		String geometry) {

		ComplexResponse sr = new ComplexResponse();
		
		boolean ccurrent = false;
		boolean cplace = false;
		boolean cactivity = false;
		boolean cfounding = false;
		boolean cdissolution = false;
		
		for (String s : dimension.split(",")) {
			if (s.equals("selection")) {
				ccurrent = true;
			} else if (s.equals("place")) {
				cplace = true;
			} else if (s.equals("activity")) {
				cactivity = true;
			} else if (s.equals("foundingDate")) {
				cfounding = true;
			} else if (s.equals("dissolutionDate")) {
				cdissolution = true;
			}
		}	
		

		String country = null;
		PlaceDB placedb = null;
		ActivityDB activitydb = null;
		
		if (place != null) {
			if (!(place.isNuts() || place.isLau())) {
				return ResponseEntity.ok(sr);
			} else {
				if (place.isNuts()) {
					country = place.getNutsCountry();
				} else {
					country = place.getLauCountry();
				}
				
				if (!place.isNutsCountry()) {
					placedb = placeRepository.findByCode(place);
					if (placedb == null) {
						return ResponseEntity.ok(sr);
					}
				}
			}
		}
		
		if (activity != null) {
			if (!activity.isNaceRev2()) {
				return ResponseEntity.ok(sr);
			} else {
				activitydb = activityRepository.findByCode(activity);
				if (activitydb == null) {
					return ResponseEntity.ok(sr);					
				}
			}
		}
		
		if (founding != null) {
			founding = founding.normalizeDate(defaultFromDate);
		}
		
		if (dissolution != null) {
			dissolution = dissolution.normalizeDate(defaultFromDate);
		}
		
		List<GenericResponse> entity = null;
		List<GenericResponse> activities = null;
		List<GenericResponse> places = null;
		List<GenericResponse> foundingDates = null;
		List<GenericResponse> dissolutionDates = null;

		if (country == null) {
			if (placedb == null && activitydb == null && founding == null && dissolution == null) {
				if (cactivity) {
					List<StatisticDB> activityStats = statisticsRepository.findByDimensionAndParentActivityIsNullGroupByActivity(Dimension.NACE.toString());
					activities = iter(activityStats, null, null, null, null, language);
				}
					
				if (cplace) {
					List<StatisticDB> placeStats = statisticsRepository.findByDimension(Dimension.DATA.toString());
					places = iter(placeStats, null, null, null, null, language);
				}
				
			} else if (placedb == null && founding == null && dissolution == null) {
				if (ccurrent) {
					List<StatisticDB> entityStats = statisticsRepository.findByDimensionAndActivityGroupByActivity(Dimension.NACE.toString(), activitydb);
					entity = iter(entityStats, null, null, null, null, language);
				}
				
				if (cactivity) {
					List<StatisticDB> activityStats = statisticsRepository.findByDimensionAndParentActivityGroupByActivity(Dimension.NACE.toString(), activitydb);
					activities = iter(activityStats, null, null, null, null, language);
				}
				
				if (cplace) {
					List<StatisticDB> placeStats = statisticsRepository.findByDimensionAndActivity(Dimension.NACE.toString(), activitydb);
					places = iter(placeStats, null, null, null, null, language);
				}
				
			} else {
				return ResponseEntity.ok(sr);
			} 
			
		} else {
			
			CountryConfiguration cc = countryConfigurations.get(country);
			if (cc == null) {
				return ResponseEntity.ok(sr);
			}
			
			Set<Dimension> stats = cc.getStatistics();
			
			if (placedb != null && !(cc.isNuts() || cc.isLau())) {
				return ResponseEntity.ok(sr);
			}
			if (activitydb != null && !cc.isNace()) {
				return ResponseEntity.ok(sr);
			}
			if (founding != null && !cc.isFoundingDate()) {
				return ResponseEntity.ok(sr);
			}
			if (dissolution != null && !cc.isDissolutionDate()) {
				return ResponseEntity.ok(sr);
			}
	
			cplace &= cc.isNuts() || cc.isLau();
			cactivity &= cc.isNace();
			cfounding &= cc.isFoundingDate();
			cdissolution &= cc.isDissolutionDate();
			
			Code defaultDate = Code.createDateCode(defaultFromDate, new Date(new java.util.Date().getTime()), Code.date10Y).normalizeDate(defaultFromDate);
		    
			if (placedb != null && activitydb != null) {
				if (stats.contains(Dimension.NUTSLAU_NACE) && founding == null && dissolution == null) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndActivityAndPlace(country, Dimension.NUTSLAU_NACE.toString(), activitydb, placedb);
						entity = iter(entityStats, null, null, null, null, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndParentActivity(country, Dimension.NUTSLAU_NACE.toString(), placedb, activitydb);
						activities = iter(activityStats, null, null, null, null, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndActivityAndParentPlace(country, Dimension.NUTSLAU_NACE.toString(), activitydb, placedb);
						places = iter(placeStats, null, null, null, null, language);
					}
					
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, placedb.getCode(), activitydb.getCode(), founding, dissolution));
						entity = iter(entityStats, placedb, activitydb, founding, dissolution, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, activitydb.getCode(), Arrays.asList(placedb.getCode()), null, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, placedb, null, founding, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, placedb.getCode(), null, Arrays.asList(activitydb.getCode()), founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, activitydb, founding, dissolution, language);
					}
					
				}
				
				if (cfounding) {
					List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate, Arrays.asList(placedb.getCode()), Arrays.asList(activitydb.getCode()), null, dissolution, false), Dimension.FOUNDING);
					foundingDates = iterFounding(foundingStats, placedb, activitydb, null, dissolution, language);
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, Arrays.asList(placedb.getCode()), Arrays.asList(activitydb.getCode()), founding, null, false), Dimension.DISSOLUTION);
					dissolutionDates = iterDissolution(dissolutionStats, placedb, activitydb, founding, null, language);
				}
				
			} else if (activitydb != null) {
				if (stats.contains(Dimension.NACE)  && founding == null && dissolution == null) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndActivity(country, Dimension.NACE.toString(), activitydb);
						entity = iter(entityStats, null, null, null, null, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivity(country, Dimension.NACE.toString(), activitydb);
						activities = iter(activityStats, null, null, null, null, language);
					}
					
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, null, activitydb.getCode(), founding, dissolution));
						entity = iter(entityStats, null, activitydb, founding, dissolution, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, activitydb.getCode(), null, null, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, null, null, founding, dissolution, language);
					}
				}
	
				if (cplace) {
					if (cc.getStatistics().contains(Dimension.NUTSLAU_NACE)  && founding == null && dissolution == null) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndActivityAndParentPlaceIsNull(country, Dimension.NUTSLAU_NACE.toString(), activitydb);
						places = iter(placeStats, null, null, null, null, language);
					} else {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, null, null, Arrays.asList(activitydb.getCode()), founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, activitydb, founding, dissolution, language);
					}
				}
				
				if (cfounding) {
					List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate,  null, Arrays.asList(activitydb.getCode()), null, dissolution, false), Dimension.FOUNDING);
					foundingDates =  iterFounding(foundingStats, null, activitydb, null, dissolution, language);
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, null, Arrays.asList(activitydb.getCode()), founding, null, false), Dimension.DISSOLUTION);
					dissolutionDates = iterDissolution(dissolutionStats, null, activitydb, founding, null, language);
				}
				
			} else if (placedb != null && founding != null) {
				
				if (stats.contains(Dimension.NUTSLAU_FOUNDING) && dissolution == null) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndDateInterval(country, Dimension.NUTSLAU_FOUNDING.toString(), placedb, founding.getDateFrom(), founding.getDateTo());
						entity = iter(entityStats, null, null, null, null, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceAndDateInterval(country, Dimension.NUTSLAU_FOUNDING.toString(), placedb, founding.getDateFrom(), founding.getDateTo());
						places = iter(placeStats, null, null, null, null, language);
					}
	
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, placedb.getCode(), null, founding, dissolution));
						entity = iter(entityStats, placedb, null, founding, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, placedb.getCode(), null, null, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, null, founding, dissolution, language);
					}
				}
				
				if (cactivity) {
	//				if (cc.getStatistics().contains(Dimension.NUTSLAU_NACE) && founding == null && dissolution == null) {
	//					List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndParentActivity(country, Dimension.NUTSLAU_NACE.toString(), placedb, null);
	//					activities = iter(activityStats, null, null, null, null, language);
	//				} else {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, null, Arrays.asList(placedb.getCode()), null, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, placedb, null, founding, dissolution, language);
	//				}
				}
	
				if (cfounding) {
					if (cc.getStatistics().contains(Dimension.NUTSLAU_FOUNDING) && dissolution == null) {
						List<StatisticDB> foundingStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndDateRange(country, Dimension.NUTSLAU_FOUNDING.toString(), placedb, founding.getDateFrom(), founding.getDateTo(), Code.date1M, dateRangeName(founding.getDateInterval()));
						foundingDates = iterFounding(foundingStats, placedb, null, null, null, language);
					} else {
						List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate, Arrays.asList(placedb.getCode()), null, null, dissolution, false), Dimension.FOUNDING);
						foundingDates = iterFounding(foundingStats, placedb, null, null, dissolution, language);
					}
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, null, Arrays.asList(placedb.getCode()), null, founding, false), Dimension.DISSOLUTION);
					dissolutionDates = iterDissolution(dissolutionStats, placedb, null, founding, null, language);
				}
							
			} else if (placedb != null) {
				
				if (stats.contains(Dimension.NUTSLAU) && founding == null && dissolution == null) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndPlace(country, Dimension.NUTSLAU.toString(), placedb);
						entity = iter(entityStats, null, null, null, null, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlace(country, Dimension.NUTSLAU.toString(), placedb);
						places = iter(placeStats, null, null, null, null, language);
					}
	
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, placedb.getCode(), null, founding, dissolution));
						entity = iter(entityStats, placedb, null, founding, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, placedb.getCode(), null, null, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, null, founding, dissolution, language);
					}
				}
				
				if (cactivity) {
					if (cc.getStatistics().contains(Dimension.NUTSLAU_NACE) && founding == null && dissolution == null) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndParentActivityIsNull(country, Dimension.NUTSLAU_NACE.toString(), placedb);
						activities = iter(activityStats, null, null, null, null, language);
					} else {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, null, Arrays.asList(placedb.getCode()), null, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, placedb, null, founding, dissolution, language);
					}
				}
				
				if (cfounding) {
					List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate, Arrays.asList(placedb.getCode()), null, null, dissolution, false), Dimension.FOUNDING);
					foundingDates = iterFounding(foundingStats, placedb, null, null, dissolution, language);
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, null, Arrays.asList(placedb.getCode()), null, founding, false), Dimension.DISSOLUTION);
					dissolutionDates = iterDissolution(dissolutionStats, placedb, null, founding, null, language);
				}
				
			} else {
				if (ccurrent) {
					List<StatisticDB> entityStats;
					if (stats.contains(Dimension.DATA) && founding == null && dissolution == null) {
						entityStats = statisticsRepository.findByCountryAndDimension(country, Dimension.DATA.toString());
					} else {
						entityStats = mapResults(statisticsService.singleStatistic(cc, null, null, founding, dissolution));
					}
					entity = iter(entityStats, null, null, founding, dissolution, language);
				}
	
				
				if (cactivity) {
					List<StatisticDB> activityStats;
					if (stats.contains(Dimension.NACE) && founding == null && dissolution == null) {
						activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivityIsNull(country, Dimension.NACE.toString());
					} else {
						activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, null, null, null, founding, dissolution, false), Dimension.NACE);
					}
					activities = iter(activityStats, null, null, founding, dissolution, language);
				}
				
				if (cplace) {
					List<StatisticDB> placeStats;
					if (stats.contains(Dimension.NUTSLAU) && founding == null && dissolution == null) {
						placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceIsNull(country, Dimension.NUTSLAU.toString());
					} else if (stats.contains(Dimension.NUTSLAU_FOUNDING) && founding != null && dissolution == null) {
						placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceIsNull(country, Dimension.NUTSLAU.toString());
//						placeStats = statisticsRepository.findByCountryAndDimensionAndDateRange(country, Dimension.FOUNDING.toString(), founding.getFromDate(), founding.getToDate(), Code.date1M, dateRangeName(founding.getDateInterval()));
					} else {
						placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, null, null, null, founding, dissolution, false), Dimension.NUTSLAU);
					}
					places = iter(placeStats, null, null, founding, dissolution, language);
				}
				
				if (cfounding) {
					if (founding == null) {
						founding = defaultDate;
					}
					
					List<StatisticDB> foundingStats;
					if (stats.contains(Dimension.FOUNDING) && dissolution == null) {
	//					foundingStats = statisticsRepository.findByCountryAndDimensionAndParentActivityAndParentPlaceAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDateInterval(country, Dimension.FOUNDING.toString(), null, null, founding.getFromDate(), founding.getToDate(), founding.getDateInterval());
						foundingStats = statisticsRepository.findByCountryAndDimensionAndDateRange(country, Dimension.FOUNDING.toString(), founding.getDateFrom(), founding.getDateTo(), Code.date1M, dateRangeName(founding.getDateInterval()));
					} else {
						foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding, null, null, null, dissolution, false), Dimension.FOUNDING);
					}
					foundingDates = iterFounding(foundingStats, null, null, null, dissolution, language);
	
				}
				
				if (cdissolution) {
					if (dissolution == null) {
						dissolution = defaultDate;
					}
					
					List<StatisticDB> dissolutionStats;
					if (stats.contains(Dimension.DISSOLUTION) && founding == null) {
	//					dissolutionStats = statisticsRepository.findByCountryAndDimensionAndParentActivityAndParentPlaceAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDateInterval(country, Dimension.DISSOLUTION.toString(), null, null, dissolution.getFromDate(), dissolution.getToDate(), dissolution.getDateInterval());
						dissolutionStats = statisticsRepository.findByCountryAndDimensionAndDateRange(country, Dimension.FOUNDING.toString(), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M, dateRangeName(dissolution.getDateInterval()));
					} else {
						dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution, null, null, founding, null, false), Dimension.DISSOLUTION);
					}
					dissolutionDates = iterDissolution(dissolutionStats, null, null, founding, null, language);
	
				}
			}
			
		}

		if (entity != null && entity.size() != 1) {
			return ResponseEntity.ok(sr);
		}
		
		if (entity != null) {
			sr.setEntity(entity.get(0));
		}
		if (activities != null) {
			sr.setActivities(activities);
		}
		if (places != null) {
			sr.setPlaces(places);
		}
		if (foundingDates != null) {
			sr.setFoundingDates(foundingDates);
		}
		if (dissolutionDates != null) { 
			sr.setDissolutionDates(dissolutionDates);
		}
		
		return ResponseEntity.ok(sr);
    }
	
	private static String dateRangeName(String range) {
		if (range.equals(Code.date1M)) {
			return "month";
		} else if (range.equals(Code.date3M)) {
			return "quarter";
		} else if (range.equals(Code.date1Y)) {
			return "year";
		} else if (range.equals(Code.date10Y)) {
			return "decade";
		}
		
		return "year";
	}
	
	private List<GenericResponse> iter(List<StatisticDB> stats, PlaceDB placedb, ActivityDB activitydb, Code founding, Code dissolution, String language) {
		List<GenericResponse> result = new ArrayList<>();
		for (StatisticDB st : stats) {
			result.add(GenericResponse.createFromStatistic(st, countryConfigurations.get(st.getCountry()), placedb, activitydb, founding, dissolution, language));
		}
		
		return result;
	}

	private List<GenericResponse> iterFounding(List<StatisticDB> stats, PlaceDB placedb, ActivityDB activitydb, Code founding, Code dissolution, String language) {
		List<GenericResponse> result = new ArrayList<>();
		for (StatisticDB st : stats) {
			result.add(GenericResponse.createFromFoundingStatistic(st, countryConfigurations.get(st.getCountry()), placedb, activitydb, founding, dissolution, language));
		}
		
		return result;
	}
	
	private List<GenericResponse> iterDissolution(List<StatisticDB> stats, PlaceDB placedb, ActivityDB activitydb, Code founding, Code dissolution, String language) {
		List<GenericResponse> result = new ArrayList<>();
		for (StatisticDB st : stats) {
			result.add(GenericResponse.createFromDissolutionStatistic(st, countryConfigurations.get(st.getCountry()), placedb, activitydb, founding, dissolution, language));
		}
		
		return result;
	}
	
	private List<StatisticDB> mapResults(StatisticResult sr) {
		StatisticDB gr = new StatisticDB();
		gr.setCountry(sr.getCountry());
		gr.setCount(sr.getCount());
		
		return Arrays.asList(gr);
	}
	
	private List<StatisticDB> mapResults(List<StatisticResult> list, Dimension dimension) {
		if (list.size() == 0) {
			return null;
		}
		
		List<StatisticDB> res = new ArrayList<>();
		
		for (StatisticResult sr : list) {
			StatisticDB gr = new StatisticDB();
			if (dimension == Dimension.NUTSLAU) {
				PlaceDB placedb = placeRepository.findByCode(sr.getCode());
				gr.setPlace(placedb);
			} else if (dimension == Dimension.NACE) {
				ActivityDB activitydb = activityRepository.findByCode(sr.getCode());
				gr.setActivity(activitydb);
			} else if (dimension == Dimension.FOUNDING || dimension == Dimension.DISSOLUTION) {
				gr.setFromDate(sr.getCode().getDateFrom());
				gr.setToDate(sr.getCode().getDateTo());

			}
			
			gr.setCountry(sr.getCountry());
			gr.setCount(sr.getCount());
			res.add(gr);
		}
		
		return res;
	}
	
	@GetMapping("/compute")
	public ResponseEntity<?> statistics(@RequestParam(required = true) String country, 
	    		                            @RequestParam(required = true) Dimension dimension, 
	    		                            @RequestParam(required = false) String top,
	    		                            @RequestParam(defaultValue = "false") boolean allLevels,
	    		                            @RequestParam(required = false) List<Code> place,
	                                        @RequestParam(required = false) List<Code> activity,
	                                        @RequestParam(required = false) Code founding,
	                                        @RequestParam(required = false) Code dissolution) {
		CountryConfiguration cc = countryConfigurations.get(country);

	    if (cc != null) {
	    	List<GenericResponse> res = new ArrayList<>();
	    		
	    	if (dimension == Dimension.NUTS || dimension == Dimension.NACE) {
	    		List<StatisticResult> list = statisticsService.statistics(cc, dimension, top != null ? new Code(top) : null, place, activity, founding, dissolution, allLevels);
	    			
	    		res = mapGenericResponseFromList(list, cc, dimension);
	    			
	    	} else if (dimension == Dimension.FOUNDING || dimension == Dimension.DISSOLUTION) {
	    		try {
			    	Date fromDate = null;
			    	Date toDate = null;

	    			if (top != null) {
	    		    	Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})?--(\\d{4}-\\d{2}-\\d{2})?");
	    			    	
	    		    	Matcher m = p.matcher(top);
	    		    	if (m.find()) {
	    		    		fromDate = Date.valueOf(m.group(1));
	    		    		toDate = Date.valueOf(m.group(2));
	    		    	}
	    			}
	    				
//	    				statisticsService.dateStatistics(cc, dimension, fromDate, toDate, null, place.orElse(null), activity.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
//			    		if (allLevels) {
//			    			for (int i = 0; i < res.size(); i++) {
//			    				statisticsService.dateStatistics(cc, dimension, res.get(i).getFromDate(), res.get(i).getToDate(), res.get(i).getInterval(), place.orElse(null), activity.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
//			    			}
//			    		}
	    				
	    			List<StatisticResult> list = statisticsService.dateStatistics(cc, dimension, Code.createDateCode(fromDate, toDate), place, activity, founding, dissolution, allLevels);
	    				
	    			res = mapGenericResponseFromList(list, cc, dimension);
			    		
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    			return ResponseEntity.badRequest().build();
	    		}
	    	}
	    		

	    	return ResponseEntity.ok(res);
	    } else {
	    	return ResponseEntity.notFound().build();
	    }
	}


	
	private List<GenericResponse> mapGenericResponseFromList(List<StatisticResult> list, CountryConfiguration cc, Dimension dimension) {
		List<GenericResponse> res = new ArrayList<>();
		   
   		for (StatisticResult sr : list) {
			GenericResponse gr;
			if (dimension == Dimension.NUTS) {
				PlaceDB placedb = placeRepository.findByCode(sr.getCode());
				gr = GenericResponse.createFromPlace(placedb, null);
			} else {
				ActivityDB activitydb = activityRepository.findByCode(sr.getCode());
				gr = GenericResponse.createFromActivity(activitydb, null);
			}
			if (sr.getCountry() != null) {
				gr.setCountry(new CodeLabel(sr.getCountry(), cc.getCountryLabel()));
			}
			gr.setCount(sr.getCount());
			res.add(gr);
			
		}

   		return res;
	}
	   
//  From Mongo	
//	@GetMapping(produces = "application/json")
//    public ResponseEntity<?> getStatistics(@RequestParam(required = false) Optional<String> country,
//    		                               @RequestParam(required = false) Optional<String> place,
//                                           @RequestParam(required = false) Optional<String> activity) {
//		
//		if (!country.isPresent()) {
//			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//		}
//
//		String vCountry = country.get();
//		
//		List<Statistic> mainStat = null;
//		List<Statistic> subActivityStats = null;
//		List<Statistic> subPlaceStats = null;
//		
//		if (place.isPresent() && place.get().equals(NutsService.nutsPrefix + vCountry)) {
//			place = Optional.empty();
//		}
//		
//		if (place.isPresent() && activity.isPresent()) {
//			mainStat = statisticsRepository.findByCountryAndDimensionAndActivityAndPlace(vCountry, Dimension.PLACE_ACTIVITY.toString(), activity.get(), place.get());
//			subActivityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndParentActivity(vCountry, Dimension.PLACE_ACTIVITY.toString(), place.get(), activity.get());
//			subPlaceStats = statisticsRepository.findByCountryAndDimensionAndActivityAndParentPlace(vCountry, Dimension.PLACE_ACTIVITY.toString(), activity.get(), place.get());
//		} else if (activity.isPresent()) {
//			mainStat = statisticsRepository.findByCountryAndDimensionAndActivity(vCountry, Dimension.ACTIVITY.toString(), activity.get());
//			subActivityStats = statisticsRepository.findByCountryAndDimensionAndParentActivity(vCountry, Dimension.ACTIVITY.toString(), activity.get());
//			subPlaceStats = statisticsRepository.findByCountryAndDimensionAndActivityAndNoParentPlace(vCountry, Dimension.PLACE_ACTIVITY.toString(), activity.get());
//		} else if (place.isPresent()) {
//			mainStat = statisticsRepository.findByCountryAndDimensionAndPlace(vCountry, Dimension.PLACE.toString(), place.get());
//			subActivityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndNoParentActivity(vCountry, Dimension.PLACE_ACTIVITY.toString(), place.get());
//			subPlaceStats = statisticsRepository.findByCountryAndDimensionAndParentPlace(vCountry, Dimension.PLACE.toString(), place.get());
//		} else {
//			mainStat = statisticsRepository.findByCountryAndDimension(vCountry, Dimension.DATA.toString());
//			subActivityStats = statisticsRepository.findByCountryAndDimensionAndNoParentActivity(vCountry, Dimension.ACTIVITY.toString());
//			subPlaceStats = statisticsRepository.findByCountryAndDimensionAndNoParentPlace(vCountry, Dimension.PLACE.toString());
//
//		}
//		
//		if (mainStat.size() != 1) {
//			return ResponseEntity.ok(null);
//		}
//		
//		StatisticsResponse sr = new StatisticsResponse();
//		sr.setMain(StatisticResponse.createFromStatistic(mainStat.get(0)));
//		sr.setSubActivities(subActivityStats.stream().map(st -> StatisticResponse.createFromStatistic(st)).collect(Collectors.toList()));
//		sr.setSubPlaces(subPlaceStats.stream().map(st -> StatisticResponse.createFromStatistic(st)).collect(Collectors.toList()));
//		
//		return ResponseEntity.ok(sr);
//    }
	
}
