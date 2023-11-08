package com.ails.stirdatabackend.controller;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.model.StatisticResult;
import com.ails.stirdatabackend.payload.GenericResponse;
import com.ails.stirdatabackend.payload.ComplexResponse;
import com.ails.stirdatabackend.repository.ActivitiesDBRepository;
import com.ails.stirdatabackend.repository.PlacesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.service.NaceService;
import com.ails.stirdatabackend.service.NutsService;
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
	NutsService nutsService;

	@Autowired 
	NaceService naceService;
	
    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    @Qualifier("default-from-date")
    private Date defaultFromDate;
    
    @Autowired
    @Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;

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
													description = "Place definition in the form [prefix]:[code]" +
																			"where prefix= nuts, lau.",
													schema = @Schema(implementation = String.class),
													example = "nuts:NO"
												)
												List<Code> place,
	                                            @RequestParam(required = false) 
											   	@Parameter(
													description = "Activity definition in the form nace-rev2:[code].",
													schema = @Schema(implementation = String.class),
													example = "nace-rev2:F"
												) 
												List<Code> activity,
	                                            @RequestParam(required = false)  
											   	@Parameter(
													description = "Founding date range in the form date-range:[start-date]:[end-date].",
													schema = @Schema(implementation = String.class),
													example = "date-range:2020-01-01:2020-31-12"
												) 
												Code founding,
	                                            @RequestParam(required = false)  
												@Parameter(
													description = "Dissolution date range in the form date-range:[start-date]:[end-date].",
													schema = @Schema(implementation = String.class),
													example = "date-range:2020-01-01:2020-31-12"

												) 
												Code dissolution,
	                                            @RequestParam(defaultValue = "selection")  
											   	@Parameter(
											   		description = 	"The statistics dimensions to be returned. Multiple dimensions should be separated by comma. "+
																	"Available dimensions: selection, place, activity, foundingDate, dissolutionDate",
													example = "place,activity"
												) 
												String dimension,
	                                            @RequestParam(defaultValue = "en") 
											   	@Parameter(
													description = "Select the language of the labels in the response"
												) 
												String language
											   	) {

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
		List<PlaceDB> placedb = new ArrayList<>();
		List<ActivityDB> activitydb = new ArrayList<>();
		List<Code> statCodes = new ArrayList<>();
		
		if (place != null) {
			place = new ArrayList<>(new HashSet<>(place));
			
			// assumes all places are in the same country
			for (Code pl : place) {
				if (!(pl.isNuts() || pl.isLau() || pl.isStat())) {
					return ResponseEntity.ok(sr);
				} else {
					if (pl.isNuts()) {
						country = pl.getNutsCountry();
					} else if (pl.isLau()) {
						country = pl.getLauCountry();
					} else if (pl.isStat()) {
						statCodes.add(pl);
						continue;
					}
					
					if (!pl.isNutsCountry()) {
						PlaceDB pldb = placeRepository.findByCode(pl);
						if (pldb == null) {
							return ResponseEntity.ok(sr);
						} else {
							placedb.add(pldb);
						}
					}
				}
			}
		}
		
		if (activity != null) {
			activity = new ArrayList<>(new HashSet<>(activity));
			
			for (Code ac : activity) {
				if (!ac.isNaceRev2()) {
					return ResponseEntity.ok(sr);
				} else {
					ActivityDB acdb = activityRepository.findByCode(ac);
					if (acdb == null) {
						return ResponseEntity.ok(sr);					
					} else {
						activitydb.add(acdb);
					}
				}
			}
		}
		
		if (founding != null) {
			if (!founding.isDate()) {
				return ResponseEntity.ok(sr);
			}
			founding = founding.normalizeDate(defaultFromDate);
		}
		
		if (dissolution != null) {
			if (!dissolution.isDate()) {
				return ResponseEntity.ok(sr);
			}

			dissolution = dissolution.normalizeDate(defaultFromDate);
		}
		
		Code placeParent = null;
		
		//remove redundant
		if (placedb.size() > 1) {
			List<List<PlaceDB>> iparents = new ArrayList<>();
			
			for (int i = 0; i < placedb.size(); i++) {
				iparents.add(nutsService.getParents(placedb.get(i)));
			}
			
//			String s = placedb + "";

			loop:
			for (int i = 1; i < placedb.size();) {
				for (int j = 0; j < i; ) {
					if (iparents.get(i).contains(placedb.get(j))) {
						placedb.remove(i);
						iparents.remove(i);
						continue loop;
					} else if (iparents.get(j).contains(placedb.get(i))) {
						placedb.remove(j);
						iparents.remove(j);
						i--;
					} else {
						j++;
					}
				}
				
				i++;
			}
		}
		
//		System.out.println("B1P-- " + country);
//		System.out.println("B2P-- " + placedb);
		
		//find common place parent
		if (placedb.size() > 1) {
			List<PlaceDB> parents = nutsService.getParents(placedb.get(0));
			for (int i = 1; i < placedb.size(); i++) {
				List<PlaceDB> nextParents = nutsService.getParents(placedb.get(i));

				if (parents.size() > nextParents.size()) {  
					for (int k = nextParents.size(); k < parents.size(); k++) {
						parents.remove(k);
					}	
				}
				
				for (int j = 0; j < Math.min(parents.size(), nextParents.size()); j++) {
					if (!parents.get(j).getCode().equals(nextParents.get(j).getCode())) {
						for (int k = j; k < parents.size(); k++) {
							parents.remove(j);
						}
					}
				}
			}
			
			if (parents.size() > 0) {
				placeParent = parents.get(parents.size() - 1).getCode();	
			}
			
			if (placeParent != null && placeParent.isNutsCountry()) {
				placeParent = null;
			}
			
//			System.out.println("P3 " + placeParent);
			
		} else if (placedb.size() == 1) {
			placeParent = placedb.get(0).getCode();
		}

		Code activityParent = null;
		
		//remove redundant
		if (activitydb.size() > 1) {
			List<List<ActivityDB>> iparents = new ArrayList<>();
			
			for (int i = 0; i < activitydb.size(); i++) {
				iparents.add(naceService.getParents(activitydb.get(i)));
			}

//			String s = activitydb + "";
				
			loop:
			for (int i = 1; i < activitydb.size();) {
				for (int j = 0; j < i; ) {
					if (iparents.get(i).contains(activitydb.get(j))) {
						activitydb.remove(i);
						iparents.remove(i);
						continue loop;
					} else if (iparents.get(j).contains(activitydb.get(i))) {
						activitydb.remove(j);
						iparents.remove(j);
					    i--;
					} else {
						j++;
					}
				}
				
				i++;
			}
		}

		//find common activity parent
		if (activitydb.size() > 1) {
			
			List<ActivityDB> parents = naceService.getParents(activitydb.get(0));
			for (int i = 1; i < activitydb.size(); i++) {
				List<ActivityDB> nextParents = naceService.getParents(activitydb.get(i));
				
				if (parents.size() > nextParents.size()) {  
					for (int k = nextParents.size(); k < parents.size(); k++) {
						parents.remove(k);
					}	
				}
				
				for (int j = 0; j < Math.min(parents.size(), nextParents.size()); j++) {
					if (!parents.get(j).getCode().equals(nextParents.get(j).getCode())) {
						for (int k = j; k < parents.size(); k++) {
							parents.remove(j);
						}					
						break;
					}
				}
			}
			
			if (parents.size() > 0) {
				activityParent = parents.get(parents.size() - 1).getCode();	
			}
			
		} else if (activitydb.size() == 1) {
			activityParent = activitydb.get(0).getCode();
		}

		List<GenericResponse> entity = null;
		List<GenericResponse> activities = null;
		List<GenericResponse> places = null;
		List<GenericResponse> foundingDates = null;
		List<GenericResponse> dissolutionDates = null;
		
//		System.out.println("COUNTRY " + country);
//		
//		System.out.println("PLACE " + placedbCodes);
//		System.out.println("PARENT PLACE " + placeParent);
//
//		System.out.println("ACTIVITY " + activitydbCodes);
//		System.out.println("PARENT ACTIVITY " + activityParent);

		boolean usePrecomputed = true;

		if (country == null) {
			if (placedb.isEmpty() && activitydb.isEmpty() && founding == null && dissolution == null) {
				
				if (cactivity) {
					List<StatisticDB> activityStats = statisticsRepository.findByDimensionAndParentActivityIsNullGroupByActivity(Dimension.NACE.toString());
					activities = iter(activityStats, null, null, null, null, language);
				}
					
				if (cplace) {
					List<StatisticDB> placeStats = statisticsRepository.findByDimension(Dimension.DATA.toString());
					
					List<StatisticDB> placeStats2 = new ArrayList<>();
					for (StatisticDB s : placeStats) {
						if (countryConfigurations.keySet().contains(s.getCountry())) {
							placeStats2.add(s);
						}
					}
					
					places = iter(placeStats2, null, null, null, null, language);
				}
				
			} else if (placedb.isEmpty() && founding == null && dissolution == null) {
				
				// ASSUME HERE ONE SELECTED ACTIVITY ONLY :: FIX LATER
				
				if (ccurrent) {
					List<StatisticDB> entityStats = statisticsRepository.findByDimensionAndActivityGroupByActivity(Dimension.NACE.toString(), activitydb.get(0));
					entity = iter(entityStats, null, null, null, null, language);
				}
				
				if (cactivity) {
					List<StatisticDB> activityStats = statisticsRepository.findByDimensionAndParentActivityGroupByActivity(Dimension.NACE.toString(), activitydb.get(0));
					activities = iter(activityStats, null, null, null, null, language);
				}
				
				if (cplace) {
					List<StatisticDB> placeStats = statisticsRepository.findByDimensionAndActivity(Dimension.NACE.toString(), activitydb.get(0));
					
					List<StatisticDB> placeStats2 = new ArrayList<>();
					for (StatisticDB s : placeStats) {
						if (countryConfigurations.keySet().contains(s.getCountry())) {
							placeStats2.add(s);
						}
					}
					
					places = iter(placeStats2, null, null, null, null, language);
				}
				
			} else {
				return ResponseEntity.ok(sr);
			} 
			
		} else {
			
			CountryDB cc = countryConfigurations.get(country);
			if (cc == null) {
				return ResponseEntity.ok(sr);
			}
			
			if (!placedb.isEmpty() && !(cc.isNuts() || cc.isLau())) {
				return ResponseEntity.ok(sr);
			}
			if (!activitydb.isEmpty() && !cc.isNace()) {
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
			
			Code defaultDate = Code.createDateCode(defaultFromDate, new Date(new java.util.Date().getTime()), Code.date1Y).normalizeDate(defaultFromDate);

			List<Code> placedbCodes = new ArrayList<>();
			for (PlaceDB pl : placedb) {
				placedbCodes.add(pl.getCode());
			}
			
			if (statCodes != null) {
				placedbCodes.addAll(statCodes);
			}
			
			List<Code> activitydbCodes = new ArrayList<>();
			for (ActivityDB ac : activitydb) {
				activitydbCodes.add(ac.getCode());
			}
			
//			System.out.println(cc);
//			System.out.println(placedb);
//			System.out.println(activitydb);
//			System.out.println(founding);
//			System.out.println(dissolution);
//			System.out.println(statCodes);
//			System.out.println(placedbCodes);
			
			/////////////////////
			if (!placedbCodes.isEmpty() && !activitydb.isEmpty()) {
				
				if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_NACE) != null && placedb.size() == 1 && activitydb.size() == 1 && founding == null && dissolution == null && statCodes.isEmpty()) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndActivityAndPlace(country, Dimension.NUTSLAU_NACE.toString(), activitydb.get(0), placedb.get(0));
						entity = iter(entityStats, null, null, null, null, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndParentActivity(country, Dimension.NUTSLAU_NACE.toString(), placedb.get(0), activitydb.get(0));
						activities = iter(activityStats, null, null, null, null, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndActivityAndParentPlace(country, Dimension.NUTSLAU_NACE.toString(), activitydb.get(0), placedb.get(0));
						places = iter(placeStats, null, null, null, null, language);
					}
					
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, placedbCodes, true, activitydbCodes, founding, dissolution));
						entity = iter(entityStats, placedb, activitydb, founding, dissolution, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, activityParent, placedbCodes, true, activitydb.size() == 1 ? null : activitydbCodes, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, placedb, null, founding, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, placeParent, placedb.size() == 1 ? null : placedbCodes, true, activitydbCodes, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, activitydb, founding, dissolution, language);
					}
				}
				
				if (cfounding) {
					List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate, placedbCodes, true, activitydbCodes, null, dissolution, false), Dimension.FOUNDING);
					foundingDates = iterFounding(foundingStats, placedb, activitydb, null, dissolution, language);
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, placedbCodes, true, activitydbCodes, founding, null, false), Dimension.DISSOLUTION);					
					dissolutionDates = iterDissolution(dissolutionStats, placedb, activitydb, founding, null, language);
				}

			} else if (!activitydb.isEmpty() && founding != null) { // && placedb = null
				if (usePrecomputed && cc.getStatsDate(Dimension.NACE_FOUNDING) != null && activitydb.size() == 1 && dissolution == null) {
					
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndActivityAndFoundingDateRange(country, Dimension.NACE_FOUNDING.toString(), activitydb.get(0), founding.getDateFrom(), founding.getDateTo(), Code.date1M);
						entity = iter(entityStats, null, activitydb, founding, null, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivityAndFoundingDateRange(country, Dimension.NACE_FOUNDING.toString(), activitydb.get(0), founding.getDateFrom(), founding.getDateTo(), Code.date1M);
						activities = iter(activityStats, null, activitydb, founding, null, language);
					}
					
				} else {
					
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, null, true, activitydbCodes, founding, dissolution));
						entity = iter(entityStats, null, activitydb, founding, dissolution, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, activityParent, null, true, activitydb.size() == 1 ? null : activitydbCodes, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, null, null, founding, dissolution, language);
					}
				}
	
				if (cplace) {
					List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, null, null, true, activitydbCodes, founding, dissolution, false), Dimension.NUTSLAU);
					places = iter(placeStats, null, activitydb, founding, dissolution, language);
				}
				
				if (cfounding) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_FOUNDING) != null && activitydb.size() == 1 && dissolution == null) {
						List<StatisticDB> foundingStats = statisticsRepository.findByCountryAndDimensionAndActivityAndFoundingDateRange(country, Dimension.NACE_FOUNDING.toString(), activitydb.get(0), founding.getDateFrom(), founding.getDateTo(), Code.date1M, dateRangeName(founding.getDateInterval()));
						foundingDates = iterFounding(foundingStats, null, activitydb, null, null, language);
					} else {
						List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate, null, true, activitydbCodes, null, dissolution, false), Dimension.FOUNDING);
						foundingDates = iterFounding(foundingStats, null, activitydb, null, dissolution, language);
					}					
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, null, true, activitydbCodes, founding, null, false), Dimension.DISSOLUTION);					
					dissolutionDates = iterDissolution(dissolutionStats, null, activitydb, founding, null, language);
				}
				
			} else if (!activitydb.isEmpty() && dissolution != null) { // && placedb = null && founding == null
				if (usePrecomputed && cc.getStatsDate(Dimension.NACE_FOUNDING) != null && activitydb.size() == 1) {
					
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndActivityAndDissolutionDateRange(country, Dimension.NACE_DISSOLUTION.toString(), activitydb.get(0), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M);
						entity = iter(entityStats, null, activitydb, null, dissolution, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivityAndDissolutionDateRange(country, Dimension.NACE_DISSOLUTION.toString(), activitydb.get(0), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M);
						activities = iter(activityStats, null, activitydb, null, dissolution, language);
					}
					
				} else {
					
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, null, true, activitydbCodes, founding, dissolution));
						entity = iter(entityStats, null, activitydb, founding, dissolution, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, activityParent, null, true, activitydb.size() == 1 ? null : activitydbCodes, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, null, null, founding, dissolution, language);
					}
				}
	
				if (cplace) {
					List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, null, null, true, activitydbCodes, founding, dissolution, false), Dimension.NUTSLAU);
					places = iter(placeStats, null, activitydb, founding, dissolution, language);
				}
				
				if (cfounding) {
					List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, defaultDate, null, true, activitydbCodes, null, dissolution, false), Dimension.FOUNDING);
					foundingDates = iterFounding(foundingStats, null, activitydb, null, dissolution, language);
				}
				
				if (cdissolution) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NACE_DISSOLUTION) != null && activitydb.size() == 1) {
						List<StatisticDB> dissolutionStats = statisticsRepository.findByCountryAndDimensionAndActivityAndDissolutionDateRange(country, Dimension.NACE_DISSOLUTION.toString(), activitydb.get(0), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M, dateRangeName(dissolution.getDateInterval()));
						dissolutionDates = iterDissolution(dissolutionStats, null, activitydb, null, null, language);
					} else {
						List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution, null, true, activitydbCodes, founding, null, false), Dimension.DISSOLUTION);
						dissolutionDates = iterDissolution(dissolutionStats, null, activitydb, founding, null, language);
					}
				}				
				
			} else if (!activitydb.isEmpty()) { // && placedb == null && founding == null && dissolution == null
				if (usePrecomputed && cc.getStatsDate(Dimension.NACE) != null && activitydb.size() == 1) {
					
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndActivity(country, Dimension.NACE.toString(), activitydb.get(0));
						entity = iter(entityStats, null, null, null, null, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivity(country, Dimension.NACE.toString(), activitydb.get(0));
						activities = iter(activityStats, null, null, null, null, language);
					}
					
				} else {
					
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, null, true, activitydbCodes, founding, dissolution));
						entity = iter(entityStats, null, activitydb, founding, dissolution, language);
					}
					
					if (cactivity) {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, activityParent, null, true, activitydb.size() == 1 ? null : activitydbCodes, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, null, null, founding, dissolution, language);
					}
				}
				
				if (cplace) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_NACE) != null && activitydb.size() == 1 && founding == null && dissolution == null) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndActivityAndParentPlaceIsNull(country, Dimension.NUTSLAU_NACE.toString(), activitydb.get(0));
						places = iter(placeStats, null, null, null, null, language);
					} else {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, null, null, true, activitydbCodes, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, activitydb, founding, dissolution, language);
					}
				}
				
				if (cfounding) {
					List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate, null, true, activitydbCodes, null, dissolution, false), Dimension.FOUNDING);
					foundingDates =  iterFounding(foundingStats, null, activitydb, null, dissolution, language);
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, null, true, activitydbCodes, founding, null, false), Dimension.DISSOLUTION);
					dissolutionDates = iterDissolution(dissolutionStats, null, activitydb, founding, null, language);
				}
