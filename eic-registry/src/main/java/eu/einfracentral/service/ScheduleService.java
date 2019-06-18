package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Measurement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;


@Component
public class ScheduleService {

    private static final Logger logger = LogManager.getLogger(ScheduleService.class);
    private SynchronizerService synchronizerService;

    @Autowired
    public ScheduleService(SynchronizerService synchronizerService){
        this.synchronizerService = synchronizerService;
    }


    @Scheduled(initialDelay = 0, fixedRate = 600000) //run every 10 min
    public void retrySync() {

        BlockingQueue<InfraService> serviceQueue = synchronizerService.getServiceQueue();
        BlockingQueue<Measurement> measurementQueue = synchronizerService.getMeasurementQueue();
        BlockingQueue<String> serviceActionQueue = synchronizerService.getServiceAction();
        BlockingQueue<String> measurementActionQueue = synchronizerService.getMeasurementAction();
        int syncTries = 0;

        logger.info("retrySync started");
        if(serviceQueue.isEmpty() && measurementQueue.isEmpty()){
            logger.info("There are no Services or Measurements waiting to be Synchronized in the Queue!");
        }

        try {
            while ((!serviceQueue.isEmpty() || !measurementQueue.isEmpty()) && syncTries<1) {
                if (!serviceQueue.isEmpty()){
                    InfraService infraService = serviceQueue.take();
                    String serviceAction = serviceActionQueue.take();
                    logger.info("Head Service on serviceQueue: " +infraService.getId());
                    logger.info("Head serviceAction on serviceActionQueue: " +serviceAction);
                    switch (serviceAction){
                        case "add":
                            synchronizerService.syncAdd(infraService);
                            break;
                        case "update":
                            synchronizerService.syncUpdate(infraService);
                            break;
                        case "delete":
                            synchronizerService.syncDelete(infraService);
                            break;
                    }
                }
                if (!measurementQueue.isEmpty()){
                    Measurement measurement = measurementQueue.take();
                    String measurementAction = measurementActionQueue.take();
                    logger.info("Head Measurement on measurementQueue " +measurement);
                    logger.info("Head measurementAction on measurementActionQueue " +measurementAction);
                    switch (measurementAction){
                        case "add":
                            synchronizerService.syncAdd(measurement);
                            break;
                        case "update":
                            synchronizerService.syncUpdate(measurement);
                            break;
                        case "delete":
                            synchronizerService.syncDelete(measurement);
                            break;
                    }
                }
                syncTries++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
