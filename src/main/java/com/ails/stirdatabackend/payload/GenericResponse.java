package com.ails.stirdatabackend.payload;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
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

	private CodeLabel place;

//	private String placeLabel;

//	private String placeGeometry;

//   private String placeLatinName;

	private CodeLabel activity;

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

	public void setPlace(String code, String label) {
		place = new CodeLabel(code, label);
	}
	
	public void setPlace(String code, String label, String geometry) {
		place = new CodeLabel(code, label, geometry);
	}

	public void setActivity(String code, String label) {
		activity = new CodeLabel(code, label);
	}

	public static GenericResponse createFromStatistic(StatisticDB st, CountryConfiguration cc, PlaceDB placedb, ActivityDB activitydb, Code founding, Code dissolution, String lang) {
		GenericResponse sr = new GenericResponse();
//		sr.setCountryCode(st.getCountry());
		if (cc != null) {
			sr.setCountry(new CodeLabel(st.getCountry(), cc.getCountryLabel()));
		}
		
		if (placedb == null) {
			placedb = st.getPlace();
		}
		
		if (placedb != null) {
			sr.setPlace(placedb.getCode().toString(), placedb.getLatinName() != null ? placedb.getLatinName() : placedb.getNationalName());
		}
		
		if (activitydb == null) {
			activitydb = st.getActivity();
		}

		if (activitydb != null) {
			sr.setActivity(activitydb.getCode().toString(), activitydb.getLabel(lang));
		}
		
		if (founding != null) {
			sr.setFoundingDate(new Interval(founding.getFromDate(), founding.getToDate()));
		} 
		
		if (dissolution != null) {
			sr.setDissolutionDate(new Interval(dissolution.getFromDate(), dissolution.getToDate()));
		}
		
//		sr.setDateInterval(st.getDateInterval());
		sr.setCount(st.getCount());

		return sr;

	}
	
	public static GenericResponse createFromFoundingStatistic(StatisticDB st, CountryConfiguration cc, PlaceDB placedb, ActivityDB activitydb, Code founding, Code dissolution, String lang) {
		GenericResponse sr = new GenericResponse();
//		sr.setCountryCode(st.getCountry());
		if (cc != null) {
			sr.setCountry(new CodeLabel(st.getCountry(), cc.getCountryLabel()));
		}
		
		if (placedb == null) {
			placedb = st.getPlace();
		}
		
		if (placedb != null) {
			sr.setPlace(placedb.getCode().toString(), placedb.getLatinName() != null ? placedb.getLatinName() : placedb.getNationalName());
		}
		
		if (activitydb == null) {
			activitydb = st.getActivity();
		}

		if (activitydb != null) {
			sr.setActivity(activitydb.getCode().toString(), activitydb.getLabel(lang));
		}
		
		if (founding == null) {
//			if (st.getFromDate() != null) {
//				sr.setFoundingFromDate(st.getFromDate().toString());
//			}
//			if (st.getToDate() != null) {
//				sr.setFoundingToDate(st.getToDate().toString());
//			}
			sr.setFoundingDate(new Interval(st.getFromDate(), st.getToDate()));
		} else {
//			if (founding.getFromDate() != null) {
//				sr.setFoundingFromDate(founding.getFromDate().toString());
//			}
//			if (founding.getToDate() != null) {
//				sr.setFoundingToDate(founding.getToDate().toString());
//			}
			sr.setFoundingDate(new Interval(founding.getFromDate(), founding.getToDate()));
		} 
		
		if (dissolution != null) {
			sr.setDissolutionDate(new Interval(dissolution.getFromDate(), dissolution.getToDate()));
		}
		
		
//		sr.setDateInterval(st.getDateInterval());
		sr.setCount(st.getCount());

		return sr;

	}

	public static GenericResponse createFromDissolutionStatistic(StatisticDB st, CountryConfiguration cc, PlaceDB placedb, ActivityDB activitydb, Code founding, Code dissolution, String lang) {
		GenericResponse sr = new GenericResponse();
//		sr.setCountryCode(st.getCountry());
		if (cc != null) {
			sr.setCountry(new CodeLabel(st.getCountry(), cc.getCountryLabel()));
		}
		
		if (placedb == null) {
			placedb = st.getPlace();
		}
		
		if (placedb != null) {
			sr.setPlace(placedb.getCode().toString(), placedb.getLatinName() != null ? placedb.getLatinName() : placedb.getNationalName());
		}
		
		if (activitydb == null) {
			activitydb = st.getActivity();
		}

		if (activitydb != null) {
			sr.setActivity(activitydb.getCode().toString(), activitydb.getLabel(lang));
		}
		
		if (founding != null) {
			sr.setFoundingDate(new Interval(founding.getFromDate(), founding.getToDate()));
		}
		
		if (dissolution == null) {
//			if (st.getFromDate() != null) {
//				sr.setDissolutionFromDate(st.getFromDate().toString());
//			}
//			if (st.getToDate() != null) {
//				sr.setDissolutionToDate(st.getToDate().toString());
//			}
			sr.setDissolutionDate(new Interval(st.getFromDate(), st.getToDate()));
		} else {
//			if (dissolution.getFromDate() != null) {
//				sr.setDissolutionFromDate(dissolution.getFromDate().toString());
//			}
//			if (dissolution.getToDate() != null) {
//				sr.setDissolutionToDate(dissolution.getToDate().toString());
//			}
			sr.setDissolutionDate(new Interval(dissolution.getFromDate(), dissolution.getToDate()));
		} 
		
		
//		sr.setDateInterval(st.getDateInterval());
		sr.setCount(st.getCount());

		return sr;

	}
	
	public static GenericResponse createFromActivity(ActivityDB activity, String lang) {
		GenericResponse sr = new GenericResponse();
		sr.setActivity(activity.getCode().toString(), activity.getLabel(lang));

		return sr;

	}

	public static GenericResponse createFromPlace(PlaceDB place, String resolution) {
		GenericResponse sr = new GenericResponse();
		sr.setPlace(place.getCode().toString(), place.getNationalName(), place.getGeometry(resolution));

		return sr;
	}
}