//				
			} else if (!placedbCodes.isEmpty() && founding != null) { // && activity == null
				
				if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_FOUNDING) != null && placedb.size() == 1 && dissolution == null  && statCodes.isEmpty()) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndFoundingDateRange(country, Dimension.NUTSLAU_FOUNDING.toString(), placedb.get(0), founding.getDateFrom(), founding.getDateTo(), Code.date1M);
						entity = iter(entityStats, placedb, null, founding, null, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceAndFoundingDateRange(country, Dimension.NUTSLAU_FOUNDING.toString(), placedb.get(0), founding.getDateFrom(), founding.getDateTo(), Code.date1M);
						places = iter(placeStats, null, null, founding, null, language);
					}
	
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, placedbCodes, true, null, founding, dissolution));
						entity = iter(entityStats, placedb, null, founding, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, placeParent, placedb.size() == 1? null : placedbCodes, true, null, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, null, founding, dissolution, language);
					}
				}
				
				if (cactivity) {
					List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, null, placedbCodes, true, null, founding, dissolution, false), Dimension.NACE);
					activities = iter(activityStats, placedb, null, founding, dissolution, language);
				}
	
				if (cfounding) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_FOUNDING) != null && placedb.size() == 1 && dissolution == null && statCodes.isEmpty()) {
						List<StatisticDB> foundingStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndFoundingDateRange(country, Dimension.NUTSLAU_FOUNDING.toString(), placedb.get(0), founding.getDateFrom(), founding.getDateTo(), Code.date1M, dateRangeName(founding.getDateInterval()));
						foundingDates = iterFounding(foundingStats, placedb, null, null, null, language);
					} else {
						List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding != null ? founding : defaultDate, placedbCodes, true, null, null, dissolution, false), Dimension.FOUNDING);
						foundingDates = iterFounding(foundingStats, placedb, null, null, dissolution, language);
					}
				}
				
				if (cdissolution) {
					List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate,  placedbCodes, true, null, founding, null, false), Dimension.DISSOLUTION);
					dissolutionDates = iterDissolution(dissolutionStats, placedb, null, founding, null, language);
				}
				
			} else if (!placedbCodes.isEmpty() && dissolution != null) { // && activity == null && founding == null
				
				if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_DISSOLUTION) != null && placedb.size() == 1 && statCodes.isEmpty()) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndDissolutionDateRange(country, Dimension.NUTSLAU_DISSOLUTION.toString(), placedb.get(0), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M);
						entity = iter(entityStats, placedb, null, null, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceAndDissolutionDateRange(country, Dimension.NUTSLAU_DISSOLUTION.toString(), placedb.get(0), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M);
						places = iter(placeStats, placedb, null, null, dissolution, language);
					}
	
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, placedbCodes, true, null, founding, dissolution));
						entity = iter(entityStats, placedb, null, founding, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, placeParent, placedb.size() == 1? null : placedbCodes, true, null, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, null, founding, dissolution, language);
					}
				}
				
				if (cactivity) {
					List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, null, placedbCodes, true, null, founding, dissolution, false), Dimension.NACE);
					activities = iter(activityStats, placedb, null, founding, dissolution, language);
				}
	
				
				if (cfounding) {
					List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, defaultDate, placedbCodes, true, null, null, dissolution, false), Dimension.FOUNDING);
					foundingDates = iterDissolution(foundingStats, placedb, null, null, dissolution, language);
				}
				
				if (cdissolution) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_DISSOLUTION) != null && placedb.size() == 1 && statCodes.isEmpty()) {
						List<StatisticDB> dissolutionStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndDissolutionDateRange(country, Dimension.NUTSLAU_DISSOLUTION.toString(), placedb.get(0), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M, dateRangeName(dissolution.getDateInterval()));
						dissolutionDates = iterDissolution(dissolutionStats, placedb, null, null, null, language);
					} else {
						List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution != null ? dissolution : defaultDate, placedbCodes, true, null, founding, null, false), Dimension.DISSOLUTION);
						dissolutionDates = iterDissolution(dissolutionStats, placedb, null, founding, null, language);
					}
				}
				
			} else if (!placedbCodes.isEmpty()) { // && activitydb == null && founding == null && dissolution == null

				if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU) != null && placedb.size() == 1 && statCodes.isEmpty()) {
					if (ccurrent) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndPlace(country, Dimension.NUTSLAU.toString(), placedb.get(0));
						entity = iter(entityStats, null, null, null, null, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlace(country, Dimension.NUTSLAU.toString(), placedb.get(0));
						places = iter(placeStats, null, null, null, null, language);
					}
	
				} else {
					if (ccurrent) {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, placedbCodes, true, null, founding, dissolution));
						entity = iter(entityStats, placedb, null, founding, dissolution, language);
					}
					
					if (cplace) {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, placeParent, place.size() == 1 ? null : placedbCodes, true, null, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, null, founding, dissolution, language);
					}
				}
				
				if (cactivity) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_NACE) != null && placedb.size() == 1 && statCodes.isEmpty()) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndParentActivityIsNull(country, Dimension.NUTSLAU_NACE.toString(), placedb.get(0));
						activities = iter(activityStats, null, null, null, null, language);
					} else {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, null, placedbCodes, true, null, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, placedb, null, founding, dissolution, language);
					}
				}
				
				if (cfounding) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_FOUNDING) != null && placedb.size() == 1 && statCodes.isEmpty()) {
						List<StatisticDB> foundingStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndFoundingDateRange(country, Dimension.NUTSLAU_FOUNDING.toString(), placedb.get(0), defaultDate.getDateFrom(), defaultDate.getDateTo(), Code.date1M, dateRangeName(defaultDate.getDateInterval()));
						foundingDates = iterFounding(foundingStats, placedb, null, null, dissolution, language);
					} else {
						List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, defaultDate, placedbCodes, true, null, null, dissolution, false), Dimension.FOUNDING);
						foundingDates = iterFounding(foundingStats, placedb, null, null, dissolution, language);
					}
				}
				
				if (cdissolution) {
					
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_DISSOLUTION) != null && placedb.size() == 1 && statCodes.isEmpty()) {
						List<StatisticDB> dissolutionStats = statisticsRepository.findByCountryAndDimensionAndPlaceAndDissolutionDateRange(country, Dimension.NUTSLAU_DISSOLUTION.toString(), placedb.get(0), defaultDate.getDateFrom(), defaultDate.getDateTo(), Code.date1M, dateRangeName(defaultDate.getDateInterval()));
						dissolutionDates = iterDissolution(dissolutionStats, placedb, null, founding, null, language);
					} else {
						List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, defaultDate, placedbCodes, true, null, founding, null, false), Dimension.DISSOLUTION);
						dissolutionDates = iterDissolution(dissolutionStats, placedb, null, founding, null, language);
					}
				}
			
			} else {
				if (ccurrent) {
					
					if (usePrecomputed && cc.getStatsDate(Dimension.DATA) != null && founding == null && dissolution == null) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimension(country, Dimension.DATA.toString());
						entity = iter(entityStats, null, null, founding, dissolution, language);
					} else if (usePrecomputed && cc.getStatsDate(Dimension.FOUNDING) != null && founding != null && dissolution == null) {
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndFoundingDateRange(country, Dimension.FOUNDING.toString(), founding.getDateFrom(), founding.getDateTo(), Code.date1M);
						entity = iterFounding(entityStats, null, null, null, dissolution, language);
					} else if (usePrecomputed && cc.getStatsDate(Dimension.DISSOLUTION) != null && founding == null && dissolution != null) { 
						List<StatisticDB> entityStats = statisticsRepository.findByCountryAndDimensionAndDissolutionDateRange(country, Dimension.DISSOLUTION.toString(), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M);
						entity = iterDissolution(entityStats, null, null, founding, null, language);
					} else {
						List<StatisticDB> entityStats = mapResults(statisticsService.singleStatistic(cc, null, true, null, founding, dissolution));
						entity = iter(entityStats, null, null, founding, dissolution, language);
					}
					
				}
	
				
				if (cactivity) {
					if (usePrecomputed && cc.getStatsDate(Dimension.NACE) != null && founding == null && dissolution == null) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivityIsNull(country, Dimension.NACE.toString());
						activities = iter(activityStats, null, null, founding, dissolution, language);
					} else if (usePrecomputed && cc.getStatsDate(Dimension.NACE_FOUNDING) != null && founding != null && dissolution == null) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivityIsNullAndFoundingDateRange2(country, Dimension.NACE_FOUNDING.toString(), founding.getDateFrom(), founding.getDateTo(), Code.date1M);
						activities = iterFounding(activityStats, null, null, null, dissolution, language);
					} else if (usePrecomputed && cc.getStatsDate(Dimension.NACE_DISSOLUTION) != null && founding == null && dissolution != null) {
						List<StatisticDB> activityStats = statisticsRepository.findByCountryAndDimensionAndParentActivityIsNullAndDissolutionDateRange2(country, Dimension.NACE_DISSOLUTION.toString(), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M);
						activities = iterDissolution(activityStats, null, null, founding, null, language);
					} else {
						List<StatisticDB> activityStats = mapResults(statisticsService.statistics(cc, Dimension.NACE, null, null, true, null, founding, dissolution, false), Dimension.NACE);
						activities = iter(activityStats, null, null, founding, dissolution, language);
					}
					
				}
				
				if (cplace) {
					
					if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU) != null && founding == null && dissolution == null) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceIsNull(country, Dimension.NUTSLAU.toString());
						places = iter(placeStats, null, null, founding, dissolution, language);
					} else if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_FOUNDING) != null && founding != null && dissolution == null) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceIsNullAndFoundingDateRange2(country, Dimension.NUTSLAU_FOUNDING.toString(), founding.getDateFrom(), founding.getDateTo(), Code.date1M);
						places = iterFounding(placeStats, null, null, null, dissolution, language);
					} else if (usePrecomputed && cc.getStatsDate(Dimension.NUTSLAU_DISSOLUTION) != null && founding == null && dissolution != null) {
						List<StatisticDB> placeStats = statisticsRepository.findByCountryAndDimensionAndParentPlaceIsNullAndDissolutionDateRange2(country, Dimension.NUTSLAU_DISSOLUTION.toString(), dissolution.getDateFrom(), dissolution.getDateTo(), Code.date1M);
						places = iterDissolution(placeStats, null, null, founding, null, language);
					} else {
						List<StatisticDB> placeStats = mapResults(statisticsService.statistics(cc, Dimension.NUTSLAU, null, null, true, null, founding, dissolution, false), Dimension.NUTSLAU);
						places = iter(placeStats, null, null, founding, dissolution, language);
					}
					
				}
				
				if (cfounding) {
					
					if (usePrecomputed && cc.getStatsDate(Dimension.FOUNDING) != null && dissolution == null) {
						Code xfounding = founding != null ? founding : defaultDate;

						List<StatisticDB> foundingStats = statisticsRepository.findByCountryAndDimensionAndFoundingDateRange(country, Dimension.FOUNDING.toString(), xfounding.getDateFrom(), xfounding.getDateTo(), Code.date1M, dateRangeName(xfounding.getDateInterval()));
						foundingDates = iterFounding(foundingStats, null, null, null, dissolution, language);
					} else {
						List<StatisticDB> foundingStats = mapResults(statisticsService.dateStatistics(cc, Dimension.FOUNDING, founding, null, true, null, null, dissolution, false), Dimension.FOUNDING);
						foundingDates = iterFounding(foundingStats, null, null, null, dissolution, language);
					}
					
	
				}
				
				if (cdissolution) {
					
					if (usePrecomputed && cc.getStatsDate(Dimension.DISSOLUTION) != null && founding == null) {
						Code xdissolution = dissolution != null ? dissolution : defaultDate;

						List<StatisticDB> dissolutionStats = statisticsRepository.findByCountryAndDimensionAndDissolutionDateRange(country, Dimension.DISSOLUTION.toString(), xdissolution.getDateFrom(), xdissolution.getDateTo(), Code.date1M, dateRangeName(xdissolution.getDateInterval()));
						dissolutionDates = iterDissolution(dissolutionStats, null, null, founding, null, language);
					} else {
						List<StatisticDB> dissolutionStats = mapResults(statisticsService.dateStatistics(cc, Dimension.DISSOLUTION, dissolution, null, true, null, founding, null, false), Dimension.DISSOLUTION);
						dissolutionDates = iterDissolution(dissolutionStats, null, null, founding, null, language);
					}
	
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
	
	private static String finerDateRangeName(String range) {
		if (range.equals(Code.date1M)) {
			return "month";
		} else if (range.equals(Code.date3M)) {
			return "month";
		} else if (range.equals(Code.date1Y)) {
			return "quarter";
		} else if (range.equals(Code.date10Y)) {
			return "year";
		}
		
		return "year";
	}
	
	private List<GenericResponse> iter(List<StatisticDB> stats, List<PlaceDB> placedb, List<ActivityDB> activitydb, Code founding, Code dissolution, String language) {
		if (stats == null) {
			return null;
		}
		
		List<GenericResponse> result = new ArrayList<>();
		for (StatisticDB st : stats) {
			result.add(GenericResponse.createFromStatistic(st, countryConfigurations.get(st.getCountry()), placedb, activitydb, founding, dissolution, language));
		}
		
		return result;
	}

	private List<GenericResponse> iterFounding(List<StatisticDB> stats, List<PlaceDB> placedb, List<ActivityDB> activitydb, Code founding, Code dissolution, String language) {
		if (stats == null) {
			return null;
		}
		List<GenericResponse> result = new ArrayList<>();
		for (StatisticDB st : stats) {
			result.add(GenericResponse.createFromFoundingStatistic(st, countryConfigurations.get(st.getCountry()), placedb, activitydb, founding, dissolution, language));
		}
		
		return result;
	}
	
	private List<GenericResponse> iterDissolution(List<StatisticDB> stats, List<PlaceDB> placedb, List<ActivityDB> activitydb, Code founding, Code dissolution, String language) {
		if (stats == null) {
			return null;
		}
		List<GenericResponse> result = new ArrayList<>();
		for (StatisticDB st : stats) {
			result.add(GenericResponse.createFromDissolutionStatistic(st, countryConfigurations.get(st.getCountry()), placedb, activitydb, founding, dissolution, language));
		}
		
		return result;
	}
	
	private List<StatisticDB> mapResults(StatisticResult sr) {
		if (sr != null) {
			StatisticDB gr = new StatisticDB();
			gr.setCountry(sr.getCountry());
			gr.setCount(sr.getCount());
			
			return Arrays.asList(gr);
		} else {
			return new ArrayList<>();
		}
	}
	
	private List<StatisticDB> mapResults(List<StatisticResult> list, Dimension dimension) {
//		if (list.size() == 0) {
//			return null;
//		}
		
		List<StatisticDB> res = new ArrayList<>();
		
		for (StatisticResult sr : list) {
			StatisticDB gr = new StatisticDB();
			if (dimension == Dimension.NUTSLAU) {
				PlaceDB placedb = placeRepository.findByCode(sr.getCode());
				gr.setPlace(placedb);
			} else if (dimension == Dimension.NACE) {
				ActivityDB activitydb = activityRepository.findByCode(sr.getCode());
				gr.setActivity(activitydb);
			} else if (dimension == Dimension.FOUNDING) {
				gr.setFoundingFromDate(sr.getCode().getDateFrom());
				gr.setFoundingToDate(sr.getCode().getDateTo());
			} else if (dimension == Dimension.DISSOLUTION) {
				gr.setDissolutionFromDate(sr.getCode().getDateFrom());
				gr.setDissolutionToDate(sr.getCode().getDateTo());
			}
			
			gr.setCountry(sr.getCountry());
			gr.setCount(sr.getCount());
			
			res.add(gr);
		}
		
		return res;
	}

//	@GetMapping("/compute")
//	public ResponseEntity<?> statistics(@RequestParam(required = true) String country, 
//	    		                            @RequestParam(required = true) Dimension dimension, 
//	    		                            @RequestParam(required = false) String top,
//	    		                            @RequestParam(defaultValue = "false") boolean allLevels,
//	    		                        @RequestParam(required = false) List<Code> place,
//	                                    @RequestParam(required = false) List<Code> activity,
//	                                    @RequestParam(required = false) Code founding,
//	                                    @RequestParam(required = false) Code dissolution) {
//		CountryDB cc = countryConfigurations.get(country);
//
//	    if (cc != null) {
//	    	List<GenericResponse> res = new ArrayList<>();
//	    		
//	    	if (dimension == Dimension.NUTS || dimension == Dimension.NACE) {
//	    		List<StatisticResult> list = statisticsService.statistics(cc, dimension, top != null ? new Code(top) : null, place, activity, founding, dissolution, allLevels);
//	    			
//	    		res = mapGenericResponseFromList(list, cc, dimension);
//	    			
//	    	} else if (dimension == Dimension.FOUNDING || dimension == Dimension.DISSOLUTION) {
//	    		try {
//			    	Date fromDate = null;
//			    	Date toDate = null;
//
//	    			if (top != null) {
//	    		    	Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})?--(\\d{4}-\\d{2}-\\d{2})?");
//	    			    	
//	    		    	Matcher m = p.matcher(top);
//	    		    	if (m.find()) {
//	    		    		fromDate = Date.valueOf(m.group(1));
//	    		    		toDate = Date.valueOf(m.group(2));
//	    		    	}
//	    			}
//	    				
////	    				statisticsService.dateStatistics(cc, dimension, fromDate, toDate, null, place.orElse(null), activity.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
////			    		if (allLevels) {
////			    			for (int i = 0; i < res.size(); i++) {
////			    				statisticsService.dateStatistics(cc, dimension, res.get(i).getFromDate(), res.get(i).getToDate(), res.get(i).getInterval(), place.orElse(null), activity.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
////			    			}
////			    		}
//	    				
//	    			List<StatisticResult> list = statisticsService.dateStatistics(cc, dimension, Code.createDateCode(fromDate, toDate), place, activity, founding, dissolution, allLevels);
//	    				
//	    			res = mapGenericResponseFromList(list, cc, dimension);
//			    		
//	    		} catch (Exception e) {
//	    			e.printStackTrace();
//	    			return ResponseEntity.badRequest().build();
//	    		}
//	    	}
//	    		
//
//	    	return ResponseEntity.ok(res);
//	    } else {
//	    	return ResponseEntity.notFound().build();
//	    }
//	}


	
//	private List<GenericResponse> mapGenericResponseFromList(List<StatisticResult> list, CountryDB cc, Dimension dimension) {
//		List<GenericResponse> res = new ArrayList<>();
//		   
//   		for (StatisticResult sr : list) {
//			GenericResponse gr;
//			if (dimension == Dimension.NUTS) {
//				PlaceDB placedb = placeRepository.findByCode(sr.getCode());
//				gr = GenericResponse.createFromPlace(placedb, null);
//			} else {
//				ActivityDB activitydb = activityRepository.findByCode(sr.getCode());
//				gr = GenericResponse.createFromActivity(activitydb, null);
//			}
//			if (sr.getCountry() != null) {
//				gr.setCountry(new CodeLabel(sr.getCountry(), cc.getLabel()));
//			}
//			gr.setCount(sr.getCount());
//			res.add(gr);
//			
//		}
//
//   		return res;
//	}
	   

	
}
