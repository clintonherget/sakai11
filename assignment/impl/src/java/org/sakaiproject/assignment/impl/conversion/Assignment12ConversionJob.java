package org.sakaiproject.assignment.impl.conversion;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sakaiproject.assignment.api.conversion.AssignmentConversionService;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.component.cover.ComponentManager;

import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.assignment.api.conversion.AssignmentDataProvider;
import org.sakaiproject.assignment.api.persistence.AssignmentRepository;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@DisallowConcurrentExecution
public class Assignment12ConversionJob implements Job {

    public static final String SIZE_PROPERTY = "length.attribute.property";
    public static final String NUMBER_PROPERTY = "number.attributes.property";

    @Setter
    private AssignmentConversionService assignmentConversionService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("<===== Assignment Conversion Job start =====>");

        // never run as a recovery
        if (context.isRecovering()) {
            log.warn("<===== Assignment Conversion Job doesn't support recovery, job will terminate... =====>");
        } else {
            JobDataMap map = context.getMergedJobDataMap();
            // Integer size = Integer.parseInt((String) map.get(SIZE_PROPERTY));
            // Integer number = Integer.parseInt((String) map.get(NUMBER_PROPERTY));

            Integer size = 10240000;
            Integer number = 5000;

            AssignmentDataProvider dataProvider = (AssignmentDataProvider)ComponentManager.get("org.sakaiproject.assignment.api.conversion.AssignmentDataProvider");
            AssignmentRepository assignmentRepository = (AssignmentRepository)ComponentManager.get("org.sakaiproject.assignment.api.persistence.AssignmentRepository");
            ServerConfigurationService serverConfigurationService = (ServerConfigurationService)ComponentManager.get("org.sakaiproject.component.api.ServerConfigurationService");
            SiteService siteService = (SiteService)ComponentManager.get("org.sakaiproject.site.api.SiteService");

            Map<String, List<String>> preAssignments = dataProvider.fetchAssignmentsToConvertByTerm();
            List<String> alreadyConvertedAssignments = assignmentRepository.findAllAssignmentIds();

            ExecutorService threadPool = Executors.newFixedThreadPool(16);

            List<String> termsToProcess = new ArrayList<>();
            termsToProcess.addAll(Arrays.asList("Fall_2018", "Summer_2018", "Spring_2018", "January_2018",
                                                "Fall_2017", "Summer_2017", "Spring_2017", "January_2017",
                                                "Fall_2016", "Summer_2016", "Spring_2016", "January_2016"));

            termsToProcess.addAll(preAssignments.keySet());

            for (String termEid : termsToProcess) {
                List<String> assignmentIds = preAssignments.remove(termEid);

                if (assignmentIds == null) {
                    continue;
                }

                int start = 0;
                while (start < assignmentIds.size()) {
                    int end = Math.min(start + 250, assignmentIds.size());

                    List<String> sublist = assignmentIds.subList(start, end);
                    final int jobStart = start;
                    final int jobEnd = end;

                    threadPool.execute(() -> {
                            Thread.currentThread().setName("AssignmentConversion::" + termEid + "::" + jobStart);
                            log.info(String.format("Converting term %s range %d--%d: ", termEid, jobStart, jobEnd));

                            AssignmentConversionServiceImpl converter = new AssignmentConversionServiceImpl();

                            converter.setAssignmentRepository(assignmentRepository);
                            converter.setDataProvider(dataProvider);
                            converter.setServerConfigurationService(serverConfigurationService);
                            converter.setSiteService(siteService);

                            converter.init();
                            converter.runConversion(number, size, setSubtract(sublist, alreadyConvertedAssignments));
                        });

                    start = end;
                }
            }

            threadPool.shutdown();

            try {
                threadPool.awaitTermination(365, java.util.concurrent.TimeUnit.DAYS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        log.info("<===== Assignment Conversion Job end =====>");
    }

    // a - b
    private List<String> setSubtract(List<String> a, List<String> b) {
        List<String> result = new ArrayList<>();
        Set<String> setB = new HashSet<>(b);

        for (String s : a) {
            if (!setB.contains(s)) {
                result.add(s);
            }
        }

        return result;
    }


}
