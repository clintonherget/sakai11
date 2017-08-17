package org.sakaiproject.site.tool;

import java.util.List;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.util.ResourceLoader;

import lombok.Data;

/**
 * Stores some extracted data about a site, used by the membership tool
 * 
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
@Data
public class MembershipSiteMetadata {

	private String siteId;
	private String term;
	private String siteStatus;
	private Date createdDate;

	private String instructorEids;
	private List<String> groupSectionEids; //needs to be processed in the vm

	public String getCreatedDate() {
		ResourceLoader rb = new ResourceLoader("membership");

		final DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, rb.getLocale());
		final TimeZone tz = TimeService.getLocalTimeZone();
		df.setTimeZone(tz);

		return df.format(createdDate);
	}
}
