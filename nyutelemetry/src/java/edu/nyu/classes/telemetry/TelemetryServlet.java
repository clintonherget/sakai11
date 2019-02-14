package edu.nyu.classes.telemetry;

import java.util.*;
import java.util.stream.*;
import java.text.SimpleDateFormat;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.net.MalformedURLException;
import java.net.URL;

import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.component.cover.ServerConfigurationService;

import org.sakaiproject.telemetry.cover.Telemetry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TelemetryServlet extends HttpServlet {

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        checkAccessControl();

        response.setHeader("Content-Type", "text/html");

        URL toolBaseURL = determineBaseURL();
        Handlebars handlebars = loadHandlebars(toolBaseURL);

        try {
            Template template = handlebars.compile("edu/nyu/classes/telemetry/views/layout");
            Map<String, Object> context = new HashMap<String, Object>();

            context.put("baseURL", toolBaseURL);
            context.put("layout", true);
            context.put("skinRepo", ServerConfigurationService.getString("skin.repo", ""));
            context.put("randomSakaiHeadStuff", request.getAttribute("sakai.html.head"));

            Collection<Telemetry.TelemetryReading> allReadings = Telemetry.fetchReadings(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000));

            Map<String, List<Telemetry.TelemetryReading>> groupedReadings = allReadings.stream().collect(Collectors.groupingBy(Telemetry.TelemetryReading::getKey));

            List<LineChart> lineCharts = new ArrayList<>();
            List<HistogramChart> histogramCharts = new ArrayList<>();

            List<String> excluded = Arrays.asList(ServerConfigurationService.getString("telemetry.metrics.excluded_from_report", "help_views").split(", *"));

            for (String metricName : groupedReadings.keySet()) {
                if (excluded.indexOf(metricName) >= 0) {
                    // Don't show this one.
                    continue;
                }

                List<Telemetry.TelemetryReading> readings = groupedReadings.get(metricName);
                Collections.sort(readings, (Telemetry.TelemetryReading a, Telemetry.TelemetryReading b) -> { return Long.compare(a.getTime(), b.getTime()); } );

                if (readings.get(0).getMetricType().equals(Telemetry.MetricType.TIMER) ||
                    readings.get(0).getMetricType().equals(Telemetry.MetricType.COUNTER)) {
                    lineCharts.add(new LineChart(metricName,
                                                 readings.stream().map(Telemetry.TelemetryReading::getTime).collect(Collectors.toList()),
                                                 readings.stream().map(Telemetry.TelemetryReading::getValue).collect(Collectors.toList())));
                } else {
                    // Histogram
                    Map<Long, List<Telemetry.TelemetryReading>> histogramsByTime = readings.stream().collect(Collectors.groupingBy(Telemetry.TelemetryReading::getTime));

                    if (histogramsByTime.size() > 1) {
                        histogramCharts.add(new HistogramChart(metricName, histogramsByTime));
                    }
                }
            }

            context.put("lineCharts", lineCharts);
            context.put("histogramCharts", histogramCharts);

            response.getWriter().write(template.apply(context));
        } catch (IOException e) {
            log.warn("Write failed", e);
        }
    }

    private void checkAccessControl() {
        // if (!SecurityService.unlock("FIXME", "/site/FIXME")) {
        //     log.error("Access denied to Telemetry tool for user " + SessionManager.getCurrentSessionUserId());
        //     throw new RuntimeException("Access denied");
        // }
    }

    private URL determineBaseURL() {
        String siteId = ToolManager.getCurrentPlacement().getContext();
        String toolId = ToolManager.getCurrentPlacement().getId();

        try {
            return new URL(ServerConfigurationService.getPortalUrl() + "/site/" + siteId + "/tool/" + toolId + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Couldn't determine tool URL", e);
        }
    }

    private Handlebars loadHandlebars(final URL baseURL) {
        return new Handlebars();
    }

    private class LineChart {
        private String name;
        private List<Reading> readings;

        public String getName() { return name; }
        public List<Reading> getReadings() { return readings; }

        public TreeMap<String, Long> getCountsByDay() {
            TreeMap<String, Long> countsByDay = new TreeMap<>();

            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
            sdf.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("America/New_York")));

            for (Reading reading : readings) {
                String label = sdf.format(reading.getX());

                if (!countsByDay.containsKey(label)) {
                    countsByDay.put(label, 0L);
                }

                countsByDay.put(label, (countsByDay.get(label) + 1));
            }

            return countsByDay;
        }



        private class Reading {
            private long x;
            private long y;

            public long getX() { return x; }
            public long getY() { return y; }

            public Reading(long x, long y) {
                this.x = x;
                this.y = y;
            }
        }

        private List<Reading> buildReadings(List<Long> xvals, List<Long> yvals) {
            List<Reading> result = new ArrayList<>();

            for (int i = 0; i < xvals.size(); i++) {
                result.add(new Reading(xvals.get(i), yvals.get(i)));
            }

            return result;
        }

        public LineChart(String name, List<Long> xvals, List<Long> yvals) {
            this.name = name;
            this.readings = buildReadings(xvals, yvals);
        }
    }

    private class HistogramChart {
        private String name;
        private long timeStep;
        private TreeMap<String, List<Reading>> bucketsMap = new TreeMap<>();

        public String getName() { return name; }
        public TreeMap<String, List<Reading>> getBucketsMap() { return bucketsMap; }

        public TreeMap<String, Long> getCountsByDay() {
            TreeMap<String, Long> countsByDay = new TreeMap<>();

            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
            sdf.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("America/New_York")));

            for (List<Reading> readings : bucketsMap.values()) {
                for (Reading reading : readings) {
                    String label = sdf.format(reading.getTime());

                    if (!countsByDay.containsKey(label)) {
                        countsByDay.put(label, 0L);
                    }

                    countsByDay.put(label, (countsByDay.get(label) + reading.getCount()));
                }
            }

            return countsByDay;
        }

        private class Reading {
            private long time;
            private long count;

            public long getTime() { return time; }
            public long getTimeStep() { return timeStep; }
            public long getCount() { return count; }

            public Reading(long time, long count) {
                this.time = time;
                this.count = count;
            }
        }

        private String reformatBucket(String bucket) {
            if (bucket.indexOf(" - ") < 0) {
                return bucket;
            }

            String[] bits = bucket.split(" - ");

            if (bits.length != 2) {
                return bucket;
            }

            // Leading zeroes to spaces.  Add a unit.
            return String.format("%5s ms - %5s ms",
                                 bits[0].replaceAll("^0*([0-9])", "$1"),
                                 bits[1].replaceAll("^0*([0-9])", "$1"));
        }


        public HistogramChart(String name, Map<Long, List<Telemetry.TelemetryReading>> rawReadings) {
            this.name = name;

            // We're passed in something like:
            //
            //  <timestamp> => [(bucket1 reading), (bucket2 reading), ...]
            //

            // Want something like {bucket => [[time1, count1], [time2, count2], ...]}

            List<String> buckets = new ArrayList<>();
            Map<String, Long> frequencies = new HashMap<>();

            // Extract all known times and buckets
            for (long time : rawReadings.keySet()) {
                List<Telemetry.TelemetryReading> telemetryReadings = rawReadings.get(time);

                for (Telemetry.TelemetryReading rawReading : telemetryReadings) {
                    buckets.add(rawReading.getSubKey());

                    String key = time + "__" + rawReading.getSubKey();
                    frequencies.put(key, rawReading.getValue());
                }
            }

            Collections.sort(buckets);

            // Determine our list of timestamps.  We'll take the min & max
            // timestamps from our incoming data and fill out the range
            // ourselves to ensure there isn't any gaps.
            long firstTime = rawReadings.keySet().stream().min(Long::compare).get();
            long lastTime = rawReadings.keySet().stream().max(Long::compare).get();

            long step = Long.MAX_VALUE;
            List<Long> readingTimes = new ArrayList(rawReadings.keySet());
            Collections.sort(readingTimes);

            for (int i = 1; i < readingTimes.size(); i++) {
                long diff = readingTimes.get(i) - readingTimes.get(i - 1);

                if (diff < step) {
                    step = diff;
                }
            }

            List<Long> times = new ArrayList<>();
            for (long time = firstTime; time <= lastTime; time += step) {
                times.add(time);
            }

            this.timeStep = step;

            // Build up our final structure
            for (String bucket : buckets) {
                String prettyBucket = reformatBucket(bucket);

                bucketsMap.put(prettyBucket, new ArrayList<>());

                for (long time : times) {
                    String key = time + "__" + bucket;

                    if (frequencies.containsKey(key)) {
                        bucketsMap.get(prettyBucket).add(new Reading(time, frequencies.get(key)));
                    } else {
                        bucketsMap.get(prettyBucket).add(new Reading(time, 0));
                    }
                }
            }
        }
    }

}
