package com.ails.stirdatabackend.payload;

import java.util.ArrayList;
import java.util.List;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.StatisticDB;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse {

	private CodeLabel country;

	private List<CodeLabel> place;

//	private String placeLabel;

//	private String placeGeometry;

//   private String placeLatinName;

	private List<CodeLabel> activity;

//	private String activityLabel;

	private Interval foundingDate;

	private Interval dissolutionDate;

//	@JsonIgnore
//	private String dateInterval;

	private Integer count;

//	public static GenericResponse createFromStatistic(Statistic st) {
//		GenericResponse sr = new GenericResponse();
//		sr.setCountry(st.getCountry());
//		sr.setPlace(st.getPlace());
//		sr.setActivity(st.getActivity());
//		sr.setFromDate(st.getFromDate());
//		sr.setToDate(st.getToDate());
//		sr.setDateInterval(st.getDateInterval());
//		sr.setCount(st.getCount());
//
//		return sr;
//
//	}

	public void addPlace(String code, String label) {
		if (place == null) {
			place = new ArrayList<>();
		}
		place.add(new CodeLabel(code, label));
	}
	
	public void addPlace(String code, String label, String geometry) {
		if (place == null) {
			place = new ArrayList<>();
		}
		place.add(new CodeLabel(code, label, geometry));
	}

	public void addActivity(String code, String label) {
		if (activity == null) {
			activity = new ArrayList<>();
		}
		activity.add(new CodeLabel(code, label));
	}

	public static GenericResponse createFromStatistic(StatisticDB st, CountryDB cc, List<PlaceDB> placedb, List<ActivityDB> activitydb, Code founding, Code dissolution, String lang) {
		GenericResponse sr = new GenericResponse();
//		sr.setCountryCode(st.getCountry());
		if (cc != null) {
			sr.setCountry(new CodeLabel(st.getCountry(), cc.getLabel()));
		}
		
		if (placedb == null && st.getPlace() != null) {
			placedb = new ArrayList<>();
			placedb.add(st.getPlace());
		}
		
		if (placedb != null) {
			for (PlaceDB pl : placedb) {
				sr.addPlace(pl.getCode().toString(), pl.getLatinName() != null ? pl.getLatinName() : pl.getNationalName());
			}
		}
		
		if (activitydb == null && st.getActivity() != null) {
			activitydb = new ArrayList<>();
			activitydb.add(st.getActivity());
		}

		if (activitydb != null) {
			for (ActivityDB ac : activitydb) {
				sr.addActivity(ac.getCode().toString(), ac.getLabel(lang));
			}
		}
		
		if (founding != null) {
			sr.setFoundingDate(new Interval(founding.getDateFrom(), founding.getDateTo()));
		} 
		
		if (dissolution != null) {
			sr.setDissolutionDate(new Interval(dissolution.getDateFrom(), dissolution.getDateTo()));
		}
		
//		sr.setDateInterval(st.getDateInterval());
		sr.setCount(st.getCount());

		return sr;

	}
	
	public static GenericResponse createFromFoundingStatistic(StatisticDB st, CountryDB cc, List<PlaceDB> placedb, List<ActivityDB> activitydb, Code founding, Code dissolution, String lang) {
		GenericResponse sr = new GenericResponse();
//		sr.setCountryCode(st.getCountry());
		if (cc != null) {
			sr.setCountry(new CodeLabel(st.getCountry(), cc.getLabel()));
		}
		
		if (placedb == null && st.getPlace() != null) {
			placedb = new ArrayList<>();
			placedb.add(st.getPlace());
		}
		
		if (placedb != null) {
			for (PlaceDB pl : placedb) {
				sr.addPlace(pl.getCode().toString(), pl.getLatinName() != null ? pl.getLatinName() : pl.getNationalName());
			}
		}
		
		if (activitydb == null && st.getActivity() != null) {
			activitydb = new ArrayList<>();
			activitydb.add(st.getActivity());
		}

		if (activitydb != null) {
			for (ActivityDB ac : activitydb) {
				sr.addActivity(ac.getCode().toString(), ac.getLabel(lang));
			}
		}
		
		if (founding == null) {
//			if (st.getFromDate() != null) {
//				sr.setFoundingFromDate(st.getFromDate().toString());
//			}
//			if (st.getToDate() != null) {
//				sr.setFoundingToDate(st.getToDate().toString());
//			}
			
			sr.setFoundingDate(new Interval(st.getFoundingFromDate(), st.getFoundingToDate()));
		} else {
//			if (founding.getFromDate() != null) {
//				sr.setFoundingFromDate(founding.getFromDate().toString());
//			}
//			if (founding.getToDate() != null) {
//				sr.setFoundingToDate(founding.getToDate().toString());
//			}
			sr.setFoundingDate(new Interval(founding.getDateFrom(), founding.getDateTo()));
		} 
		
		if (dissolution != null) {
			sr.setDissolutionDate(new Interval(dissolution.getDateFrom(), dissolution.getDateTo()));
		}
		
		
//		sr.setDateInterval(st.getDateInterval());
		sr.setCount(st.getCount());

		return sr;

	}

	public static GenericResponse createFromDissolutionStatistic(StatisticDB st, CountryDB cc, List<PlaceDB> placedb, List<ActivityDB> activitydb, Code founding, Code dissolution, String lang) {
		GenericResponse sr = new GenericResponse();
//		sr.setCountryCode(st.getCountry());
		if (cc != null) {
			sr.setCountry(new CodeLabel(st.getCountry(), cc.getLabel()));
		}
		
		if (placedb == null && st.getPlace() != null) {
			placedb = new ArrayList<>();
			placedb.add(st.getPlace());
		}
		
		if (placedb != null) {
			for (PlaceDB pl : placedb) {
				sr.addPlace(pl.getCode().toString(), pl.getLatinName() != null ? pl.getLatinName() : pl.getNationalName());
			}
		}
		
		if (activitydb == null && st.getActivity() != null) {
			activitydb = new ArrayList<>();
			activitydb.add(st.getActivity());
		}

		if (activitydb != null) {
			for (ActivityDB ac : activitydb) {
				sr.addActivity(ac.getCode().toString(), ac.getLabel(lang));
			}
		}
		
		if (founding != null) {
			sr.setFoundingDate(new Interval(founding.getDateFrom(), founding.getDateTo()));
		}
		
		if (dissolution == null) {
//			if (st.getFromDate() != null) {
//				sr.setDissolutionFromDate(st.getFromDate().toString());
//			}
//			if (st.getToDate() != null) {
//				sr.setDissolutionToDate(st.getToDate().toString());
//			}
			sr.setDissolutionDate(new Interval(st.getDissolutionFromDate(), st.getDissolutionToDate()));
		} else {
//			if (dissolution.getFromDate() != null) {
//				sr.setDissolutionFromDate(dissolution.getFromDate().toString());
//			}
//			if (dissolution.getToDate() != null) {
//				sr.setDissolutionToDate(dissolution.getToDate().toString());
//			}
			sr.setDissolutionDate(new Interval(dissolution.getDateFrom(), dissolution.getDateTo()));
		} 
		
		
//		sr.setDateInterval(st.getDateInterval());
		sr.setCount(st.getCount());

		return sr;

	}
	
	public static GenericResponse createFromActivity(ActivityDB activity, String lang) {
		GenericResponse sr = new GenericResponse();
		sr.addActivity(activity.getCode().toString(), activity.getLabel(lang));

		return sr;

	}

	public static GenericResponse createFromPlace(PlaceDB place, List<String> resolution) {
		GenericResponse sr = new GenericResponse();

		if (resolution != null) { 
			for (int i = 0; i < resolution.size(); i++) {
				String geometry = place.getGeometry(resolution.get(i));
				if (geometry != null) {
					sr.addPlace(place.getCode().toString(), place.getNationalName(), geometry);
					break;
				}
			}
		}
		
		if (sr.getPlace() == null) {
			sr.addPlace(place.getCode().toString(), place.getNationalName());
		}
		
		return sr;
	}
}
