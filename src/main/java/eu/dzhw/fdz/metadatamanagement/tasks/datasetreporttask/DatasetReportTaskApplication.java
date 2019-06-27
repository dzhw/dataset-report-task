package eu.dzhw.fdz.metadatamanagement.tasks.datasetreporttask;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.loader.tools.RunProcess;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

import eu.dzhw.fdz.metadatamanagement.tasks.datasetreporttask.config.MdmProperties;
import eu.dzhw.fdz.metadatamanagement.tasks.datasetreporttask.config.TaskProperties;
import eu.dzhw.fdz.metadatamanagement.tasks.datasetreporttask.mdm.MdmRestClient;
import lombok.extern.slf4j.Slf4j;

/**
 * This Spring Boot Task generates one Dataset Report and uploads it to the MDM.
 *
 * @author René Reitmann
 */
@SpringBootApplication
@EnableConfigurationProperties({TaskProperties.class, MdmProperties.class})
@Slf4j
@EnableTask
public class DatasetReportTaskApplication {

  public static void main(String[] args) {
    SpringApplication.run(DatasetReportTaskApplication.class, args);
  }

  /**
   * Run the task for instance with 'java --jar dataset-report-task.jar
   * --task.data-set-id=dat-cmp2014-ds1$ --task.on-behalf-of=rreitmann --task.version=1.0.0'.
   * 
   * @param mdmClient The fully configured {@link MdmRestClient}
   * @param taskProperties All properties for this task.
   * @param zippedTemplate The zipped template folder.
   * @return springs {@link CommandLineRunner}
   * @throws Exception if anything goes wrong.
   */
  @Bean
  public CommandLineRunner run(MdmRestClient mdmClient, TaskProperties taskProperties,
      FileSystemResource zippedTemplate) throws Exception {
    log.info("Start report generation for dataset '{}' on behalf of '{}'.",
        taskProperties.getDataSetId(), taskProperties.getOnBehalfOf());
    return args -> {
      byte[] filledTemplate = mdmClient.fillTemplate(zippedTemplate, taskProperties.getDataSetId(),
          taskProperties.getVersion());
      FileUtils.cleanDirectory(new File(taskProperties.getLatexInputDir().getFile(), "variables"));
      ZipUtils.unzip(filledTemplate, taskProperties.getLatexInputDir().getFile());
      log.info("Successfully unzipped template to folder: "
          + taskProperties.getLatexInputDir().getFile().getAbsolutePath());
      RunProcess latexCompileProcess = new RunProcess(
          taskProperties.getLatexProcessWorkingDir().getFile(), "./bin/compile-report.sh",
          taskProperties.getLatexInputDir().getPath(), taskProperties.getPdfReport().getPath());
      latexCompileProcess.run(true);
      log.info("Successfully created report: " + taskProperties.getPdfReport().getPath());
      mdmClient.uploadReport(taskProperties.getPdfReport(), taskProperties.getDataSetId(),
          taskProperties.getOnBehalfOf());
      log.info("Successfully uploaded report to the MDM.");
    };
  }
}
